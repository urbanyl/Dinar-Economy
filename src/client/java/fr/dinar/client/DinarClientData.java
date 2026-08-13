package fr.dinar.client;

public final class DinarClientData {
    private static double walletBalance = 0;
    private static double bankBalance = 0;
    private static String currencySymbol = "D";

    private DinarClientData() {}

    public static double getWalletBalance() { return walletBalance; }
    public static double getBankBalance() { return bankBalance; }
    public static String getCurrencySymbol() { return currencySymbol; }

    public static void update(double wallet, double bank, String symbol) {
        walletBalance = wallet;
        bankBalance = bank;
        if (symbol != null) currencySymbol = symbol;
    }
}
