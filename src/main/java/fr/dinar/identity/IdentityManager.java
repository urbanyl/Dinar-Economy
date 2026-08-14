package fr.dinar.identity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IdentityManager {

    private final Map<UUID, RpProfile> profiles = new ConcurrentHashMap<>();
    private Path dataFile;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("identity.json");
        profiles.clear();
        load();
    }

    public void onServerStop() {
        save();
    }

    public RpProfile get(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> new RpProfile());
    }

    public boolean isComplete(UUID uuid) {
        RpProfile p = profiles.get(uuid);
        return p != null && p.isComplete();
    }

    public String setRpName(UUID uuid, String rpName) {
        if (rpName == null || rpName.isBlank()) return "§cLe prénom RP ne peut pas être vide.";
        if (rpName.length() > 24) return "§cPrénom RP trop long (§f24 caractères max§c).";
        get(uuid).rpName = rpName.trim();
        save();
        return null;
    }

    public String setJob(UUID uuid, String job) {
        if (job == null || job.isBlank()) return "§cLe métier ne peut pas être vide.";
        if (job.length() > 40) return "§cMétier trop long (§f40 caractères max§c).";
        get(uuid).job = job.trim();
        save();
        return null;
    }

    public String formatName(UUID uuid) {
        if (!isComplete(uuid)) return null;
        RpProfile p = profiles.get(uuid);
        String real = DinarMod.economy.accountName(uuid);
        return "§7[" + p.job + "§7] §e" + p.rpName + " §8(" + real + "§8)";
    }

    public ItemStack createCard(UUID ownerUuid) {
        if (!isComplete(ownerUuid)) return null;
        RpProfile p = profiles.get(ownerUuid);
        String real = DinarMod.economy.accountName(ownerUuid);
        String idShort = ownerUuid.toString().replace("-", "").substring(0, 8).toUpperCase();

        ItemStack card = new ItemStack(DinarMod.IDENTITY_CARD);
        card.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6Carte d'identité §7» §e" + p.rpName));
        card.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§8━━━━━━━━━━━━━━━━━━━"),
                Text.literal("§7Prénom RP : §f" + p.rpName),
                Text.literal("§7Métier : §f" + p.job),
                Text.literal("§7Pseudo : §f" + real),
                Text.literal("§7N° : §f" + idShort),
                Text.literal("§8━━━━━━━━━━━━━━━━━━━"),
                Text.literal("§7Carte officielle de la ville."),
                Text.literal("§7Présentez-la lors des contrôles RP.")
        )));
        card.setCount(1);
        return card;
    }

    public String giveCard(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!DinarMod.accounts.isLoggedIn(uuid)) {
            return "§cConnectez-vous d'abord (§f/login§c).";
        }
        ItemStack card = createCard(uuid);
        if (card == null) {
            return "§cComplétez votre identité d'abord : §f/identite prenom <prénom> §7puis §f/identite metier <métier>§c.";
        }
        boolean added = player.getInventory().insertStack(card);
        if (!added) {
            player.dropItem(card, true);
            return "§aCarte d'identité posée à vos pieds §7(inventaire plein)§a.";
        }
        return null;
    }

    public void save() {
        if (dataFile == null) return;
        try {
            JsonArray arr = new JsonArray();
            for (Map.Entry<UUID, RpProfile> e : profiles.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getKey().toString());
                o.addProperty("rpName", e.getValue().rpName);
                o.addProperty("job", e.getValue().job);
                arr.add(o);
            }
            JsonObject root = new JsonObject();
            root.add("profiles", arr);
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("identity.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde identités.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("profiles")) {
                for (var el : root.getAsJsonArray("profiles")) {
                    JsonObject o = el.getAsJsonObject();
                    RpProfile p = new RpProfile();
                    p.rpName = o.has("rpName") ? o.get("rpName").getAsString() : "";
                    p.job = o.has("job") ? o.get("job").getAsString() : "";
                    profiles.put(UUID.fromString(o.get("uuid").getAsString()), p);
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement identités.", e);
        }
    }
}
