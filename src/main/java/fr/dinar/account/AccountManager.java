package fr.dinar.account;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {

    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessions = new ConcurrentHashMap<>();
    private Path dataFile;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("accounts.json");
        accounts.clear();
        sessions.clear();
        load();
    }

    public void onServerStop() {
        save();
    }

    public boolean hasAccount(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    public boolean isLoggedIn(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public String register(UUID uuid, String name, String password) {
        if (accounts.containsKey(uuid)) {
            return DinarLang.t("§cUn compte existe déjà. Utilisez §f/login <mot de passe>§c.");
        }
        if (password == null || password.length() < 4) {
            return DinarLang.t("§cMot de passe trop court (§f4 caractères minimum§c).");
        }
        if (password.length() > 64) {
            return DinarLang.t("§cMot de passe trop long (§f64 caractères maximum§c).");
        }
        if (password.matches(".*\\s.*")) {
            return DinarLang.t("§cLe mot de passe ne doit pas contenir d'espaces.");
        }
        accounts.put(uuid, new Account(uuid, name, hash(password, uuid.toString())));
        sessions.put(uuid, System.currentTimeMillis());
        save();
        return null;
    }

    public String login(UUID uuid, String password) {
        Account a = accounts.get(uuid);
        if (a == null) {
            return DinarLang.t("§cAucun compte. Créez-en un : §f/register <mot de passe>§c.");
        }
        if (password == null || !hash(password, uuid.toString()).equals(a.passwordHash)) {
            return DinarLang.t("§cMot de passe incorrect.");
        }
        sessions.put(uuid, System.currentTimeMillis());
        return null;
    }

    public String logout(UUID uuid) {
        if (!sessions.containsKey(uuid)) {
            return DinarLang.t("§cVous n'êtes pas connecté.");
        }
        sessions.remove(uuid);
        return null;
    }

    private static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] d = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!hasAccount(p.getUuid()) || isLoggedIn(p.getUuid())) continue;
            p.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
            p.velocityModified = true;
            if (server.getTicks() % 40 == 0) {
                ServerWorld overworld = server.getOverworld();
                if (overworld != null) {
                    var spawn = overworld.getSpawnPos();
                    double dx = p.getX() - spawn.getX() - 0.5;
                    double dy = p.getY() - spawn.getY();
                    double dz = p.getZ() - spawn.getZ() - 0.5;
                    if (dx * dx + dy * dy + dz * dz > 25.0) {
                        p.teleport(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                                p.getYaw(), p.getPitch());
                    }
                }
                p.sendMessage(DinarLang.text("§c🔒 Connectez-vous pour jouer : §a/login <mot de passe>"
                        + " §7ou créez un compte : §a/register <mot de passe>"), true);
            }
        }
    }

    public void save() {
        if (dataFile == null) return;
        try {
            JsonArray arr = new JsonArray();
            for (Account a : accounts.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", a.uuid.toString());
                o.addProperty("name", a.name);
                o.addProperty("passwordHash", a.passwordHash);
                arr.add(o);
            }
            JsonObject root = new JsonObject();
            root.add("accounts", arr);
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("accounts.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde comptes.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("accounts")) {
                for (var el : root.getAsJsonArray("accounts")) {
                    JsonObject o = el.getAsJsonObject();
                    String uuid = o.get("uuid").getAsString();
                    String name = o.has("name") ? o.get("name").getAsString() : "Inconnu";
                    String hash = o.has("passwordHash") ? o.get("passwordHash").getAsString() : "";
                    accounts.put(UUID.fromString(uuid), new Account(UUID.fromString(uuid), name, hash));
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement comptes.", e);
        }
    }
}
