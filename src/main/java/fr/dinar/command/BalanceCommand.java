package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class BalanceCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String name : new String[]{"bal", "balance", "money"}) {
            dispatcher.register(CommandManager.literal(name)
                    .executes(BalanceCommand::runSelf)
                    .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                            .executes(BalanceCommand::runOther)));
        }
    }

    private static int runSelf(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCette commande doit être exécutée par un joueur."));
            return 0;
        }
        double bal = DinarMod.economy.balance(player.getUuid());
        int rank = DinarMod.economy.rank(player.getUuid());
        double finalBal = bal;
        src.sendFeedback(() -> Text.literal("§6§lDinar §r§7» §fVotre solde : §e" + DinarMod.economy.money(finalBal)
                + " §7(§8#" + rank + "§7)"), false);
        return 1;
    }

    private static int runOther(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        double bal = DinarMod.economy.balance(ref.uuid());
        int rank = DinarMod.economy.rank(ref.uuid());
        double finalBal = bal;
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§6§lDinar §r§7» §e" + display + " §fa un solde de §e"
                + DinarMod.economy.money(finalBal) + " §7(§8#" + rank + "§7)"), false);
        return 1;
    }

    private BalanceCommand() {}
}
