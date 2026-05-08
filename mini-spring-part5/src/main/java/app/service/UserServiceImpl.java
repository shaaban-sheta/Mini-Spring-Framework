package app.service;

import app.dto.User;
import minispring.annotation.Component;
import minispring.annotation.PostConstruct;
import minispring.aop.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UserServiceImpl — demonstrates all four AOP annotations working together.
 *
 * Every annotated method is transparently intercepted by the proxy:
 *   @Timed       → prints execution time to console
 *   @Secured     → checks ThreadLocal security context before proceeding
 *   @Transactional → wraps the call in a simulated transaction
 *   @Cacheable   → caches the result; returns cached value on subsequent calls
 *
 * The class implements UserService so that the JDK Proxy can wrap it.
 */
@Component
public class UserServiceImpl implements UserService {

    private final Map<Integer, User> store = new ConcurrentHashMap<>();
    private final AtomicInteger      seq   = new AtomicInteger(1);

    @PostConstruct
    private void init() {
        store.put(1, new User(1, "Shaaban",   "shaaban@example.com"));
        store.put(2, new User(2, "Anas",     "anas@example.com"));
        store.put(3, new User(3, "Sheta", "sheta@example.com"));
        seq.set(4);
        System.out.println("[UserServiceImpl] Initialized with " + store.size() + " users");
    }

    @Override
    @Timed("user.findById")
    @Secured({"USER", "ADMIN"})
    @Transactional
    @Cacheable(key = "user")
    public User findById(int id) {
        System.out.println("[UserServiceImpl] DB query for id=" + id);
        return store.get(id);
    }

    @Override
    @Timed("user.create")
    @Secured("ADMIN")
    @Transactional
    public User create(String name, String email) {
        int id = seq.getAndIncrement();
        User user = new User(id, name, email);
        store.put(id, user);
        System.out.println("[UserServiceImpl] Created: " + user);
        return user;
    }

    @Override
    @Timed("user.delete")
    @Secured("ADMIN")
    @Transactional
    public void deleteUser(int id) {
        User removed = store.remove(id);
        if (removed == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        System.out.println("[UserServiceImpl] Deleted: " + removed);
    }
}
