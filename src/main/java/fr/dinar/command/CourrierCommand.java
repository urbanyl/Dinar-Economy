package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.lang.DinarLang;
import fr.dinar.mail.MailEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class CourrierCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("courrier")
                .then(CommandManager.literal("envoyer")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(CourrierCommand::sendText))))
                .then(CommandManager.literal("donner")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0.01))
                                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                                .executes(CourrierCommand::sendMoney)))))
                .then(CommandManager.literal("liste").executes(CourrierCommand::list))
                .then(CommandManager.literal("lire")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(CourrierCommand::read)))
                .then(CommandManager.literal("supprimer")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(CourrierCommand::delete)))
                .then(CommandManager.literal("annuler")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(CourrierCommand::cancel)))
                .executes(CourrierCommand::help));
    }

    private static int sendText(CommandContext<ServerCommandSource> ctx) {
        return send(ctx, 0);
    }

    private static int sendMoney(CommandContext<ServerCommandSource> ctx) {
        return send(ctx, DoubleArgumentType.getDouble(ctx, "montant"));
    }

    private static int send(CommandContext<ServerCommandSource> ctx, double amount) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        String message = StringArgumentType.getString(ctx, "message");
        if (message.isBlank() || message.length() > 500) {
            ctx.getSource().sendError(DinarLang.text("§cMessage vide ou trop long (500 caractères max)."));
            return 0;
        }
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", targetName));
            return 0;
        }
        String err = DinarMod.mail.send(player.getUuid(), player.getName().getString(),
                ref.uuid(), ref.displayName(), message, amount);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        double finalAmount = amount;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§d✉ §aLa lettre a été envoyée à §e%s"
                + (finalAmount > 0 ? " §7avec §e%s" : ""), ref.displayName(),
                finalAmount > 0 ? DinarMod.economy.money(finalAmount) : ""), false);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        List<MailEntry> mails = DinarMod.mail.list(player.getUuid());
        if (mails.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Vous n'avez aucune lettre."), false);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lBoîte aux lettres §r§7(§f%s§7)", mails.size()), false);
        for (MailEntry m : mails) {
            String marker = m.read ? "§7" : "§e§lNON LUE §r";
            String moneyMark = m.attachedMoney > 0 ? " §7(+§e" + DinarMod.economy.money(m.attachedMoney) + "§7)" : "";
            String sender = m.senderName;
            String msg = truncate(m.message, 40);
            ctx.getSource().sendFeedback(() -> DinarLang.text("§f#%s §7%s§e%s%s §7» §f%s",
                    m.id, marker, sender, moneyMark, msg), false);
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Lire : §f/courrier lire <id>"), false);
        return 1;
    }

    private static int read(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        MailEntry m = DinarMod.mail.get(player.getUuid(), id);
        if (m == null) {
            ctx.getSource().sendError(DinarLang.text("§cLettre introuvable."));
            return 0;
        }
        boolean hadMoney = m.attachedMoney > 0 && !m.moneyClaimed;
        String err = DinarMod.mail.read(player.getUuid(), id);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        String sender = m.senderName;
        String msg = m.message;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§d✉ §6Lettre de §e%s §7(#%s)", sender, m.id), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7» §f%s", msg), false);
        if (hadMoney) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§a+ Vous avez récupéré §e%s §a(contenu de la lettre).",
                    DinarMod.economy.money(m.attachedMoney)), false);
        }
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        String err = DinarMod.mail.delete(player.getUuid(), id);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Lettre supprimée."), false);
        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cRéservé aux joueurs."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        String err = DinarMod.mail.cancel(player.getUuid(), id);
        if (err != null) {
            ctx.getSource().sendError(Text.literal(err));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Lettre annulée (argent éventuel récupéré)."), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lCourrier §7» §f/courrier envoyer <joueur> <message> "
                + "§7| §f/courrier donner <joueur> <montant> <message> §7| §f/courrier liste §7| §f/courrier lire <id> "
                + "§7| §f/courrier supprimer <id> §7| §f/courrier annuler <id>"), false);
        return 1;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private CourrierCommand() {}
}
