package fr.dinar.mail;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

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

public class MailManager {
    private final Map<UUID, List<MailEntry>> inboxes = new ConcurrentHashMap<>();
    private int nextId = 1;
    private Path dataFile;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("mail.json");
        inboxes.clear();
        load();
    }

    public void onServerStop() {
        save();
    }

    public String send(UUID senderUuid, String senderName, UUID targetUuid, String targetName,
                       String message, double attachedMoney) {
        if (senderUuid.equals(targetUuid)) {
            return "§cImpossible de s'envoyer une lettre à soi-même.";
        }
        if (attachedMoney > 0 && !DinarMod.economy.deductFromBalance(senderUuid, senderName, attachedMoney)) {
            return "§cSolde insuffisant pour joindre " + DinarMod.economy.money(attachedMoney) + ".";
        }
        MailEntry entry = new MailEntry();
        entry.id = nextId++;
        entry.senderUuid = senderUuid.toString();
        entry.senderName = senderName;
        entry.receiverName = targetName;
        entry.message = message;
        entry.attachedMoney = attachedMoney;
        entry.moneyClaimed = attachedMoney <= 0;
        entry.read = false;
        entry.sentAt = System.currentTimeMillis();
        inboxes.computeIfAbsent(targetUuid, k -> new ArrayList<>()).add(entry);

        DinarMod.rpLog.log("MAIL", senderName + " a envoyé une lettre à " + targetName
                + (attachedMoney > 0 ? " avec " + DinarMod.economy.money(attachedMoney) : ""));

        ServerPlayerEntity target = DinarMod.economy.online(targetUuid);
        if (target != null) {
            target.sendMessage(Text.literal("§d✉ §fVous avez reçu une lettre de §e" + senderName
                    + " §7(§f/courrier liste§7)"), false);
        }
        return null;
    }

    public List<MailEntry> list(UUID uuid) {
        List<MailEntry> list = new ArrayList<>(inboxes.getOrDefault(uuid, new ArrayList<>()));
        list.sort(Comparator.comparingInt((MailEntry m) -> m.id).reversed());
        return list;
    }

    public MailEntry get(UUID uuid, int id) {
        for (MailEntry m : inboxes.getOrDefault(uuid, List.of())) {
            if (m.id == id) return m;
        }
        return null;
    }

    public int unreadCount(UUID uuid) {
        return (int) inboxes.getOrDefault(uuid, List.of()).stream().filter(m -> !m.read).count();
    }

    public String read(UUID uuid, int id) {
        MailEntry m = get(uuid, id);
        if (m == null) return "§cLettre introuvable.";
        m.read = true;
        if (m.attachedMoney > 0 && !m.moneyClaimed) {
            m.moneyClaimed = true;
            DinarMod.economy.add(uuid, DinarMod.economy.accountName(uuid), m.attachedMoney);
            DinarMod.rpLog.log("MAIL", DinarMod.economy.accountName(uuid) + " a récupéré "
                    + DinarMod.economy.money(m.attachedMoney) + " envoyés par " + m.senderName);
        }
        return null;
    }

    public String delete(UUID uuid, int id) {
        List<MailEntry> list = inboxes.getOrDefault(uuid, new ArrayList<>());
        for (MailEntry m : list) {
            if (m.id == id) {
                if (m.attachedMoney > 0 && !m.moneyClaimed) {
                    DinarMod.economy.add(UUID.fromString(m.senderUuid), m.senderName, m.attachedMoney);
                    DinarMod.rpLog.log("MAIL", m.senderName + " a récupéré "
                            + DinarMod.economy.money(m.attachedMoney) + " (lettre refusée par " + m.receiverName + ")");
                }
                list.remove(m);
                return null;
            }
        }
        return "§cLettre introuvable.";
    }

    public String cancel(UUID senderUuid, int id) {
        for (Map.Entry<UUID, List<MailEntry>> e : inboxes.entrySet()) {
            for (MailEntry m : e.getValue()) {
                if (m.id == id && m.senderUuid.equals(senderUuid.toString())) {
                    if (m.attachedMoney > 0 && !m.moneyClaimed) {
                        DinarMod.economy.add(senderUuid, DinarMod.economy.accountName(senderUuid), m.attachedMoney);
                        DinarMod.rpLog.log("MAIL", m.senderName + " a annulé sa lettre à " + m.receiverName
                                + " et récupéré " + DinarMod.economy.money(m.attachedMoney));
                    }
                    e.getValue().remove(m);
                    return null;
                }
            }
        }
        return "§cLettre introuvable ou déjà reçue.";
    }

    public void save() {
        if (dataFile == null) return;
        try {
            JsonArray arr = new JsonArray();
            for (Map.Entry<UUID, List<MailEntry>> e : inboxes.entrySet()) {
                for (MailEntry m : e.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("owner", e.getKey().toString());
                    o.addProperty("id", m.id);
                    o.addProperty("senderUuid", m.senderUuid);
                    o.addProperty("senderName", m.senderName);
                    o.addProperty("receiverName", m.receiverName);
                    o.addProperty("message", m.message);
                    o.addProperty("attachedMoney", m.attachedMoney);
                    o.addProperty("moneyClaimed", m.moneyClaimed);
                    o.addProperty("read", m.read);
                    o.addProperty("sentAt", m.sentAt);
                    arr.add(o);
                }
            }
            JsonObject root = new JsonObject();
            root.addProperty("nextId", nextId);
            root.add("mails", arr);
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("mail.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde courrier.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("nextId")) nextId = root.get("nextId").getAsInt();
            if (root.has("mails")) {
                for (var el : root.getAsJsonArray("mails")) {
                    JsonObject o = el.getAsJsonObject();
                    MailEntry m = new MailEntry();
                    m.id = o.get("id").getAsInt();
                    m.senderUuid = o.has("senderUuid") ? o.get("senderUuid").getAsString() : "";
                    m.senderName = o.has("senderName") ? o.get("senderName").getAsString() : "";
                    m.receiverName = o.has("receiverName") ? o.get("receiverName").getAsString() : "";
                    m.message = o.has("message") ? o.get("message").getAsString() : "";
                    m.attachedMoney = o.has("attachedMoney") ? o.get("attachedMoney").getAsDouble() : 0;
                    m.moneyClaimed = o.has("moneyClaimed") && o.get("moneyClaimed").getAsBoolean();
                    m.read = o.has("read") && o.get("read").getAsBoolean();
                    m.sentAt = o.has("sentAt") ? o.get("sentAt").getAsLong() : System.currentTimeMillis();
                    UUID owner = UUID.fromString(o.get("owner").getAsString());
                    inboxes.computeIfAbsent(owner, k -> new ArrayList<>()).add(m);
                    if (m.id >= nextId) nextId = m.id + 1;
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement courrier.", e);
        }
    }
}
