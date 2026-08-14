package fr.dinar.justice;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import fr.dinar.logs.DiscordWebhook;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PrisonManager {

    public static final class PrisonLocation {
        public String world = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;
    }

    public static final class PrisonSession {
        public String name;
        public long releaseAt;
    }

    private PrisonLocation location;
    private final Map<UUID, PrisonSession> sessions = new HashMap<>();
    private Path dataFile;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("prison.json");
        sessions.clear();
        location = null;
        load();
    }

    public void onServerStop() {
        save();
    }

    public boolean hasLocation() {
        return location != null;
    }

    public void setLocation(ServerPlayerEntity player) {
        PrisonLocation loc = new PrisonLocation();
        loc.world = player.getWorld().getRegistryKey().getValue().toString();
        loc.x = player.getX();
        loc.y = player.getY();
        loc.z = player.getZ();
        loc.yaw = player.getYaw();
        loc.pitch = player.getPitch();
        location = loc;
    }

    public String imprison(UUID uuid, String name, long minutes, String officerName) {
        if (minutes <= 0) return "§cLa durée doit être positive.";
        if (!hasLocation()) return "§cLa position de la prison n'est pas définie (§f/prison setpos§c).";
        PrisonSession s = new PrisonSession();
        s.name = name;
        s.releaseAt = System.currentTimeMillis() + minutes * 60000L;
        sessions.put(uuid, s);

        ServerPlayerEntity target = DinarMod.economy.online(uuid);
        if (target != null) {
            teleportToPrison(target);
            target.sendMessage(Text.literal("§c§lARRESTATION §r§7» §fVous avez été incarcéré pour §e"
                    + minutes + " min§f."), false);
        }

        DinarMod.rpLog.log("PRISON", officerName + " a incarcéré " + name + " pour " + minutes + " minutes");
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title("🚨 Arrestation")
                .field("Suspect", name)
                .field("Durée", minutes + " minute(s)")
                .field("Officier", officerName)
                .color(0xE74C3C)
                .footer("Dinar RP"));
        DinarMod.government.broadcast("§c§lARRESTATION §r§7» §e" + name
                + " §fa été incarcéré pour §e" + minutes + " minutes§f.");
        return null;
    }

    public String release(UUID uuid, String officerName) {
        if (!sessions.containsKey(uuid)) return "§cCe joueur n'est pas en prison.";
        releaseNow(uuid, false);
        return null;
    }

    public boolean isImprisoned(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public long remainingSeconds(UUID uuid) {
        PrisonSession s = sessions.get(uuid);
        if (s == null) return 0;
        return Math.max(0, (s.releaseAt - System.currentTimeMillis()) / 1000);
    }

    public Map<UUID, PrisonSession> getSessions() {
        return sessions;
    }

    public void tick(MinecraftServer server) {
        if (sessions.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<UUID> toRelease = new ArrayList<>();
        for (Map.Entry<UUID, PrisonSession> e : sessions.entrySet()) {
            PrisonSession s = e.getValue();
            if (now >= s.releaseAt) {
                toRelease.add(e.getKey());
                continue;
            }
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null) continue;
            if (location != null && !isAtPrison(p)) {
                teleportToPrison(p);
            }
            if (server.getTicks() % 20 == 0) {
                long rem = remainingSeconds(e.getKey());
                p.sendMessage(Text.literal("§8[§cPrison§8] §fIl vous reste §e"
                        + (rem / 60) + "m " + (rem % 60) + "s"), true);
            }
        }
        for (UUID u : toRelease) {
            releaseNow(u, true);
        }
    }

    private boolean isAtPrison(ServerPlayerEntity p) {
        if (location == null) return true;
        double dx = p.getX() - location.x;
        double dy = p.getY() - location.y;
        double dz = p.getZ() - location.z;
        return dx * dx + dy * dy + dz * dz < 4.0;
    }

    private void teleportToPrison(ServerPlayerEntity p) {
        if (location == null) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(location.world)));
        if (world == null) return;
        p.teleport(world, location.x, location.y, location.z, location.yaw, location.pitch);
    }

    private void releaseNow(UUID uuid, boolean expired) {
        PrisonSession s = sessions.remove(uuid);
        if (s == null) return;
        MinecraftServer server = DinarMod.economy.getServer();
        ServerPlayerEntity p = server != null ? server.getPlayerManager().getPlayer(uuid) : null;
        if (p != null && server != null) {
            ServerWorld world = server.getOverworld();
            var pos = world.getSpawnPos().toCenterPos();
            p.teleport(world, pos.getX(), pos.getY(), pos.getZ(), p.getYaw(), p.getPitch());
            p.sendMessage(Text.literal("§a§lLIBÉRÉ §r§7» §fVous êtes libéré."), false);
        }
        DinarMod.rpLog.log("PRISON", s.name + (expired ? " a purgé sa peine" : " a été libéré"));
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title("✅ Libération")
                .field("Détenu", s.name)
                .description(expired ? "A purgé sa peine." : "Libéré.")
                .color(0x27AE60)
                .footer("Dinar RP"));
        DinarMod.government.broadcast("§a§lLIBÉRATION §r§7» §e" + s.name + " §fa été libéré.");
    }

    public void save() {
        if (dataFile == null) return;
        try {
            JsonObject root = new JsonObject();
            if (location != null) {
                JsonObject loc = new JsonObject();
                loc.addProperty("world", location.world);
                loc.addProperty("x", location.x);
                loc.addProperty("y", location.y);
                loc.addProperty("z", location.z);
                loc.addProperty("yaw", location.yaw);
                loc.addProperty("pitch", location.pitch);
                root.add("location", loc);
            }
            JsonArray arr = new JsonArray();
            for (Map.Entry<UUID, PrisonSession> e : sessions.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getKey().toString());
                o.addProperty("name", e.getValue().name);
                o.addProperty("releaseAt", e.getValue().releaseAt);
                arr.add(o);
            }
            root.add("sessions", arr);
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("prison.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde prison.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("location")) {
                JsonObject loc = root.getAsJsonObject("location");
                PrisonLocation l = new PrisonLocation();
                l.world = loc.has("world") ? loc.get("world").getAsString() : "minecraft:overworld";
                l.x = loc.get("x").getAsDouble();
                l.y = loc.get("y").getAsDouble();
                l.z = loc.get("z").getAsDouble();
                l.yaw = loc.has("yaw") ? loc.get("yaw").getAsFloat() : 0;
                l.pitch = loc.has("pitch") ? loc.get("pitch").getAsFloat() : 0;
                location = l;
            }
            if (root.has("sessions")) {
                for (var el : root.getAsJsonArray("sessions")) {
                    JsonObject o = el.getAsJsonObject();
                    PrisonSession s = new PrisonSession();
                    s.name = o.has("name") ? o.get("name").getAsString() : "Inconnu";
                    s.releaseAt = o.has("releaseAt") ? o.get("releaseAt").getAsLong() : 0;
                    sessions.put(UUID.fromString(o.get("uuid").getAsString()), s);
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement prison.", e);
        }
    }
}
