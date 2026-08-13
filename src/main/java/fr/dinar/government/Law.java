package fr.dinar.government;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Law {
    public final int id;
    public String title;
    public String content;
    public UUID authorUuid;
    public String authorName;
    public String status = "PENDING";
    public long createdAt;
    public long decidedAt;
    public int yesVotes = 0;
    public int noVotes = 0;
    public List<String> voters = new ArrayList<>();

    public Law(int id, String title, String content, UUID authorUuid, String authorName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isAdopted() {
        return "ADOPTED".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean hasVoted(UUID uuid) {
        return voters.contains(uuid.toString());
    }

    public int totalVotes() {
        return yesVotes + noVotes;
    }
}
