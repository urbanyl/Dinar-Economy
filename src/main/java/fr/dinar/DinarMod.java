package fr.dinar;

import fr.dinar.command.ModCommands;
import fr.dinar.config.DinarConfig;
import fr.dinar.economy.EconomyManager;
import fr.dinar.government.GovernmentManager;
import fr.dinar.placeholder.DinarPlaceholders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DinarMod implements ModInitializer {
    public static final String MOD_ID = "dinar";
    public static final Logger LOGGER = LoggerFactory.getLogger("Dinar");

    public static DinarConfig config;
    public static EconomyManager economy;
    public static GovernmentManager government;

    @Override
    public void onInitialize() {
        config = DinarConfig.load();
        economy = new EconomyManager();
        government = new GovernmentManager();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            economy.onServerStart(server);
            government.onServerStart(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            economy.onServerStop(server);
            government.onServerStop();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> economy.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> economy.getScoreboard().removePlayer(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register(ModCommands::register);

        ServerTickEvents.END_SERVER_TICK.register(this::onTick);

        if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
            try {
                DinarPlaceholders.register();
                LOGGER.info("[Dinar] Placeholder API détecté, placeholders enregistrés.");
            } catch (Throwable t) {
                LOGGER.warn("[Dinar] Échec de l'enregistrement des placeholders.", t);
            }
        }
    }

    private void onTick(MinecraftServer server) {
        if (economy.getServer() == null) return;
        economy.tick(server);
        government.tick();
    }
}
