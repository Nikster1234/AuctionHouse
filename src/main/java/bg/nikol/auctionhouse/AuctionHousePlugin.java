package bg.nikol.auctionhouse;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Locale;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionHousePlugin extends JavaPlugin {
    private Economy economy;
    private AuctionService auctionService;
    private AuctionGuiManager guiManager;

    private DecimalFormat moneyFormat;
    private String currencySymbol;
    private long auctionDurationMillis;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginSettings();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        String listingsFile = getConfig().getString("files.listings-file", "auctions.yml");
        String expiredFile = getConfig().getString("files.expired-file", "expired.yml");

        AuctionStorage auctionStorage = new AuctionStorage(this, new File(getDataFolder(), listingsFile));
        ExpiredStorage expiredStorage = new ExpiredStorage(this, new File(getDataFolder(), expiredFile));

        auctionService = new AuctionService(auctionStorage, expiredStorage);
        auctionService.load();

        int expiredOnStartup = auctionService.expireListings(System.currentTimeMillis());
        if (expiredOnStartup > 0) {
            getLogger().info("Expired " + expiredOnStartup + " listings during startup.");
            auctionService.saveAll();
        }

        guiManager = new AuctionGuiManager(this, auctionService);

        AuctionCommand command = new AuctionCommand(this, auctionService, guiManager);
        command.register();

        Bukkit.getPluginManager().registerEvents(new AuctionListener(this, auctionService, guiManager), this);

        long intervalTicks = 20L * 60;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            int expired = auctionService.expireListings(System.currentTimeMillis());
            if (expired > 0) {
                auctionService.saveAll();
            }
        }, intervalTicks, intervalTicks);

        getLogger().info("DonutAuctionHouse enabled.");
    }

    @Override
    public void onDisable() {
        if (auctionService != null) {
            auctionService.saveAll();
        }
    }

    public void reloadPluginSettings() {
        reloadConfig();

        String pattern = getConfig().getString("currency-format", "#,##0.##");
        moneyFormat = new DecimalFormat(pattern);
        currencySymbol = getConfig().getString("currency-symbol", "$");

        String durationRaw = getConfig().getString("auction-duration", "48h");
        auctionDurationMillis = parseDurationMillis(durationRaw);
    }

    public String formatMoney(double amount) {
        return ChatColor.GOLD + currencySymbol + moneyFormat.format(amount);
    }

    public long getAuctionDurationMillis() {
        return auctionDurationMillis;
    }

    public Economy getEconomy() {
        return economy;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return false;
        }

        economy = registration.getProvider();
        return economy != null;
    }

    private long parseDurationMillis(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return 48L * 60L * 60L * 1000L;
        }

        String raw = rawInput.trim().toLowerCase(Locale.ROOT);
        long multiplier = 60L * 60L * 1000L;

        if (raw.endsWith("s")) {
            multiplier = 1000L;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("m")) {
            multiplier = 60L * 1000L;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("h")) {
            multiplier = 60L * 60L * 1000L;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("d")) {
            multiplier = 24L * 60L * 60L * 1000L;
            raw = raw.substring(0, raw.length() - 1);
        }

        try {
            long value = Long.parseLong(raw);
            if (value <= 0) {
                return 48L * 60L * 60L * 1000L;
            }
            return value * multiplier;
        } catch (NumberFormatException ex) {
            getLogger().warning("Invalid auction-duration value in config: " + rawInput + ". Falling back to 48h.");
            return 48L * 60L * 60L * 1000L;
        }
    }
}
