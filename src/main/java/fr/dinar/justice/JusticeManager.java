package fr.dinar.justice;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import fr.dinar.logs.DiscordWebhook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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

public class JusticeManager {
    private final Map<UUID, List<RecordEntry>> records = new HashMap<>();
    private final Map<Integer, CaseEntry> cases = new HashMap<>();
    private int nextCaseId = 1;
    private Path dataFile;

    public void onServerStart(MinecraftServer server) {
        dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("justice.json");
        records.clear();
        cases.clear();
        load();
    }

    public void onServerStop() {
        save();
    }

    // ------------------------------------------------------------------
    // Casier judiciaire
    // ------------------------------------------------------------------

    public List<RecordEntry> getRecord(UUID uuid) {
        return records.getOrDefault(uuid, List.of());
    }

    public boolean hasRecord(UUID uuid) {
        return !getRecord(uuid).isEmpty();
    }

    public List<UUID> getPlayersWithRecords() {
        List<UUID> out = new ArrayList<>();
        for (Map.Entry<UUID, List<RecordEntry>> e : records.entrySet()) {
            if (!e.getValue().isEmpty()) out.add(e.getKey());
        }
        return out;
    }

    public void addRecord(UUID accused, String type, String detail, String officerName, String extra) {
        RecordEntry entry = new RecordEntry();
        entry.type = type;
        entry.date = System.currentTimeMillis();
        entry.detail = detail;
        entry.authorName = officerName;
        entry.extra = extra;
        records.computeIfAbsent(accused, k -> new ArrayList<>()).add(entry);
    }

    // ------------------------------------------------------------------
    // Affaires ouvertes
    // ------------------------------------------------------------------

    public CaseEntry createCase(UUID accused, String accusedName, String motif, String officerName) {
        CaseEntry c = new CaseEntry();
        c.id = nextCaseId++;
        c.accusedUuid = accused.toString();
        c.accusedName = accusedName;
        c.motif = motif;
        c.policeName = officerName;
        c.createdAt = System.currentTimeMillis();
        cases.put(c.id, c);
        return c;
    }

    public CaseEntry getCase(int id) {
        return cases.get(id);
    }

    public CaseEntry removeCase(int id) {
        return cases.remove(id);
    }

    public List<CaseEntry> getOpenCases() {
        return new ArrayList<>(cases.values());
    }

    // ------------------------------------------------------------------
    // Actions judiciaires (journal + webhook)
    // ------------------------------------------------------------------

