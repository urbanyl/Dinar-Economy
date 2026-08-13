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

        // Row 0: Economy commands
        panelInv.setStack(0, named(Items.BARRIER, "§cFermer"));
        panelInv.setStack(4, titled(Items.GOLD_INGOT, "§6§l═══ ÉCONOMIE ═══"));

        panelInv.setStack(9, cmd(Items.EMERALD, "/bal", "Voir votre solde"));
        panelInv.setStack(10, cmd(Items.EMERALD, "/bal <joueur>", "Solde d'un joueur"));
        panelInv.setStack(11, cmd(Items.EMERALD, "/pay <joueur> <montant>", "Envoyer de l'argent"));
        panelInv.setStack(12, cmd(Items.EMERALD, "/dmd <joueur> <montant>", "Demander de l'argent"));
        panelInv.setStack(13, cmd(Items.EMERALD, "/dmd accept|deny <id>", "Traiter une demande"));
        panelInv.setStack(14, cmd(Items.EMERALD, "/baltop", "Classement des plus riches"));
        panelInv.setStack(15, cmd(Items.EMERALD, "/dinar scoreboard on|off", "Scoreboard latéral"));
        panelInv.setStack(16, cmd(Items.EMERALD, "/dinar about", "À propos du mod"));

        // Row 1: Bank & Loan commands
        panelInv.setStack(18, titled(Items.GOLD_BLOCK, "§6§l═══ BANQUE & PRÊTS ═══"));

        panelInv.setStack(19, cmd(Items.GOLD_BLOCK, "/bank balance", "Solde bancaire"));
        panelInv.setStack(20, cmd(Items.GOLD_BLOCK, "/bank deposit <montant>", "Déposer en banque"));
        panelInv.setStack(21, cmd(Items.GOLD_BLOCK, "/bank withdraw <montant>", "Retirer de la banque"));
        panelInv.setStack(22, cmd(Items.GOLD_NUGGET, "/loan take <montant> <taux> <durée>", "Contracter un prêt"));
        panelInv.setStack(23, cmd(Items.GOLD_NUGGET, "/loan repay <montant>", "Rembourser un prêt"));
        panelInv.setStack(24, cmd(Items.GOLD_NUGGET, "/loan info", "Votre prêt"));

        // Row 2: Government commands
        panelInv.setStack(22, titled(Items.BOOK, "§6§l═══ CALIPHAT ═══"));

        panelInv.setStack(27, cmd(Items.WRITABLE_BOOK, "/caliphat info", "Info sur le calife"));
        panelInv.setStack(28, cmd(Items.WRITABLE_BOOK, "/loi liste", "Voir toutes les lois"));
        panelInv.setStack(29, cmd(Items.WRITABLE_BOOK, "/loi livre", "Livre des lois adoptées"));
        panelInv.setStack(30, cmd(Items.WRITABLE_BOOK, "/loi voter", "Voter sur une loi"));
        panelInv.setStack(31, cmd(Items.WRITABLE_BOOK, "/loi decret", "Voir le décret en cours"));
        panelInv.setStack(32, cmd(Items.WRITABLE_BOOK, "/loi calife", "Info du calife"));
        panelInv.setStack(33, cmd(Items.WRITABLE_BOOK, "/loi info <id>", "Détails d'une loi"));

        // Row 4: Admin government commands
        panelInv.setStack(40, titled(Items.NETHERITE_INGOT, "§6§l═══ ADMIN ═══"));

        panelInv.setStack(41, cmd(Items.NETHERITE_INGOT, "/caliphat set <joueur>", "Nommer un calife"));
        panelInv.setStack(42, cmd(Items.NETHERITE_INGOT, "/caliphat remove", "Retirer le calife"));
        panelInv.setStack(43, cmd(Items.NETHERITE_INGOT, "/caliphat loi proposer", "Proposer une loi"));
        panelInv.setStack(44, cmd(Items.NETHERITE_INGOT, "/caliphat loi promulguer", "Promulguer une loi"));
        panelInv.setStack(45, cmd(Items.NETHERITE_INGOT, "/caliphat loi voter <id>", "Ouvrir un vote"));
        panelInv.setStack(46, cmd(Items.NETHERITE_INGOT, "/caliphat decret <texte>", "Publier un décret"));
        panelInv.setStack(47, cmd(Items.NETHERITE_INGOT, "/caliphat config titre", "Titres à l'écran"));
        panelInv.setStack(48, cmd(Items.NETHERITE_INGOT, "/eco panel", "Panel admin économie"));
        panelInv.setStack(49, cmd(Items.NETHERITE_INGOT, "/salary set <joueur> <montant>", "Définir un salaire"));
        panelInv.setStack(50, cmd(Items.NETHERITE_INGOT, "/tax set <joueur> <%>", "Taxe personnelle"));
        panelInv.setStack(51, cmd(Items.NETHERITE_INGOT, "/bank deposit|withdraw", "Banque"));
        panelInv.setStack(52, cmd(Items.NETHERITE_INGOT, "/loan take|repay", "Système de prêts"));
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
