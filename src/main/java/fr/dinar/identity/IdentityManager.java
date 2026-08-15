package fr.dinar.identity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
        if (rpName == null || rpName.isBlank()) return DinarLang.t("§cLe prénom RP ne peut pas être vide.");
        if (rpName.length() > 24) return DinarLang.t("§cPrénom RP trop long (§f24 caractères max§c).");
        get(uuid).rpName = rpName.trim();
        save();
        return null;
    }

    public String setJob(UUID uuid, String job) {
        if (job == null || job.isBlank()) return DinarLang.t("§cLe métier ne peut pas être vide.");
        if (job.length() > 40) return DinarLang.t("§cMétier trop long (§f40 caractères max§c).");
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

    public String describeCard(UUID ownerUuid) {
        RpProfile p = profiles.get(ownerUuid);
        if (p == null || !p.isComplete()) return null;
        String real = DinarMod.economy.accountName(ownerUuid);
        String idShort = ownerUuid.toString().replace("-", "").substring(0, 8).toUpperCase();
        return DinarLang.t("§6§lCarte d'identité §r§7» %s", formatName(ownerUuid)) + "\n"
                + "§8━━━━━━━━━━━━━━━━━━━\n"
                + DinarLang.t("§7Prénom RP : §e%s", p.rpName) + "\n"
                + DinarLang.t("§7Métier : §e%s", p.job) + "\n"
                + DinarLang.t("§7Pseudo : §f%s", real) + "\n"
                + DinarLang.t("§7N° d'identité : §f%s", idShort);
    }

    public ItemStack createCard(UUID ownerUuid) {
        if (!isComplete(ownerUuid)) return null;
        RpProfile p = profiles.get(ownerUuid);
        String real = DinarMod.economy.accountName(ownerUuid);
        String idShort = ownerUuid.toString().replace("-", "").substring(0, 8).toUpperCase();

        ItemStack card = new ItemStack(DinarMod.IDENTITY_CARD);
        NbtCompound data = new NbtCompound();
        data.putString("owner", ownerUuid.toString());
        card.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        card.set(DataComponentTypes.CUSTOM_NAME, DinarLang.text("§6Carte d'identité §7» §e%s", p.rpName));
        card.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§8━━━━━━━━━━━━━━━━━━━"),
                DinarLang.text("§7Prénom RP : §f%s", p.rpName),
                DinarLang.text("§7Métier : §f%s", p.job),
                DinarLang.text("§7Pseudo : §f%s", real),
                DinarLang.text("§7N° : §f%s", idShort),
                Text.literal("§8━━━━━━━━━━━━━━━━━━━"),
                DinarLang.text("§7Carte officielle de la ville."),
                DinarLang.text("§7Présentez-la lors des contrôles RP.")
        )));
        card.setCount(1);
        return card;
    }

    public String giveCard(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!DinarMod.accounts.isLoggedIn(uuid)) {
            return DinarLang.t("§cConnectez-vous d'abord (§f/login§c).");
        }
        ItemStack card = createCard(uuid);
        if (card == null) {
            return DinarLang.t("§cComplétez votre identité d'abord : §f/identite prenom <prénom> §7puis §f/identite metier <métier>§c.");
        }
        if (hasCard(player)) {
            return null;
        }
        boolean added = player.getInventory().insertStack(card);
        if (!added) {
            player.dropItem(card, true);
            return DinarLang.t("§aCarte d'identité posée à vos pieds §7(inventaire plein)§a.");
        }
        return null;
    }

    public boolean hasCard(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        for (ItemStack stack : player.getInventory().main) {
            if (isOwnCard(stack, uuid)) return true;
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (isOwnCard(stack, uuid)) return true;
        }
        return false;
    }

    private boolean isOwnCard(ItemStack stack, UUID uuid) {
        if (stack.isEmpty() || !stack.isOf(DinarMod.IDENTITY_CARD)) return false;
        return uuid.equals(IdentityCardItem.readOwner(stack));
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
