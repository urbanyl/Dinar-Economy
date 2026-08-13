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

public class CompanyManager {
    private final Map<Integer, CompanyEntry> companies = new ConcurrentHashMap<>();
    private int nextId = 1;

    public CompanyEntry create(String name, UUID ownerUuid, String ownerName) {
        for (CompanyEntry c : companies.values()) {
            if (c.name.equalsIgnoreCase(name)) return null;
        }
        CompanyEntry company = new CompanyEntry(nextId++, name, ownerUuid, ownerName);
        companies.put(company.id, company);
        return company;
    }

    public CompanyEntry get(int id) {
        return companies.get(id);
    }

    public CompanyEntry getByName(String name) {
        return companies.values().stream()
                .filter(c -> c.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public void remove(int id) {
        companies.remove(id);
    }

    public List<CompanyEntry> getAll() {
        return new ArrayList<>(companies.values());
    }

    public List<CompanyEntry> getByOwner(UUID uuid) {
        return companies.values().stream()
                .filter(c -> c.isOwner(uuid))
                .sorted(Comparator.comparingInt(c -> c.id))
                .toList();
    }

    public List<CompanyEntry> getByMember(UUID uuid) {
        return companies.values().stream()
                .filter(c -> c.isMember(uuid))
                .sorted(Comparator.comparingInt(c -> c.id))
                .toList();
    }

    public boolean deposit(CompanyEntry company, UUID playerUuid, String playerName, double amount, EconomyManager eco) {
        if (amount <= 0) return false;
        if (!eco.deductFromBalance(playerUuid, playerName, amount)) return false;
        company.balance = eco.round(company.balance + amount);
        return true;
    }

    public boolean withdraw(CompanyEntry company, UUID playerUuid, String playerName, double amount, EconomyManager eco) {
        if (amount <= 0) return false;
        if (company.balance < amount) return false;
        company.balance = eco.round(company.balance - amount);
        eco.add(playerUuid, playerName, amount);
        return true;
    }

    public void save(Path dataDir) {
        try {
            Path file = dataDir.resolve("companies.json");
            JsonArray arr = new JsonArray();
            for (CompanyEntry c : companies.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id);
                o.addProperty("name", c.name);
                o.addProperty("ownerUuid", c.ownerUuid != null ? c.ownerUuid.toString() : "");
                o.addProperty("ownerName", c.ownerName);
                o.addProperty("balance", c.balance);
                o.addProperty("createdAt", c.createdAt);
                JsonArray members = new JsonArray();
                for (String m : c.members) members.add(m);
                o.add("members", members);
                arr.add(o);
            }
            Files.createDirectories(dataDir);
            Path tmp = file.resolveSibling("companies.json.tmp");
            Files.writeString(tmp, arr.toString());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde entreprises.", e);
        }
    }

    public void load(Path dataDir) {
        Path file = dataDir.resolve("companies.json");
        if (!Files.exists(file)) return;
        try {
            String content = Files.readString(file);
            if (content.isBlank()) return;
            JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                CompanyEntry c = new CompanyEntry();
                c.id = o.get("id").getAsInt();
                c.name = o.has("name") ? o.get("name").getAsString() : "";
                c.ownerUuid = o.has("ownerUuid") && !o.get("ownerUuid").getAsString().isEmpty()
                        ? UUID.fromString(o.get("ownerUuid").getAsString()) : null;
                c.ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
                c.balance = o.has("balance") ? o.get("balance").getAsDouble() : 0;
                c.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : System.currentTimeMillis();
                if (o.has("members")) {
                    for (var m : o.getAsJsonArray("members")) {
                        c.members.add(m.getAsString());
                    }
                }
                companies.put(c.id, c);
                if (c.id >= nextId) nextId = c.id + 1;
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement entreprises.", e);
        }
    }
}
