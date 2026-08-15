package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PoliceCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("police")
                .requires(PoliceCommand::admin)
                .then(CommandManager.literal("ajouter")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(PoliceCommand::add)))
                .then(CommandManager.literal("retirer")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(PoliceCommand::remove)))
                .then(CommandManager.literal("liste").executes(PoliceCommand::list))
                .executes(PoliceCommand::help));
    }

    private static boolean admin(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        return src.hasPermissionLevel(2)
                || DinarMod.government.isLeader(p != null ? p.getUuid() : null);
    }

    private static int add(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgument(ctx);
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), name);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        String err = DinarMod.police.add(ref.uuid(), ref.displayName());
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        DinarMod.rpLog.log("POLICE", ref.displayName() + " est désormais policier.");
        ctx.getSource().sendFeedback(() -> DinarLang.text("§a%s est maintenant policier.", ref.displayName()), false);
        DinarMod.government.broadcast("§9§lPOLICE §r§7» §e" + ref.displayName() + " §aest nommé policier.");
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgument(ctx);
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), name);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        String err = DinarMod.police.remove(ref.uuid(), ref.displayName());
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        DinarMod.rpLog.log("POLICE", ref.displayName() + " n'est plus policier.");
        ctx.getSource().sendFeedback(() -> DinarLang.text("§a%s n'est plus policier.", ref.displayName()), false);
        DinarMod.government.broadcast("§9§lPOLICE §r§7» §e" + ref.displayName() + " §cn'est plus policier.");
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        var officers = DinarMod.police.listOfficers();
        if (officers.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Aucun policier."), false);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§9§lPolice §r§7(§f%s§7)", officers.size()), false);
        for (String name : officers) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§f• §e%s", name), false);
        }
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lPolice §7» §f/police ajouter <joueur> "
                + "§7| §f/police retirer <joueur> §7| §f/police liste"), false);
        return 1;
    }

    private static String StringArgument(CommandContext<ServerCommandSource> ctx) {
        return com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "joueur");
    }

    private PoliceCommand() {}
}
