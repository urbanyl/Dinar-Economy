package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.SalaryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

public final class SalaryCommand {
    private static final long DEFAULT_INTERVAL = 3600;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("salary").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> set(ctx, DEFAULT_INTERVAL))
                                        .then(CommandManager.argument("intervalle_secondes", LongArgumentType.longArg(1))
                                                .executes(ctx -> set(ctx, LongArgumentType.getLong(ctx, "intervalle_secondes")))))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(SalaryCommand::remove)))
                .then(CommandManager.literal("list").executes(SalaryCommand::list))
                .then(CommandManager.literal("payall").executes(SalaryCommand::payAll))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(SalaryCommand::info))));
    }

    private static int set(CommandContext<ServerCommandSource> ctx, long intervalSeconds) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        DinarMod.economy.setSalary(ref.uuid(), amount, intervalSeconds);
        double finalAmount = amount;
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§aSalaire de §e" + display + " §adéfini à §e" + DinarMod.economy.money(finalAmount)
                + " §atoutes les §e" + formatInterval(intervalSeconds) + "§a."), true);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        if (DinarMod.economy.getSalary(ref.uuid()) == null) {
            src.sendError(Text.literal("§c" + ref.displayName() + " n'a pas de salaire."));
            return 0;
        }
        DinarMod.economy.removeSalary(ref.uuid());
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§aSalaire de §e" + display + " §asupprimé."), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Map<UUID, SalaryEntry> salaries = DinarMod.economy.getSalaries();
        if (salaries.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Aucun salaire configuré."), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§6§l=== Salaires configurés ==="), false);
        for (Map.Entry<UUID, SalaryEntry> e : salaries.entrySet()) {
            SalaryEntry s = e.getValue();
            String name = DinarMod.economy.accountName(e.getKey());
            double amount = s.amount;
            long interval = s.intervalSeconds;
            src.sendFeedback(() -> Text.literal("§e" + name + " §7» §e" + DinarMod.economy.money(amount)
                    + " §7/ §e" + formatInterval(interval)), false);
        }
        return 1;
    }

    private static int payAll(CommandContext<ServerCommandSource> ctx) {
        int n = DinarMod.economy.payAllSalaries();
        ctx.getSource().sendFeedback(() -> Text.literal("§a" + n + " salaire(s) payé(s)."), true);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        SalaryEntry s = DinarMod.economy.getSalary(ref.uuid());
        if (s == null) {
            src.sendFeedback(() -> Text.literal("§e" + ref.displayName() + " §7n'a pas de salaire."), false);
            return 0;
        }
        double tax = DinarMod.economy.salaryTaxFor(ref.uuid());
        long next = Math.max(0, (s.lastPaid + s.intervalSeconds * 1000L - System.currentTimeMillis()) / 1000);
        String display = ref.displayName();
        double amount = s.amount;
        long interval = s.intervalSeconds;
        src.sendFeedback(() -> Text.literal("§6§lSalaire de §e" + display + "§6\n"
                + "§7  Montant : §e" + DinarMod.economy.money(amount) + "\n"
                + "§7  Intervalle : §e" + formatInterval(interval) + "\n"
                + "§7  Taxe appliquée : §e" + (tax > 0 ? (int) (tax * 100) + "%" : "0%") + "\n"
                + "§7  Net : §e" + DinarMod.economy.money(amount * (1 - tax)) + "\n"
                + "§7  Prochain paiement dans : §e" + formatDuration(next)), false);
        return 1;
    }

    public static String formatInterval(long seconds) {
        if (seconds >= 86400) return (seconds / 86400) + "j";
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "min";
        return seconds + "s";
    }

    private static String formatDuration(long seconds) {
        if (seconds >= 86400) return (seconds / 86400) + "j " + ((seconds % 86400) / 3600) + "h";
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "min";
        if (seconds >= 60) return (seconds / 60) + "min " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private SalaryCommand() {}
}
