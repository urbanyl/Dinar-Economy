package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.AuctionEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AuctionCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ah")
                .then(CommandManager.literal("sell")
                        .then(CommandManager.argument("prix", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> AuctionCommand.sell(ctx, 1))
                                .then(CommandManager.argument("quantite", IntegerArgumentType.integer(1))
                                        .executes(AuctionCommand::sell))))
                .then(CommandManager.literal("buy")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(AuctionCommand::buy)))
                .then(CommandManager.literal("list")
                        .executes(ctx -> AuctionCommand.list(ctx, 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> AuctionCommand.list(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(CommandManager.literal("cancel")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(AuctionCommand::cancel)))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(AuctionCommand::info)))
                .then(CommandManager.literal("help").executes(AuctionCommand::help)));
    }

    private static int sell(CommandContext<ServerCommandSource> ctx, int quantity) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        double price = DoubleArgumentType.getDouble(ctx, "prix");
        if (price <= 0) {
            ctx.getSource().sendError(Text.literal("§cLe prix doit être positif."));
            return 0;
        }
        if (player.getMainHandStack().isEmpty()) {
            ctx.getSource().sendError(Text.literal("§cVous devez tenir un item en main."));
            return 0;
        }
        String itemName = player.getMainHandStack().getName().getString();
        AuctionEntry auction = DinarMod.auctions.create(player.getUuid(), player.getName().getString(),
                itemName, quantity, price, 86400);
        ctx.getSource().sendFeedback(() -> Text.literal("§aMise en vente §e#" + auction.id + " §7: §f" + itemName
                + " x" + quantity + " §7pour §e" + DinarMod.economy.money(price) + " §7(24h)"), false);
        return 1;
    }

    private static int sell(CommandContext<ServerCommandSource> ctx) {
        return sell(ctx, 1);
    }

    private static int buy(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        AuctionEntry auction = DinarMod.auctions.get(id);
        if (auction == null || auction.isExpired()) {
            ctx.getSource().sendError(Text.literal("§cVente introuvable ou expirée."));
            return 0;
        }
        if (auction.sellerUuid.equals(player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cVous ne pouvez pas acheter votre propre vente."));
            return 0;
        }
        if (!DinarMod.economy.deductFromBalance(player.getUuid(), player.getName().getString(), auction.price)) {
            ctx.getSource().sendError(Text.literal("§cSolde insuffisant. Il vous faut §e" + DinarMod.economy.money(auction.price)));
            return 0;
        }
        DinarMod.economy.add(auction.sellerUuid, auction.sellerName, auction.price);
        DinarMod.economy.logTransaction(player.getUuid(), "AH_BUY", auction.price, auction.sellerName,
                "Achat: " + auction.itemName + " x" + auction.itemCount);
        DinarMod.economy.logTransaction(auction.sellerUuid, "AH_SELL", auction.price, player.getName().getString(),
                "Vente: " + auction.itemName + " x" + auction.itemCount);
        String itemName = auction.itemName;
        int itemCount = auction.itemCount;
        String sellerName = auction.sellerName;
        double price = auction.price;
        DinarMod.auctions.remove(id);
        ctx.getSource().sendFeedback(() -> Text.literal("§aAchat §e#" + id + " §7: §f" + itemName + " x" + itemCount
                + " §7à §e" + sellerName + " §7pour §e" + DinarMod.economy.money(price)), false);
        ServerPlayerEntity seller = DinarMod.economy.online(auction.sellerUuid);
        if (seller != null) {
            String buyerName = player.getName().getString();
            int fCount = itemCount;
            double fPrice = price;
            String fItem = itemName;
            seller.sendMessage(Text.literal("§e" + buyerName + " §aachète votre vente §e#" + id
                    + " §7: §f" + fItem + " x" + fCount + " §7pour §e" + DinarMod.economy.money(fPrice)), false);
        }
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx, int page) {
        var list = DinarMod.auctions.getPage(page - 1, 10);
        int pages = DinarMod.auctions.pageCount(10);
        int currentPage = Math.min(page, pages);
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══ Auction House (§e" + currentPage + "/" + pages + "§6) ═══"), false);
        if (list.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Aucune vente en cours."), false);
        }
        for (AuctionEntry a : list) {
            int id = a.id;
            String item = a.itemName;
            int count = a.itemCount;
            double price = a.price;
            String seller = a.sellerName;
            long time = a.timeRemainingSeconds();
            ctx.getSource().sendFeedback(() -> Text.literal("§e#" + id + " §f" + item + " x" + count
                    + " §7par §e" + seller + " §7pour §e" + DinarMod.economy.money(price)
                    + " §7(§e" + formatTime(time) + "§7)"), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l════════════════════════"), false);
        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!DinarMod.auctions.cancel(id, player.getUuid())) {
            ctx.getSource().sendError(Text.literal("§cImpossible d'annuler cette vente."));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§aVente §e#" + id + " §aannulée."), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        AuctionEntry auction = DinarMod.auctions.get(id);
        if (auction == null) {
            ctx.getSource().sendError(Text.literal("§cVente introuvable : #" + id));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lVente #" + id), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Item : §e" + auction.itemName + " x" + auction.itemCount), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Vendeur : §e" + auction.sellerName), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Prix : §e" + DinarMod.economy.money(auction.price)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Expire dans : §e" + formatTime(auction.timeRemainingSeconds())), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l═══ Auction House ═══"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/ah sell <prix> [quantite]"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/ah buy <id>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/ah list [page]"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/ah cancel <id>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/ah info <id>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l════════════════════════"), false);
        return 1;
    }

    private static String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h" + ((seconds % 3600) / 60) + "min";
        if (seconds >= 60) return (seconds / 60) + "min " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private AuctionCommand() {}
}
