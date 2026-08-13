package fr.dinar.scoreboard;

import fr.dinar.DinarMod;
import fr.dinar.economy.EconomyManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class BalanceScoreboard {
    public static final String OBJECTIVE_NAME = "dinar_bal";

    private ScoreboardObjective objective;
    private final Set<String> tracked = new HashSet<>();

    public void start(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        objective = sb.getNullableObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = sb.addObjective(OBJECTIVE_NAME, ScoreboardCriterion.DUMMY,
                    Text.literal("§6§l" + DinarMod.config.scoreboard.title),
                    ScoreboardCriterion.RenderType.INTEGER, false, null);
        }
        sb.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
        update(DinarMod.economy);
    }

    public void stop() {
        MinecraftServer server = DinarMod.economy.getServer();
        if (server == null) return;
        Scoreboard sb = server.getScoreboard();
        if (objective != null) {
            sb.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
        }
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            removePlayer(p);
        }
        objective = null;
        tracked.clear();
    }

    public void update(EconomyManager eco) {
        MinecraftServer server = eco.getServer();
        if (server == null || objective == null) return;
        Scoreboard sb = server.getScoreboard();
        Set<String> now = new HashSet<>();
        server.getPlayerManager().getPlayerList().stream()
                .sorted(Comparator.comparingDouble((ServerPlayerEntity p) -> -eco.balance(p.getUuid())))
                .forEach(p -> {
                    String name = p.getGameProfile().getName();
                    now.add(name);
                    int score = (int) Math.round(eco.balance(p.getUuid()));
                    sb.getOrCreateScore(p, objective).setScore(score);
                });
        for (String old : tracked) {
            if (!now.contains(old)) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(old);
                if (p != null) {
                    sb.removeScore(p, objective);
                }
            }
        }
        tracked.clear();
        tracked.addAll(now);
    }

    public void removePlayer(ServerPlayerEntity player) {
        MinecraftServer server = DinarMod.economy.getServer();
        if (server == null) return;
        Scoreboard sb = server.getScoreboard();
        String name = player.getGameProfile().getName();
        if (objective != null) {
            sb.removeScore(player, objective);
        }
        tracked.remove(name);
    }

    public boolean isActive() {
        return objective != null;
    }
}
