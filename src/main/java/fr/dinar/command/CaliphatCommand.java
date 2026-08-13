package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.government.GovernmentManager;
import fr.dinar.government.Law;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class CaliphatCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("caliphat")
                .then(CommandManager.literal("help").executes(CaliphatCommand::help))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(CaliphatCommand::setLeader))
                        .requires(s -> s.hasPermissionLevel(2)))
                .then(CommandManager.literal("remove")
                        .executes(CaliphatCommand::removeLeader)
                        .requires(s -> s.hasPermissionLevel(2)))
                .then(CommandManager.literal("info").executes(CaliphatCommand::leaderInfo))
                .then(CommandManager.literal("loi")
                        .then(CommandManager.literal("proposer")
                                .then(CommandManager.argument("titre", StringArgumentType.word())
                                        .then(CommandManager.argument("contenu", StringArgumentType.greedyString())
                                                .executes(CaliphatCommand::proposeLaw))))
                        .then(CommandManager.literal("promulguer")
                                .then(CommandManager.argument("titre", StringArgumentType.word())
                                        .then(CommandManager.argument("contenu", StringArgumentType.greedyString())
                                                .executes(CaliphatCommand::enactLaw))))
                        .then(CommandManager.literal("voter")
                                .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                        .executes(CaliphatCommand::startVote)))
                        .then(CommandManager.literal("liste").executes(CaliphatCommand::listLaws))
                        .then(CommandManager.literal("en attente").executes(CaliphatCommand::pendingLaws))
                        .then(CommandManager.literal("info")
                                .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                        .executes(CaliphatCommand::lawInfo))))
                .then(CommandManager.literal("decret")
                        .then(CommandManager.argument("texte", StringArgumentType.greedyString())
                                .executes(CaliphatCommand::setDecree))
                        .executes(CaliphatCommand::showDecree))
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("titre")
                                .then(CommandManager.literal("on").executes(ctx -> setTitle(ctx, true)))
                                .then(CommandManager.literal("off").executes(ctx -> setTitle(ctx, false))))
                        .then(CommandManager.literal("duree_vote")
                                .then(CommandManager.argument("secondes", IntegerArgumentType.integer(30, 3600))
                                        .executes(CaliphatCommand::setVoteDuration)))
                        .then(CommandManager.literal("votes_requis")
                                .then(CommandManager.argument("nombre", IntegerArgumentType.integer(1, 100))
                                        .executes(CaliphatCommand::setRequiredVotes)))
                        .requires(s -> s.hasPermissionLevel(2))));
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6§l══════ Caliphat ══════"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat info §7- Info sur le calife"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat set <joueur> §7- Nommer un calife"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat remove §7- Retirer le calife"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat loi proposer <titre> <contenu> §7- Proposer une loi"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat loi promulguer <titre> <contenu> §7- Promulguer"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat loi voter <id> §7- Ouvrir un vote"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat loi liste §7- Toutes les lois"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat loi info <id> §7- Détails d'une loi"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat decret <texte> §7- Publier un décret"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat config titre on|off §7- Titres à l'écran"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat config duree_vote <sec> §7- Durée du vote"), false);
        src.sendFeedback(() -> Text.literal("§e/caliphat config votes_requis <n> §7- Votes nécessaires"), false);
        src.sendFeedback(() -> Text.literal("§6§l════════════════════"), false);
        return 1;
    }

    private static int setLeader(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "joueur");
        var profile = src.getServer().getUserCache().findByName(name).orElse(null);
        if (profile == null) {
            src.sendError(Text.literal("§cJoueur introuvable : §e" + name));
            return 0;
        }
        GovernmentManager gov = DinarMod.government;
        gov.setLeader(profile.getId(), profile.getName());
        src.sendFeedback(() -> Text.literal("§a§lCalife nommé : §e" + profile.getName()), true);
        return 1;
    }

    private static int removeLeader(CommandContext<ServerCommandSource> ctx) {
        GovernmentManager gov = DinarMod.government;
        gov.removeLeader();
        ctx.getSource().sendFeedback(() -> Text.literal("§aCalife retiré."), true);
        return 1;
    }

    private static int leaderInfo(CommandContext<ServerCommandSource> ctx) {
        GovernmentManager gov = DinarMod.government;
        ServerCommandSource src = ctx.getSource();
        if (!gov.hasLeader()) {
            src.sendFeedback(() -> Text.literal("§6§lCaliphat §r§7» §7Aucun calife n'est nommé."), false);
            return 1;
        }
        src.sendFeedback(() -> Text.literal("§6§lCaliphat §r§7» §eCalife : §f" + gov.getLeaderName()), false);
        src.sendFeedback(() -> Text.literal("§7Lois adoptées : §e" + gov.getAdoptedLawCount()), false);
        if (gov.getDecree() != null && !gov.getDecree().isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Dernier décret : §f" + gov.getDecree()), false);
        }
        return 1;
    }

    private static int proposeLaw(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String title = StringArgumentType.getString(ctx, "titre");
        String content = StringArgumentType.getString(ctx, "contenu");
        GovernmentManager gov = DinarMod.government;
        Law law = gov.proposeLaw(player, title, content);
        src.sendFeedback(() -> Text.literal("§aLoi proposée §7» §f" + title + " §7(#" + law.id + ")"), false);
        return 1;
    }

    private static int enactLaw(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String title = StringArgumentType.getString(ctx, "titre");
        String content = StringArgumentType.getString(ctx, "contenu");
        GovernmentManager gov = DinarMod.government;
        Law law = gov.enactLaw(player, title, content);
        src.sendFeedback(() -> Text.literal("§aLoi promulguée §7» §f" + title + " §7(#" + law.id + ")"), false);
        return 1;
    }

    private static int startVote(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        GovernmentManager gov = DinarMod.government;
        if (gov.startVote(id)) {
            ctx.getSource().sendFeedback(() -> Text.literal("§aVote lancé pour la loi #" + id), false);
        } else {
            ctx.getSource().sendError(Text.literal("§cImpossible de lancer le vote (loi introuvable ou déjà en cours)."));
        }
        return 1;
    }

    private static int listLaws(CommandContext<ServerCommandSource> ctx) {
        GovernmentManager gov = DinarMod.government;
        List<Law> all = gov.getAllLaws();
        ServerCommandSource src = ctx.getSource();
        if (all.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Aucune loi enregistrée."), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§6§l══════ Lois ══════"), false);
        for (Law law : all) {
            String status;
            if (law.isAdopted()) status = "§aADOPTÉE";
            else if ("REJECTED".equals(law.status)) status = "§cREJETÉE";
            else status = "§eEN ATTENTE";
            final String s = status;
            final int lid = law.id;
            final String lt = law.title;
            final String la = law.authorName;
            src.sendFeedback(() -> Text.literal("§7#" + lid + " §f" + lt + " " + s
                    + " §7(par §e" + la + "§7)"), false);
        }
        src.sendFeedback(() -> Text.literal("§6§l════════════════"), false);
        return 1;
    }

    private static int pendingLaws(CommandContext<ServerCommandSource> ctx) {
        GovernmentManager gov = DinarMod.government;
        List<Law> pending = gov.getPendingLaws();
        ServerCommandSource src = ctx.getSource();
        if (pending.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Aucune loi en attente."), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§6§l══════ Lois en attente ══════"), false);
        for (Law law : pending) {
            final int lid = law.id;
            final String lt = law.title;
            final String la = law.authorName;
            src.sendFeedback(() -> Text.literal("§7#" + lid + " §f" + lt + " §7(par §e" + la + "§7)"), false);
        }
        src.sendFeedback(() -> Text.literal("§6§l══════════════════════════"), false);
        return 1;
    }

    private static int lawInfo(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        GovernmentManager gov = DinarMod.government;
        Law law = gov.getLaw(id);
        ServerCommandSource src = ctx.getSource();
        if (law == null) {
            src.sendError(Text.literal("§cLoi introuvable : #" + id));
            return 0;
        }
        String status;
        if (law.isAdopted()) status = "§aADOPTÉE";
        else if ("REJECTED".equals(law.status)) status = "§cREJETÉE";
        else status = "§eEN ATTENTE";
        src.sendFeedback(() -> Text.literal("§6§l══════ Loi #" + id + " ══════"), false);
        src.sendFeedback(() -> Text.literal("§eTitre : §f" + law.title), false);
        src.sendFeedback(() -> Text.literal("§eContenu : §f" + law.content), false);
        src.sendFeedback(() -> Text.literal("§eAuteur : §f" + law.authorName), false);
        src.sendFeedback(() -> Text.literal("§eStatut : " + status), false);
        if (law.totalVotes() > 0) {
            src.sendFeedback(() -> Text.literal("§eVotes : §a" + law.yesVotes + " OUI §7/ §c" + law.noVotes + " NON"), false);
        }
        return 1;
    }

    private static int setDecree(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String text = StringArgumentType.getString(ctx, "texte");
        DinarMod.government.setDecree(player, text);
        src.sendFeedback(() -> Text.literal("§aDécret publié."), false);
        return 1;
    }

    private static int showDecree(CommandContext<ServerCommandSource> ctx) {
        String d = DinarMod.government.getDecree();
        if (d == null || d.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Aucun décret en vigueur."), false);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("§6§lDécret §r§7» §f" + d), false);
        }
        return 1;
    }

    private static int setTitle(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        DinarMod.government.setTitleEnabled(enabled);
        ctx.getSource().sendFeedback(() -> Text.literal("§aTitres à l'écran " + (enabled ? "activés" : "désactivés") + "."), true);
        return 1;
    }

    private static int setVoteDuration(CommandContext<ServerCommandSource> ctx) {
        int sec = IntegerArgumentType.getInteger(ctx, "secondes");
        DinarMod.government.setVoteDurationSeconds(sec);
        ctx.getSource().sendFeedback(() -> Text.literal("§aDurée de vote : §e" + sec + " secondes"), true);
        return 1;
    }

    private static int setRequiredVotes(CommandContext<ServerCommandSource> ctx) {
        int n = IntegerArgumentType.getInteger(ctx, "nombre");
        DinarMod.government.setRequiredVotes(n);
        ctx.getSource().sendFeedback(() -> Text.literal("§aVotes requis : §e" + n), true);
        return 1;
    }

    private static ServerCommandSource src(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource();
    }

    private CaliphatCommand() {}
}
