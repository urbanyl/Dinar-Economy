package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.LoanEntry;
import fr.dinar.economy.PlayerRef;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class LoanCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loan")
                .then(CommandManager.literal("take")
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .then(CommandManager.argument("taux_interet", DoubleArgumentType.doubleArg(0, 1))
                                        .then(CommandManager.argument("duree_secondes", IntegerArgumentType.integer(60))
                                                .executes(LoanCommand::take)))
                                .then(CommandManager.argument("duree_secondes", IntegerArgumentType.integer(60))
                                        .executes(ctx -> LoanCommand.takeWithDefault(ctx, 0.1)))))
                .then(CommandManager.literal("repay")
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .executes(LoanCommand::repay)))
                .then(CommandManager.literal("info").executes(LoanCommand::info)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(LoanCommand::infoOther)))
                .then(CommandManager.literal("list").executes(LoanCommand::list))
                .then(CommandManager.literal("help").executes(LoanCommand::help)));
    }

    private static int take(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        double rate = DoubleArgumentType.getDouble(ctx, "taux_interet");
        int duration = IntegerArgumentType.getInteger(ctx, "duree_secondes");
        return processLoan(src, player, amount, rate, duration);
    }

    private static int takeWithDefault(CommandContext<ServerCommandSource> ctx, double defaultRate) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        int duration = IntegerArgumentType.getInteger(ctx, "duree_secondes");
        return processLoan(src, player, amount, defaultRate, duration);
    }

    private static int processLoan(ServerCommandSource src, ServerPlayerEntity player, double amount, double rate, int duration) {
        if (amount <= 0) {
            src.sendError(Text.literal("§cLe montant doit être supérieur à zéro."));
            return 0;
        }
        LoanEntry existing = DinarMod.economy.getLoan(player.getUuid());
        if (existing != null && !existing.isRepaid()) {
            src.sendError(Text.literal("§cVous avez déjà un prêt en cours. Remboursez-le d'abord."));
            return 0;
        }
        LoanEntry loan = DinarMod.economy.createLoan(player.getUuid(), player.getName().getString(), amount, rate, duration);
        if (loan == null) {
            src.sendError(Text.literal("§cImpossible de créer le prêt."));
            return 0;
        }
        double interest = amount * rate;
        src.sendFeedback(() -> Text.literal("§aPrêt contracté : §e" + DinarMod.economy.money(amount)
                + " §7(taux : §e" + (int)(rate * 100) + "%§7, intérêts : §e" + DinarMod.economy.money(interest)
                + "§7, total à rembourser : §e" + DinarMod.economy.money(loan.totalOwed) + "§7)"), false);
        return 1;
    }

    private static int repay(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            src.sendError(Text.literal("§cLe montant doit être supérieur à zéro."));
            return 0;
        }
        LoanEntry loan = DinarMod.economy.getLoan(player.getUuid());
        if (loan == null || loan.isRepaid()) {
            src.sendFeedback(() -> Text.literal("§7Vous n'avez aucun prêt en cours."), false);
            return 0;
        }
        double repaid = DinarMod.economy.repayLoan(player.getUuid(), player.getName().getString(), amount);
        if (repaid <= 0) {
            src.sendError(Text.literal("§cSolde insuffisant pour rembourser."));
            return 0;
        }
        double remaining = loan.remaining();
        if (loan.isRepaid()) {
            src.sendFeedback(() -> Text.literal("§aPrêt entièrement remboursé ! §e" + DinarMod.economy.money(repaid) + " §apayé."), false);
        } else {
            src.sendFeedback(() -> Text.literal("§aRemboursement de §e" + DinarMod.economy.money(repaid)
                    + " §aeffectué. §eReste : §f" + DinarMod.economy.money(remaining)), false);
        }
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        return showLoanInfo(src, player.getUuid(), player.getName().getString());
    }

    private static int infoOther(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        return showLoanInfo(src, ref.uuid(), ref.displayName());
    }

    private static int showLoanInfo(ServerCommandSource src, java.util.UUID uuid, String name) {
        LoanEntry loan = DinarMod.economy.getLoan(uuid);
        if (loan == null || loan.isRepaid()) {
            src.sendFeedback(() -> Text.literal("§7" + name + " §7n'a aucun prêt en cours."), false);
            return 0;
        }
        long remaining = loan.timeRemainingSeconds();
        src.sendFeedback(() -> Text.literal("§6§lPrêt de §e" + name + "§6"), false);
        src.sendFeedback(() -> Text.literal("§7  Montant emprunté : §e" + DinarMod.economy.money(loan.amount)), false);
        src.sendFeedback(() -> Text.literal("§7  Taux d'intérêt : §e" + (int)(loan.interestRate * 100) + "%"), false);
        src.sendFeedback(() -> Text.literal("§7  Total dû : §e" + DinarMod.economy.money(loan.totalOwed)), false);
        src.sendFeedback(() -> Text.literal("§7  Remboursé : §e" + DinarMod.economy.money(loan.amountRepaid)), false);
        src.sendFeedback(() -> Text.literal("§7  Reste : §c" + DinarMod.economy.money(loan.remaining())), false);
        src.sendFeedback(() -> Text.literal("§7  Temps restant : §e" + formatDuration(remaining)), false);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        var loans = DinarMod.economy.getLoans();
        if (loans.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Aucun prêt en cours."), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§6§l=== Prêts en cours ==="), false);
        for (var entry : loans.entrySet()) {
            LoanEntry l = entry.getValue();
            if (l.isRepaid()) continue;
            String name = DinarMod.economy.accountName(entry.getKey());
            src.sendFeedback(() -> Text.literal("§e" + name + " §7» §e" + DinarMod.economy.money(l.remaining())
                    + " §7restant sur §e" + DinarMod.economy.money(l.totalOwed)), false);
        }
        src.sendFeedback(() -> Text.literal("§6§l════════════════════"), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6§l══════ Prêts ══════"), false);
        src.sendFeedback(() -> Text.literal("§e/loan take <montant> <taux> <durée> §7- Contracter un prêt"), false);
        src.sendFeedback(() -> Text.literal("§e/loan repay <montant> §7- Rembourser un prêt"), false);
        src.sendFeedback(() -> Text.literal("§e/loan info §7- Votre prêt"), false);
        src.sendFeedback(() -> Text.literal("§e/loan list §7- Tous les prêts"), false);
        src.sendFeedback(() -> Text.literal("§6§l════════════════════"), false);
        return 1;
    }

    private static String formatDuration(long seconds) {
        if (seconds >= 86400) return (seconds / 86400) + "j " + ((seconds % 86400) / 3600) + "h";
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "min";
        if (seconds >= 60) return (seconds / 60) + "min " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private LoanCommand() {}
}
