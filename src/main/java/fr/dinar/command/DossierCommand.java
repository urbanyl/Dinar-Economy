package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.justice.CaseEntry;
import fr.dinar.justice.RecordEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class DossierCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dossier")
                .then(CommandManager.literal("moi").executes(DossierCommand::self))
                .then(CommandManager.literal("voir")
                        .requires(DossierCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(DossierCommand::view)))
                .then(CommandManager.literal("delit")
                        .requires(DossierCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("motif", StringArgumentType.greedyString())
                                        .executes(DossierCommand::delit))))
                .then(CommandManager.literal("mandat")
                        .requires(DossierCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("motif", StringArgumentType.greedyString())
                                        .executes(DossierCommand::mandat))))
                .then(CommandManager.literal("jugement")
                        .requires(DossierCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("detail", StringArgumentType.word())
                                        .then(CommandManager.argument("peine", StringArgumentType.greedyString())
                                                .executes(DossierCommand::jugement)))))
                .then(CommandManager.literal("affaire")
                        .requires(DossierCommand::police)
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .then(CommandManager.argument("motif", StringArgumentType.greedyString())
                                        .executes(DossierCommand::affaire))))
                .then(CommandManager.literal("cloturer")
                        .requires(DossierCommand::police)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(DossierCommand::cloturer)))
                .then(CommandManager.literal("liste")
                        .requires(DossierCommand::police)
                        .executes(DossierCommand::liste))
                .executes(DossierCommand::help));
    }

    public static boolean police(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        return src.hasPermissionLevel(2)
                || DinarMod.government.isLeader(p != null ? p.getUuid() : null)
                || (p != null && DinarMod.police.isPolice(p.getUuid()));
    }

    private static int self(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        showFile(ctx, player.getUuid(), player.getName().getString());
        return 1;
    }

    private static int view(CommandContext<ServerCommandSource> ctx) {
        PlayerRef ref = resolve(ctx, StringArgumentType.getString(ctx, "joueur"));
        if (ref == null) return 0;
        showFile(ctx, ref.uuid(), ref.displayName());
        return 1;
    }

    private static void showFile(CommandContext<ServerCommandSource> ctx, UUID uuid, String name) {
        List<RecordEntry> recs = DinarMod.justice.getRecord(uuid);
        long delits = recs.stream().filter(r -> "DELIT".equals(r.type)).count();
        long mandats = recs.stream().filter(r -> "MANDAT".equals(r.type)).count();
        long jugements = recs.stream().filter(r -> "JUGEMENT".equals(r.type)).count();

        ctx.getSource().sendFeedback(() -> Text.literal("§6§lDossier judiciaire §r§7» §e" + name), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Casier : §c" + delits + " délit(s) §7| §e" + mandats
                + " mandat(s) §7| §d" + jugements + " jugement(s)"), false);

        if (recs.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Aucun antécédent."), false);
        } else {
            for (RecordEntry r : recs) {
                String line = "§8" + time(r.date) + " §7[" + typeColor(r.type) + "] §f" + r.detail
                        + (r.extra != null && !r.extra.isBlank() ? " §7(peine : §e" + r.extra + "§7)" : "")
                        + " §8— " + r.authorName;
                ctx.getSource().sendFeedback(() -> Text.literal(line), false);
            }
        }

        for (CaseEntry c : DinarMod.justice.getOpenCases()) {
            if (c.accusedUuid.equals(uuid.toString())) {
                ctx.getSource().sendFeedback(() -> Text.literal("§bAffaire ouverte #" + c.id + " §7» §f"
                        + c.motif + " §8(" + c.policeName + ")"), false);
            }
        }
    }

    private static int delit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        PlayerRef ref = resolve(ctx, StringArgumentType.getString(ctx, "joueur"));
        if (ref == null) return 0;
        String motif = StringArgumentType.getString(ctx, "motif");
        DinarMod.justice.addOffense(officer, ref.uuid(), ref.displayName(), motif);
        ctx.getSource().sendFeedback(() -> Text.literal("§aDélit enregistré pour §e" + ref.displayName() + " §7: §f" + motif), false);
        return 1;
    }

    public static int mandat(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        PlayerRef ref = resolve(ctx, StringArgumentType.getString(ctx, "joueur"));
        if (ref == null) return 0;
        String motif = StringArgumentType.getString(ctx, "motif");
        DinarMod.justice.issueWarrant(officer, ref.uuid(), ref.displayName(), motif);
        ctx.getSource().sendFeedback(() -> Text.literal("§aMandat d'arrêt émis contre §e" + ref.displayName()
                + " §7: §f" + motif), false);
        return 1;
    }

    private static int jugement(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        PlayerRef ref = resolve(ctx, StringArgumentType.getString(ctx, "joueur"));
        if (ref == null) return 0;
        String detail = StringArgumentType.getString(ctx, "detail");
        String peine = StringArgumentType.getString(ctx, "peine");
        DinarMod.justice.recordJudgment(officer, ref.uuid(), ref.displayName(), detail, peine);
        ctx.getSource().sendFeedback(() -> Text.literal("§aJugement rendu pour §e" + ref.displayName()
                + " §7: §f" + detail + " §7→ §e" + peine), false);
        return 1;
    }

    private static int affaire(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        PlayerRef ref = resolve(ctx, StringArgumentType.getString(ctx, "joueur"));
        if (ref == null) return 0;
        String motif = StringArgumentType.getString(ctx, "motif");
        DinarMod.justice.openCase(officer, ref.uuid(), ref.displayName(), motif);
        ctx.getSource().sendFeedback(() -> Text.literal("§aAffaire ouverte contre §e" + ref.displayName()), false);
        return 1;
    }

    private static int cloturer(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity officer = ctx.getSource().getPlayer();
        if (officer == null) {
            ctx.getSource().sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        CaseEntry c = DinarMod.justice.getCase(id);
        if (c == null) {
            ctx.getSource().sendError(Text.literal("§cAffaire introuvable : §e#" + id));
            return 0;
        }
        DinarMod.justice.closeCase(officer, id);
        ctx.getSource().sendFeedback(() -> Text.literal("§aAffaire #" + id + " clôturée."), false);
        return 1;
    }

    private static int liste(CommandContext<ServerCommandSource> ctx) {
        List<CaseEntry> cases = DinarMod.justice.getOpenCases();
        if (cases.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Aucune affaire ouverte."), false);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lAffaires ouvertes §r§7(§f" + cases.size() + "§7)"), false);
        for (CaseEntry c : cases) {
            ctx.getSource().sendFeedback(() -> Text.literal("§b#" + c.id + " §7» §e" + c.accusedName
                    + " §7— §f" + c.motif + " §8(" + c.policeName + ")"), false);
        }
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lDossier §7» §f/dossier moi §7| §f/dossier voir <joueur> "
                + "§7| §f/dossier delit <joueur> <motif> §7| §f/dossier mandat <joueur> <motif> "
                + "§7| §f/dossier jugement <joueur> <délit> <peine> §7| §f/dossier affaire <joueur> <motif> "
                + "§7| §f/dossier cloturer <id> §7| §f/dossier liste"), false);
        return 1;
    }

    private static PlayerRef resolve(CommandContext<ServerCommandSource> ctx, String name) {
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), name);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return null;
        }
        return ref;
    }

    private static String typeColor(String type) {
        switch (type) {
            case "DELIT": return "§cDÉLIT";
            case "MANDAT": return "§eMANDAT";
            case "JUGEMENT": return "§dJUGEMENT";
            default: return "§7" + type;
        }
    }

    private static String time(long millis) {
        return new SimpleDateFormat("dd/MM HH:mm").format(new Date(millis));
    }

    private DossierCommand() {}
}
