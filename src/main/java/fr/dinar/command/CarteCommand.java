package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.lang.DinarLang;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CarteCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("carte")
                .then(CommandManager.literal("donner")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(CarteCommand::donner)))
                .executes(CarteCommand::moi));
    }

    private static int moi(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        String err = DinarMod.identity.giveCard(player);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        if (DinarMod.identity.hasCard(player)) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Vous avez déjà votre carte d'identité."), false);
        } else {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§6📛 §fCarte d'identité donnée."), false);
        }
        return 1;
    }

    private static int donner(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        if (!DinarMod.identity.isComplete(player.getUuid())) {
            ctx.getSource().sendError(Text.literal(DinarLang.t("§cComplétez d'abord votre identité : §f/identite")));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal(DinarLang.t("§cJoueur introuvable : §e%s", targetName)));
            return 0;
        }
        if (ref.online() == null) {
            ctx.getSource().sendError(Text.literal(DinarLang.t("§cLe joueur §e%s §cn'est pas en ligne.", ref.displayName())));
            return 0;
        }
        ItemStack card = DinarMod.identity.createCard(player.getUuid());
        if (card == null) {
            ctx.getSource().sendError(Text.literal(DinarLang.t("§cImpossible de créer votre carte.")));
            return 0;
        }
        boolean added = ref.online().getInventory().insertStack(card.copy());
        if (!added) {
            ref.online().dropItem(card, true);
            ctx.getSource().sendFeedback(() -> DinarLang.text("§aCarte posée aux pieds de §e%s §7(inventaire plein)§a.",
                    ref.displayName()), false);
        } else {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§aVotre carte d'identité a été donnée à §e%s§a.",
                    ref.displayName()), false);
            ref.online().sendMessage(DinarLang.text("§6📛 §e%s §fa vous remis sa carte d'identité.",
                    player.getGameProfile().getName()), false);
        }
        return 1;
    }

    private CarteCommand() {}
}
