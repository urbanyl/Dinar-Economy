package fr.dinar.economy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestManager {
    private final Map<Integer, TransactionRequest> requests = new ConcurrentHashMap<>();
    private int nextId = 1;

    public synchronized TransactionRequest create(UUID sender, UUID target, double amount, String message, int expirySeconds) {
        TransactionRequest r = new TransactionRequest();
        r.id = nextId++;
        r.sender = sender;
        r.target = target;
        r.amount = amount;
        r.message = message;
        r.expiresAt = System.currentTimeMillis() + expirySeconds * 1000L;
        requests.put(r.id, r);
        return r;
    }

    public TransactionRequest get(int id) {
        purge();
        return requests.get(id);
    }

    public void remove(int id) {
        requests.remove(id);
    }

    public void purge() {
        long now = System.currentTimeMillis();
        requests.values().removeIf(r -> r.expiresAt < now);
    }

    public List<TransactionRequest> forPlayer(UUID uuid) {
        purge();
        return requests.values().stream()
                .filter(r -> r.sender.equals(uuid) || r.target.equals(uuid))
                .sorted(Comparator.comparingInt((TransactionRequest r) -> -r.id))
                .toList();
    }
}
