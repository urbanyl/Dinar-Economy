package fr.dinar.government;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VoteSession {
    public final int lawId;
    public final long startedAt;
    public long expiresAt;
    public final int requiredVotes;

    private final Set<UUID> yesVoters = new HashSet<>();
    private final Set<UUID> noVoters = new HashSet<>();

    public VoteSession(int lawId, long startedAt, long durationSeconds, int requiredVotes) {
        this.lawId = lawId;
        this.startedAt = startedAt;
        this.expiresAt = startedAt + durationSeconds * 1000;
        this.requiredVotes = requiredVotes;
    }

    public boolean vote(UUID uuid, boolean yes) {
        if (yesVoters.contains(uuid) || noVoters.contains(uuid)) return false;
        if (yes) yesVoters.add(uuid); else noVoters.add(uuid);
        return true;
    }

    public boolean hasVoted(UUID uuid) {
        return yesVoters.contains(uuid) || noVoters.contains(uuid);
    }

    public int getYes() { return yesVoters.size(); }
    public int getNo() { return noVoters.size(); }
    public int getTotal() { return yesVoters.size() + noVoters.size(); }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public boolean isPassed() {
        return getTotal() >= requiredVotes && getYes() > getNo();
    }

    public boolean isRejected() {
        return isExpired() && getYes() <= getNo();
    }

    public Set<UUID> getYesVoters() { return yesVoters; }
    public Set<UUID> getNoVoters() { return noVoters; }
    public int getRequiredVotes() { return requiredVotes; }
}
