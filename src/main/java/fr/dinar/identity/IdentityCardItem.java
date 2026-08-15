package fr.dinar.identity;

import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.UUID;

public class IdentityCardItem extends Item {

    public IdentityCardItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            UUID owner = readOwner(stack);
            if (owner == null) {
                user.sendMessage(DinarLang.text("§cCette carte d'identité est illisible."), false);
            } else {
                String info = DinarMod.identity.describeCard(owner);
                if (info == null) {
                    user.sendMessage(DinarLang.text("§cCette carte n'est plus valide."), false);
                } else {
                    for (String line : info.split("\n")) {
                        user.sendMessage(Text.literal(line), false);
                    }
                }
            }
        }
        return TypedActionResult.success(stack);
    }

    public static UUID readOwner(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        NbtCompound nbt = data.copyNbt();
        if (nbt == null || !nbt.contains("owner")) return null;
        try {
            return UUID.fromString(nbt.getString("owner"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
