package fr.dinar.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompanyEntry {
    public int id;
    public String name;
    public UUID ownerUuid;
    public String ownerName;
    public List<String> members = new ArrayList<>();
    public double balance;
    public long createdAt;

    public CompanyEntry() {}

    public CompanyEntry(int id, String name, UUID ownerUuid, String ownerName) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.balance = 0;
        this.createdAt = System.currentTimeMillis();
        this.members.add(ownerUuid.toString());
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid.toString());
    }

    public void addMember(UUID uuid) {
        if (!isMember(uuid)) {
            members.add(uuid.toString());
        }
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid.toString());
    }
}
