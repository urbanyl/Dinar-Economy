package fr.dinar;

import fr.dinar.command.ModCommands;
import fr.dinar.command.PlayerArgumentType;
import fr.dinar.config.DinarConfig;
import fr.dinar.economy.AuctionManager;
import fr.dinar.economy.CompanyManager;
import fr.dinar.economy.ContractManager;
import fr.dinar.economy.EconomyManager;
import fr.dinar.economy.ShopManager;
import fr.dinar.government.GovernmentManager;
import fr.dinar.placeholder.DinarPlaceholders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DinarMod implements ModInitializer {
    public static final String MOD_ID = "dinar";
    public static final Logger LOGGER = LoggerFactory.getLogger("Dinar");

    public static DinarConfig config;
    public static EconomyManager economy;
    public static GovernmentManager government;
    public static ShopManager shops;
    public static CompanyManager companies;
    public static AuctionManager auctions;
    public static ContractManager contracts;

    private final Map<UUID, Integer> lastSyncTick = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> dirtyPlayers = new ConcurrentHashMap<>();
    private static final int SYNC_INTERVAL = 40;
    private static DinarMod INSTANCE;

    public static void markDirty(UUID uuid) {
        if (uuid == null || INSTANCE == null) return;
        INSTANCE.dirtyPlayers.put(uuid, Boolean.TRUE);
    }

    @Override
    public void onInitialize() {
        INSTANCE = this;
        ArgumentTypeRegistry.registerArgumentType(Identifier.of(MOD_ID, "player"),
                PlayerArgumentType.class, ConstantArgumentSerializer.of(PlayerArgumentType::player));
        config = DinarConfig.load();
        economy = new EconomyManager();
        government = new GovernmentManager();
        shops = new ShopManager();
        companies = new CompanyManager();
        auctions = new AuctionManager();
        contracts = new ContractManager();

        PayloadTypeRegistry.playS2C().register(BalanceSyncPayload.ID, BalanceSyncPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            economy.onServerStart(server);
            government.onServerStart(server);
            var dataDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("dinar");
            shops.load(dataDir);
            companies.load(dataDir);
            auctions.load(dataDir);
            contracts.load(dataDir);
            LOGGER.info("[Dinar] Shops: {}, Entreprises: {}, Ventes: {}, Contrats: {}",
                    shops.getAll().size(), companies.getAll().size(),
                    auctions.getAll().size(), contracts.getAll().size());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            economy.onServerStop(server);
            government.onServerStop();
            var dataDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("dinar");
            shops.save(dataDir);
            companies.save(dataDir);
            auctions.save(dataDir);
            contracts.save(dataDir);
            INSTANCE.lastSyncTick.clear();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            economy.onPlayerJoin(handler.getPlayer());
            syncBalance(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            economy.getScoreboard().removePlayer(handler.getPlayer());
            INSTANCE.lastSyncTick.remove(handler.getPlayer().getUuid());
            INSTANCE.dirtyPlayers.remove(handler.getPlayer().getUuid());
        });

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

        if (!dirtyPlayers.isEmpty()) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (dirtyPlayers.remove(player.getUuid()) != null) {
                    syncBalance(player);
                }
            }
        }

        int currentTick = server.getTicks();
        if (currentTick % SYNC_INTERVAL == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                int last = lastSyncTick.getOrDefault(player.getUuid(), 0);
                if (currentTick - last >= SYNC_INTERVAL) {
                    syncBalance(player);
                }
            }
        }
    }

    public static void syncBalance(ServerPlayerEntity player) {
        if (player == null || economy == null) return;
        try {
            UUID uuid = player.getUuid();
            double wallet = economy.balance(uuid);
            double bank = economy.bankBalance(uuid);
            var ownedCompany = companies.getByOwner(uuid).stream().findFirst();
            double company = ownedCompany.isPresent() ? ownedCompany.get().balance : 0;
            boolean hasCompany = ownedCompany.isPresent();
            String symbol = config != null ? config.currencySymbol : "D";

            NbtCompound nbt = new NbtCompound();
            nbt.putDouble("wallet", wallet);
            nbt.putDouble("bank", bank);
            nbt.putDouble("company", company);
            nbt.putBoolean("hasCompany", hasCompany);
            nbt.putString("symbol", symbol);

            ServerPlayNetworking.send(player, new BalanceSyncPayload(nbt));

            if (INSTANCE != null && player.getServer() != null) {
                INSTANCE.lastSyncTick.put(uuid, player.getServer().getTicks());
            }
        } catch (Exception e) {
            LOGGER.warn("[Dinar] Erreur envoi packet balance à {}.", player.getName().getString(), e);
        }
    }
}
