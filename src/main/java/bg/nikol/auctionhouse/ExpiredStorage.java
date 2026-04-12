package bg.nikol.auctionhouse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ExpiredStorage {
    private final JavaPlugin plugin;
    private final File file;

    public ExpiredStorage(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public Map<UUID, List<ItemStack>> loadClaims() {
        Map<UUID, List<ItemStack>> claims = new HashMap<>();
        if (!file.exists()) {
            return claims;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return claims;
        }

        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                List<ItemStack> items = new ArrayList<>();
                List<?> rawList = players.getList(key + ".items", List.of());
                for (Object obj : rawList) {
                    if (obj instanceof ItemStack item) {
                        items.add(item);
                    }
                }

                if (!items.isEmpty()) {
                    claims.put(playerId, items);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid claim entry: " + key);
            }
        }

        return claims;
    }

    public void saveClaims(Map<UUID, List<ItemStack>> claims) {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, List<ItemStack>> entry : claims.entrySet()) {
            Collection<ItemStack> items = entry.getValue();
            if (items == null || items.isEmpty()) {
                continue;
            }

            String path = "players." + entry.getKey();
            yaml.set(path + ".items", new ArrayList<>(items));
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
