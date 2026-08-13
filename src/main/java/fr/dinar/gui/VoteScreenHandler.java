package fr.dinar.gui;

import fr.dinar.DinarMod;
import fr.dinar.government.Law;
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

public class VoteScreenHandler extends GenericContainerScreenHandler {
    private final SimpleInventory panelInv;
    private final ServerPlayerEntity voter;
    private final Law law;

    public VoteScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity voter, Law law) {
        this(syncId, playerInventory, voter, law, new SimpleInventory(27));
    }

    private VoteScreenHandler(int syncId, PlayerInventory pi, ServerPlayerEntity voter, Law law, SimpleInventory inv) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, pi, inv, 3);
        this.panelInv = inv;
        this.voter = voter;
        this.law = law;
        build();
    }

    public static void open(ServerPlayerEntity player, Law law) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new VoteScreenHandler(syncId, inv, player, law),
                Text.literal("Vote — Loi #" + law.id).setStyle(Style.EMPTY.withColor(Formatting.GOLD).withItalic(false))));
    }

    private void build() {
        for (int i = 0; i < 27; i++) panelInv.setStack(i, ItemStack.EMPTY);

        ItemStack title = new ItemStack(Items.WRITTEN_BOOK);
        title.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6§lLoi #" + law.id + " §e» " + law.title)
                .setStyle(Style.EMPTY.withItalic(false)));
        title.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7" + law.content),
                Text.literal("§7Auteur : §e" + law.authorName),
                Text.literal("§7Votes : §a" + law.yesVotes + " OUI §7/ §c" + law.noVotes + " NON"))));
        panelInv.setStack(4, title);

        ItemStack yes = new ItemStack(Items.LIME_DYE);
        yes.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a§lOUI").setStyle(Style.EMPTY.withItalic(false)));
        yes.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Voter en faveur de cette loi"))));
        panelInv.setStack(11, yes);

        ItemStack no = new ItemStack(Items.RED_DYE);
        no.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c§lNON").setStyle(Style.EMPTY.withItalic(false)));
        no.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Voter contre cette loi"))));
        panelInv.setStack(15, no);

        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§cFermer").setStyle(Style.EMPTY.withItalic(false)));
        panelInv.setStack(22, close);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp && sp.getUuid().equals(voter.getUuid())) {
            if (slotIndex == 11) {
                if (DinarMod.government.vote(voter.getUuid(), law.id, true)) {
                    voter.sendMessage(Text.literal("§aVote §lOUI §renregistré pour §f" + law.title), false);
                } else {
                    voter.sendMessage(Text.literal("§cVote impossible."), false);
                }
                voter.closeHandledScreen();
            } else if (slotIndex == 15) {
                if (DinarMod.government.vote(voter.getUuid(), law.id, false)) {
                    voter.sendMessage(Text.literal("§cVote §lNON §renregistré pour §f" + law.title), false);
                } else {
                    voter.sendMessage(Text.literal("§cVote impossible."), false);
                }
                voter.closeHandledScreen();
            } else if (slotIndex == 22) {
                voter.closeHandledScreen();
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
