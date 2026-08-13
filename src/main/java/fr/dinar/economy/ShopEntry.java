package fr.dinar.economy;

import java.util.UUID;

public class ShopEntry {
    public int id;
    public UUID ownerUuid;
    public String ownerName;
    public String itemName;
    public double buyPrice;
    public double sellPrice;
    public int stock;
    public int maxStock;
    public long createdAt;

    public ShopEntry() {}

    public ShopEntry(int id, UUID ownerUuid, String ownerName, String itemName, double buyPrice, double sellPrice, int maxStock) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.itemName = itemName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stock = 0;
        this.maxStock = maxStock;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }
}
