package fr.dinar.economy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class Account {
    public UUID uuid;
    public String name;
    public double balance;
    public Deque<TransactionEntry> history = new ArrayDeque<>();

    public Account() {}

    public Account(UUID uuid, String name, double balance) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
    }
}
