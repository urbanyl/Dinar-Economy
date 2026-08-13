package fr.dinar.economy;

import java.util.UUID;

public class LoanEntry {
    public UUID uuid;
    public double amount;
    public double interestRate;
    public double totalOwed;
    public double amountRepaid;
    public long createdAt;
    public long expiresAt;
    public boolean repaid;

    public LoanEntry() {}

    public LoanEntry(UUID uuid, double amount, double interestRate, double totalOwed, long durationSeconds) {
        this.uuid = uuid;
        this.amount = amount;
        this.interestRate = interestRate;
        this.totalOwed = totalOwed;
        this.amountRepaid = 0;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + durationSeconds * 1000;
        this.repaid = false;
    }

    public boolean isRepaid() {
        return repaid || amountRepaid >= totalOwed;
    }

    public boolean isExpired() {
        return !isRepaid() && System.currentTimeMillis() > expiresAt;
    }

    public double remaining() {
        return Math.max(0, totalOwed - amountRepaid);
    }

    public long timeRemainingSeconds() {
        return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000);
    }
}
