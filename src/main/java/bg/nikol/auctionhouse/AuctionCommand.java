package bg.nikol.auctionhouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionCommand implements CommandExecutor, TabCompleter {
    private final AuctionHousePlugin plugin;
    private final AuctionService auctionService;
    private final AuctionGuiManager guiManager;

    public AuctionCommand(AuctionHousePlugin plugin, AuctionService auctionService, AuctionGuiManager guiManager) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.guiManager = guiManager;
    }

    public void register() {
        PluginCommand command = plugin.getCommand("ah");
        if (command == null) {
            throw new IllegalStateException("Command ah is missing from plugin.yml");
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (!hasAny(player, "auctionhouse.use", "donutauction.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        int expired = auctionService.expireListings(System.currentTimeMillis());
        if (expired > 0) {
            auctionService.saveAll();
        }

        if (args.length == 0) {
            guiManager.openMain(player, 1, null);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sell" -> handleSell(player, args);
            case "search" -> handleSearch(player, args);
            case "expired", "claim" -> handleClaimExpired(player);
            case "reload" -> handleReload(player);
            default -> {
                Integer page = parsePage(sub);
                if (page != null) {
                    guiManager.openMain(player, page, null);
                } else {
                    sendHelp(player);
                }
            }
        }

        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (!hasAny(player, "auctionhouse.sell", "donutauction.sell")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to list items.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /ah sell <price>");
            player.sendMessage(ChatColor.GRAY + "Examples: /ah sell 500 | /ah sell 1k | /ah sell 2.5m");
            return;
        }

        Double price = parsePrice(args[1]);
        if (price == null || price <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid price format.");
            return;
        }

        double minPrice = plugin.getConfig().getDouble("min-price", 1.0D);
        double maxPrice = plugin.getConfig().getDouble("max-price", 1_000_000_000D);
        if (price < minPrice || price > maxPrice) {
            player.sendMessage(ChatColor.RED + "Price must be between "
                    + plugin.formatMoney(minPrice)
                    + ChatColor.RED
                    + " and "
                    + plugin.formatMoney(maxPrice));
            return;
        }

        int maxListings = plugin.getConfig().getInt("max-listings-per-player", 20);
        boolean unlimited = hasAny(player, "auctionhouse.unlimited", "donutauction.unlimited");
        int currentListings = auctionService.countBySeller(player.getUniqueId());
        if (!unlimited && currentListings >= maxListings) {
            player.sendMessage(ChatColor.RED + "You have reached the listing limit of " + maxListings + ".");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item in your main hand first.");
            return;
        }

        ItemStack toSell = hand.clone();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        long now = System.currentTimeMillis();
        AuctionEntry entry = new AuctionEntry(
                UUID.randomUUID(),
                player.getUniqueId(),
                toSell,
                price,
                now,
                now + plugin.getAuctionDurationMillis());

        auctionService.addListing(entry);
        auctionService.saveAll();

        player.sendMessage(ChatColor.GREEN + "Listing created for " + plugin.formatMoney(price));
        player.sendMessage(ChatColor.GRAY + "Use /ah to view your item in the market.");
    }

    private void handleSearch(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /ah search <name>");
            return;
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        if (query.length() < 2) {
            player.sendMessage(ChatColor.RED + "Search text must be at least 2 characters.");
            return;
        }

        guiManager.openMain(player, 1, query);
    }

    private void handleClaimExpired(Player player) {
        List<ItemStack> items = auctionService.claimExpired(player.getUniqueId());
        if (items.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no expired items to claim.");
            return;
        }

        int delivered = 0;
        for (ItemStack item : items) {
            var leftovers = player.getInventory().addItem(item);
            delivered += item.getAmount();
            if (!leftovers.isEmpty()) {
                for (ItemStack left : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }
        }

        auctionService.saveAll();
        player.sendMessage(ChatColor.GREEN + "Claimed expired items. Delivered total amount: " + delivered);
    }

    private void handleReload(Player player) {
        if (!hasAny(player, "auctionhouse.admin", "donutauction.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        plugin.reloadPluginSettings();
        player.sendMessage(ChatColor.GREEN + "Auction House config reloaded.");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Donut Auction House");
        player.sendMessage(ChatColor.YELLOW + "/ah" + ChatColor.GRAY + " - Open market UI");
        player.sendMessage(ChatColor.YELLOW + "/ah <page>" + ChatColor.GRAY + " - Open specific page");
        player.sendMessage(ChatColor.YELLOW + "/ah sell <price>" + ChatColor.GRAY + " - List held item");
        player.sendMessage(ChatColor.YELLOW + "/ah search <name>" + ChatColor.GRAY + " - Search listings");
        player.sendMessage(ChatColor.YELLOW + "/ah expired" + ChatColor.GRAY + " - Claim expired items");
        if (hasAny(player, "auctionhouse.admin", "donutauction.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/ah reload" + ChatColor.GRAY + " - Reload config");
        }
    }

    private boolean hasAny(Player player, String... permissions) {
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private Integer parsePage(String raw) {
        try {
            int page = Integer.parseInt(raw);
            return page > 0 ? page : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parsePrice(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return null;
        }

        String raw = rawInput.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
        if (raw.contains(",") && raw.contains(".")) {
            raw = raw.replace(",", "");
        } else if (raw.contains(",")) {
            raw = raw.replace(",", ".");
        }
        double multiplier = 1.0D;

        if (raw.endsWith("k")) {
            multiplier = 1_000D;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("m")) {
            multiplier = 1_000_000D;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("b")) {
            multiplier = 1_000_000_000D;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("t")) {
            multiplier = 1_000_000_000_000D;
            raw = raw.substring(0, raw.length() - 1);
        }

        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) {
                return null;
            }
            return value * multiplier;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("sell", "search", "expired", "reload"), args[0]);
        }

        if (args.length == 2 && "sell".equalsIgnoreCase(args[0])) {
            return List.of("1k", "100", "2.5m");
        }

        if (args.length == 2 && "search".equalsIgnoreCase(args[0])) {
            return List.of("diamond", "totem", "netherite");
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return options;
        }

        String lowered = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lowered)) {
                filtered.add(option);
            }
        }

        return filtered;
    }
}
