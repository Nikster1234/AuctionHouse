package bg.nikol.auctionhouse;

import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class AuctionListener implements Listener {
    private final AuctionHousePlugin plugin;
    private final AuctionService auctionService;
    private final AuctionGuiManager guiManager;

    public AuctionListener(AuctionHousePlugin plugin, AuctionService auctionService, AuctionGuiManager guiManager) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int expired = auctionService.expireListings(System.currentTimeMillis());
        if (expired > 0) {
            auctionService.saveAll();
        }

        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AuctionGuiManager.MainHolder mainHolder) {
            handleMainClick(event, player, mainHolder);
            return;
        }

        if (holder instanceof AuctionGuiManager.ConfirmHolder confirmHolder) {
            handleConfirmClick(event, player, confirmHolder);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AuctionGuiManager.MainHolder || holder instanceof AuctionGuiManager.ConfirmHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainClick(InventoryClickEvent event, Player player, AuctionGuiManager.MainHolder holder) {
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < AuctionGuiManager.MAIN_LISTING_SLOTS) {
            UUID listingId = holder.getListingAt(slot);
            if (listingId == null) {
                return;
            }

            AuctionEntry entry = auctionService.getListing(listingId);
            if (entry == null) {
                player.sendMessage(ChatColor.RED + "This listing is no longer available.");
                guiManager.openMain(player, holder.getPage(), holder.getQuery());
                return;
            }

            if (player.getUniqueId().equals(entry.getSellerId())) {
                if (!event.isRightClick()) {
                    player.sendMessage(ChatColor.YELLOW + "Right click your own listing to remove it.");
                    return;
                }

                AuctionEntry removed = auctionService.removeListing(listingId);
                if (removed != null) {
                    giveItem(player, removed.getItemStack());
                    auctionService.saveAll();
                    player.sendMessage(ChatColor.GREEN + "Listing removed and item returned.");
                }

                guiManager.openMain(player, holder.getPage(), holder.getQuery());
                return;
            }

            if (!event.isLeftClick()) {
                return;
            }

            if (!hasAny(player, "auctionhouse.buy", "donutauction.buy")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to buy items.");
                return;
            }

            guiManager.openConfirm(player, entry, holder.getPage(), holder.getQuery());
            return;
        }

        switch (slot) {
            case AuctionGuiManager.SLOT_PREV -> {
                if (holder.getPage() > 1) {
                    guiManager.openMain(player, holder.getPage() - 1, holder.getQuery());
                }
            }
            case AuctionGuiManager.SLOT_CLOSE -> player.closeInventory();
            case AuctionGuiManager.SLOT_CLAIM -> {
                claimExpiredToPlayer(player);
                guiManager.openMain(player, holder.getPage(), holder.getQuery());
            }
            case AuctionGuiManager.SLOT_NEXT -> {
                if (holder.getPage() < holder.getMaxPage()) {
                    guiManager.openMain(player, holder.getPage() + 1, holder.getQuery());
                }
            }
            default -> {
            }
        }
    }

    private void handleConfirmClick(InventoryClickEvent event, Player player, AuctionGuiManager.ConfirmHolder holder) {
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 11) {
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        if (slot != 15) {
            return;
        }

        UUID listingId = holder.getListingId();
        AuctionEntry entry = auctionService.getListing(listingId);
        if (entry == null) {
            player.sendMessage(ChatColor.RED + "This listing is no longer available.");
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        if (player.getUniqueId().equals(entry.getSellerId())) {
            player.sendMessage(ChatColor.RED + "You cannot buy your own listing.");
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        Economy economy = plugin.getEconomy();
        double price = entry.getPrice();

        if (!economy.has(player, price)) {
            player.sendMessage(ChatColor.RED + "Not enough money. Required: " + plugin.formatMoney(price));
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        AuctionEntry removed = auctionService.removeListing(listingId);
        if (removed == null) {
            player.sendMessage(ChatColor.RED + "This listing was just purchased by someone else.");
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        EconomyResponse withdraw = economy.withdrawPlayer(player, price);
        if (!withdraw.transactionSuccess()) {
            auctionService.addListing(removed);
            player.sendMessage(ChatColor.RED + "Payment failed. Try again.");
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        OfflinePlayer seller = Bukkit.getOfflinePlayer(removed.getSellerId());
        EconomyResponse deposit = economy.depositPlayer(seller, price);
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(player, price);
            auctionService.addListing(removed);
            player.sendMessage(ChatColor.RED + "Could not pay the seller. Purchase cancelled.");
            guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
            return;
        }

        giveItem(player, removed.getItemStack());
        auctionService.saveAll();

        player.sendMessage(ChatColor.GREEN + "Purchase successful for " + plugin.formatMoney(price));
        if (seller.isOnline() && seller.getPlayer() != null) {
            seller.getPlayer().sendMessage(ChatColor.GREEN + player.getName() + " bought your auction for "
                    + plugin.formatMoney(price));
        }

        guiManager.openMain(player, holder.getReturnPage(), holder.getQuery());
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    private void claimExpiredToPlayer(Player player) {
        var items = auctionService.claimExpired(player.getUniqueId());
        if (items.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no expired items to claim.");
            return;
        }

        int deliveredStacks = 0;
        for (ItemStack item : items) {
            giveItem(player, item);
            deliveredStacks++;
        }

        auctionService.saveAll();
        player.sendMessage(ChatColor.GREEN + "Claimed " + deliveredStacks + " expired item stack(s).");
    }

    private boolean hasAny(Player player, String... permissions) {
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
