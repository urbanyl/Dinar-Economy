package fr.dinar.economy;

import java.util.UUID;

public class AuctionEntry {
    public int id;
    public UUID sellerUuid;
    public String sellerName;
    public String itemName;
    public int itemCount;
    public double price;
    public long createdAt;
    public long expiresAt;

    public AuctionEntry() {}

    public AuctionEntry(int id, UUID sellerUuid, String sellerName, String itemName, int itemCount, double price, long durationSeconds) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.itemCount = itemCount;
        this.price = price;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + durationSeconds * 1000;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long timeRemainingSeconds() {
        return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000);
    }
}
