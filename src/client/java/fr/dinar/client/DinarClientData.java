package fr.dinar.client;

public final class DinarClientData {
    private static double walletBalance = 0;
    private static double bankBalance = 0;
    private static double companyBalance = 0;
    private static boolean hasCompany = false;
    private static String currencySymbol = "D";

    private DinarClientData() {}

    public static double getWalletBalance() { return walletBalance; }
    public static double getBankBalance() { return bankBalance; }
    public static double getCompanyBalance() { return companyBalance; }
    public static boolean getHasCompany() { return hasCompany; }
    public static String getCurrencySymbol() { return currencySymbol; }

    public static void update(double wallet, double bank, double company, boolean hasCompanyFlag, String symbol) {
        walletBalance = wallet;
        bankBalance = bank;
        companyBalance = company;
        hasCompany = hasCompanyFlag;
        if (symbol != null) currencySymbol = symbol;
    }
}
