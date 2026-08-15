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

import java.util.Map;
import java.util.UUID;

public final class TaxCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tax").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("global")
                        .then(CommandManager.argument("pourcent", DoubleArgumentType.doubleArg(0, 100))
                                .executes(TaxCommand::global)))
                .then(CommandManager.literal("salary")
                        .then(CommandManager.argument("pourcent", DoubleArgumentType.doubleArg(0, 100))
                                .executes(TaxCommand::salary)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("pourcent", DoubleArgumentType.doubleArg(0, 100))
                                        .executes(TaxCommand::setPlayer))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(TaxCommand::removePlayer)))
                .then(CommandManager.literal("list").executes(TaxCommand::list))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(TaxCommand::info))));
    }

    private static int global(CommandContext<ServerCommandSource> ctx) {
        double percent = DoubleArgumentType.getDouble(ctx, "pourcent");
        DinarMod.economy.setGlobalTransactionTax(percent / 100.0);
        double rate = DinarMod.economy.getGlobalTransactionTax();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aTaxe globale sur les transactions définie à §e%s%§a.",
                (int) (rate * 100)), true);
        return 1;
    }

    private static int salary(CommandContext<ServerCommandSource> ctx) {
        double percent = DoubleArgumentType.getDouble(ctx, "pourcent");
        DinarMod.economy.setSalaryTax(percent / 100.0);
        double rate = DinarMod.economy.getSalaryTax();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aTaxe sur les salaires définie à §e%s%§a.",
                (int) (rate * 100)), true);
        return 1;
    }

    private static int setPlayer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double percent = DoubleArgumentType.getDouble(ctx, "pourcent");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        DinarMod.economy.setPersonalTax(ref.uuid(), percent / 100.0);
        String display = ref.displayName();
        src.sendFeedback(() -> DinarLang.text("§aTaxe personnelle de §e%s §adéfinie à §e%s%§a.",
                display, (int) percent), true);
        return 1;
    }

    private static int removePlayer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(DinarLang.text("§cJoueur introuvable : §e%s", name));
            return 0;
        }
        if (!DinarMod.economy.hasPersonalTax(ref.uuid())) {
            src.sendError(DinarLang.text("§c%s n'a pas de taxe personnelle.", ref.displayName()));
            return 0;
        }
        DinarMod.economy.setPersonalTax(ref.uuid(), 0);
        String display = ref.displayName();
        src.sendFeedback(() -> DinarLang.text("§aTaxe personnelle de §e%s §asupprimée.", display), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        double global = DinarMod.economy.getGlobalTransactionTax();
        double salary = DinarMod.economy.getSalaryTax();
        Map<UUID, Double> taxes = DinarMod.economy.getPersonalTaxes();
        src.sendFeedback(() -> DinarLang.text("§6§l=== Taxes ===\n"
                + "§7  Globale (transactions) : §e%s%\n"
                + "§7  Salaires : §e%s%\n"
                + "§7  Trésorerie : §e%s",
                (int) (global * 100), (int) (salary * 100), DinarMod.economy.money(DinarMod.economy.getTreasury())), false);
        if (taxes.isEmpty()) {
            src.sendFeedback(() -> DinarLang.text("§7  Aucune taxe personnelle."), false);
        } else {
            for (Map.Entry<UUID, Double> e : taxes.entrySet()) {
                String name = DinarMod.economy.accountName(e.getKey());
                double rate = e.getValue();
                src.sendFeedback(() -> DinarLang.text("§7  §e%s §7» §e%s%", name, (int) (rate * 100)), false);
            }
        }
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
        Double personal = DinarMod.economy.getPersonalTax(ref.uuid());
        double effective = DinarMod.economy.effectiveTax(ref.uuid());
        String display = ref.displayName();
        src.sendFeedback(() -> DinarLang.text("§6§lTaxes de §e%s§6\n"
                + "§7  Personnelle : §e%s\n"
                + "§7  Effective (transaction reçue) : §e%s%\n"
                + "§7  Effective (salaire) : §e%s%",
                display,
                personal != null ? (int) (personal * 100) + "%" : "aucune (globale)",
                (int) (effective * 100),
                (int) (DinarMod.economy.salaryTaxFor(ref.uuid()) * 100)), false);
        return 1;
    }

    private TaxCommand() {}
}
