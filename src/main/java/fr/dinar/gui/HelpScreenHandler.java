package fr.dinar.gui;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class HelpScreenHandler extends GenericContainerScreenHandler {
    private final SimpleInventory panelInv;
    private final ServerPlayerEntity viewer;

    public HelpScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity viewer) {
        this(syncId, playerInventory, viewer, new SimpleInventory(54));
    }

    private HelpScreenHandler(int syncId, PlayerInventory pi, ServerPlayerEntity viewer, SimpleInventory inv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, pi, inv, 6);
        this.panelInv = inv;
        this.viewer = viewer;
        build();
    }

    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new HelpScreenHandler(syncId, inv, player),
                Text.literal("§6§lAide Dinar").setStyle(Style.EMPTY.withColor(Formatting.GOLD).withItalic(false))));
    }

    private void build() {
        for (int i = 0; i < 54; i++) panelInv.setStack(i, ItemStack.EMPTY);

        panelInv.setStack(0, named(Items.BARRIER, "§cFermer"));

        panelInv.setStack(4, titled(Items.GOLD_INGOT, "§6§l═══ ÉCONOMIE ═══"));
        panelInv.setStack(9, cmd(Items.EMERALD, "/bal", "Voir votre solde"));
        panelInv.setStack(10, cmd(Items.EMERALD, "/pay <joueur> <montant>", "Envoyer de l'argent"));
        panelInv.setStack(11, cmd(Items.EMERALD, "/dmd <joueur> <montant>", "Demander de l'argent"));
        panelInv.setStack(12, cmd(Items.EMERALD, "/baltop", "Classement des plus riches"));

        panelInv.setStack(13, titled(Items.GOLD_BLOCK, "§6§l═══ BANQUE & PRÊTS ═══"));
        panelInv.setStack(14, cmd(Items.GOLD_BLOCK, "/bank balance", "Solde bancaire"));
        panelInv.setStack(15, cmd(Items.GOLD_BLOCK, "/bank deposit|withdraw", "Déposer/Retirer"));
        panelInv.setStack(16, cmd(Items.GOLD_NUGGET, "/loan take|repay", "Prêts & remboursement"));

        panelInv.setStack(22, titled(Items.CHEST, "§6§l═══ RP ═══"));
        panelInv.setStack(27, cmd(Items.EMERALD_BLOCK, "/shop create|buy|sell", "Magasins de joueurs"));
        panelInv.setStack(28, cmd(Items.EMERALD_BLOCK, "/shop list", "Voir les shops"));
        panelInv.setStack(29, cmd(Items.GOLD_BLOCK, "/ah sell|buy|list", "Auction House"));
        panelInv.setStack(30, cmd(Items.NETHER_STAR, "/entreprise create", "Créer une entreprise"));
        panelInv.setStack(31, cmd(Items.NETHER_STAR, "/entreprise invite|kick", "Gérer les membres"));
        panelInv.setStack(32, cmd(Items.NETHER_STAR, "/entreprise depot|withdraw", "Trésor d'entreprise"));
        panelInv.setStack(33, cmd(Items.PAPER, "/contract create|sign", "Contrats entre joueurs"));

        panelInv.setStack(36, titled(Items.BOOK, "§6§l═══ CALIPHAT ═══"));
        panelInv.setStack(40, cmd(Items.WRITABLE_BOOK, "/caliphat info", "Info sur le calife"));
        panelInv.setStack(41, cmd(Items.WRITABLE_BOOK, "/loi liste|livre|voter", "Système de lois"));
        panelInv.setStack(42, cmd(Items.WRITABLE_BOOK, "/loi decret", "Décret en cours"));
        panelInv.setStack(43, cmd(Items.WRITABLE_BOOK, "/amende <joueur> <montant>", "Amende (calife)"));

        panelInv.setStack(45, titled(Items.NETHERITE_INGOT, "§6§l═══ ADMIN ═══"));
        panelInv.setStack(46, cmd(Items.NETHERITE_INGOT, "/eco give|take|set|panel", "Admin économie"));
        panelInv.setStack(47, cmd(Items.NETHERITE_INGOT, "/salary set|list|payall", "Gestion salaires"));
        panelInv.setStack(48, cmd(Items.NETHERITE_INGOT, "/tax global|set|list", "Gestion taxes"));
        panelInv.setStack(49, cmd(Items.NETHERITE_INGOT, "/caliphat set|remove", "Gérer le calife"));
        panelInv.setStack(50, cmd(Items.NETHERITE_INGOT, "/dinar reload", "Recharger config"));
    }

    private static ItemStack named(net.minecraft.item.Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return s;
    }

    private static ItemStack titled(net.minecraft.item.Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        s.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Commandes de la catégorie"))));
        return s;
    }

    private static ItemStack cmd(net.minecraft.item.Item item, String command, String desc) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§e" + command).setStyle(Style.EMPTY.withItalic(false)));
        s.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7" + desc))));
        return s;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp && sp.getUuid().equals(viewer.getUuid())) {
            if (slotIndex == 0) {
                viewer.closeHandledScreen();
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
