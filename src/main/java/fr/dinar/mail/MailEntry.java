package fr.dinar.mail;

public final class MailEntry {
    public int id;
    public String senderUuid;
    public String senderName;
    public String receiverName;
    public String message;
    public double attachedMoney;
    public boolean moneyClaimed;
    public boolean read;
    public long sentAt;
}
