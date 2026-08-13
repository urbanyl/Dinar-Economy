package fr.dinar.client;

import fr.dinar.BalanceSyncPayload;
import fr.dinar.DinarMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
                context.client().execute(() -> DinarClientData.update(wallet, bank, company, hasCompany, symbol));
            } catch (Exception e) {
                DinarMod.LOGGER.warn("[Dinar] Erreur réception packet balance.", e);
            }
        });
    }
}
