package fr.dinar.identity;

public final class RpProfile {
    public String job = "";
    public String rpName = "";

    public boolean isComplete() {
        return !job.isBlank() && !rpName.isBlank();
    }
}
