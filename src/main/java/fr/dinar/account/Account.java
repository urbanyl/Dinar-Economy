package fr.dinar.account;

import java.util.UUID;

public final class Account {
    public final UUID uuid;
    public String name;
    public String passwordHash;

    public Account(UUID uuid, String name, String passwordHash) {
        this.uuid = uuid;
        this.name = name;
        this.passwordHash = passwordHash;
    }
}
