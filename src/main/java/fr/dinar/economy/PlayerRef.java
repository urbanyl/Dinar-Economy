package fr.dinar.economy;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public record PlayerRef(UUID uuid, String name, ServerPlayerEntity online) {
    public static PlayerRef ofOnline(ServerPlayerEntity player) {
        return new PlayerRef(player.getUuid(), player.getGameProfile().getName(), player);
    }

    public boolean isOnline() {
        return online != null;
    }

    public String displayName() {
        return name == null ? "Inconnu" : name;
    }
}
