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

public class AuctionManager {
    private final Map<Integer, AuctionEntry> auctions = new ConcurrentHashMap<>();
    private int nextId = 1;

    public AuctionEntry create(UUID sellerUuid, String sellerName, String itemName, int itemCount, double price, long durationSeconds) {
        AuctionEntry auction = new AuctionEntry(nextId++, sellerUuid, sellerName, itemName, itemCount, price, durationSeconds);
        auctions.put(auction.id, auction);
        return auction;
    }

    public AuctionEntry get(int id) {
        return auctions.get(id);
    }

    public void remove(int id) {
        auctions.remove(id);
    }

    public List<AuctionEntry> getAll() {
        purgeExpired();
        return new ArrayList<>(auctions.values());
    }

    public List<AuctionEntry> getPage(int page, int perPage) {
        purgeExpired();
        List<AuctionEntry> all = new ArrayList<>(auctions.values());
        all.sort(Comparator.comparingInt(a -> a.id));
        int start = page * perPage;
        return all.subList(Math.min(start, all.size()), Math.min(start + perPage, all.size()));
    }

    public int pageCount(int perPage) {
        return Math.max(1, (int) Math.ceil(auctions.size() / (double) perPage));
    }

    public boolean buy(int auctionId, UUID buyerUuid, String buyerName, EconomyManager eco) {
        AuctionEntry auction = auctions.get(auctionId);
        if (auction == null || auction.isExpired()) return false;
        if (auction.sellerUuid.equals(buyerUuid)) return false;
        if (!eco.deductFromBalance(buyerUuid, buyerName, auction.price)) return false;
        eco.add(auction.sellerUuid, auction.sellerName, auction.price);
        eco.logTransaction(buyerUuid, "AH_BUY", auction.price, auction.sellerName,
                "Achat: " + auction.itemName + " x" + auction.itemCount);
        eco.logTransaction(auction.sellerUuid, "AH_SELL", auction.price, buyerName,
                "Vente: " + auction.itemName + " x" + auction.itemCount);
        auctions.remove(auctionId);
        return true;
    }

    public boolean cancel(int auctionId, UUID playerUuid) {
        AuctionEntry auction = auctions.get(auctionId);
        if (auction == null) return false;
        if (!auction.sellerUuid.equals(playerUuid)) return false;
        auctions.remove(auctionId);
        return true;
    }

    public void purgeExpired() {
        auctions.values().removeIf(AuctionEntry::isExpired);
    }

    public void save(Path dataDir) {
        try {
            Path file = dataDir.resolve("auctions.json");
            JsonArray arr = new JsonArray();
            for (AuctionEntry a : auctions.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", a.id);
                o.addProperty("sellerUuid", a.sellerUuid != null ? a.sellerUuid.toString() : "");
                o.addProperty("sellerName", a.sellerName);
                o.addProperty("itemName", a.itemName);
                o.addProperty("itemCount", a.itemCount);
                o.addProperty("price", a.price);
                o.addProperty("createdAt", a.createdAt);
                o.addProperty("expiresAt", a.expiresAt);
                arr.add(o);
            }
            Files.createDirectories(dataDir);
            Path tmp = file.resolveSibling("auctions.json.tmp");
            Files.writeString(tmp, arr.toString());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde auctions.", e);
        }
    }

    public void load(Path dataDir) {
        Path file = dataDir.resolve("auctions.json");
        if (!Files.exists(file)) return;
        try {
            String content = Files.readString(file);
            if (content.isBlank()) return;
            JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                AuctionEntry a = new AuctionEntry();
                a.id = o.get("id").getAsInt();
                a.sellerUuid = o.has("sellerUuid") && !o.get("sellerUuid").getAsString().isEmpty()
                        ? UUID.fromString(o.get("sellerUuid").getAsString()) : null;
                a.sellerName = o.has("sellerName") ? o.get("sellerName").getAsString() : "";
                a.itemName = o.has("itemName") ? o.get("itemName").getAsString() : "";
                a.itemCount = o.has("itemCount") ? o.get("itemCount").getAsInt() : 1;
                a.price = o.has("price") ? o.get("price").getAsDouble() : 0;
                a.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : System.currentTimeMillis();
                a.expiresAt = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : System.currentTimeMillis();
                if (!a.isExpired()) {
                    auctions.put(a.id, a);
                }
                if (a.id >= nextId) nextId = a.id + 1;
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement auctions.", e);
        }
    }
}
