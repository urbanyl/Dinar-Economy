package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.dinar.DinarMod;
import fr.dinar.config.DinarConfig;
import fr.dinar.economy.Account;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.TransactionEntry;
import fr.dinar.gui.AdminPanelScreenHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class EcoCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("eco").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::give))))
                .then(CommandManager.literal("take")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::take))))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::set))))
                .then(CommandManager.literal("reset")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(EcoCommand::reset)))
                .then(CommandManager.literal("resetall").executes(EcoCommand::resetAll))
                .then(CommandManager.literal("panel").executes(EcoCommand::panel))
                .then(CommandManager.literal("reload").executes(EcoCommand::reload))
                .then(CommandManager.literal("save").executes(EcoCommand::save))
                .then(CommandManager.literal("treasury")
                        .executes(EcoCommand::treasuryShow)
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::treasuryAdd)))
                        .then(CommandManager.literal("take")
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::treasuryTake))))
                .then(CommandManager.literal("history")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(EcoCommand::history))));
    }

    private static int give(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        double newBal = DinarMod.economy.add(ref.uuid(), ref.displayName(), amount);
        double finalAmount = amount;
        String display = ref.displayName();
        double bal = newBal;
        src.sendFeedback(() -> Text.literal("§a" + display + " §freçoit §e" + DinarMod.economy.money(finalAmount)
                + " §f→ nouveau solde : §e" + DinarMod.economy.money(bal)), true);
        ServerPlayerEntity p = ref.online();
        if (p != null) {
            p.sendMessage(Text.literal("§aUn administrateur vous a donné §e" + DinarMod.economy.money(finalAmount)
                    + "§a. Nouveau solde : §e" + DinarMod.economy.money(bal)), false);
        }
        return 1;
    }

    private static int take(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        double actual = DinarMod.economy.take(ref.uuid(), ref.displayName(), amount);
        double bal = DinarMod.economy.balance(ref.uuid());
        double finalAmount = actual;
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§c" + display + " §fperd §e" + DinarMod.economy.money(finalAmount)
                + " §f→ nouveau solde : §e" + DinarMod.economy.money(bal)), true);
        ServerPlayerEntity p = ref.online();
        if (p != null) {
            p.sendMessage(Text.literal("§cUn administrateur vous a retiré §e" + DinarMod.economy.money(finalAmount)
                    + "§c. Nouveau solde : §e" + DinarMod.economy.money(bal)), false);
        }
        return 1;
    }

    private static int set(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        DinarMod.economy.setBalance(ref.uuid(), ref.displayName(), amount);
        double finalAmount = amount;
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§aSolde de §e" + display + " §adéfini à §e" + DinarMod.economy.money(finalAmount)), true);
        ServerPlayerEntity p = ref.online();
        if (p != null) {
            p.sendMessage(Text.literal("§aUn administrateur a défini votre solde à §e" + DinarMod.economy.money(finalAmount)), false);
        }
        return 1;
    }

    private static int reset(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        DinarMod.economy.setBalance(ref.uuid(), ref.displayName(), DinarMod.config.startingBalance);
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§aSolde de §e" + display + " §arétabli."), true);
        return 1;
    }

    private static int resetAll(CommandContext<ServerCommandSource> ctx) {
        DinarMod.economy.resetAll();
        ctx.getSource().sendFeedback(() -> Text.literal("§aTous les soldes ont été réinitialisés."), true);
        return 1;
    }

    private static int panel(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        AdminPanelScreenHandler.open(player);
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        DinarMod.config = DinarConfig.load();
        ctx.getSource().sendFeedback(() -> Text.literal("§aConfig Dinar rechargée."), true);
        return 1;
    }

    private static int save(CommandContext<ServerCommandSource> ctx) {
        DinarMod.economy.save();
        ctx.getSource().sendFeedback(() -> Text.literal("§aDonnées Dinar sauvegardées."), true);
        return 1;
    }

    private static int treasuryShow(CommandContext<ServerCommandSource> ctx) {
        double treasury = DinarMod.economy.getTreasury();
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lTrésorerie §r§7» §e" + DinarMod.economy.money(treasury)), true);
        return 1;
    }

    private static int treasuryAdd(CommandContext<ServerCommandSource> ctx) {
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        DinarMod.economy.addTreasury(amount);
        double treasury = DinarMod.economy.getTreasury();
        ctx.getSource().sendFeedback(() -> Text.literal("§aTrésorerie augmentée de §e" + DinarMod.economy.money(amount)
                + " §f→ §e" + DinarMod.economy.money(treasury)), true);
        return 1;
    }

    private static int treasuryTake(CommandContext<ServerCommandSource> ctx) {
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        DinarMod.economy.takeTreasury(amount);
        double treasury = DinarMod.economy.getTreasury();
        ctx.getSource().sendFeedback(() -> Text.literal("§cTrésorerie réduite de §e" + DinarMod.economy.money(amount)
                + " §f→ §e" + DinarMod.economy.money(treasury)), true);
        return 1;
    }

    private static int history(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        Account acc = DinarMod.economy.account(ref.uuid());
        if (acc == null || acc.history.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Aucune transaction pour §e" + ref.displayName() + "§7."), false);
            return 0;
        }
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§6§l=== Historique de §e" + display + " §6==="), false);
        for (TransactionEntry t : acc.history) {
            String line = switch (t.type()) {
                case "SEND" -> "§c→ §e" + DinarMod.economy.money(t.amount()) + " §7vers " + t.otherName();
                case "RECEIVE" -> "§a← §e" + DinarMod.economy.money(t.amount()) + " §7de " + t.otherName();
                case "SALARY" -> "§b$ §e" + DinarMod.economy.money(t.amount()) + " §7(salaire)";
                default -> "§7" + t.type() + " §e" + DinarMod.economy.money(t.amount());
            };
            String reason = t.reason();
            src.sendFeedback(() -> Text.literal(line + (reason != null ? " §7» " + reason : "")), false);
        }
        return 1;
    }

    private EcoCommand() {}
}