    public void addOffense(ServerPlayerEntity officer, UUID accused, String accusedName, String motif) {
        addRecord(accused, "DELIT", motif, officer.getGameProfile().getName(), null);
        String officerName = officer.getGameProfile().getName();
        DinarMod.rpLog.log("JUSTICE", DinarLang.t("%s a enregistré un délit pour %s : %s", officerName, accusedName, motif));
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title(DinarLang.t("⚖️ Délit enregistré"))
                .field(DinarLang.t("Suspect"), accusedName)
                .field(DinarLang.t("Motif"), motif)
                .field(DinarLang.t("Officier"), officerName)
                .color(0xE74C3C)
                .footer("Dinar RP"));
        ServerPlayerEntity target = DinarMod.economy.online(accused);
        if (target != null) {
            target.sendMessage(DinarLang.text("§c§lDÉLIT §r§7» §fUn délit a été enregistré contre vous : §e%s", motif), false);
        }
    }

    public void issueWarrant(ServerPlayerEntity officer, UUID accused, String accusedName, String motif) {
        addRecord(accused, "MANDAT", motif, officer.getGameProfile().getName(), null);
        String officerName = officer.getGameProfile().getName();
        DinarMod.rpLog.log("JUSTICE", DinarLang.t("%s a émis un mandat d'arrêt contre %s : %s", officerName, accusedName, motif));
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title(DinarLang.t("📜 Mandat d'arrêt"))
                .field(DinarLang.t("Suspect"), accusedName)
                .field(DinarLang.t("Motif"), motif)
                .field(DinarLang.t("Émis par"), officerName)
                .color(0xF39C12)
                .footer("Dinar RP"));
        DinarMod.government.broadcast(DinarLang.t("§c§lMANDAT D'ARRÊT §r§7» §e%s §7est recherché pour : §f%s",
                accusedName, motif));
        DinarMod.government.showTitle(DinarLang.t("§c§lMANDAT D'ARRÊT"),
                "§e" + accusedName + " §7— §f" + motif, 10, 60, 10);
    }

    public void recordJudgment(ServerPlayerEntity officer, UUID accused, String accusedName,
                               String detail, String penalty) {
        addRecord(accused, "JUGEMENT", detail, officer.getGameProfile().getName(), penalty);
        String officerName = officer.getGameProfile().getName();
        DinarMod.rpLog.log("JUSTICE", DinarLang.t("%s a rendu un jugement pour %s : %s → %s",
                officerName, accusedName, detail, penalty));
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title(DinarLang.t("⚖️ Jugement rendu"))
                .field(DinarLang.t("Accusé"), accusedName)
                .field(DinarLang.t("Délit"), detail)
                .field(DinarLang.t("Peine"), penalty)
                .field(DinarLang.t("Juge"), officerName)
                .color(0x9B59B6)
                .footer("Dinar RP"));
        ServerPlayerEntity target = DinarMod.economy.online(accused);
        if (target != null) {
            target.sendMessage(DinarLang.text("§d§lJUGEMENT §r§7» §f%s §7→ §e%s", detail, penalty), false);
        }
    }

    public void openCase(ServerPlayerEntity officer, UUID accused, String accusedName, String motif) {
        CaseEntry c = createCase(accused, accusedName, motif, officer.getGameProfile().getName());
        String officerName = officer.getGameProfile().getName();
        DinarMod.rpLog.log("JUSTICE", DinarLang.t("%s a ouvert l'affaire #%s contre %s : %s",
                officerName, c.id, accusedName, motif));
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title(DinarLang.t("🗂️ Affaire ouverte #%s", c.id))
                .field(DinarLang.t("Suspect"), accusedName)
                .field(DinarLang.t("Motif"), motif)
                .field(DinarLang.t("Officier"), officerName)
                .color(0x3498DB)
                .footer("Dinar RP"));
        DinarMod.government.broadcast(DinarLang.t("§b§lAFFAIRE #%s §r§7» §e%s §7est soupçonné de : §f%s",
                c.id, accusedName, motif));
    }

    public void closeCase(ServerPlayerEntity officer, int id) {
        CaseEntry c = removeCase(id);
        if (c == null) return;
        String officerName = officer.getGameProfile().getName();
        DinarMod.rpLog.log("JUSTICE", DinarLang.t("%s a clôturé l'affaire #%s de %s", officerName, id, c.accusedName));
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title(DinarLang.t("✅ Affaire clôturée #%s", id))
                .field(DinarLang.t("Suspect"), c.accusedName)
                .field(DinarLang.t("Clôturé par"), officerName)
                .color(0x27AE60)
                .footer("Dinar RP"));
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public void save() {
        if (dataFile == null) return;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("nextCaseId", nextCaseId);

            JsonArray recs = new JsonArray();
            for (Map.Entry<UUID, List<RecordEntry>> e : records.entrySet()) {
                for (RecordEntry r : e.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("uuid", e.getKey().toString());
                    o.addProperty("type", r.type);
                    o.addProperty("date", r.date);
                    o.addProperty("detail", r.detail);
                    o.addProperty("authorName", r.authorName);
                    o.addProperty("extra", r.extra);
                    recs.add(o);
                }
            }
            root.add("records", recs);

            JsonArray casesArr = new JsonArray();
            for (CaseEntry c : cases.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id);
                o.addProperty("accusedUuid", c.accusedUuid);
                o.addProperty("accusedName", c.accusedName);
                o.addProperty("motif", c.motif);
                o.addProperty("policeName", c.policeName);
                o.addProperty("createdAt", c.createdAt);
                casesArr.add(o);
            }
            root.add("cases", casesArr);

            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("justice.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Echec sauvegarde justice.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("nextCaseId")) nextCaseId = root.get("nextCaseId").getAsInt();

            if (root.has("records")) {
                for (var el : root.getAsJsonArray("records")) {
                    JsonObject o = el.getAsJsonObject();
                    RecordEntry r = new RecordEntry();
                    r.type = o.has("type") ? o.get("type").getAsString() : "DELIT";
                    r.date = o.has("date") ? o.get("date").getAsLong() : 0;
                    r.detail = o.has("detail") ? o.get("detail").getAsString() : "";
                    r.authorName = o.has("authorName") ? o.get("authorName").getAsString() : "";
                    r.extra = o.has("extra") ? o.get("extra").getAsString() : null;
                    records.computeIfAbsent(UUID.fromString(o.get("uuid").getAsString()), k -> new ArrayList<>()).add(r);
                }
            }

            if (root.has("cases")) {
                for (var el : root.getAsJsonArray("cases")) {
                    JsonObject o = el.getAsJsonObject();
                    CaseEntry c = new CaseEntry();
                    c.id = o.get("id").getAsInt();
                    c.accusedUuid = o.has("accusedUuid") ? o.get("accusedUuid").getAsString() : "";
                    c.accusedName = o.has("accusedName") ? o.get("accusedName").getAsString() : "";
                    c.motif = o.has("motif") ? o.get("motif").getAsString() : "";
                    c.policeName = o.has("policeName") ? o.get("policeName").getAsString() : "";
                    c.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : 0;
                    cases.put(c.id, c);
                    if (c.id >= nextCaseId) nextCaseId = c.id + 1;
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Echec chargement justice.", e);
        }
    }
}
