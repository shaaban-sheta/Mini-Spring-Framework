package app.service;

import app.dto.User;
import minispring.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UserService {

    private final Map<Integer, User> store = new ConcurrentHashMap<>();
    private final AtomicInteger      seq   = new AtomicInteger(1);

    public UserService() {
        // Seed some data
        store.put(1, new User(1, "Alice",   "alice@example.com"));
        store.put(2, new User(2, "Bob",     "bob@example.com"));
        store.put(3, new User(3, "Charlie", "charlie@example.com"));
        seq.set(4);
    }

    public User findById(int id) {
        return store.get(id);
    }

    public List<User> findAll(int limit) {
        return store.values().stream().limit(limit).toList();
    }

    /**
     * Minimal JSON parsing: expects {"name":"Alice","email":"alice@example.com"}
     */
    public User createFromJson(String json) {
        String name  = extractField(json, "name");
        String email = extractField(json, "email");
        int id = seq.getAndIncrement();
        User user = new User(id, name, email);
        store.put(id, user);
        System.out.println("[UserService] Created: " + user);
        return user;
    }

    private String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "unknown";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? "unknown" : json.substring(start, end);
    }
}
