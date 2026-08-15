package fr.dinar.gui;

import fr.dinar.DinarMod;
import fr.dinar.government.Law;
import fr.dinar.lang.DinarLang;
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

import java.util.ArrayList;
import java.util.List;

public class LawBookScreenHandler extends GenericContainerScreenHandler {
    private static final int LAWS_START = 9;
    private static final int LAWS_PER_PAGE = 45;

    private final SimpleInventory panelInv;
    private final ServerPlayerEntity viewer;
    private int page = 0;

    public LawBookScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity viewer) {
        this(syncId, playerInventory, viewer, new SimpleInventory(54));
    }

    private LawBookScreenHandler(int syncId, PlayerInventory pi, ServerPlayerEntity viewer, SimpleInventory inv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, pi, inv, 6);
        this.panelInv = inv;
        this.viewer = viewer;
        build();
    }

    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new LawBookScreenHandler(syncId, inv, player),
                Text.literal(DinarLang.t("§6Livre des Lois")).setStyle(Style.EMPTY.withColor(Formatting.GOLD).withItalic(false))));
    }

    private void build() {
        for (int i = 0; i < 54; i++) panelInv.setStack(i, ItemStack.EMPTY);

        List<Law> adopted = DinarMod.government.getAdoptedLaws();
        int totalPages = Math.max(1, (int) Math.ceil(adopted.size() / (double) LAWS_PER_PAGE));

        panelInv.setStack(0, named(Items.BARRIER, DinarLang.t("§cFermer")));

        ItemStack header = named(Items.ENCHANTED_BOOK, DinarLang.t("§6§lLivre des Lois"));
        header.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal(DinarLang.t("§7Lois adoptées : §e%s", adopted.size())),
                Text.literal(DinarLang.t("§7Page §e%s/%s", page + 1, totalPages)))));
        panelInv.setStack(4, header);

        panelInv.setStack(36, named(Items.ARROW, DinarLang.t("§ePage précédente")));
        panelInv.setStack(44, named(Items.ARROW, DinarLang.t("§ePage suivante")));

        int start = page * LAWS_PER_PAGE;
        int end = Math.min(start + LAWS_PER_PAGE, adopted.size());
        for (int i = start; i < end; i++) {
            Law law = adopted.get(i);
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            book.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a#" + law.id + " §f" + law.title)
                    .setStyle(Style.EMPTY.withItalic(false)));
            book.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("§7" + law.content),
                    Text.literal(DinarLang.t("§7Auteur : §e%s", law.authorName)),
                    Text.literal(DinarLang.t("§aVotes : §e%s OUI §7/ §c%s NON", law.yesVotes, law.noVotes)))));
            panelInv.setStack(LAWS_START + (i - start), book);
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp && sp.getUuid().equals(viewer.getUuid())) {
            if (slotIndex == 0) {
                viewer.closeHandledScreen();
            } else if (slotIndex == 36) {
                page = Math.max(0, page - 1);
                build();
                syncState();
            } else if (slotIndex == 44) {
                int totalPages = Math.max(1, (int) Math.ceil(DinarMod.government.getAdoptedLaws().size() / (double) LAWS_PER_PAGE));
                page = Math.min(totalPages - 1, page + 1);
                build();
                syncState();
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private static ItemStack named(net.minecraft.item.Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return s;
    }
}
