package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.logs.RpLogManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class JournalCommand {

    private static final int PER_PAGE = 10;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("journal")
                .executes(ctx -> show(ctx, null, 0))
                .then(CommandManager.literal("voir")
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> show(ctx, null, IntegerArgumentType.getInteger(ctx, "page") - 1))))
                .then(CommandManager.literal("cat")
                        .then(CommandManager.argument("categorie", StringArgumentType.word())
                                .executes(ctx -> show(ctx, StringArgumentType.getString(ctx, "categorie"), 0))
                                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> show(ctx, StringArgumentType.getString(ctx, "categorie"),
                                                IntegerArgumentType.getInteger(ctx, "page") - 1))))));
    }

    private static int show(CommandContext<ServerCommandSource> ctx, String category, int page) {
        List<RpLogManager.RpLogEntry> entries = category == null
                ? DinarMod.rpLog.getPage(page, PER_PAGE)
                : DinarMod.rpLog.getCategory(category, page, PER_PAGE);
        if (entries.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Aucune entrée dans le journal."), false);
            return 0;
        }
        String title = "§6§lJournal RP §r§7(page " + (page + 1) + ")"
                + (category != null ? " §7— §f" + category : "");
        ctx.getSource().sendFeedback(() -> Text.literal(title), false);
        for (RpLogManager.RpLogEntry e : entries) {
            String line = "§8" + time(e.time) + " §7[" + e.category + "] §f" + e.message;
            ctx.getSource().sendFeedback(() -> Text.literal(line), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§7Catégories : §f"
                + "ECO, SALAIRE, AMENDE, BANQUE, JUSTICE, POLICE, PRISON, MAIL, GOUVERNEMENT"), false);
        return 1;
    }

    private static String time(long millis) {
        return new SimpleDateFormat("dd/MM HH:mm").format(new Date(millis));
    }

    private JournalCommand() {}
}
