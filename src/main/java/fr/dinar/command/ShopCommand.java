package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.dinar.DinarMod;
import fr.dinar.economy.PlayerRef;
import fr.dinar.economy.ShopEntry;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

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
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        String itemName = StringArgumentType.getString(ctx, "nom_item");
        double buyPrice = DoubleArgumentType.getDouble(ctx, "prix_achat");
        double sellPrice = DoubleArgumentType.getDouble(ctx, "prix_vente");
        int maxStock = IntegerArgumentType.getInteger(ctx, "stock_max");
        if (buyPrice <= 0 || sellPrice <= 0) {
            ctx.getSource().sendError(DinarLang.text("§cLes prix doivent être positifs."));
            return 0;
        }
        ShopEntry shop = DinarMod.shops.create(player.getUuid(), player.getName().getString(),
                itemName, buyPrice, sellPrice, maxStock);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aShop §e#%s §acréé : §f%s §7(achat: §e%s §7| vente: §e%s§7 | stock max: §e%s§7)",
                shop.id, itemName, DinarMod.economy.money(buyPrice), DinarMod.economy.money(sellPrice), maxStock), false);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx, int page) {
        int pages = DinarMod.shops.pageCount(10);
        int currentPage = Math.min(page, pages);
        var list = DinarMod.shops.getPage(currentPage - 1, 10);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l══════ Shops (§e%s/%s§6) ══════", currentPage, pages), false);
        if (list.isEmpty()) {
            ctx.getSource().sendFeedback(() -> DinarLang.text("§7Aucun shop."), false);
        }
        for (ShopEntry s : list) {
            int id = s.id;
            String name = s.itemName;
            String owner = s.ownerName;
            double buy = s.buyPrice;
            double sell = s.sellPrice;
            int stock = s.stock;
            int max = s.maxStock;
            ctx.getSource().sendFeedback(() -> DinarLang.text("§e#%s §f%s §7par §e%s §7| Achat: §e%s §7| Vente: §e%s §7| Stock: §e%s/%s",
                    id, name, owner, DinarMod.economy.money(buy), DinarMod.economy.money(sell), stock, max), false);
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l════════════════════════"), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(DinarLang.text("§cShop introuvable : #%s", id));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§lShop #%s", id), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Item : §e%s", shop.itemName), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Propriétaire : §e%s", shop.ownerName), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Prix d'achat : §e%s", DinarMod.economy.money(shop.buyPrice)), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Prix de vente : §e%s", DinarMod.economy.money(shop.sellPrice)), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§7Stock : §e%s/%s", shop.stock, shop.maxStock), false);
        return 1;
    }

    private static int buy(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        int qty = IntegerArgumentType.getInteger(ctx, "quantite");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(DinarLang.text("§cShop introuvable : #%s", id));
            return 0;
        }
        if (shop.stock < qty) {
            ctx.getSource().sendError(DinarLang.text("§cStock insuffisant (§e%s§c disponible).", shop.stock));
            return 0;
        }
        double total = shop.buyPrice * qty;
        if (!DinarMod.economy.deductFromBalance(player.getUuid(), player.getName().getString(), total)) {
            ctx.getSource().sendError(DinarLang.text("§cSolde insuffisant. Il vous faut §e%s", DinarMod.economy.money(total)));
            return 0;
        }
        DinarMod.economy.add(shop.ownerUuid, shop.ownerName, total);
        DinarMod.economy.logTransaction(player.getUuid(), "SHOP_BUY", total, shop.ownerName,
                "Achat: " + shop.itemName + " x" + qty);
        shop.stock -= qty;
        String item = shop.itemName;
        double price = shop.buyPrice;
        int finalQty = qty;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aAchat de §e%s x%s §apour §e%s",
                item, finalQty, DinarMod.economy.money(price * finalQty)), false);
        ServerPlayerEntity owner = DinarMod.economy.online(shop.ownerUuid);
        if (owner != null) {
            String buyerName = player.getName().getString();
            int fQty = qty;
            double fTotal = total;
            String fItem = shop.itemName;
            owner.sendMessage(DinarLang.text("§e%s §aachète §e%s x%s §adans votre shop §7(+%s)",
                    buyerName, fItem, fQty, DinarMod.economy.money(fTotal)), false);
        }
        return 1;
    }

    private static int sell(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        int qty = IntegerArgumentType.getInteger(ctx, "quantite");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(DinarLang.text("§cShop introuvable : #%s", id));
            return 0;
        }
        if (shop.stock + qty > shop.maxStock) {
            ctx.getSource().sendError(DinarLang.text("§cStock maximal atteint (§e%s§c).", shop.maxStock));
            return 0;
        }
        double total = shop.sellPrice * qty;
        if (!DinarMod.economy.deductFromBalance(shop.ownerUuid, shop.ownerName, total)) {
            ctx.getSource().sendError(DinarLang.text("§cLe propriétaire n'a pas assez d'argent pour acheter."));
            return 0;
        }
        DinarMod.economy.add(player.getUuid(), player.getName().getString(), total);
        DinarMod.economy.logTransaction(shop.ownerUuid, "SHOP_SELL", total, player.getName().getString(),
                "Vente: " + shop.itemName + " x" + qty);
        shop.stock += qty;
        String item = shop.itemName;
        double price = shop.sellPrice;
        int finalQty = qty;
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aVente de §e%s x%s §apour §e%s",
                item, finalQty, DinarMod.economy.money(price * finalQty)), false);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(DinarLang.text("§cCommande joueur uniquement."));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ShopEntry shop = DinarMod.shops.get(id);
        if (shop == null) {
            ctx.getSource().sendError(DinarLang.text("§cShop introuvable : #%s", id));
            return 0;
        }
        if (!shop.isOwner(player.getUuid()) && !ctx.getSource().hasPermissionLevel(2)) {
            ctx.getSource().sendError(DinarLang.text("§cCe shop ne vous appartient pas."));
            return 0;
        }
        DinarMod.shops.remove(id);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§aShop §e#%s §asupprimé.", id), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l══════ Shops ══════"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/shop create <item> <prix_achat> <prix_vente> <stock_max>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/shop list [page]"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/shop info <id>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/shop buy <id> <quantite>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/shop sell <id> <quantite>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§e/shop remove <id>"), false);
        ctx.getSource().sendFeedback(() -> DinarLang.text("§6§l════════════════════"), false);
        return 1;
    }

    private ShopCommand() {}
}
