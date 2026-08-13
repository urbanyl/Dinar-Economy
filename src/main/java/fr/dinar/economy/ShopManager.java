package fr.dinar.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    private final Map<Integer, ShopEntry> shops = new ConcurrentHashMap<>();
    private int nextId = 1;

    public ShopEntry create(UUID ownerUuid, String ownerName, String itemName, double buyPrice, double sellPrice, int maxStock) {
        ShopEntry shop = new ShopEntry(nextId++, ownerUuid, ownerName, itemName, buyPrice, sellPrice, maxStock);
        shops.put(shop.id, shop);
        return shop;
    }

    public ShopEntry get(int id) {
        return shops.get(id);
    }

    public void remove(int id) {
        shops.remove(id);
    }

    public List<ShopEntry> getAll() {
        return new ArrayList<>(shops.values());
    }

    public List<ShopEntry> getByOwner(UUID uuid) {
        return shops.values().stream()
                .filter(s -> s.isOwner(uuid))
                .sorted(Comparator.comparingInt(s -> s.id))
                .toList();
    }

    public List<ShopEntry> getByItem(String itemName) {
        return shops.values().stream()
                .filter(s -> s.itemName.equalsIgnoreCase(itemName))
                .toList();
    }

    public List<ShopEntry> getPage(int page, int perPage) {
        List<ShopEntry> all = new ArrayList<>(shops.values());
        all.sort(Comparator.comparingInt(s -> s.id));
        int start = page * perPage;
        return all.subList(Math.min(start, all.size()), Math.min(start + perPage, all.size()));
    }

    public int pageCount(int perPage) {
        return Math.max(1, (int) Math.ceil(shops.size() / (double) perPage));
    }

    public boolean buy(ShopEntry shop, UUID buyerUuid, String buyerName, int quantity, EconomyManager eco) {
        if (shop.stock < quantity) return false;
        double total = shop.buyPrice * quantity;
        if (!eco.deductFromBalance(buyerUuid, buyerName, total)) return false;
        shop.stock -= quantity;
        eco.add(shop.ownerUuid, shop.ownerName, total);
        eco.logTransaction(buyerUuid, "SHOP_BUY", total, shop.ownerName, "Achat: " + shop.itemName + " x" + quantity);
        return true;
    }

    public boolean sell(ShopEntry shop, UUID sellerUuid, String sellerName, int quantity, EconomyManager eco) {
        if (shop.stock + quantity > shop.maxStock) return false;
        double total = shop.sellPrice * quantity;
        if (!eco.deductFromBalance(shop.ownerUuid, shop.ownerName, total)) return false;
        shop.stock += quantity;
        eco.add(sellerUuid, sellerName, total);
        eco.logTransaction(shop.ownerUuid, "SHOP_SELL", total, sellerName, "Vente: " + shop.itemName + " x" + quantity);
        return true;
    }

    public void save(Path dataDir) {
        try {
            Path file = dataDir.resolve("shops.json");
            JsonArray arr = new JsonArray();
            for (ShopEntry s : shops.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", s.id);
                o.addProperty("ownerUuid", s.ownerUuid != null ? s.ownerUuid.toString() : "");
                o.addProperty("ownerName", s.ownerName);
                o.addProperty("itemName", s.itemName);
                o.addProperty("buyPrice", s.buyPrice);
                o.addProperty("sellPrice", s.sellPrice);
                o.addProperty("stock", s.stock);
                o.addProperty("maxStock", s.maxStock);
                o.addProperty("createdAt", s.createdAt);
                arr.add(o);
            }
            Files.createDirectories(dataDir);
            Path tmp = file.resolveSibling("shops.json.tmp");
            Files.writeString(tmp, arr.toString());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde shops.", e);
        }
    }

    public void load(Path dataDir) {
        Path file = dataDir.resolve("shops.json");
        if (!Files.exists(file)) return;
        try {
            String content = Files.readString(file);
            if (content.isBlank()) return;
            JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                ShopEntry s = new ShopEntry();
                s.id = o.get("id").getAsInt();
                s.ownerUuid = o.has("ownerUuid") && !o.get("ownerUuid").getAsString().isEmpty()
                        ? UUID.fromString(o.get("ownerUuid").getAsString()) : null;
                s.ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
                s.itemName = o.has("itemName") ? o.get("itemName").getAsString() : "";
                s.buyPrice = o.has("buyPrice") ? o.get("buyPrice").getAsDouble() : 0;
                s.sellPrice = o.has("sellPrice") ? o.get("sellPrice").getAsDouble() : 0;
                s.stock = o.has("stock") ? o.get("stock").getAsInt() : 0;
                s.maxStock = o.has("maxStock") ? o.get("maxStock").getAsInt() : 64;
                s.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : System.currentTimeMillis();
                shops.put(s.id, s);
                if (s.id >= nextId) nextId = s.id + 1;
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement shops.", e);
        }
    }
}
