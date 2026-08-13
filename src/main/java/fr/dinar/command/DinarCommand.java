package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.gui.HelpScreenHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class DinarCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dinar")
                .then(CommandManager.literal("help").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null) {
                        HelpScreenHandler.open(player);
                    } else {
                        sendHelp(ctx);
                    }
                    return 1;
                }))
                .then(CommandManager.literal("scoreboard")
                        .then(CommandManager.literal("on").executes(ctx -> setScoreboard(ctx, true)))
                        .then(CommandManager.literal("off").executes(ctx -> setScoreboard(ctx, false)))
                        .then(CommandManager.literal("status").executes(DinarCommand::scoreboardStatus))
                        .requires(s -> s.hasPermissionLevel(2)))
                .then(CommandManager.literal("about").executes(DinarCommand::about)));
    }

    private static int setScoreboard(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        DinarMod.config.scoreboard.enabled = enabled;
        DinarMod.config.save();
        if (enabled) {
            DinarMod.economy.getScoreboard().start(ctx.getSource().getServer());
        } else {
            DinarMod.economy.getScoreboard().stop();
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§aScoreboard Dinar " + (enabled ? "activé." : "désactivé.")), true);
        return 1;
    }

    private static int scoreboardStatus(CommandContext<ServerCommandSource> ctx) {
        boolean active = DinarMod.economy.getScoreboard().isActive();
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lScoreboard Dinar §r§7» §e"
                + (active ? "activé" : "désactivé")), false);
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lDinar Economy §r§7v1.0.0 §7- Mod d'économie et de caliphat."), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/dinar help §7pour les commandes"), false);
        if (DinarMod.government.hasLeader()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Calife : §e" + DinarMod.government.getLeaderName()), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§7Lois adoptées : §e" + DinarMod.government.getAdoptedLawCount()), false);
        return 1;
    }

    private static void sendHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6§l══════ Dinar Help ══════"), false);
        src.sendFeedback(() -> Text.literal("§e/bal §7- Voir votre solde"), false);
        src.sendFeedback(() -> Text.literal("§e/pay <joueur> <montant> §7- Envoyer de l'argent"), false);
        src.sendFeedback(() -> Text.literal("§e/dmd <joueur> <montant> §7- Demander de l'argent"), false);
        src.sendFeedback(() -> Text.literal("§e/baltop §7- Classement"), false);
        src.sendFeedback(() -> Text.literal("§e/loi liste §7- Voir les lois"), false);
        src.sendFeedback(() -> Text.literal("§e/loi livre §7- Livre des lois adoptées"), false);
        src.sendFeedback(() -> Text.literal("§e/loi voter §7- Voter sur une loi"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat info §7- Info calife"), false);
        src.sendFeedback(() -> Text.literal("§6§l══════════════════════"), false);
    }

    private DinarCommand() {}
}
