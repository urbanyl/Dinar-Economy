package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.ShopEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ShopCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("shop")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("nom_item", StringArgumentType.word())
                                .then(CommandManager.argument("prix_achat", DoubleArgumentType.doubleArg(0))
                                        .then(CommandManager.argument("prix_vente", DoubleArgumentType.doubleArg(0))
                                                .then(CommandManager.argument("stock_max", IntegerArgumentType.integer(1))
                                                        .executes(ShopCommand::create))))))
                .then(CommandManager.literal("list")
                        .executes(ctx -> ShopCommand.list(ctx, 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> ShopCommand.list(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ShopCommand::info)))
                .then(CommandManager.literal("buy")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("quantite", IntegerArgumentType.integer(1))
                                        .executes(ShopCommand::buy))))
                .then(CommandManager.literal("sell")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("quantite", IntegerArgumentType.integer(1))
                                        .executes(ShopCommand::sell))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ShopCommand::remove)))
                .then(CommandManager.literal("help").executes(ShopCommand::help)));
    }

    private static int create(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        String itemName = StringArgumentType.getString(ctx, "nom_item");
        double buyPrice = DoubleArgumentType.getDouble(ctx, "prix_achat");
        double sellPrice = DoubleArgumentType.getDouble(ctx, "prix_vente");
        int maxStock = IntegerArgumentType.getInteger(ctx, "stock_max");
        if (buyPrice <= 0 || sellPrice <= 0) {
            ctx.getSource().sendError(Text.literal("§cLes prix doivent être positifs."));
            return 0;
        }
        ShopEntry shop = DinarMod.shops.create(player.getUuid(), player.getName().getString(),
                itemName, buyPrice, sellPrice, maxStock);
        ctx.getSource().sendFeedback(() -> Text.literal("§aShop §e#" + shop.id + " §acréé : §f" + itemName
                + " §7(achat: §e" + DinarMod.economy.money(buyPrice) + " §7| vente: §e" + DinarMod.economy.money(sellPrice)
                + "§7 | stock max: §e" + maxStock + "§7)"), false);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx, int page) {
        int pages = DinarMod.shops.pageCount(10);
        int currentPage = Math.min(page, pages);
        var list = DinarMod.shops.getPage(currentPage - 1, 10);
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l══════ Shops (§e" + currentPage + "/" + pages + "§6) ══════"), false);
        if (list.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7Aucun shop."), false);
        }
        for (ShopEntry s : list) {
            int id = s.id;
            String name = s.itemName;
            String owner = s.ownerName;
            double buy = s.buyPrice;
            double sell = s.sellPrice;
            int stock = s.stock;
            int max = s.maxStock;
            ctx.getSource().sendFeedback(() -> Text.literal("§e#" + id + " §f" + name + " §7par §e" + owner
                    + " §7| Achat: §e" + DinarMod.economy.money(buy) + " §7| Vente: §e" + DinarMod.economy.money(sell)
                    + " §7| Stock: §e" + stock + "/" + max), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l════════════════════════"), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(Text.literal("§cShop introuvable : #" + id));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§6§lShop #" + id), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Item : §e" + shop.itemName), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Propriétaire : §e" + shop.ownerName), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Prix d'achat : §e" + DinarMod.economy.money(shop.buyPrice)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Prix de vente : §e" + DinarMod.economy.money(shop.sellPrice)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Stock : §e" + shop.stock + "/" + shop.maxStock), false);
        return 1;
    }

    private static int buy(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        int qty = IntegerArgumentType.getInteger(ctx, "quantite");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(Text.literal("§cShop introuvable : #" + id));
            return 0;
        }
        if (shop.stock < qty) {
            ctx.getSource().sendError(Text.literal("§cStock insuffisant (§e" + shop.stock + "§c disponible)."));
            return 0;
        }
        double total = shop.buyPrice * qty;
        if (!DinarMod.economy.deductFromBalance(player.getUuid(), player.getName().getString(), total)) {
            ctx.getSource().sendError(Text.literal("§cSolde insuffisant. Il vous faut §e" + DinarMod.economy.money(total)));
            return 0;
        }
        DinarMod.economy.add(shop.ownerUuid, shop.ownerName, total);
        DinarMod.economy.logTransaction(player.getUuid(), "SHOP_BUY", total, shop.ownerName,
                "Achat: " + shop.itemName + " x" + qty);
        shop.stock -= qty;
        String item = shop.itemName;
        double price = shop.buyPrice;
        int finalQty = qty;
        ctx.getSource().sendFeedback(() -> Text.literal("§aAchat de §e" + item + " x" + finalQty
                + " §apour §e" + DinarMod.economy.money(price * finalQty)), false);
        ServerPlayerEntity owner = DinarMod.economy.online(shop.ownerUuid);
        if (owner != null) {
            String buyerName = player.getName().getString();
            int fQty = qty;
            double fTotal = total;
            String fItem = shop.itemName;
            owner.sendMessage(Text.literal("§e" + buyerName + " §aachète §e" + fItem + " x" + fQty
                    + " §adans votre shop §7(+" + DinarMod.economy.money(fTotal) + ")"), false);
        }
        return 1;
    }

    private static int sell(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        int qty = IntegerArgumentType.getInteger(ctx, "quantite");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(Text.literal("§cShop introuvable : #" + id));
            return 0;
        }
        if (shop.stock + qty > shop.maxStock) {
            ctx.getSource().sendError(Text.literal("§cStock maximal atteint (§e" + shop.maxStock + "§c)."));
            return 0;
        }
        double total = shop.sellPrice * qty;
        if (!DinarMod.economy.deductFromBalance(shop.ownerUuid, shop.ownerName, total)) {
            ctx.getSource().sendError(Text.literal("§cLe propriétaire n'a pas assez d'argent pour acheter."));
            return 0;
        }
        DinarMod.economy.add(player.getUuid(), player.getName().getString(), total);
        DinarMod.economy.logTransaction(shop.ownerUuid, "SHOP_SELL", total, player.getName().getString(),
                "Vente: " + shop.itemName + " x" + qty);
        shop.stock += qty;
        String item = shop.itemName;
        double price = shop.sellPrice;
        int finalQty = qty;
        ctx.getSource().sendFeedback(() -> Text.literal("§aVente de §e" + item + " x" + finalQty
                + " §apour §e" + DinarMod.economy.money(price * finalQty)), false);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(Text.literal("§cShop introuvable : #" + id));
            return 0;
        }
        if (!shop.isOwner(player.getUuid()) && !ctx.getSource().hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("§cCe shop ne vous appartient pas."));
            return 0;
        }
        DinarMod.shops.remove(id);
        ctx.getSource().sendFeedback(() -> Text.literal("§aShop §e#" + id + " §asupprimé."), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l══════ Shops ══════"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/shop create <item> <prix_achat> <prix_vente> <stock_max>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/shop list [page]"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/shop info <id>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/shop buy <id> <quantite>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/shop sell <id> <quantite>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e/shop remove <id>"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§6§l════════════════════"), false);
        return 1;
    }

    private ShopCommand() {}
}
