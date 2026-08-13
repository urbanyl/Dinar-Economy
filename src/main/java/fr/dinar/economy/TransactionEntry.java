package fr.dinar.economy;

public record TransactionEntry(long time, String type, double amount, String otherName, String reason) {
}
