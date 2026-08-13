package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.CompanyEntry;
import fr.dinar.economy.PlayerRef;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class CompanyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("entreprise")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("nom", StringArgumentType.word())
                                .executes(CompanyCommand::create)))
                .then(CommandManager.literal("info")
                        .executes(CompanyCommand::infoSelf)
                        .then(CommandManager.argument("nom", StringArgumentType.word())
                                .executes(CompanyCommand::info)))
                .then(CommandManager.literal("list").executes(CompanyCommand::list))
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(CompanyCommand::invite)))
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(CompanyCommand::kick)))
                .then(CommandManager.literal("depot")
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .executes(CompanyCommand::deposit)))
                .then(CommandManager.literal("withdraw")
                        .then(CommandManager.argument("montant", DoubleArgumentType.doubleArg(0))
                                .executes(CompanyCommand::withdraw)))
                .then(CommandManager.literal("members").executes(CompanyCommand::members)
                        .then(CommandManager.argument("nom", StringArgumentType.word())
                                .executes(CompanyCommand::membersOf)))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("nom", StringArgumentType.word())
                                .executes(CompanyCommand::delete)))
                .then(CommandManager.literal("help").executes(CompanyCommand::help)));
    }

    private static int create(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "nom");
        if (name.length() < 2 || name.length() > 20) {
            ctx.getSource().sendError(Text.literal("§cLe nom doit faire entre 2 et 20 caractères."));
            return 0;
        }
        CompanyEntry company = DinarMod.companies.create(name, player.getUuid(), player.getName().getString());
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cUne entreprise avec ce nom existe déjà."));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§aEntreprise §e" + name + " §acréée !"), false);
        return 1;
    }

    private static int infoSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        var myCompanies = DinarMod.companies.getByMember(player.getUuid());
        if (myCompanies.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Vous n'avez pas d'entreprise."), false);
            return 0;
        }
        for (CompanyEntry c : myCompanies) {
            showInfo(ctx, c);
        }
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "nom");
        CompanyEntry company = DinarMod.companies.getByName(name);
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cEntreprise introuvable : §e" + name));
            return 0;
        }
        showInfo(ctx, company);
        return 1;
    }

    private static void showInfo(CommandContext<ServerCommandSource> ctx, CompanyEntry c) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══ " + c.name + " ═══"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Fondateur : §e" + c.ownerName), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Membres : §e" + c.members.size()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Trésor : §e" + DinarMod.economy.money(c.balance)), false);
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        var all = DinarMod.companies.getAll();
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══ Entreprises (" + all.size() + ") ═══"), false);
        for (CompanyEntry c : all) {
            int id = c.id;
            String name = c.name;
            String owner = c.ownerName;
            int members = c.members.size();
            double bal = c.balance;
            ctx.getSource().sendFeedback(() -> Text.literal("§e#" + id + " §f" + name + " §7par §e" + owner
                    + " §7| §e" + members + " §7membres | §e" + DinarMod.economy.money(bal)), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══════════════════"), false);
        return 1;
    }

    private static int invite(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!company.isOwner(player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cSeul le fondateur peut inviter."));
            return 0;
        }
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + targetName));
            return 0;
        }
        if (company.isMember(ref.uuid())) {
            ctx.getSource().sendError(Text.literal("§cCe joueur est déjà membre."));
            return 0;
        }
        company.addMember(ref.uuid());
        String companyName = company.name;
        String addedName = ref.displayName();
        ctx.getSource().sendFeedback(() -> Text.literal("§a" + addedName + " §aajouté à §e" + companyName), false);
        ServerPlayerEntity target = ref.online();
        if (target != null) {
            String cn = company.name;
            target.sendMessage(Text.literal("§aVous avez été ajouté à l'entreprise §e" + cn + "§a."), false);
        }
        return 1;
    }

    private static int kick(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!company.isOwner(player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cSeul le fondateur peut expulser."));
            return 0;
        }
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(Text.literal("§cJoueur introuvable : §e" + targetName));
            return 0;
        }
        if (ref.uuid().equals(player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cVous ne pouvez pas vous expulser vous-même."));
            return 0;
        }
        company.removeMember(ref.uuid());
        String addedName = ref.displayName();
        ctx.getSource().sendFeedback(() -> Text.literal("§c" + addedName + " §cexpulsé de §e" + company.name), false);
        return 1;
    }

    private static int deposit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            ctx.getSource().sendError(Text.literal("§cLe montant doit être positif."));
            return 0;
        }
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!DinarMod.companies.deposit(company, player.getUuid(), player.getName().getString(), amount, DinarMod.economy)) {
            ctx.getSource().sendError(Text.literal("§cSolde insuffisant."));
            return 0;
        }
        String companyName = company.name;
        double bal = company.balance;
        ctx.getSource().sendFeedback(() -> Text.literal("§aDépôt de §e" + DinarMod.economy.money(amount)
                + " §7dans §e" + companyName + " §7→ §e" + DinarMod.economy.money(bal)), false);
        return 1;
    }

    private static int withdraw(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            ctx.getSource().sendError(Text.literal("§cLe montant doit être positif."));
            return 0;
        }
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!company.isMember(player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cVous n'êtes pas membre de cette entreprise."));
            return 0;
        }
        if (!DinarMod.companies.withdraw(company, player.getUuid(), player.getName().getString(), amount, DinarMod.economy)) {
            ctx.getSource().sendError(Text.literal("§cTrésor insuffisant."));
            return 0;
        }
        String companyName = company.name;
        double bal = company.balance;
        ctx.getSource().sendFeedback(() -> Text.literal("§aRetrait de §e" + DinarMod.economy.money(amount)
                + " §7de §e" + companyName + " §7→ §e" + DinarMod.economy.money(bal)), false);
        return 1;
    }

    private static int members(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        showMembers(ctx, company);
        return 1;
    }

    private static int membersOf(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "nom");
        CompanyEntry company = DinarMod.companies.getByName(name);
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cEntreprise introuvable : §e" + name));
            return 0;
        }
        showMembers(ctx, company);
        return 1;
    }

    private static void showMembers(CommandContext<ServerCommandSource> ctx, CompanyEntry c) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lMembres de " + c.name + " (" + c.members.size() + ")"), false);
        for (String m : c.members) {
            UUID uuid = UUID.fromString(m);
            String memberName = DinarMod.economy.accountName(uuid);
            boolean isOwner = c.isOwner(uuid);
            ctx.getSource().sendFeedback(() -> Text.literal((isOwner ? "§6★ " : "§e") + memberName), false);
        }
    }

    private static int delete(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "nom");
        CompanyEntry company = DinarMod.companies.getByName(name);
        if (company == null) {
            ctx.getSource().sendError(Text.literal("§cEntreprise introuvable : §e" + name));
            return 0;
        }
        if (!company.isOwner(player.getUuid()) && !ctx.getSource().hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("§cVous n'êtes pas le fondateur."));
            return 0;
        }
        DinarMod.companies.remove(company.id);
        ctx.getSource().sendFeedback(() -> Text.literal("§aEntreprise §e" + name + " §adissoute."), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══ Entreprises ═══"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise create <nom>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise info [nom]"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise list"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise invite <joueur>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise kick <joueur>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise depot <montant>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise withdraw <montant>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise members [nom]"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/entreprise delete <nom>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══════════════════"), false);
        return 1;
    }

    private static CompanyEntry getMyCompany(UUID uuid) {
        var list = DinarMod.companies.getByMember(uuid);
        return list.isEmpty() ? null : list.get(0);
    }

    private CompanyCommand() {}
}
