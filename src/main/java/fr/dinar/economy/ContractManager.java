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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ContractManager {
    private final Map<Integer, ContractEntry> contracts = new ConcurrentHashMap<>();
    private int nextId = 1;

    public ContractEntry create(UUID creatorUuid, String creatorName, UUID targetUuid, String targetName,
                                String type, String details, double amount) {
        ContractEntry contract = new ContractEntry(nextId++, creatorUuid, creatorName, targetUuid, targetName,
                type, details, amount);
        contracts.put(contract.id, contract);
        return contract;
    }

    public ContractEntry get(int id) {
        return contracts.get(id);
    }

    public List<ContractEntry> getAll() {
        return new ArrayList<>(contracts.values());
    }

    public void remove(int id) {
        contracts.remove(id);
    }

    public List<ContractEntry> getByPlayer(UUID uuid) {
        return contracts.values().stream()
                .filter(c -> c.involves(uuid))
                .sorted((a, b) -> Integer.compare(b.id, a.id))
                .toList();
    }

    public List<ContractEntry> getPendingFor(UUID uuid) {
        return contracts.values().stream()
                .filter(c -> c.isPending() && c.targetUuid.equals(uuid))
                .sorted((a, b) -> Integer.compare(b.id, a.id))
                .toList();
    }

    public boolean sign(int contractId, UUID signerUuid) {
        ContractEntry contract = contracts.get(contractId);
        if (contract == null || !contract.isPending()) return false;
        if (!contract.targetUuid.equals(signerUuid)) return false;
        contract.status = ContractEntry.STATUS_SIGNED;
        contract.signedAt = System.currentTimeMillis();
        return true;
    }

    public boolean cancel(int contractId, UUID playerUuid) {
        ContractEntry contract = contracts.get(contractId);
        if (contract == null) return false;
        if (!contract.isPending()) return false;
        if (!contract.involves(playerUuid)) return false;
        contract.status = ContractEntry.STATUS_CANCELLED;
        return true;
    }

    public void save(Path dataDir) {
        try {
            Path file = dataDir.resolve("contracts.json");
            JsonArray arr = new JsonArray();
            for (ContractEntry c : contracts.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id);
                o.addProperty("creatorUuid", c.creatorUuid != null ? c.creatorUuid.toString() : "");
                o.addProperty("creatorName", c.creatorName);
                o.addProperty("targetUuid", c.targetUuid != null ? c.targetUuid.toString() : "");
                o.addProperty("targetName", c.targetName);
                o.addProperty("type", c.type);
                o.addProperty("details", c.details);
                o.addProperty("amount", c.amount);
                o.addProperty("status", c.status);
                o.addProperty("createdAt", c.createdAt);
                o.addProperty("signedAt", c.signedAt);
                arr.add(o);
            }
            Files.createDirectories(dataDir);
            Path tmp = file.resolveSibling("contracts.json.tmp");
            Files.writeString(tmp, arr.toString());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde contrats.", e);
        }
    }

    public void load(Path dataDir) {
        Path file = dataDir.resolve("contracts.json");
        if (!Files.exists(file)) return;
        try {
            String content = Files.readString(file);
            if (content.isBlank()) return;
            JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                ContractEntry c = new ContractEntry();
                c.id = o.get("id").getAsInt();
                c.creatorUuid = o.has("creatorUuid") && !o.get("creatorUuid").getAsString().isEmpty()
                        ? UUID.fromString(o.get("creatorUuid").getAsString()) : null;
                c.creatorName = o.has("creatorName") ? o.get("creatorName").getAsString() : "";
                c.targetUuid = o.has("targetUuid") && !o.get("targetUuid").getAsString().isEmpty()
                        ? UUID.fromString(o.get("targetUuid").getAsString()) : null;
                c.targetName = o.has("targetName") ? o.get("targetName").getAsString() : "";
                c.type = o.has("type") ? o.get("type").getAsString() : "";
                c.details = o.has("details") ? o.get("details").getAsString() : "";
                c.amount = o.has("amount") ? o.get("amount").getAsDouble() : 0;
                c.status = o.has("status") ? o.get("status").getAsString() : ContractEntry.STATUS_PENDING;
                c.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : System.currentTimeMillis();
                c.signedAt = o.has("signedAt") ? o.get("signedAt").getAsLong() : 0;
                contracts.put(c.id, c);
                if (c.id >= nextId) nextId = c.id + 1;
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement contrats.", e);
        }
    }
}
