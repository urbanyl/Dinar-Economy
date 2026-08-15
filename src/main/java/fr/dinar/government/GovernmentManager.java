package fr.dinar.government;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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

public class GovernmentManager {
    private UUID leaderUuid;
    private String leaderName;
    private final List<Law> laws = new ArrayList<>();
    private final Map<Integer, VoteSession> activeVotes = new HashMap<>();
    private String decree = "";
    private int nextLawId = 1;
    private boolean titleEnabled = true;
    private long voteDurationSeconds = 300;
    private int requiredVotes = 3;

    private MinecraftServer server;
    private Path dataFile;

    // ------------------------------------------------------------------
    // Leader
    // ------------------------------------------------------------------

    public boolean hasLeader() {
        return leaderUuid != null;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeader(UUID uuid, String name) {
        this.leaderUuid = uuid;
        this.leaderName = name;
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §e%s §7a été nommé §6Calife §7du serveur.", name));
        showTitle(DinarLang.t("§6§lNouveau Calife"), "§e" + name, 10, 40, 10);
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("%s a été nommé Calife du serveur", name));
    }

    public void removeLeader() {
        if (leaderUuid == null) return;
        String old = leaderName;
        leaderUuid = null;
        leaderName = null;
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §c%s §7n'est plus Calife.", old));
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("%s n'est plus Calife", old));
    }

    public boolean isLeader(UUID uuid) {
        return leaderUuid != null && leaderUuid.equals(uuid);
    }

    // ------------------------------------------------------------------
    // Lois
    // ------------------------------------------------------------------

    public Law proposeLaw(ServerPlayerEntity author, String title, String content) {
        Law law = new Law(nextLawId++, title, content, author.getUuid(), author.getGameProfile().getName());
        laws.add(law);
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §e%s §apropose une loi §e» §f%s",
                author.getGameProfile().getName(), title));
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("%s propose la loi «%s» : %s",
                author.getGameProfile().getName(), title, content));
        return law;
    }

    public Law enactLaw(ServerPlayerEntity author, String title, String content) {
        Law law = new Law(nextLawId++, title, content, author.getUuid(), author.getGameProfile().getName());
        law.status = "ADOPTED";
        law.decidedAt = System.currentTimeMillis();
        laws.add(law);
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §e%s §apromulgue la loi §e» §f%s",
                author.getGameProfile().getName(), title));
        if (titleEnabled) {
            showTitle(DinarLang.t("§6§lLoi Promulguée"), "§f" + title, 10, 60, 10);
        }
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("%s promulgue la loi «%s»",
                author.getGameProfile().getName(), title));
        return law;
    }

    public Law getLaw(int id) {
        return laws.stream().filter(l -> l.id == id).findFirst().orElse(null);
    }

    public List<Law> getAllLaws() {
        return laws;
    }

    public List<Law> getAdoptedLaws() {
        return laws.stream().filter(Law::isAdopted).toList();
    }

    public List<Law> getPendingLaws() {
        return laws.stream().filter(Law::isPending).toList();
    }

    public void rejectLaw(int id) {
        Law law = getLaw(id);
        if (law == null) return;
        law.status = "REJECTED";
        law.decidedAt = System.currentTimeMillis();
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §cLa loi §e» §f%s §ca été rejetée.", law.title));
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("La loi «%s» a été rejetée", law.title));
    }

    // ------------------------------------------------------------------
    // Votes
    // ------------------------------------------------------------------

    public boolean startVote(int lawId) {
        Law law = getLaw(lawId);
        if (law == null || !law.isPending()) return false;
        if (activeVotes.containsKey(lawId)) return false;
        VoteSession vs = new VoteSession(lawId, System.currentTimeMillis(), voteDurationSeconds, requiredVotes);
        activeVotes.put(lawId, vs);
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §eVote ouvert §7pour la loi §f» %s §7(/loi voter)", law.title));
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("Vote ouvert pour la loi «%s»", law.title));
        return true;
    }

    public VoteSession getActiveVote(int lawId) {
        VoteSession vs = activeVotes.get(lawId);
        if (vs == null) return null;
        if (vs.isExpired() && !vs.isResolved()) {
            resolveVote(vs);
            activeVotes.remove(lawId);
            return null;
        }
        if (vs.isResolved()) {
            activeVotes.remove(lawId);
            return null;
        }
        return vs;
    }

    public VoteSession getActiveVoteFor(UUID playerUuid) {
        return activeVotes.values().stream()
                .filter(vs -> !vs.hasVoted(playerUuid) && !vs.isExpired())
                .findFirst().orElse(null);
    }

    public boolean vote(UUID uuid, int lawId, boolean yes) {
        VoteSession vs = getActiveVote(lawId);
        if (vs == null || vs.hasVoted(uuid)) return false;
        vs.vote(uuid, yes);
        Law law = getLaw(lawId);
        String voterName = resolveName(uuid);
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §e%s §avote %s §7pour §f» %s",
                voterName, yes ? "§aOUI" : "§cNON", law != null ? law.title : "#" + lawId));
        if (vs.getTotal() >= vs.getRequiredVotes()) {
            resolveVote(vs);
            activeVotes.remove(lawId);
        }
        return true;
    }

    private void resolveVote(VoteSession vs) {
        if (vs.isResolved()) return;
        vs.setResolved(true);
        Law law = getLaw(vs.lawId);
        if (law == null) return;
        law.yesVotes = vs.getYes();
        law.noVotes = vs.getNo();
        for (UUID v : vs.getYesVoters()) law.voters.add(v + ":YES");
        for (UUID v : vs.getNoVoters()) law.voters.add(v + ":NO");
        if (vs.isPassed()) {
            law.status = "ADOPTED";
            law.decidedAt = System.currentTimeMillis();
            broadcast(DinarLang.t("§6§lCaliphat §r§7» §aLa loi §e» §f%s §aest §6ADOPTÉE §7(%s OUI, %s NON)",
                    law.title, vs.getYes(), vs.getNo()));
            if (titleEnabled) {
                showTitle(DinarLang.t("§6§lLoi Adoptée"), "§f" + law.title, 10, 60, 10);
            }
            DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("La loi «%s» a été adoptée (%s OUI / %s NON)",
                    law.title, vs.getYes(), vs.getNo()));
        } else {
            law.status = "REJECTED";
            law.decidedAt = System.currentTimeMillis();
            broadcast(DinarLang.t("§6§lCaliphat §r§7» §cLa loi §e» §f%s §cest §cREJETÉE §7(%s OUI, %s NON)",
                    law.title, vs.getYes(), vs.getNo()));
            if (titleEnabled) {
                showTitle(DinarLang.t("§c§lLoi Rejetée"), "§f" + law.title, 10, 40, 10);
            }
            DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("La loi «%s» a été rejetée (%s OUI / %s NON)",
                    law.title, vs.getYes(), vs.getNo()));
        }
    }

    // ------------------------------------------------------------------
    // Décrets
    // ------------------------------------------------------------------

    public void setDecree(ServerPlayerEntity author, String text) {
        this.decree = text;
        broadcast(DinarLang.t("§6§lCaliphat §r§7» §e%s §apublie un décret §7» §f%s",
                author.getGameProfile().getName(), text));
        if (titleEnabled) {
            showTitle(DinarLang.t("§6§lDécret du Calife"), "§f" + text, 10, 60, 10);
        }
        DinarMod.rpLog.log("GOUVERNEMENT", DinarLang.t("%s publie le décret «%s»",
                author.getGameProfile().getName(), text));
    }

    public String getDecree() {
        return decree;
    }

    // ------------------------------------------------------------------
    // Scoreboard
    // ------------------------------------------------------------------

    public int getAdoptedLawCount() {
        return (int) laws.stream().filter(Law::isAdopted).count();
    }

    // ------------------------------------------------------------------
    // Config
    // ------------------------------------------------------------------

    public boolean isTitleEnabled() { return titleEnabled; }
    public void setTitleEnabled(boolean enabled) { this.titleEnabled = enabled; }

    public long getVoteDurationSeconds() { return voteDurationSeconds; }
    public void setVoteDurationSeconds(long seconds) { this.voteDurationSeconds = seconds; }

    public int getRequiredVotes() { return requiredVotes; }
    public void setRequiredVotes(int n) { this.requiredVotes = n; }

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    public void broadcast(String msg) {
        MinecraftServer s = getServer();
        if (s == null) return;
        Text text = Text.literal(msg);
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.sendMessage(text, false);
        }
    }

    public void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        MinecraftServer s = getServer();
        if (s == null) return;
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.literal(""), false);
            p.sendMessage(Text.literal(title), false);
            p.sendMessage(Text.literal(subtitle), false);
            p.sendMessage(Text.literal(""), false);
        }
    }

    public void showActionBar(String msg) {
        MinecraftServer s = getServer();
        if (s == null) return;
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.literal(msg), false);
        }
    }

    // ------------------------------------------------------------------
    // Util
    // ------------------------------------------------------------------

    public String resolveName(UUID uuid) {
        MinecraftServer s = getServer();
        if (s != null) {
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(uuid);
            if (p != null) return p.getGameProfile().getName();
        }
        return leaderName != null && leaderUuid != null && leaderUuid.equals(uuid) ? leaderName : uuid.toString().substring(0, 8);
    }

    public MinecraftServer getServer() {
        return server;
    }

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    public void onServerStart(MinecraftServer server) {
        this.server = server;
        this.dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("government.json");
        load();
        DinarMod.LOGGER.info("[Dinar] Gouvernement chargé : calife={}, lois={}", leaderName, laws.size());
    }

    public void onServerStop() {
        save();
        this.server = null;
    }

    public void tick() {
        List<Integer> expired = new ArrayList<>();
        for (Map.Entry<Integer, VoteSession> e : activeVotes.entrySet()) {
            VoteSession vs = e.getValue();
            if (vs.isExpired() && !vs.isResolved()) {
                resolveVote(vs);
                expired.add(e.getKey());
            } else if (vs.isResolved()) {
                expired.add(e.getKey());
            }
        }
        for (int id : expired) {
            activeVotes.remove(id);
        }
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public void save() {
        if (server == null || dataFile == null) return;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("leaderUuid", leaderUuid != null ? leaderUuid.toString() : "");
            root.addProperty("leaderName", leaderName != null ? leaderName : "");
            root.addProperty("decree", decree);
            root.addProperty("nextLawId", nextLawId);
            root.addProperty("titleEnabled", titleEnabled);
            root.addProperty("voteDurationSeconds", voteDurationSeconds);
            root.addProperty("requiredVotes", requiredVotes);

            JsonArray arr = new JsonArray();
            for (Law law : laws) {
                JsonObject o = new JsonObject();
                o.addProperty("id", law.id);
                o.addProperty("title", law.title);
                o.addProperty("content", law.content);
                o.addProperty("authorUuid", law.authorUuid != null ? law.authorUuid.toString() : "");
                o.addProperty("authorName", law.authorName != null ? law.authorName : "");
                o.addProperty("status", law.status);
                o.addProperty("createdAt", law.createdAt);
                o.addProperty("decidedAt", law.decidedAt);
                o.addProperty("yesVotes", law.yesVotes);
                o.addProperty("noVotes", law.noVotes);
                JsonArray voters = new JsonArray();
                for (String v : law.voters) voters.add(v);
                o.add("voters", voters);
                arr.add(o);
            }
            root.add("laws", arr);

            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("government.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Échec de la sauvegarde du gouvernement.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("leaderUuid") && !root.get("leaderUuid").getAsString().isEmpty()) {
                leaderUuid = UUID.fromString(root.get("leaderUuid").getAsString());
            }
            if (root.has("leaderName")) leaderName = root.get("leaderName").getAsString();
            if (root.has("decree")) decree = root.get("decree").getAsString();
            if (root.has("nextLawId")) nextLawId = root.get("nextLawId").getAsInt();
            if (root.has("titleEnabled")) titleEnabled = root.get("titleEnabled").getAsBoolean();
            if (root.has("voteDurationSeconds")) voteDurationSeconds = root.get("voteDurationSeconds").getAsLong();
            if (root.has("requiredVotes")) requiredVotes = root.get("requiredVotes").getAsInt();

            if (root.has("laws")) {
                for (var el : root.getAsJsonArray("laws")) {
                    JsonObject o = el.getAsJsonObject();
                    Law law = new Law(
                            o.get("id").getAsInt(),
                            o.get("title").getAsString(),
                            o.get("content").getAsString(),
                            o.has("authorUuid") && !o.get("authorUuid").getAsString().isEmpty()
                                    ? UUID.fromString(o.get("authorUuid").getAsString()) : null,
                            o.has("authorName") ? o.get("authorName").getAsString() : null
                    );
                    law.status = o.has("status") ? o.get("status").getAsString() : "PENDING";
                    law.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : 0;
                    law.decidedAt = o.has("decidedAt") ? o.get("decidedAt").getAsLong() : 0;
                    law.yesVotes = o.has("yesVotes") ? o.get("yesVotes").getAsInt() : 0;
                    law.noVotes = o.has("noVotes") ? o.get("noVotes").getAsInt() : 0;
                    if (o.has("voters")) {
                        for (var v : o.getAsJsonArray("voters")) {
                            law.voters.add(v.getAsString());
                        }
                    }
                    laws.add(law);
                    if (law.id >= nextLawId) nextLawId = law.id + 1;
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Échec du chargement du gouvernement.", e);
        }
    }
}
