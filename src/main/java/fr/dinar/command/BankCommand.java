package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double bank = DinarMod.economy.bankBalance(player.getUuid());
        double wallet = DinarMod.economy.balance(player.getUuid());
        src.sendFeedback(() -> Text.literal("§6§lBanque §r§7» §eSolde bancaire : §f" + DinarMod.economy.money(bank)
                + " §7| §ePortefeuille : §f" + DinarMod.economy.money(wallet)), false);
        return 1;
    }

    private static int balanceOther(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        double bank = DinarMod.economy.bankBalance(ref.uuid());
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§6§lBanque §r§7» §e" + display + " §fa en banque : §f" + DinarMod.economy.money(bank)), false);
        return 1;
    }

    private static int deposit(CommandContext<ServerCommandSource> ctx) {
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
        double wallet = DinarMod.economy.balance(player.getUuid());
        if (wallet < amount) {
            src.sendError(Text.literal("§cSolde insuffisant. Vous avez §e" + DinarMod.economy.money(wallet)));
            return 0;
        }
        double newBank = DinarMod.economy.bankDeposit(player.getUuid(), player.getName().getString(), amount);
        src.sendFeedback(() -> Text.literal("§aDépôt de §e" + DinarMod.economy.money(amount)
                + " §affectué. §eSolde bancaire : §f" + DinarMod.economy.money(newBank)), false);
        return 1;
    }

    private static int withdraw(CommandContext<ServerCommandSource> ctx) {
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
        double bank = DinarMod.economy.bankBalance(player.getUuid());
        if (bank < amount) {
            src.sendError(Text.literal("§cSolde bancaire insuffisant. Vous avez §e" + DinarMod.economy.money(bank)));
            return 0;
        }
        double newBank = DinarMod.economy.bankWithdraw(player.getUuid(), player.getName().getString(), amount);
        src.sendFeedback(() -> Text.literal("§aRetrait de §e" + DinarMod.economy.money(amount)
                + " §affectué. §eSolde bancaire : §f" + DinarMod.economy.money(newBank)), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6§l══════ Banque ══════"), false);
        src.sendFeedback(() -> Text.literal("§e/bank balance §7- Voir votre solde bancaire"), false);
        src.sendFeedback(() -> Text.literal("§e/bank deposit <montant> §7- Déposer de l'argent"), false);
        src.sendFeedback(() -> Text.literal("§e/bank withdraw <montant> §7- Retirer de l'argent"), false);
        src.sendFeedback(() -> Text.literal("§e/bank balance <joueur> §7- Solde bancaire d'un joueur"), false);
        src.sendFeedback(() -> Text.literal("§6§l════════════════════"), false);
        return 1;
    }

    private BankCommand() {}
}
