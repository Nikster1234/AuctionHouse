package bg.nikol.auctionhouse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionStorage {
    private final JavaPlugin plugin;
    private final File file;

    public AuctionStorage(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public List<AuctionEntry> loadEntries() {
        List<AuctionEntry> entries = new ArrayList<>();
        if (!file.exists()) {
            return entries;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection listings = yaml.getConfigurationSection("listings");
        if (listings == null) {
            return entries;
        }

        for (String key : listings.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String sellerRaw = listings.getString(key + ".seller");
                if (sellerRaw == null) {
                    continue;
                }

                UUID sellerId = UUID.fromString(sellerRaw);
                double price = listings.getDouble(key + ".price");
                if (price <= 0) {
                    continue;
                }

                long createdAt = listings.getLong(key + ".createdAt", System.currentTimeMillis());
                long expiresAt = listings.getLong(key + ".expiresAt", createdAt);
                ItemStack item = listings.getItemStack(key + ".item");
                if (item == null) {
                    continue;
                }

                entries.add(new AuctionEntry(id, sellerId, item, price, createdAt, expiresAt));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid listing: " + key);
            }
        }

        return entries;
    }

    public void saveEntries(Collection<AuctionEntry> entries) {
        YamlConfiguration yaml = new YamlConfiguration();

        for (AuctionEntry entry : entries) {
            String path = "listings." + entry.getId();
            yaml.set(path + ".seller", entry.getSellerId().toString());
            yaml.set(path + ".price", entry.getPrice());
            yaml.set(path + ".createdAt", entry.getCreatedAt());
            yaml.set(path + ".expiresAt", entry.getExpiresAt());
            yaml.set(path + ".item", entry.getItemStack());
        }

        saveYaml(yaml);
    }

    private void saveYaml(YamlConfiguration yaml) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save " + file.getName() + ": " + ex.getMessage());
        }
    }
}
