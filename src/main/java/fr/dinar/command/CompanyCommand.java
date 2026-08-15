package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.CompanyEntry;
import fr.dinar.economy.PlayerRef;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

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
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
                                .executes(CompanyCommand::invite)))
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("joueur", PlayerArgumentType.player())
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
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "nom");
        if (name.length() < 2 || name.length() > 20) {
            ctx.getSource().sendError(DinarLang.text("§cLe nom doit faire entre 2 et 20 caractères."));
            return 0;
        }
        CompanyEntry company = DinarMod.companies.create(name, player.getUuid(), player.getName().getString());
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cUne entreprise avec ce nom existe déjà."));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aEntreprise §e%s §acréée !", name), false);
        return 1;
    }

    private static int infoSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        var myCompanies = DinarMod.companies.getByMember(player.getUuid());
        if (myCompanies.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Vous n'avez pas d'entreprise."), false);
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
            ctx.getSource().sendError(DinarLang.text("§cEntreprise introuvable : §e%s", name));
            return 0;
        }
        showInfo(ctx, company);
        return 1;
    }

    private static void showInfo(CommandContext<ServerCommandSource> ctx, CompanyEntry c) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══ %s ═══", c.name), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Fondateur : §e%s", c.ownerName), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Membres : §e%s", c.members.size()), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Trésor : §e%s", DinarMod.economy.money(c.balance)), false);
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        var all = DinarMod.companies.getAll();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══ Entreprises (%s) ═══", all.size()), false);
        for (CompanyEntry c : all) {
            int id = c.id;
            String name = c.name;
            String owner = c.ownerName;
            int members = c.members.size();
            double bal = c.balance;
            ctx.getSource().sendFeedback(() -> DinarLang.text("§e#%s §f%s §7par §e%s §7| §e%s §7membres | §e%s",
                    id, name, owner, members, DinarMod.economy.money(bal)), false);
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══════════════════"), false);
        return 1;
    }

    private static int invite(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!company.isOwner(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cSeul le fondateur peut inviter."));
            return 0;
        }
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", targetName));
            return 0;
        }
        if (company.isMember(ref.uuid())) {
            ctx.getSource().sendError(DinarLang.text("§cCe joueur est déjà membre."));
            return 0;
        }
        company.addMember(ref.uuid());
        String companyName = company.name;
        String addedName = ref.displayName();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§a%s §aajouté à §e%s", addedName, companyName), false);
        ServerPlayerEntity target = ref.online();
        if (target != null) {
            String cn = company.name;
            target.sendMessage(DinarLang.text("§aVous avez été ajouté à l'entreprise §e%s§a.", cn), false);
        }
        return 1;
    }

    private static int kick(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "joueur");
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!company.isOwner(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cSeul le fondateur peut expulser."));
            return 0;
        }
        PlayerRef ref = DinarMod.economy.resolve(ctx.getSource(), targetName);
        if (ref == null) {
            ctx.getSource().sendError(DinarLang.text("§cJoueur introuvable : §e%s", targetName));
            return 0;
        }
        if (ref.uuid().equals(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cVous ne pouvez pas vous expulser vous-même."));
            return 0;
        }
        company.removeMember(ref.uuid());
        String addedName = ref.displayName();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§c%s §cexpulsé de §e%s", addedName, company.name), false);
        return 1;
    }

    private static int deposit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            ctx.getSource().sendError(DinarLang.text("§cLe montant doit être positif."));
            return 0;
        }
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!DinarMod.companies.deposit(company, player.getUuid(), player.getName().getString(), amount, DinarMod.economy)) {
            ctx.getSource().sendError(DinarLang.text("§cSolde insuffisant."));
            return 0;
        }
        String companyName = company.name;
        double bal = company.balance;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aDépôt de §e%s §7dans §e%s §7→ §e%s",
                DinarMod.economy.money(amount), companyName, DinarMod.economy.money(bal)), false);
        return 1;
    }

    private static int withdraw(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "montant");
        if (amount <= 0) {
            ctx.getSource().sendError(DinarLang.text("§cLe montant doit être positif."));
            return 0;
        }
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        if (!company.isMember(player.getUuid())) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'êtes pas membre de cette entreprise."));
            return 0;
        }
        if (!DinarMod.companies.withdraw(company, player.getUuid(), player.getName().getString(), amount, DinarMod.economy)) {
            ctx.getSource().sendError(DinarLang.text("§cTrésor insuffisant."));
            return 0;
        }
        String companyName = company.name;
        double bal = company.balance;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aRetrait de §e%s §7de §e%s §7→ §e%s",
                DinarMod.economy.money(amount), companyName, DinarMod.economy.money(bal)), false);
        return 1;
    }

    private static int members(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        CompanyEntry company = getMyCompany(player.getUuid());
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'avez pas d'entreprise."));
            return 0;
        }
        showMembers(ctx, company);
        return 1;
    }

    private static int membersOf(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "nom");
        CompanyEntry company = DinarMod.companies.getByName(name);
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cEntreprise introuvable : §e%s", name));
            return 0;
        }
        showMembers(ctx, company);
        return 1;
    }

    private static void showMembers(CommandContext<ServerCommandSource> ctx, CompanyEntry c) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lMembres de %s (%s)", c.name, c.members.size()), false);
        for (String m : c.members) {
            UUID uuid = UUID.fromString(m);
            String memberName = DinarMod.economy.accountName(uuid);
            boolean isOwner = c.isOwner(uuid);
            ctx.getSource().sendFeedback(() -> DinarLang.text(isOwner ? "§6★ %s" : "§e%s", memberName), false);
        }
    }

    private static int delete(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "nom");
        CompanyEntry company = DinarMod.companies.getByName(name);
        if (company == null) {
            ctx.getSource().sendError(DinarLang.text("§cEntreprise introuvable : §e%s", name));
            return 0;
        }
        if (!company.isOwner(player.getUuid()) && !ctx.getSource().hasPermissionLevel(2)) {
            ctx.getSource().sendError(DinarLang.text("§cVous n'êtes pas le fondateur."));
            return 0;
        }
        DinarMod.companies.remove(company.id);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aEntreprise §e%s §adissoute.", name), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══ Entreprises ═══"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise create <nom>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise info [nom]"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise list"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise invite <joueur>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise kick <joueur>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise depot <montant>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise withdraw <montant>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise members [nom]"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/entreprise delete <nom>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l═══════════════════"), false);
        return 1;
    }

    private static CompanyEntry getMyCompany(UUID uuid) {
        var list = DinarMod.companies.getByMember(uuid);
        return list.isEmpty() ? null : list.get(0);
    }

    private CompanyCommand() {}
}
