package fr.dinar.economy;

import java.util.UUID;

public class SalaryEntry {
    public UUID uuid;
    public double amount;
    public long intervalSeconds;
    public long lastPaid;

    public SalaryEntry() {}

    public SalaryEntry(UUID uuid, double amount, long intervalSeconds) {
        this.uuid = uuid;
        this.amount = amount;
        this.intervalSeconds = intervalSeconds;
        this.lastPaid = System.currentTimeMillis();
    }
}
