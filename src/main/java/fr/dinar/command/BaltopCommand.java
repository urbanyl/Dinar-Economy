package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.Account;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public final class BaltopCommand {
    private static final int PER_PAGE = 10;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("baltop")
                .executes(ctx -> run(ctx, 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "page")))));
    }

    private static int run(CommandContext<ServerCommandSource> ctx, int page) {
        ServerCommandSource src = ctx.getSource();
        int pages = DinarMod.economy.pageCount(PER_PAGE);
        page = Math.min(page, pages);
        int start = (page - 1) * PER_PAGE;
        List<Account> list = DinarMod.economy.baltop(page - 1, PER_PAGE);
        int finalPage = page;
        src.sendFeedback(() -> Text.literal("§6§l=== Classement des comptes (§e" + finalPage + "/" + pages + "§6) ==="), false);
        for (int i = 0; i < list.size(); i++) {
            Account a = list.get(i);
            int rank = start + i + 1;
            double bal = a.balance;
            String name = a.name != null ? a.name : "Inconnu";
            src.sendFeedback(() -> Text.literal("§8#" + rank + " §e" + name + " §7- §e" + DinarMod.economy.money(bal)), false);
        }
        return 1;
    }

    private BaltopCommand() {}
}
