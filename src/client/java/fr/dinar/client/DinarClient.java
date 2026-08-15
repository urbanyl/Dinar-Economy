package fr.dinar.client;

import fr.dinar.BalanceSyncPayload;
import fr.dinar.DinarMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;

public class DinarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new DinarHudRenderer());

        ClientPlayNetworking.registerGlobalReceiver(BalanceSyncPayload.ID, (payload, context) -> {
            try {
                if (payload == null || payload.data() == null) return;
                double wallet = payload.data().getDouble("wallet");
                double bank = payload.data().getDouble("bank");
                double company = payload.data().getDouble("company");
                boolean hasCompany = payload.data().getBoolean("hasCompany");
                String symbol = payload.data().contains("symbol") ? payload.data().getString("symbol") : "D";
                String lang = payload.data().contains("lang") ? payload.data().getString("lang") : "fr";
                context.client().execute(() -> {
                    boolean wasInitialized = DinarClientData.isInitialized();
                    double oldWallet = DinarClientData.getWalletBalance();
                    double oldBank = DinarClientData.getBankBalance();
                    DinarClientData.update(wallet, bank, company, hasCompany, symbol, lang);
                    if (wasInitialized) {
                        ClientPlayerEntity player = context.client().player;
                        if (player != null) {
                            if (wallet > oldWallet) {
                                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.1f);
                            } else if (wallet < oldWallet) {
                                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6f, 0.7f);
                            }
                            if (bank > oldBank) {
                                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.3f);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                DinarMod.LOGGER.warn("[Dinar] Erreur réception packet balance.", e);
            }
        });
    }
}
