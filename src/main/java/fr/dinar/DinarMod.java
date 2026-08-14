package fr.dinar;

import fr.dinar.account.AccountManager;
import fr.dinar.command.ModCommands;
import fr.dinar.command.PlayerArgumentType;
import fr.dinar.config.DinarConfig;
import fr.dinar.economy.AuctionManager;
import fr.dinar.economy.CompanyManager;
import fr.dinar.economy.ContractManager;
import fr.dinar.economy.EconomyManager;
import fr.dinar.economy.ShopManager;
import fr.dinar.government.GovernmentManager;
import fr.dinar.identity.IdentityManager;
import fr.dinar.justice.JusticeManager;
import fr.dinar.justice.PoliceManager;
import fr.dinar.justice.PrisonManager;
import fr.dinar.logs.RpLogManager;
import fr.dinar.mail.MailManager;
import fr.dinar.placeholder.DinarPlaceholders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
    public static MailManager mail;
    public static PoliceManager police;
    public static JusticeManager justice;
    public static PrisonManager prison;
    public static RpLogManager rpLog;
    public static AccountManager accounts;
    public static IdentityManager identity;
    public static Item IDENTITY_CARD;

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
        IDENTITY_CARD = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "identity_card"),
                new Item(new Item.Settings()));
        config = DinarConfig.load();
        economy = new EconomyManager();
        government = new GovernmentManager();
        shops = new ShopManager();
        companies = new CompanyManager();
        auctions = new AuctionManager();
        contracts = new ContractManager();
        mail = new MailManager();
        police = new PoliceManager();
        justice = new JusticeManager();
        prison = new PrisonManager();
        rpLog = new RpLogManager();
        accounts = new AccountManager();
        identity = new IdentityManager();

        PayloadTypeRegistry.playS2C().register(BalanceSyncPayload.ID, BalanceSyncPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            economy.onServerStart(server);
            government.onServerStart(server);
            var dataDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("dinar");
            shops.load(dataDir);
            companies.load(dataDir);
            auctions.load(dataDir);
            contracts.load(dataDir);
            mail.onServerStart(server);
            police.onServerStart(server);
            justice.onServerStart(server);
            prison.onServerStart(server);
            rpLog.onServerStart(server);
            accounts.onServerStart(server);
            identity.onServerStart(server);
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
            mail.onServerStop();
            police.onServerStop();
            justice.onServerStop();
            prison.onServerStop();
            rpLog.onServerStop();
            accounts.onServerStop();
            identity.onServerStop();
            INSTANCE.lastSyncTick.clear();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            economy.onPlayerJoin(player);
            syncBalance(player);
            UUID uuid = player.getUuid();
            if (!DinarMod.accounts.hasAccount(uuid)) {
                player.sendMessage(Text.literal("§6§lBienvenue §r§7sur le serveur RP !"), false);
                player.sendMessage(Text.literal("§7Créez votre compte : §a/register <mot de passe> §7— "
                        + "vous serez ensuite invité à définir votre identité RP."), false);
            } else if (!DinarMod.accounts.isLoggedIn(uuid)) {
                player.sendMessage(Text.literal("§6§lConnexion §r§7» §fIdentifiez-vous : §a/login <mot de passe>"), false);
            } else if (DinarMod.identity.isComplete(uuid)) {
                DinarMod.identity.giveCard(player);
            }
            int unread = DinarMod.mail.unreadCount(uuid);
            if (unread > 0) {
                player.sendMessage(Text.literal("§d✉ §fVous avez §e" + unread
                        + " §flettre(s) non lue(s) §7(§f/courrier liste§7)"), false);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            economy.getScoreboard().removePlayer(handler.getPlayer());
            INSTANCE.lastSyncTick.remove(handler.getPlayer().getUuid());
            INSTANCE.dirtyPlayers.remove(handler.getPlayer().getUuid());
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (DinarMod.accounts.isLoggedIn(newPlayer.getUuid())
                    && DinarMod.identity.isComplete(newPlayer.getUuid())) {
                DinarMod.identity.giveCard(newPlayer);
            }
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayerEntity player = sender;
            if (!DinarMod.accounts.isLoggedIn(player.getUuid())) {
                player.sendMessage(Text.literal("§c🔒 Vous devez être connecté pour parler : §a/login <mot de passe>"), false);
                return false;
            }
            String prefix = DinarMod.identity.formatName(player.getUuid());
            if (prefix == null) {
                player.sendMessage(Text.literal("§6Complétez votre identité pour parler : §f/identite prenom <prénom> "
                        + "§7puis §f/identite metier <métier>"), false);
                return false;
            }
            String formatted = prefix + " §8» §7" + message.getSignedContent();
            MinecraftServer srv = player.getServer();
            if (srv == null) return false;
            for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) {
                p.sendMessage(Text.literal(formatted), false);
            }
            return false;
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
        prison.tick(server);
        accounts.tick(server);

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
