package fr.dinar.economy;

public record TransferResult(boolean success, double tax, double received, String error) {
    public static TransferResult ok(double tax, double received) {
        return new TransferResult(true, tax, received, null);
    }

    public static TransferResult fail(String error) {
        return new TransferResult(false, 0, 0, error);
    }
}
