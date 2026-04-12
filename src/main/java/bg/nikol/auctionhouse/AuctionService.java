package bg.nikol.auctionhouse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AuctionService {
    private final AuctionStorage auctionStorage;
    private final ExpiredStorage expiredStorage;

    private final Map<UUID, AuctionEntry> listings = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> expiredClaims = new HashMap<>();

    public AuctionService(AuctionStorage auctionStorage, ExpiredStorage expiredStorage) {
        this.auctionStorage = auctionStorage;
        this.expiredStorage = expiredStorage;
    }

    public void load() {
        listings.clear();
        for (AuctionEntry entry : auctionStorage.loadEntries()) {
            listings.put(entry.getId(), entry);
        }

        expiredClaims.clear();
        expiredClaims.putAll(expiredStorage.loadClaims());
    }

    public void saveAll() {
        auctionStorage.saveEntries(listings.values());
        expiredStorage.saveClaims(expiredClaims);
    }

    public void addListing(AuctionEntry entry) {
        listings.put(entry.getId(), entry);
    }

    public AuctionEntry getListing(UUID id) {
        return listings.get(id);
    }

    public AuctionEntry removeListing(UUID id) {
        return listings.remove(id);
    }

    public List<AuctionEntry> getAllListings() {
        List<AuctionEntry> all = new ArrayList<>(listings.values());
        all.sort(Comparator.comparingLong(AuctionEntry::getCreatedAt).reversed());
        return all;
    }

    public List<AuctionEntry> searchListings(String rawQuery) {
        String query = rawQuery.toLowerCase(Locale.ROOT);
        List<AuctionEntry> filtered = new ArrayList<>();

        for (AuctionEntry entry : listings.values()) {
            ItemStack item = entry.getItemStack();
            String material = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            if (material.contains(query)) {
                filtered.add(entry);
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String strippedName = ChatColor.stripColor(meta.getDisplayName());
                if (strippedName != null && strippedName.toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(entry);
                }
            }
        }

        filtered.sort(Comparator.comparingLong(AuctionEntry::getCreatedAt).reversed());
        return filtered;
    }

    public int countBySeller(UUID sellerId) {
        int count = 0;
        for (AuctionEntry entry : listings.values()) {
            if (entry.getSellerId().equals(sellerId)) {
                count++;
            }
        }
        return count;
    }

    public int countAll() {
        return listings.size();
    }

    public int expireListings(long now) {
        List<AuctionEntry> expired = new ArrayList<>();

        for (AuctionEntry entry : listings.values()) {
            if (entry.isExpired(now)) {
                expired.add(entry);
            }
        }

        if (expired.isEmpty()) {
            return 0;
        }

        for (AuctionEntry entry : expired) {
            listings.remove(entry.getId());
            addExpiredClaim(entry.getSellerId(), entry.getItemStack());
        }

        return expired.size();
    }

    public List<ItemStack> claimExpired(UUID playerId) {
        List<ItemStack> items = expiredClaims.remove(playerId);
        if (items == null) {
            return List.of();
        }

        return new ArrayList<>(items);
    }

    public int countExpired(UUID playerId) {
        List<ItemStack> items = expiredClaims.get(playerId);
        return items == null ? 0 : items.size();
    }

    private void addExpiredClaim(UUID playerId, ItemStack itemStack) {
        expiredClaims.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(itemStack);
    }

    public Collection<AuctionEntry> values() {
        return listings.values();
    }
}
