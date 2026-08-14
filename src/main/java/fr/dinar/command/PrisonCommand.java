package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.justice.PrisonManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

public final class PrisonCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("prison")
                .then(CommandManager.literal("setpos")
                        .requires(PrisonCommand::police)
                        .executes(PrisonCommand::setPos))
                .then(CommandManager.literal("incarcere")
                        .requires(PrisonCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1))
                                        .executes(PrisonCommand::imprison))))
                .then(CommandManager.literal("libere")
                        .requires(PrisonCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(PrisonCommand::release)))
                .then(CommandManager.literal("info").executes(PrisonCommand::info))
                .executes(PrisonCommand::help));

        dispatcher.register(CommandManager.literal("mandatdarret")
                .requires(PrisonCommand::police)
                .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                        .then(CommandManager.argument("motif", StringArgumentType.greedyString())
                                .executes(DossierCommand::mandat))));
    }

    public static boolean police(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        return src.hasPermissionLevel(2)
                || DinarMod.government.isLeader(p != null ? p.getUuid() : null)
                || (p != null && DinarMod.police.isPolice(p.getUuid()));
    }

    private static int setPos(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        DinarMod.prison.setLocation(officer);
        DinarMod.rpLog.log("PRISON", officer.getName().getString() + " a défini la position de la prison.");
        ctx.getSource().sendFeedback(() -> Text.literal("§aPosition de la prison définie."), false);
        return 1;
    }

    private static int imprison(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), name);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        if (DinarMod.prison.isImprisoned(ref.uuid())) {
            ctx.getSource().sendError(Text.literal("§c" + ref.displayName() + " est déjà en prison."));
            return 0;
        }
        int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
        String err = DinarMod.prison.imprison(ref.uuid(), ref.displayName(), minutes,
                officer.getName().getString());
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§a" + ref.displayName() + " incarcéré pour §e"
                + minutes + " min§a."), false);
        return 1;
    }

    private static int release(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), name);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        String err = DinarMod.prison.release(ref.uuid(), officer.getName().getString());
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§a" + ref.displayName() + " libéré."), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        Map<UUID, PrisonManager.PrisonSession> sessions = DinarMod.prison.getSessions();
        if (sessions.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7La prison est vide."), false);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§8[§cPrison§8] §fDétenus §7(§f" + sessions.size() + "§7)"), false);
        for (Map.Entry<UUID, PrisonManager.PrisonSession> e : sessions.entrySet()) {
            long rem = DinarMod.prison.remainingSeconds(e.getKey());
            String name = e.getValue().name;
            ctx.getSource().sendFeedback(() -> Text.literal("§f• §e" + name + " §7— reste §e"
                    + (rem / 60) + "m " + (rem % 60) + "s"), false);
        }
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lPrison §7» §f/prison setpos "
                + "§7| §f/prison incarcere <joueur> <minutes> §7| §f/prison libere <joueur> "
                + "§7| §f/prison info §7| §f/mandatdarret <joueur> <motif>"), false);
        return 1;
    }

    private PrisonCommand() {}
}
