package fr.dinar.logs;

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
import java.util.ArrayList;
import java.util.List;

public class RpLogManager {
    private static final int MAX_ENTRIES = 500;
    private final List<RpLogEntry> entries = new ArrayList<>();
    private Path dataFile;
    private long lastSave = 0;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("rp_log.json");
        entries.clear();
        load();
    }

    public void onServerStop() {
        save();
    }

    public void log(String category, String message) {
        entries.add(new RpLogEntry(System.currentTimeMillis(), category, message));
        while (entries.size() > MAX_ENTRIES) entries.remove(0);
        if (webhookEnabled()) {
            DiscordWebhook.sendAsync(DinarMod.config.discordWebhook, new DiscordWebhook.DiscordEmbed()
                    .title(DinarMod.config.discordWebhookTitle)
                    .description("**" + category + "** · " + message)
                    .color(colorFor(category))
                    .footer("Dinar RP"));
        }
        long now = System.currentTimeMillis();
        if (now - lastSave > 5000) {
            lastSave = now;
            save();
        }
    }

    public void sendEmbed(DiscordWebhook.DiscordEmbed embed) {
        if (webhookEnabled()) {
            DiscordWebhook.sendAsync(DinarMod.config.discordWebhook, embed);
        }
    }

    private boolean webhookEnabled() {
        return DinarMod.config != null && DinarMod.config.discordWebhookEnabled
                && DinarMod.config.discordWebhook != null && !DinarMod.config.discordWebhook.isBlank();
    }

    public List<RpLogEntry> getEntries() {
        return entries;
    }

    public List<RpLogEntry> getPage(int page, int perPage) {
        int total = entries.size();
        int end = Math.max(0, total - page * perPage);
        int start = Math.max(0, total - (page + 1) * perPage);
        List<RpLogEntry> slice = new ArrayList<>();
        for (int i = start; i < end && i < total; i++) slice.add(entries.get(i));
        return slice;
    }

    public List<RpLogEntry> getCategory(String category, int page, int perPage) {
        List<RpLogEntry> filtered = new ArrayList<>();
        for (RpLogEntry e : entries) {
            if (e.category.equalsIgnoreCase(category)) filtered.add(e);
        }
        int total = filtered.size();
        int end = Math.max(0, total - page * perPage);
        int start = Math.max(0, total - (page + 1) * perPage);
        List<RpLogEntry> slice = new ArrayList<>();
        for (int i = start; i < end && i < total; i++) slice.add(filtered.get(i));
        return slice;
    }

    public int pageCount(int perPage) {
        return Math.max(1, (int) Math.ceil(entries.size() / (double) perPage));
    }

    private static int colorFor(String category) {
        switch (category) {
            case "JUSTICE": return 0xE74C3C;
            case "POLICE": return 0x3498DB;
            case "PRISON": return 0x8E44AD;
            case "GOUVERNEMENT": return 0xF1C40F;
            case "MAIL": return 0x9B59B6;
            case "SALAIRE": return 0x27AE60;
            case "AMENDE": return 0xE67E22;
            case "BANQUE": return 0x1ABC9C;
            default: return 0xF2C94C;
        }
    }

    public void save() {
        if (dataFile == null) return;
        try {
            JsonArray arr = new JsonArray();
            for (RpLogEntry e : entries) {
                JsonObject o = new JsonObject();
                o.addProperty("time", e.time);
                o.addProperty("category", e.category);
                o.addProperty("message", e.message);
                arr.add(o);
            }
            JsonObject root = new JsonObject();
            root.add("entries", arr);
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("rp_log.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde journal RP.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("entries")) {
                for (var el : root.getAsJsonArray("entries")) {
                    JsonObject o = el.getAsJsonObject();
                    entries.add(new RpLogEntry(
                            o.has("time") ? o.get("time").getAsLong() : 0,
                            o.has("category") ? o.get("category").getAsString() : "RP",
                            o.has("message") ? o.get("message").getAsString() : ""));
                }
            }
            while (entries.size() > MAX_ENTRIES) entries.remove(0);
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement journal RP.", e);
        }
    }

    public static final class RpLogEntry {
        public final long time;
        public final String category;
        public final String message;

        public RpLogEntry(long time, String category, String message) {
            this.time = time;
            this.category = category;
            this.message = message;
        }
    }
}
