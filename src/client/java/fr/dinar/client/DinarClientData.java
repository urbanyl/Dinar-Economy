package fr.dinar.client;

import java.util.ArrayList;
import java.util.List;

public final class DinarClientData {
    private static double walletBalance = 0;
    private static double bankBalance = 0;
    private static double companyBalance = 0;
    private static boolean hasCompany = false;
    private static String currencySymbol = "D";
    private static boolean initialized = false;

    private static final List<MoneyGainEvent> GAINS = new ArrayList<>();
    private static final int MAX_GAINS = 4;

    private DinarClientData() {}

    public static double getWalletBalance() { return walletBalance; }
    public static double getBankBalance() { return bankBalance; }
    public static double getCompanyBalance() { return companyBalance; }
    public static boolean getHasCompany() { return hasCompany; }
    public static String getCurrencySymbol() { return currencySymbol; }
    public static List<MoneyGainEvent> getMoneyGains() { return GAINS; }

    public static void update(double wallet, double bank, double company, boolean hasCompanyFlag, String symbol) {
        double oldWallet = walletBalance;
        double oldBank = bankBalance;
        walletBalance = wallet;
        bankBalance = bank;
        companyBalance = company;
        hasCompany = hasCompanyFlag;
        if (symbol != null) currencySymbol = symbol;
        if (initialized) {
            if (wallet > oldWallet) addGain(wallet - oldWallet, "Argent");
            if (bank > oldBank) addGain(bank - oldBank, "Banque");
        } else {
            initialized = true;
        }
    }

    public static void addGain(double amount, String label) {
        if (amount <= 0) return;
        GAINS.add(new MoneyGainEvent(amount, label));
        while (GAINS.size() > MAX_GAINS) GAINS.remove(0);
    }

    public static void cleanupGains() {
        GAINS.removeIf(MoneyGainEvent::finished);
    }
}
