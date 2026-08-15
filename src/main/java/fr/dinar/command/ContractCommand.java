package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.ContractEntry;
import fr.dinar.economy.PlayerRef;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class ContractCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("contract")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("type", StringArgumentType.word())
                                        .then(CommandManager.argument("details", StringArgumentType.greedyString())
                                                .executes(ctx -> ContractCommand.create(ctx, 0))
                                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                                        .executes(ctx -> ContractCommand.create(ctx, DoubleArgumentType.getDouble(ctx, "montant"))))))))
                .then(CommandManager.literal("sign")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ContractCommand::sign)))
                .then(CommandManager.literal("cancel")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ContractCommand::cancel)))
                .then(CommandManager.literal("list").executes(ContractCommand::list))
                .then(CommandManager.literal("pending").executes(ContractCommand::pending))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ContractCommand::info)))
                .then(CommandManager.literal("help").executes(ContractCommand::help)));
    }

    private static int create(CommandContext<ServerCommandSource> ctx, double defaultAmount) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        String type = StringArgumentType.getString(ctx, "type");
        String details = StringArgumentType.getString(ctx, "details");
        double amount = 0;
        try {
            amount = DoubleArgumentType.getDouble(ctx, "montant");
        } catch (Exception ignored) {
            amount = defaultAmount;
        }

        if (!type.equalsIgnoreCase("vente") && !type.equalsIgnoreCase("service")
                && !type.equalsIgnoreCase("location") && !type.equalsIgnoreCase("pret")) {
            ctx.getSource().sendError(DinarLang.text("§cTypes valides : vente, service, location, pret"));
            return 0;
        }

        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", targetName));
            return 0;
        }
        if (ref.uuid().equals(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cVous ne pouvez pas créer un contrat avec vous-même."));
            return 0;
        }

        ContractEntry contract = DinarMod.contracts.create(player.getUuid(), player.getName().getString(),
                ref.uuid(), ref.displayName(), type, details, amount);

        String creatorName = player.getName().getString();
        String targetDisplay = ref.displayName();
        String contractType = type;
        String contractDetails = details;
        double contractAmount = amount;
        int contractId = contract.id;

        ctx.getSource().sendFeedback(() -> DinarLang.text("§aContrat §e#%s §acréé avec §e%s §7(type: §e%s§7, details: §f%s"
                        + (contractAmount > 0 ? "§7, montant: §e%s" : "") + "§7)",
                contractId, targetDisplay, contractType, contractDetails,
                contractAmount > 0 ? DinarMod.economy.money(contractAmount) : ""), false);

        ServerPlayerEntity target = ref.online();
        if (target != null) {
            target.sendMessage(Text.literal("")
                    .append(DinarLang.text("§e%s §6vous propose un contrat §e#%s §7(%s)\n",
                            creatorName, contractId, contractType))
                    .append(DinarLang.text("§7%s\n", contractDetails))
                    .append(Text.literal(DinarLang.t("§a§n/contract sign %s §8§o(pour accepter)", contractId))
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/contract sign " + contractId))))
                    .append(Text.literal(DinarLang.t("   §c§n/contract cancel %s §8§o(pour refuser)", contractId))
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/contract cancel " + contractId)))), false);
        }
        return 1;
    }

    private static int sign(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ContractEntry contract = DinarMod.contracts.get(id);
        if (contract == null) {
            ctx.getSource().sendError(DinarLang.text("§cContrat introuvable : #%s", id));
            return 0;
        }
        if (!contract.isPending()) {
            ctx.getSource().sendError(DinarLang.text("§cCe contrat n'est plus en attente."));
            return 0;
        }
        if (!contract.targetUuid.equals(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cCe contrat ne vous est pas destiné."));
            return 0;
        }
        DinarMod.contracts.sign(id, player.getUuid());
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aContrat §e#%s §asigné !", id), false);
        ServerPlayerEntity creator = DinarMod.economy.online(contract.creatorUuid);
        if (creator != null) {
            String signerName = player.getName().getString();
            int cid = id;
            creator.sendMessage(DinarLang.text("§e%s §aa signé le contrat §e#%s§a.", signerName, cid), false);
        }
        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ContractEntry contract = DinarMod.contracts.get(id);
        if (contract == null) {
            ctx.getSource().sendError(DinarLang.text("§cContrat introuvable : #%s", id));
            return 0;
        }
        if (!contract.involves(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cCe contrat ne vous concerne pas."));
            return 0;
        }
        DinarMod.contracts.cancel(id, player.getUuid());
        ctx.getSource().sendFeedback(() -> DinarLang.text("§cContrat §e#%s §cannulé.", id), false);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        var contracts = DinarMod.contracts.getByPlayer(player.getUuid());
        if (contracts.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Aucun contrat."), false);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══ Vos contrats (%s) ═══", contracts.size()), false);
        for (ContractEntry c : contracts) {
            String status = c.isPending() ? "§eEN ATTENTE" : c.isSigned() ? "§aSIGNÉ" : "§cANNULÉ";
            String other = c.creatorUuid.equals(player.getUuid()) ? c.targetName : c.creatorName;
            int cid = c.id;
            String ctype = c.type;
            double camount = c.amount;
            ctx.getSource().sendFeedback(() -> DinarLang.text("§e#%s §7%s §7avec §e%s §7(%s)" + (camount > 0 ? " §e%s" : ""),
                    cid, status, other, ctype, camount > 0 ? DinarMod.economy.money(camount) : ""), false);
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l════════════════════════"), false);
        return 1;
    }

    private static int pending(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        var pending = DinarMod.contracts.getPendingFor(player.getUuid());
        if (pending.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Aucun contrat en attente."), false);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══ Contrats en attente ═══"), false);
        for (ContractEntry c : pending) {
            int cid = c.id;
            String creator = c.creatorName;
            String ctype = c.type;
            String cdetails = c.details;
            double camount = c.amount;
            ctx.getSource().sendFeedback(() -> DinarLang.text("§e#%s §7de §e%s §7(%s) §f%s" + (camount > 0 ? " §e%s" : ""),
                    cid, creator, ctype, cdetails, camount > 0 ? DinarMod.economy.money(camount) : ""), false);
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l════════════════════════"), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ContractEntry contract = DinarMod.contracts.get(id);
        if (contract == null) {
            ctx.getSource().sendError(DinarLang.text("§cContrat introuvable : #%s", id));
            return 0;
        }
        String status = contract.isPending() ? "§eEN ATTENTE" : contract.isSigned() ? "§aSIGNÉ" : "§cANNULÉ";
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lContrat #%s %s", id, status), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Créateur : §e%s", contract.creatorName), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Cible : §e%s", contract.targetName), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Type : §e%s", contract.type), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Détails : §f%s", contract.details), false);
        if (contract.amount > 0) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Montant : §e%s", DinarMod.economy.money(contract.amount)), false);
        }
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══ Contrats ═══"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/contract create <joueur> <type> <details> [montant]"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/contract sign <id>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/contract cancel <id>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/contract list"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/contract pending"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/contract info <id>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Types : vente, service, location, pret"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l════════════════════════"), false);
        return 1;
    }

    private ContractCommand() {}
}
