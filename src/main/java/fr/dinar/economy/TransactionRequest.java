package fr.dinar.economy;

import java.util.UUID;

public class TransactionRequest {
    public int id;
    public UUID sender;
    public UUID target;
    public double amount;
    public String message;
    public long expiresAt;
}
