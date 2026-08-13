package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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
        ctx.getSource().sendFeedback(() -> Text.literal("§aTaxe globale sur les transactions définie à §e"
                + (int) (rate * 100) + "%§a."), true);
        return 1;
    }

    private static int salary(CommandContext<ServerCommandSource> ctx) {
        double percent = DoubleArgumentType.getDouble(ctx, "pourcent");
        DinarMod.economy.setSalaryTax(percent / 100.0);
        double rate = DinarMod.economy.getSalaryTax();
        ctx.getSource().sendFeedback(() -> Text.literal("§aTaxe sur les salaires définie à §e"
                + (int) (rate * 100) + "%§a."), true);
        return 1;
    }

    private static int setPlayer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        double percent = DoubleArgumentType.getDouble(ctx, "pourcent");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        DinarMod.economy.setPersonalTax(ref.uuid(), percent / 100.0);
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§aTaxe personnelle de §e" + display + " §adéfinie à §e"
                + (int) percent + "%§a."), true);
        return 1;
    }

    private static int removePlayer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        PlayerRef ref = DinarMod.economy.resolve(src, name);
        if (ref == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        if (!DinarMod.economy.hasPersonalTax(ref.uuid())) {
            src.sendError(Text.literal("§c" + ref.displayName() + " n'a pas de taxe personnelle."));
            return 0;
        }
        DinarMod.economy.setPersonalTax(ref.uuid(), 0);
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§aTaxe personnelle de §e" + display + " §asupprimée."), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        double global = DinarMod.economy.getGlobalTransactionTax();
        double salary = DinarMod.economy.getSalaryTax();
        Map<UUID, Double> taxes = DinarMod.economy.getPersonalTaxes();
        src.sendFeedback(() -> Text.literal("§6§l=== Taxes ===\n"
                + "§7  Globale (transactions) : §e" + (int) (global * 100) + "%\n"
                + "§7  Salaires : §e" + (int) (salary * 100) + "%\n"
                + "§7  Trésorerie : §e" + DinarMod.economy.money(DinarMod.economy.getTreasury())), false);
        if (taxes.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7  Aucune taxe personnelle."), false);
        } else {
            for (Map.Entry<UUID, Double> e : taxes.entrySet()) {
                String name = DinarMod.economy.accountName(e.getKey());
                double rate = e.getValue();
                src.sendFeedback(() -> Text.literal("§7  §e" + name + " §7» §e" + (int) (rate * 100) + "%"), false);
            }
        }
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
        Double personal = DinarMod.economy.getPersonalTax(ref.uuid());
        double effective = DinarMod.economy.effectiveTax(ref.uuid());
        String display = ref.displayName();
        src.sendFeedback(() -> Text.literal("§6§lTaxes de §e" + display + "§6\n"
                + "§7  Personnelle : §e" + (personal != null ? (int) (personal * 100) + "%" : "aucune (globale)") + "\n"
                + "§7  Effective (transaction reçue) : §e" + (int) (effective * 100) + "%\n"
                + "§7  Effective (salaire) : §e" + (int) (DinarMod.economy.salaryTaxFor(ref.uuid()) * 100) + "%"), false);
        return 1;
    }

    private TaxCommand() {}
}
