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

public final class AmendeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("amende")
                .requires(s -> s.hasPermissionLevel(2) || DinarMod.government.isLeader(
                        s.getPlayer() != null ? s.getPlayer().getUuid() : null))
                .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .then(CommandManager.argument("raison", StringArgumentType.greedyString())
                                        .executes(AmendeCommand::amend))))
                .then(CommandManager.literal("list").executes(AmendeCommand::list)));
    }

    private static int amend(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        String targetName = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        String reason = StringArgumentType.getString(ctx, "raison");

        if (amount <= 0) {
            ctx.getSource().sendError(Text.literal("§cLe montant doit être positif."));
            return 0;
        }

        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + targetName));
            return 0;
        }

        if (!DinarMod.economy.deductFromBalance(ref.uuid(), ref.displayName(), amount)) {
            ctx.getSource().sendError(Text.literal("§c" + ref.displayName() + " n'a pas assez d'argent (§e"
                    + DinarMod.economy.money(DinarMod.economy.balance(ref.uuid())) + "§c)."));
            return 0;
        }

        DinarMod.economy.addTreasury(amount);
        DinarMod.economy.logTransaction(ref.uuid(), "AMENDE", amount, "Trésorerie", reason);

        String issuer = player != null ? player.getName().getString() : "Administrateur";
        String display = ref.displayName();
        double finalAmount = amount;
        String finalReason = reason;

        ctx.getSource().sendFeedback(() -> Text.literal("§aAmende de §e" + DinarMod.economy.money(finalAmount)
                + " §7infligée à §e" + display + " §7pour : §f" + finalReason), true);

        ServerPlayerEntity target = ref.online();
        if (target != null) {
            target.sendMessage(Text.literal("§c§lAMENDE §r§7» §e" + issuer + " §cvous a infligé une amende de §e"
                    + DinarMod.economy.money(finalAmount) + " §7pour : §f" + finalReason), false);
        }

        String finalIssuer = issuer;
        DinarMod.government.broadcast("§6§lCaliphat §r§7» §e" + display + " §ca été amende de §e"
                + DinarMod.economy.money(finalAmount) + " §7pour : §f" + finalReason);

        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        double treasury = DinarMod.economy.getTreasury();
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lTrésorerie §r§7» §e" + DinarMod.economy.money(treasury)), false);
        return 1;
    }

    private AmendeCommand() {}
}
