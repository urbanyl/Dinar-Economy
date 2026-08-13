package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.TransferResult;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PayCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String name : new String[]{"pay", "send", "payer"}) {
            dispatcher.register(CommandManager.literal(name)
                    .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                            .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                    .executes(ctx -> run(ctx, null))
                                    .then(CommandManager.argument("raison", StringArgumentType.greedyString())
                                            .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "raison")))))));
        }
    }

    private static int run(CommandContext<ServerCommandSource> ctx, String reason) {
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

        PlayerRef senderRef = PlayerRef.ofOnline(sender);
        TransferResult res = DinarMod.economy.transfer(senderRef, target, amount, reason);
        if (!res.success()) {
            src.sendError(Text.literal("§c" + res.error()));
            return 0;
        }

        String targetDisplay = target.displayName();
        double sent = amount;
        double tax = res.tax();
        src.sendFeedback(() -> Text.literal("§aVous avez envoyé §e" + DinarMod.economy.money(sent)
                + " §aà §e" + targetDisplay + "§a." + taxLine(tax, "payé")), false);

        ServerPlayerEntity targetOnline = target.online();
        if (targetOnline != null) {
            double received = res.received();
            String senderName = sender.getName().getString();
            String finalReason = reason;
            targetOnline.sendMessage(Text.literal("§e" + senderName + " §avous a envoyé §e"
                    + DinarMod.economy.money(received) + taxLine(tax, "reçue")
                    + (finalReason != null ? " §7» " + finalReason : "")), false);
        }
        return 1;
    }

    private static String taxLine(double tax, String verb) {
        return tax > 0 ? " §7(dont §e" + DinarMod.economy.money(tax) + " §7de taxe " + verb + ")" : "";
    }

    private PayCommand() {}
}
