package fr.dinar.mixin;

import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(CommandManager.class)
public abstract class MixinCommandManager {

    @Inject(method = "executeWithPrefix(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private void dinar_gateCommands(ServerCommandSource source, String command, CallbackInfo ci) {
        if (source == null || command == null || DinarMod.accounts == null) return;
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return;
        if (DinarMod.accounts.isLoggedIn(player.getUuid())) return;

        String trimmed = command.stripLeading();
        String name = trimmed.split("[ \t]", 2)[0].toLowerCase(Locale.ROOT);
        if (name.equals("register") || name.equals("login")) return;

        player.sendMessage(DinarLang.text("§c🔒 Connectez-vous d'abord : §a/register <mot de passe> §7ou §a/login <mot de passe>"), false);
        ci.cancel();
    }
}
