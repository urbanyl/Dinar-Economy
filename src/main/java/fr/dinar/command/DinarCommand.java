package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class DinarCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dinar")
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
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lDinar Economy §r§7v" + "1.0.0"
                + " §7- Mod d'économie. §e/bal §7pour votre solde, §e/pay §7pour payer, §e/dmd §7pour demander."), false);
        return 1;
    }

    private DinarCommand() {}
}
