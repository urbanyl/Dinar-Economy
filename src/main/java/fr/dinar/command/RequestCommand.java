package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.TransactionRequest;
import fr.dinar.economy.TransferResult;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public final class RequestCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dmd")
                .executes(RequestCommand::list)
                .then(CommandManager.literal("list").executes(RequestCommand::list))
                .then(CommandManager.literal("accept")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(RequestCommand::accept)))
                .then(CommandManager.literal("deny")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(RequestCommand::deny)))
                .then(CommandManager.argument("joueur", StringArgumentType.word())
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> create(ctx, null))
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "message")))))));
    }

    private static int create(CommandContext<ServerCommandSource> ctx, String message) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity sender = src.getPlayer();
        if (sender == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }

        String targetName = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");

        if (amount <= 0) {
            src.sendError(Text.literal("§cLe montant doit être supérieur à zéro."));
            return 0;
        }

        PlayerRef target = DinarMod.economy.resolve(src, targetName);
        if (target == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + targetName));
            return 0;
        }
        if (target.uuid().equals(sender.getUuid())) {
            src.sendError(Text.literal("§cVous ne pouvez pas vous demander de l'argent à vous-même."));
            return 0;
        }

        PlayerRef senderRef = PlayerRef.ofOnline(sender);
        TransactionRequest req = DinarMod.economy.createRequest(senderRef, target, amount, message);
        String targetDisplay = target.displayName();
        String finalMessage = message;
        src.sendFeedback(() -> Text.literal("§aDemande envoyée à §e" + targetDisplay + " §a: §e"
                + DinarMod.economy.money(amount) + (finalMessage != null ? " §7(" + finalMessage + ")" : "")), false);

        ServerPlayerEntity targetOnline = target.online();
        if (targetOnline != null) {
            targetOnline.sendMessage(Text.literal("")
                    .append(Text.literal("§e" + sender.getName().getString() + " §6vous demande §e"
                            + DinarMod.economy.money(amount) + (finalMessage != null ? " §7» " + finalMessage : "") + "\n"))
                    .append(Text.literal("§a§n/dmd accept " + req.id + " §8§o(pour accepter)")
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dmd accept " + req.id))))
                    .append(Text.literal("   §c§n/dmd deny " + req.id + " §8§o(pour refuser)")
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dmd deny " + req.id)))), false);
        }
        return 1;
    }

    private static int accept(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity acceptor = src.getPlayer();
        if (acceptor == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }

        int id = IntegerArgumentType.getInteger(ctx, "id");
        TransactionRequest req = DinarMod.economy.getRequest(id);
        if (req == null) {
            src.sendError(Text.literal("§cCette demande n'existe plus (expirée ou déjà traitée)."));
            return 0;
        }
        if (!req.target.equals(acceptor.getUuid())) {
            src.sendError(Text.literal("§cCette demande ne vous est pas destinée."));
            return 0;
        }

        PlayerRef target = PlayerRef.ofOnline(acceptor);
        PlayerRef sender = DinarMod.economy.resolveUuid(req.sender);
        TransferResult res = DinarMod.economy.transfer(sender, target, req.amount, "dmd accept");
        if (!res.success()) {
            src.sendError(Text.literal("§c" + res.error()));
            return 0;
        }

        DinarMod.economy.removeRequest(id);
        String senderDisplay = sender.displayName();
        src.sendFeedback(() -> Text.literal("§aVous avez envoyé §e" + DinarMod.economy.money(req.amount)
                + " §aà §e" + senderDisplay + "§a." + (res.tax() > 0 ? " §7(Taxe §e" + DinarMod.economy.money(res.tax()) + "§7)" : "")), false);

        ServerPlayerEntity requester = sender.online();
        if (requester != null) {
            requester.sendMessage(Text.literal("§e" + acceptor.getName().getString() + " §aa accepté votre demande de §e"
                    + DinarMod.economy.money(req.amount) + "§a."), false);
        }
        return 1;
    }

    private static int deny(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity accepter = src.getPlayer();
        if (accepter == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }

        int id = IntegerArgumentType.getInteger(ctx, "id");
        TransactionRequest req = DinarMod.economy.getRequest(id);
        if (req == null) {
            src.sendError(Text.literal("§cCette demande n'existe plus (expirée ou déjà traitée)."));
            return 0;
        }
        if (!req.target.equals(accepter.getUuid())) {
            src.sendError(Text.literal("§cCette demande ne vous est pas destinée."));
            return 0;
        }

        DinarMod.economy.removeRequest(id);
        String senderName = DinarMod.economy.resolveUuid(req.sender).displayName();
        src.sendFeedback(() -> Text.literal("§cVous avez refusé la demande de §e" + senderName + "§c."), false);

        ServerPlayerEntity requester = DinarMod.economy.online(req.sender);
        if (requester != null) {
            requester.sendMessage(Text.literal("§e" + accepter.getName().getString() + " §ca refusé votre demande de §e"
                    + DinarMod.economy.money(req.amount) + "§c."), false);
        }
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }

        List<TransactionRequest> reqs = DinarMod.economy.requestsFor(player.getUuid());
        if (reqs.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Aucune demande d'argent en attente."), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§6§l=== Demandes d'argent en attente ==="), false);
        for (TransactionRequest r : reqs) {
            String other = DinarMod.economy.resolveUuid(player.getUuid().equals(r.sender) ? r.target : r.sender).displayName();
            boolean isTarget = r.target.equals(player.getUuid());
            long left = Math.max(0, (r.expiresAt - System.currentTimeMillis()) / 1000);
            UUID finalOther = isTarget ? r.sender : r.target;
            src.sendFeedback(() -> Text.literal("§7#" + r.id + " §e" + other
                    + " §f» §e" + DinarMod.economy.money(r.amount)
                    + (isTarget ? " §7[à accepter] §8(" + left + "s restantes)" : " §7[envoyée]")
                    + (r.message != null ? " §7(" + r.message + ")" : "")), false);
        }
        return 1;
    }

    private RequestCommand() {}
}
