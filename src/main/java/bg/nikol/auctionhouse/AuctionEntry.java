package bg.nikol.auctionhouse;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class AuctionEntry {
    private final UUID id;
    private final UUID sellerId;
    private final ItemStack itemStack;
    private final double price;
    private final long createdAt;
    private final long expiresAt;

    public AuctionEntry(UUID id, UUID sellerId, ItemStack itemStack, double price, long createdAt, long expiresAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.itemStack = itemStack;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public double getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
