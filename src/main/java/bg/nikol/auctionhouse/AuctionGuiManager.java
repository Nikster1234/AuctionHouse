package bg.nikol.auctionhouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AuctionGuiManager {
    public static final int MAIN_SIZE = 54;
    public static final int MAIN_LISTING_SLOTS = 45;
    public static final int CONFIRM_SIZE = 27;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_SELL_HELP = 46;
    public static final int SLOT_SEARCH_HELP = 47;
    public static final int SLOT_STATS = 48;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_CLAIM = 50;
    public static final int SLOT_FILTER = 51;
    public static final int SLOT_NEXT = 53;

    private final AuctionHousePlugin plugin;
    private final AuctionService auctionService;

    public AuctionGuiManager(AuctionHousePlugin plugin, AuctionService auctionService) {
        this.plugin = plugin;
        this.auctionService = auctionService;
    }

    public void openMain(Player player, int requestedPage, String query) {
        List<AuctionEntry> listings = (query == null || query.isBlank())
                ? auctionService.getAllListings()
                : auctionService.searchListings(query);

        int maxPage = Math.max(1, (int) Math.ceil(listings.size() / (double) MAIN_LISTING_SLOTS));
        int page = Math.max(1, Math.min(requestedPage, maxPage));

        Map<Integer, UUID> slotToListing = new HashMap<>();
        MainHolder holder = new MainHolder(page, maxPage, query, slotToListing);

        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "DonutSMP Auction House "
                + ChatColor.DARK_GRAY + "• "
                + ChatColor.GRAY
                + page
                + "/"
                + maxPage;
        if (query != null && !query.isBlank()) {
            title = ChatColor.GOLD + "" + ChatColor.BOLD + "DonutSMP Search "
                    + ChatColor.DARK_GRAY + "• "
                    + ChatColor.GRAY
                    + page
                    + "/"
                    + maxPage;
        }

        Inventory inventory = Bukkit.createInventory(holder, MAIN_SIZE, title);
        int start = (page - 1) * MAIN_LISTING_SLOTS;

        for (int slot = 0; slot < MAIN_LISTING_SLOTS; slot++) {
            int index = start + slot;
            if (index >= listings.size()) {
                break;
            }

            AuctionEntry entry = listings.get(index);
            inventory.setItem(slot, createListingItem(entry, player));
            slotToListing.put(slot, entry.getId());
        }

        setupBottomBar(inventory, holder, listings.size(), auctionService.countBySeller(player.getUniqueId()),
                auctionService.countExpired(player.getUniqueId()));

        player.openInventory(inventory);
    }

    public void openConfirm(Player player, AuctionEntry entry, int returnPage, String query) {
        ConfirmHolder holder = new ConfirmHolder(entry.getId(), returnPage, query);
        Inventory inventory = Bukkit.createInventory(
                holder,
                CONFIRM_SIZE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Confirm Purchase");

        ItemStack filler = createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " ");
        for (int slot = 0; slot < CONFIRM_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        ItemStack preview = entry.getItemStack().clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (!lore.isEmpty()) {
                lore.add(" ");
            }
            lore.add(ChatColor.DARK_GRAY + "----------------");
            lore.add(ChatColor.GRAY + "Buy Price: " + plugin.formatMoney(entry.getPrice()));
            lore.add(ChatColor.GRAY + "Seller: " + ChatColor.WHITE + readableSellerName(entry.getSellerId()));
            lore.add(ChatColor.YELLOW + "Click " + ChatColor.GREEN + "Confirm" + ChatColor.YELLOW + " to buy this item.");
            lore.add(ChatColor.DARK_GRAY + "----------------");
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }

        inventory.setItem(13, preview);
        inventory.setItem(
                11,
                createSimpleItem(
                        Material.RED_CONCRETE,
                        ChatColor.RED + "" + ChatColor.BOLD + "Cancel"));
        inventory.setItem(
                15,
                createSimpleItem(
                        Material.LIME_CONCRETE,
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Buy"));

        player.openInventory(inventory);
    }

    private ItemStack createListingItem(AuctionEntry entry, Player viewer) {
        ItemStack display = entry.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();

        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (!lore.isEmpty()) {
                lore.add(" ");
            }

            long remaining = Math.max(0L, entry.getExpiresAt() - System.currentTimeMillis());

            lore.add(ChatColor.DARK_GRAY + "----------------");
            lore.add(ChatColor.GRAY + "Seller: " + ChatColor.WHITE + readableSellerName(entry.getSellerId()));
            lore.add(ChatColor.GRAY + "Price: " + plugin.formatMoney(entry.getPrice()));
            lore.add(ChatColor.GRAY + "Expires In: " + ChatColor.WHITE + formatRemaining(remaining));
            lore.add(ChatColor.DARK_GRAY + "Listing ID: " + entry.getId().toString().substring(0, 8));

            if (viewer.getUniqueId().equals(entry.getSellerId())) {
                lore.add(ChatColor.YELLOW + "Right Click: " + ChatColor.WHITE + "Remove Your Listing");
            } else {
                lore.add(ChatColor.GREEN + "Left Click: " + ChatColor.WHITE + "Buy Item");
            }
            lore.add(ChatColor.DARK_GRAY + "----------------");

            meta.setLore(lore);
            display.setItemMeta(meta);
        }

        return display;
    }

    private void setupBottomBar(Inventory inventory, MainHolder holder, int total, int ownListings, int expiredCount) {
        ItemStack filler = createSimpleItem(Material.ORANGE_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " ");
        for (int slot = MAIN_LISTING_SLOTS; slot < MAIN_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        if (holder.page > 1) {
            inventory.setItem(
                    SLOT_PREV,
                    createSimpleItem(
                            Material.ARROW,
                            ChatColor.YELLOW + "Previous Page",
                            ChatColor.GRAY + "Go to page " + ChatColor.WHITE + (holder.page - 1)));
        } else {
            inventory.setItem(
                    SLOT_PREV,
                    createSimpleItem(
                            Material.BARRIER,
                            ChatColor.RED + "No Previous Page"));
        }

        inventory.setItem(
                SLOT_SELL_HELP,
                createSimpleItem(
                        Material.EMERALD,
                        ChatColor.GREEN + "List an Item",
                        ChatColor.GRAY + "Hold item in hand",
                        ChatColor.GRAY + "Use: " + ChatColor.WHITE + "/ah sell <price>"));

        inventory.setItem(
                SLOT_SEARCH_HELP,
                createSimpleItem(
                        Material.COMPASS,
                        ChatColor.AQUA + "Search Market",
                        ChatColor.GRAY + "Use: " + ChatColor.WHITE + "/ah search <name>"));

        inventory.setItem(
                SLOT_STATS,
                createSimpleItem(
                        Material.BOOK,
                        ChatColor.GOLD + "Market Stats",
                        ChatColor.GRAY + "Total Listings: " + ChatColor.WHITE + total,
                        ChatColor.GRAY + "Your Listings: " + ChatColor.WHITE + ownListings,
                        ChatColor.GRAY + "Expired Items: " + ChatColor.WHITE + expiredCount));

        inventory.setItem(
                SLOT_CLOSE,
                createSimpleItem(
                        Material.BARRIER,
                        ChatColor.RED + "Close"));

        if (holder.page < holder.maxPage) {
            inventory.setItem(
                    SLOT_NEXT,
                    createSimpleItem(
                            Material.ARROW,
                            ChatColor.YELLOW + "Next Page",
                            ChatColor.GRAY + "Go to page " + ChatColor.WHITE + (holder.page + 1)));
        } else {
            inventory.setItem(
                    SLOT_NEXT,
                    createSimpleItem(
                            Material.BARRIER,
                            ChatColor.RED + "No Next Page"));
        }

        if (expiredCount > 0) {
            inventory.setItem(
                    SLOT_CLAIM,
                    createSimpleItem(
                            Material.CHEST,
                            ChatColor.GOLD + "" + ChatColor.BOLD + "Claim Expired",
                            ChatColor.GRAY + "Items waiting: " + ChatColor.WHITE + expiredCount,
                            ChatColor.GREEN + "Click to claim now"));
        } else {
            inventory.setItem(
                    SLOT_CLAIM,
                    createSimpleItem(
                            Material.HOPPER,
                            ChatColor.GRAY + "No Expired Items"));
        }

        if (holder.query != null && !holder.query.isBlank()) {
            inventory.setItem(
                    SLOT_FILTER,
                    createSimpleItem(
                            Material.MAP,
                            ChatColor.GOLD + "Search Filter Active",
                            ChatColor.GRAY + "Query: " + ChatColor.WHITE + holder.query,
                            ChatColor.GRAY + "Use " + ChatColor.WHITE + "/ah" + ChatColor.GRAY + " to clear"));
        } else {
            inventory.setItem(
                    SLOT_FILTER,
                    createSimpleItem(
                            Material.PAPER,
                            ChatColor.GRAY + "No Search Filter"));
        }
    }

    private ItemStack createSimpleItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines.length > 0) {
                meta.setLore(List.of(loreLines));
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private String readableSellerName(UUID sellerId) {
        OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerId);
        if (seller.getName() != null) {
            return seller.getName();
        }
        return "Unknown";
    }

    private String formatRemaining(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return Math.max(seconds, 0) + "s";
    }

    public static final class MainHolder implements InventoryHolder {
        private final int page;
        private final int maxPage;
        private final String query;
        private final Map<Integer, UUID> slotToListing;

        public MainHolder(int page, int maxPage, String query, Map<Integer, UUID> slotToListing) {
            this.page = page;
            this.maxPage = maxPage;
            this.query = query;
            this.slotToListing = slotToListing;
        }

        public int getPage() {
            return page;
        }

        public int getMaxPage() {
            return maxPage;
        }

        public String getQuery() {
            return query;
        }

        public UUID getListingAt(int slot) {
            return slotToListing.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static final class ConfirmHolder implements InventoryHolder {
        private final UUID listingId;
        private final int returnPage;
        private final String query;

        public ConfirmHolder(UUID listingId, int returnPage, String query) {
            this.listingId = listingId;
            this.returnPage = returnPage;
            this.query = query;
        }

        public UUID getListingId() {
            return listingId;
        }

        public int getReturnPage() {
            return returnPage;
        }

        public String getQuery() {
            return query;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
