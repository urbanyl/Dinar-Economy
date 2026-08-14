package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class IdentiteCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("identite")
                .then(CommandManager.literal("prenom")
                        .then(CommandManager.argument("prenom", StringArgumentType.word())
                                .executes(IdentiteCommand::setPrenom)))
                .then(CommandManager.literal("metier")
                        .then(CommandManager.argument("metier", StringArgumentType.greedyString())
                                .executes(IdentiteCommand::setMetier)))
                .executes(IdentiteCommand::status));
    }

    private static int setPrenom(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        String prenom = StringArgumentType.getString(ctx, "prenom");
        String err = DinarMod.identity.setRpName(player.getUuid(), prenom);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        player.sendMessage(Text.literal("§aPrénom RP défini : §e" + prenom.trim() + "§a."), false);
        onPartDefined(player);
        return 1;
    }

    private static int setMetier(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        String metier = StringArgumentType.getString(ctx, "metier");
        String err = DinarMod.identity.setJob(player.getUuid(), metier);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        player.sendMessage(Text.literal("§aMétier défini : §e" + metier.trim() + "§a."), false);
        onPartDefined(player);
        return 1;
    }

    private static void onPartDefined(ServerPlayerEntity player) {
        if (!DinarMod.identity.isComplete(player.getUuid())) {
            player.sendMessage(Text.literal("§7Il vous manque : "
                    + (DinarMod.identity.get(player.getUuid()).rpName.isBlank()
                            ? "§f/identite prenom <prénom RP>" : "§f/identite metier <métier>")), false);
            return;
        }
        String formatted = DinarMod.identity.formatName(player.getUuid());
        player.sendMessage(Text.literal("§a§lIDENTITÉ COMPLÈTE §r§7» §fVotre nom RP sera : " + formatted), false);
        String err = DinarMod.identity.giveCard(player);
        if (err == null) {
            player.sendMessage(Text.literal("§6📛 §fVotre carte d'identité a été délivrée."), false);
        } else {
            player.sendMessage(Text.literal(err), false);
        }
        DinarMod.government.broadcast("§6§l[RP] §r§7» §fUn nouveau citoyen a rejoint la ville : " + formatted);
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        var profile = DinarMod.identity.get(player.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lVotre identité RP"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Prénom RP : §f"
                + (profile.rpName.isBlank() ? "§c(non défini)" : profile.rpName)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Métier : §f"
                + (profile.job.isBlank() ? "§c(non défini)" : profile.job)), false);
        if (profile.isComplete()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Affiché dans le chat : " + DinarMod.identity.formatName(player.getUuid())), false);
            ctx.getSource().sendFeedback(() -> Text.literal("§7Carte : §f/carte §7| §f/carte donner <joueur>"), false);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Complétez : §f/identite prenom <prénom RP> §7puis §f/identite metier <métier>"), false);
        }
        return 1;
    }

    private IdentiteCommand() {}
}
