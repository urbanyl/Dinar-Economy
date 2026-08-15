package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class RegisterCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("register")
                .then(CommandManager.argument("motdepasse", StringArgumentType.greedyString())
                        .executes(RegisterCommand::register)));
        dispatcher.register(CommandManager.literal("login")
                .then(CommandManager.argument("motdepasse", StringArgumentType.greedyString())
                        .executes(RegisterCommand::login)));
        dispatcher.register(CommandManager.literal("logout").executes(RegisterCommand::logout));
    }

    private static int register(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        String password = StringArgumentType.getString(ctx, "motdepasse");
        String err = DinarMod.accounts.register(player.getUuid(), player.getGameProfile().getName(), password);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        player.sendMessage(DinarLang.text("§a§lCOMPTE CRÉÉ §r§7» §fCompte enregistré pour §e%s§f. Bienvenue !",
                player.getGameProfile().getName()), false);
        afterLogin(player);
        return 1;
    }

    private static int login(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        String password = StringArgumentType.getString(ctx, "motdepasse");
        String err = DinarMod.accounts.login(player.getUuid(), password);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        player.sendMessage(DinarLang.text("§a§lCONNECTÉ §r§7» §fBienvenue §e%s§f.",
                player.getGameProfile().getName()), false);
        afterLogin(player);
        return 1;
    }

    private static void afterLogin(ServerPlayerEntity player) {
        if (!DinarMod.identity.isComplete(player.getUuid())) {
            player.sendMessage(DinarLang.text("§6Votre identité n'est pas encore définie :"), false);
            player.sendMessage(DinarLang.text("§f/identite prenom <prénom RP> §7puis §f/identite metier <métier>"), false);
        } else {
            String err = DinarMod.identity.giveCard(player);
            if (err != null) {
                player.sendMessage(Text.literal(err), false);
            } else if (DinarMod.identity.hasCard(player)) {
                player.sendMessage(DinarLang.text("§6📛 §fVous avez déjà votre carte d'identité."), false);
            } else {
                player.sendMessage(DinarLang.text("§6📛 §fVotre carte d'identité a été délivrée."), false);
            }
        }
    }

    private static int logout(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        String err = DinarMod.accounts.logout(player.getUuid());
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        player.sendMessage(DinarLang.text("§7Vous êtes déconnecté. §f/login <mot de passe> §7pour revenir."), false);
        return 1;
    }

    private RegisterCommand() {}
}
