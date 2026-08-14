package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
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
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        String err = DinarMod.identity.giveCard(player);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6📛 §fCarte d'identité donnée."), false);
        return 1;
    }

    private static int donner(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        if (!DinarMod.identity.isComplete(player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cComplétez d'abord votre identité : §f/identite"));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + targetName));
            return 0;
        }
        if (ref.online() == null) {
            ctx.getSource().sendError(Text.literal("§cLe joueur §e" + ref.displayName() + " §cn'est pas en ligne."));
            return 0;
        }
        ItemStack card = DinarMod.identity.createCard(player.getUuid());
        if (card == null) {
            ctx.getSource().sendError(Text.literal("§cImpossible de créer votre carte."));
            return 0;
        }
        boolean added = ref.online().getInventory().insertStack(card.copy());
        if (!added) {
            ref.online().dropItem(card, true);
            ctx.getSource().sendFeedback(() -> Text.literal("§aCarte posée aux pieds de §e" + ref.displayName()
                    + " §7(inventaire plein)§a."), false);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("§aVotre carte d'identité a été donnée à §e"
                    + ref.displayName() + "§a."), false);
            ref.online().sendMessage(Text.literal("§6📛 §e" + player.getGameProfile().getName()
                    + " §fa vous remis sa carte d'identité."), false);
        }
        return 1;
    }

    private CarteCommand() {}
}
