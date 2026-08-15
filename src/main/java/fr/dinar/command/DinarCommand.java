package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.config.DinarConfig;
import fr.dinar.gui.HelpScreenHandler;
import fr.dinar.lang.DinarLang;
import fr.dinar.logs.DiscordWebhook;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DinarCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dinar")
                .then(CommandManager.literal("help").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null) {
                        HelpScreenHandler.open(player);
                    } else {
                        sendHelp(ctx);
                    }
                    return 1;
                }))
                .then(CommandManager.literal("scoreboard")
                        .then(CommandManager.literal("on").executes(ctx -> setScoreboard(ctx, true)))
                        .then(CommandManager.literal("off").executes(ctx -> setScoreboard(ctx, false)))
                        .then(CommandManager.literal("status").executes(DinarCommand::scoreboardStatus))
                        .requires(s -> s.hasPermissionLevel(2)))
                .then(CommandManager.literal("reload").executes(DinarCommand::reload)
                        .requires(s -> s.hasPermissionLevel(2)))
                .then(CommandManager.literal("webhook")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                        .executes(DinarCommand::webhookSet)))
                        .then(CommandManager.literal("enable").executes(ctx -> webhookToggle(ctx, true)))
                        .then(CommandManager.literal("disable").executes(ctx -> webhookToggle(ctx, false)))
                        .then(CommandManager.literal("title")
                                .then(CommandManager.argument("titre", StringArgumentType.greedyString())
                                        .executes(DinarCommand::webhookTitle)))
                        .then(CommandManager.literal("test").executes(DinarCommand::webhookTest))
                        .then(CommandManager.literal("status").executes(DinarCommand::webhookStatus))
                        .requires(s -> s.hasPermissionLevel(2)))
                .then(CommandManager.literal("about").executes(DinarCommand::about)));
    }

    private static int setScoreboard(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        DinarMod.config.scoreboard.enabled = enabled;
        DinarMod.config.save();
        if (enabled) {
            DinarMod.economy.getScoreboard().start(ctx.getSource().getServer());
        } else {
            DinarMod.economy.getScoreboard().stop();
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aScoreboard Dinar %s.",
                enabled ? DinarLang.t("activé.") : DinarLang.t("désactivé.")), true);
        return 1;
    }

    private static int scoreboardStatus(CommandContext<ServerCommandSource> ctx) {
        boolean active = DinarMod.economy.getScoreboard().isActive();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lScoreboard Dinar §r§7» §e%s",
                active ? DinarLang.t("activé") : DinarLang.t("désactivé")), false);
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        DinarMod.config = DinarConfig.load();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aConfiguration Dinar rechargée."), true);
        return 1;
    }

    private static int webhookSet(CommandContext<ServerCommandSource> ctx) {
        String url = StringArgumentType.getString(ctx, "url");
        if (!url.startsWith("https://discord.com/api/webhooks/")) {
            ctx.getSource().sendError(DinarLang.text("§cURL invalide : elle doit commencer par "
                    + "§fhttps://discord.com/api/webhooks/§c."));
            return 0;
        }
        DinarMod.config.discordWebhook = url;
        DinarMod.config.save();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aWebhook Discord défini : §f%s", url), true);
        return 1;
    }

    private static int webhookToggle(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        DinarMod.config.discordWebhookEnabled = enabled;
        DinarMod.config.save();
        ctx.getSource().sendFeedback(() -> DinarLang.text(enabled
                ? "§aWebhook Discord activé." : "§aWebhook Discord désactivé."), true);
        return 1;
    }

    private static int webhookTitle(CommandContext<ServerCommandSource> ctx) {
        String title = StringArgumentType.getString(ctx, "titre");
        if (title.length() > 60) {
            ctx.getSource().sendError(DinarLang.text("§cTitre trop long (§f60 caractères max§c)."));
            return 0;
        }
        DinarMod.config.discordWebhookTitle = title;
        DinarMod.config.save();
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aTitre du webhook défini : §e%s", title), true);
        return 1;
    }

    private static int webhookTest(CommandContext<ServerCommandSource> ctx) {
        if (DinarMod.config.discordWebhook == null || DinarMod.config.discordWebhook.isBlank()) {
            ctx.getSource().sendError(DinarLang.text("§cAucun webhook configuré (§f/dinar webhook set <url>§c)."));
            return 0;
        }
        if (!DinarMod.config.discordWebhookEnabled) {
            ctx.getSource().sendError(DinarLang.text("§cLe webhook est désactivé (§f/dinar webhook enable§c)."));
            return 0;
        }
        DinarMod.rpLog.sendEmbed(new DiscordWebhook.DiscordEmbed()
                .title(DinarMod.config.discordWebhookTitle)
                .description(DinarLang.t("✅ **Test** : le webhook est fonctionnel."))
                .field(DinarLang.t("Catégories"), DinarLang.t("Police, amendes, justice, prison, courrier, économie…"))
                .color(0x2ECC71)
                .footer("Dinar RP"));
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aMessage de test envoyé sur le webhook Discord."), true);
        return 1;
    }

    private static int webhookStatus(CommandContext<ServerCommandSource> ctx) {
        boolean configured = DinarMod.config.discordWebhook != null && !DinarMod.config.discordWebhook.isBlank();
        boolean enabled = DinarMod.config.discordWebhookEnabled && configured;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lWebhook Discord"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text(enabled ? "§7État : §a✅ actif" : "§7État : §c❌ inactif"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text(DinarMod.config.discordWebhookEnabled
                ? "§7Activé : §aoui" : "§7Activé : §cnon"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Titre : §e%s", DinarMod.config.discordWebhookTitle), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7URL : §f%s",
                configured ? DinarMod.config.discordWebhook : DinarLang.t("§7URL : §c(non définie)")), false);
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> ctx) {
        String version = FabricLoader.getInstance().getModContainer(DinarMod.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lDinar Economy §r§7v%s §7- Mod d'économie et de caliphat.", version), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/dinar help §7pour les commandes"), false);
        if (DinarMod.government.hasLeader()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Calife : §e%s", DinarMod.government.getLeaderName()), false);
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Lois adoptées : §e%s", DinarMod.government.getAdoptedLawCount()), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Trésorerie : §e%s", DinarMod.economy.money(DinarMod.economy.getTreasury())), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Comptes : §e%s", DinarMod.economy.accountCount()), false);
        return 1;
    }

    private static void sendHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> DinarLang.text("§6§l══════ Dinar Help ══════"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bal §7- Voir votre solde"), false);
        src.sendFeedback(() -> DinarLang.text("§e/pay <joueur> <montant> §7- Envoyer de l'argent"), false);
        src.sendFeedback(() -> DinarLang.text("§e/dmd <joueur> <montant> §7- Demander de l'argent"), false);
        src.sendFeedback(() -> DinarLang.text("§e/baltop §7- Classement"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank balance §7- Solde bancaire"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank deposit <montant> §7- Déposer en banque"), false);
        src.sendFeedback(() -> DinarLang.text("§e/bank withdraw <montant> §7- Retirer de la banque"), false);
        src.sendFeedback(() -> DinarLang.text("§e/loan take <montant> <taux> <durée> §7- Prêt"), false);
        src.sendFeedback(() -> DinarLang.text("§e/loan repay <montant> §7- Rembourser un prêt"), false);
        src.sendFeedback(() -> DinarLang.text("§e/loi liste §7- Voir les lois"), false);
        src.sendFeedback(() -> DinarLang.text("§e/loi livre §7- Livre des lois adoptées"), false);
        src.sendFeedback(() -> DinarLang.text("§e/loi voter §7- Voter sur une loi"), false);
        src.sendFeedback(() -> DinarLang.text("§e/caliphat info §7- Info calife"), false);
        src.sendFeedback(() -> DinarLang.text("§6§l══════════════════════"), false);
    }

    private DinarCommand() {}
}
