package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BankCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bank")
                .then(CommandManager.literal("balance").executes(BankCommand::balance)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(BankCommand::balanceOther)))
                .then(CommandManager.literal("deposit")
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .executes(BankCommand::deposit)))
                .then(CommandManager.literal("withdraw")
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .executes(BankCommand::withdraw)))
                .then(CommandManager.literal("info").executes(BankCommand::info)));
    }

    private static int balance(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(DinarLang.text("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double bank = DinarMod.economy.bankBalance(player.getUuid());
        double wallet = DinarMod.economy.balance(player.getUuid());
        src.sendFeedback(() -> DinarLang.text("§6§lBanque §r§7» §eSolde bancaire : §f%s §7| §ePortefeuille : §f%s",
                DinarMod.economy.money(bank), DinarMod.economy.money(wallet)), false);
        return 1;
    }

    private static int balanceOther(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        double bank = DinarMod.economy.bankBalance(ref.uuid());
        String display = ref.displayName();
        src.sendFeedback(() -> DinarLang.text("§6§lBanque §r§7» §e%s §fa en banque : §f%s",
                display, DinarMod.economy.money(bank)), false);
        return 1;
    }

    private static int deposit(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(DinarLang.text("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            src.sendError(DinarLang.text("§cLe montant doit être supérieur à zéro."));
            return 0;
        }
        double wallet = DinarMod.economy.balance(player.getUuid());
        if (wallet < amount) {
            src.sendError(DinarLang.text("§cSolde insuffisant. Vous avez §e%s", DinarMod.economy.money(wallet)));
            return 0;
        }
        double newBank = DinarMod.economy.bankDeposit(player.getUuid(), player.getName().getString(), amount);
        src.sendFeedback(() -> DinarLang.text("§aDépôt de §e%s §affectué. §eSolde bancaire : §f%s",
                DinarMod.economy.money(amount), DinarMod.economy.money(newBank)), false);
        return 1;
    }

    private static int withdraw(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(DinarLang.text("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            src.sendError(DinarLang.text("§cLe montant doit être supérieur à zéro."));
            return 0;
        }
        double bank = DinarMod.economy.bankBalance(player.getUuid());
        if (bank < amount) {
            src.sendError(DinarLang.text("§cSolde bancaire insuffisant. Vous avez §e%s", DinarMod.economy.money(bank)));
            return 0;
        }
        double newBank = DinarMod.economy.bankWithdraw(player.getUuid(), player.getName().getString(), amount);
        src.sendFeedback(() -> DinarLang.text("§aRetrait de §e%s §affectué. §eSolde bancaire : §f%s",
                DinarMod.economy.money(amount), DinarMod.economy.money(newBank)), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> DinarLang.text("§6§l══════ Banque ══════"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank balance §7- Voir votre solde bancaire"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank deposit <montant> §7- Déposer de l'argent"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank withdraw <montant> §7- Retirer de l'argent"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank balance <joueur> §7- Solde bancaire d'un joueur"), false);
        src.sendFeedback(() -> DinarLang.text("§6§l════════════════════"), false);
        return 1;
    }

    private BankCommand() {}
}
