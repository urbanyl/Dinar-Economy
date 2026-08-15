package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.government.GovernmentManager;
import fr.dinar.government.Law;
import fr.dinar.government.VoteSession;
import fr.dinar.gui.LawBookScreenHandler;
import fr.dinar.gui.VoteScreenHandler;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class LoiCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loi")
                .then(CommandManager.literal("livre").executes(LoiCommand::openBook))
                .then(CommandManager.literal("voter").executes(LoiCommand::openVote)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("vote", IntegerArgumentType.integer(0, 1))
                                        .executes(LoiCommand::voteById))))
                .then(CommandManager.literal("liste").executes(LoiCommand::listLaws))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(LoiCommand::lawInfo)))
                .then(CommandManager.literal("decret").executes(LoiCommand::showDecree))
                .then(CommandManager.literal("calife").executes(LoiCommand::caliphInfo)));
    }

    private static int openBook(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        LawBookScreenHandler.open(player);
        return 1;
    }

    private static int openVote(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        GovernmentManager gov = DinarMod.government;
        VoteSession vs = gov.getActiveVoteFor(player.getUuid());
        if (vs == null) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Aucun vote en cours pour vous."), false);
            return 0;
        }
        Law law = gov.getLaw(vs.lawId);
        if (law == null) return 0;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lVote en cours §r§7» §f%s §7(#%s) §e%s",
                law.title, law.id, vs.getStatusMessage()), false);
        VoteScreenHandler.open(player, law);
        return 1;
    }

    private static int voteById(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        int voteInt = IntegerArgumentType.getInteger(ctx, "vote");
        boolean yes = voteInt == 1;
        GovernmentManager gov = DinarMod.government;
        if (gov.vote(player.getUuid(), id, yes)) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§aVote enregistré : %s", yes ? "§aOUI" : "§cNON"), false);
        } else {
            ctx.getSource().sendError(DinarLang.text("§cVote impossible (loi introuvable, déjà voté, ou vote non ouvert)."));
        }
        return 1;
    }

    private static int listLaws(CommandContext<ServerCommandSource> ctx) {
        GovernmentManager gov = DinarMod.government;
        List<Law> all = gov.getAllLaws();
        ServerCommandSource src = ctx.getSource();
        if (all.isEmpty()) {
            src.sendFeedback(() -> DinarLang.text("§7Aucune loi enregistrée."), false);
            return 0;
        }
        src.sendFeedback(() -> DinarLang.text("§6§l══════ Lois ══════"), false);
        for (Law law : all) {
            String status;
            if (law.isAdopted()) status = "§aADOPTÉE";
            else if ("REJECTED".equals(law.status)) status = "§cREJETÉE";
            else status = "§eEN ATTENTE";
            final String s = status;
            final int lid = law.id;
            final String lt = law.title;
            final String la = law.authorName;
            src.sendFeedback(() -> DinarLang.text("§7#%s §f%s %s §7(par §e%s§7)", lid, lt, s, la), false);
        }
        src.sendFeedback(() -> DinarLang.text("§6§l════════════════"), false);
        return 1;
    }

    private static int lawInfo(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Law law = DinarMod.government.getLaw(id);
        ServerCommandSource src = ctx.getSource();
        if (law == null) {
            src.sendError(DinarLang.text("§cLoi introuvable : #%s", id));
            return 0;
        }
        String status;
        if (law.isAdopted()) status = "§aADOPTÉE";
        else if ("REJECTED".equals(law.status)) status = "§cREJETÉE";
        else status = "§eEN ATTENTE";
        src.sendFeedback(() -> DinarLang.text("§6§l══════ Loi #%s ══════", id), false);
        src.sendFeedback(() -> DinarLang.text("§eTitre : §f%s", law.title), false);
        src.sendFeedback(() -> DinarLang.text("§eContenu : §f%s", law.content), false);
        src.sendFeedback(() -> DinarLang.text("§eAuteur : §f%s", law.authorName), false);
        src.sendFeedback(() -> DinarLang.text("§eStatut : %s", status), false);
        if (law.totalVotes() > 0) {
            src.sendFeedback(() -> DinarLang.text("§eVotes : §a%s OUI §7/ §c%s NON", law.yesVotes, law.noVotes), false);
        }
        return 1;
    }

    private static int showDecree(CommandContext<ServerCommandSource> ctx) {
        String d = DinarMod.government.getDecree();
        if (d == null || d.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Aucun décret en vigueur."), false);
        } else {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lDécret §r§7» §f%s", d), false);
        }
        return 1;
    }

    private static int caliphInfo(CommandContext<ServerCommandSource> ctx) {
        GovernmentManager gov = DinarMod.government;
        ServerCommandSource src = ctx.getSource();
        if (!gov.hasLeader()) {
            src.sendFeedback(() -> DinarLang.text("§6§lCaliphat §r§7» §7Aucun calife n'est nommé."), false);
            return 1;
        }
        src.sendFeedback(() -> DinarLang.text("§6§lCaliphat §r§7» §eCalife : §f%s", gov.getLeaderName()), false);
        src.sendFeedback(() -> DinarLang.text("§7Lois adoptées : §e%s", gov.getAdoptedLawCount()), false);
        if (gov.getDecree() != null && !gov.getDecree().isEmpty()) {
            src.sendFeedback(() -> DinarLang.text("§7Décret : §f%s", gov.getDecree()), false);
        }
        return 1;
    }

    private LoiCommand() {}
}
