package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.SalaryEntry;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Map;
import java.util.UUID;

public final class SalaryCommand {
    private static final long DEFAULT_INTERVAL = 3600;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("salary").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> set(ctx, DEFAULT_INTERVAL))
                                        .then(CommandManager.argument("intervalle_secondes", LongArgumentType.longArg(1))
                                                .executes(ctx -> set(ctx, LongArgumentType.getLong(ctx, "intervalle_secondes")))))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(SalaryCommand::remove)))
                .then(CommandManager.literal("list").executes(SalaryCommand::list))
                .then(CommandManager.literal("payall").executes(SalaryCommand::payAll))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(SalaryCommand::info))));
    }

    private static int set(CommandContext<ServerCommandSource> ctx, long intervalSeconds) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        DinarMod.economy.setSalary(ref.uuid(), amount, intervalSeconds);
        double finalAmount = amount;
        String display = ref.displayName();
        src.sendFeedback(() -> DinarLang.text("§aSalaire de §e%s §adéfini à §e%s §atoutes les §e%s§a.",
                display, DinarMod.economy.money(finalAmount), formatInterval(intervalSeconds)), true);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        if (DinarMod.economy.getSalary(ref.uuid()) == null) {
            src.sendError(DinarLang.text("§c%s n'a pas de salaire.", ref.displayName()));
            return 0;
        }
        DinarMod.economy.removeSalary(ref.uuid());
        String display = ref.displayName();
        src.sendFeedback(() -> DinarLang.text("§aSalaire de §e%s §asupprimé.", display), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Map<UUID, SalaryEntry> salaries = DinarMod.economy.getSalaries();
        if (salaries.isEmpty()) {
            src.sendFeedback(() -> DinarLang.text("§7Aucun salaire configuré."), false);
            return 0;
        }
        src.sendFeedback(() -> DinarLang.text("§6§l=== Salaires configurés ==="), false);
        for (Map.Entry<UUID, SalaryEntry> e : salaries.entrySet()) {
            SalaryEntry s = e.getValue();
            String name = DinarMod.economy.accountName(e.getKey());
            double amount = s.amount;
            long interval = s.intervalSeconds;
            src.sendFeedback(() -> DinarLang.text("§e%s §7» §e%s §7/ §e%s",
                    name, DinarMod.economy.money(amount), formatInterval(interval)), false);
        }
        return 1;
    }

    private static int payAll(CommandContext<ServerCommandSource> ctx) {
        int n = DinarMod.economy.payAllSalaries();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§a%s salaire(s) payé(s).", n), true);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        SalaryEntry s = DinarMod.economy.getSalary(ref.uuid());
        if (s == null) {
            src.sendFeedback(() -> DinarLang.text("§e%s §7n'a pas de salaire.", ref.displayName()), false);
            return 0;
        }
        double tax = DinarMod.economy.salaryTaxFor(ref.uuid());
        long next = Math.max(0, (s.lastPaid + s.intervalSeconds * 1000L - System.currentTimeMillis()) / 1000);
        String display = ref.displayName();
        double amount = s.amount;
        long interval = s.intervalSeconds;
        src.sendFeedback(() -> DinarLang.text("§6§lSalaire de §e%s§6\n"
                + "§7  Montant : §e%s\n"
                + "§7  Intervalle : §e%s\n"
                + "§7  Taxe appliquée : §e%s\n"
                + "§7  Net : §e%s\n"
                + "§7  Prochain paiement dans : §e%s",
                display, DinarMod.economy.money(amount), formatInterval(interval),
                tax > 0 ? (int) (tax * 100) + "%" : "0%",
                DinarMod.economy.money(amount * (1 - tax)), formatDuration(next)), false);
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
