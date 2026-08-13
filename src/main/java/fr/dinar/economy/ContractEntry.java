package fr.dinar.economy;

import java.util.UUID;

public class ContractEntry {
    public int id;
    public UUID creatorUuid;
    public String creatorName;
    public UUID targetUuid;
    public String targetName;
    public String type;
    public String details;
    public double amount;
    public String status;
    public long createdAt;
    public long signedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SIGNED = "SIGNED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public ContractEntry() {}

    public ContractEntry(int id, UUID creatorUuid, String creatorName, UUID targetUuid, String targetName,
                         String type, String details, double amount) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
        this.details = details;
        this.amount = amount;
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean isSigned() { return STATUS_SIGNED.equals(status); }
    public boolean isCancelled() { return STATUS_CANCELLED.equals(status); }

    public boolean involves(UUID uuid) {
        return (creatorUuid != null && creatorUuid.equals(uuid)) || (targetUuid != null && targetUuid.equals(uuid));
    }
}
