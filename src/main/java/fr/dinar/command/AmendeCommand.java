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
            ctx.getSource().sendError(DinarLang.text("§cLe montant doit être positif."));
            return 0;
        }

        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", targetName));
            return 0;
        }

        if (!DinarMod.economy.deductFromBalance(ref.uuid(), ref.displayName(), amount)) {
            ctx.getSource().sendError(DinarLang.text("§c%s n'a pas assez d'argent (§e%s§c).",
                    ref.displayName(), DinarMod.economy.money(DinarMod.economy.balance(ref.uuid()))));
            return 0;
        }

        DinarMod.economy.addTreasury(amount);
        DinarMod.economy.logTransaction(ref.uuid(), "AMENDE", amount, "Trésorerie", reason);

        String issuer = player != null ? player.getName().getString() : "Administrateur";
        String display = ref.displayName();
        double finalAmount = amount;
        String finalReason = reason;

        ctx.getSource().sendFeedback(() -> DinarLang.text("§aAmende de §e%s §7infligée à §e%s §7pour : §f%s",
                DinarMod.economy.money(finalAmount), display, finalReason), true);

        ServerPlayerEntity target = ref.online();
        if (target != null) {
            target.sendMessage(DinarLang.text("§c§lAMENDE §r§7» §e%s §cvous a infligé une amende de §e%s §7pour : §f%s",
                    issuer, DinarMod.economy.money(finalAmount), finalReason), false);
        }

        String finalIssuer = issuer;
        DinarMod.government.broadcast("§6§lCaliphat §r§7» §e" + display + " §ca été amende de §e"
                + DinarMod.economy.money(finalAmount) + " §7pour : §f" + finalReason);

        DinarMod.rpLog.log("AMENDE", display + " a été amendé de " + DinarMod.economy.money(finalAmount)
                + " par " + finalIssuer + " pour : " + finalReason);
        DinarMod.rpLog.sendEmbed(new fr.dinar.logs.DiscordWebhook.DiscordEmbed()
                .title("💰 Amende")
                .field("Joueur", display)
                .field("Montant", DinarMod.economy.money(finalAmount))
                .field("Motif", finalReason)
                .field("Infligée par", finalIssuer)
                .color(0xE67E22)
                .footer("Dinar RP"));

        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        double treasury = DinarMod.economy.getTreasury();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lTrésorerie §r§7» §e%s", DinarMod.economy.money(treasury)), false);
        return 1;
    }

    private AmendeCommand() {}
}
