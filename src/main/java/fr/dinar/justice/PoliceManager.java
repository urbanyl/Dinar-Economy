package fr.dinar.justice;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PoliceManager {
    private final Map<UUID, String> officers = new LinkedHashMap<>();
    private Path dataFile;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("police.json");
        officers.clear();
        load();
    }

    public void onServerStop() {
        save();
    }

    public boolean isPolice(UUID uuid) {
        return uuid != null && officers.containsKey(uuid);
    }

    public String add(UUID uuid, String name) {
        if (isPolice(uuid)) return "§c" + name + " est déjà policier.";
        officers.put(uuid, name);
        return null;
    }

    public String remove(UUID uuid, String name) {
        if (!isPolice(uuid)) return "§c" + name + " n'est pas policier.";
        officers.remove(uuid);
        return null;
    }

    public List<String> listOfficers() {
        return List.copyOf(officers.values());
    }

    public int count() {
        return officers.size();
    }

    public void save() {
        if (dataFile == null) return;
        try {
            JsonArray arr = new JsonArray();
            for (Map.Entry<UUID, String> e : officers.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getKey().toString());
                o.addProperty("name", e.getValue());
                arr.add(o);
            }
            JsonObject root = new JsonObject();
            root.add("officers", arr);
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("police.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde police.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("officers")) {
                for (var el : root.getAsJsonArray("officers")) {
                    JsonObject o = el.getAsJsonObject();
                    officers.put(UUID.fromString(o.get("uuid").getAsString()), o.get("name").getAsString());
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement police.", e);
        }
    }
}
