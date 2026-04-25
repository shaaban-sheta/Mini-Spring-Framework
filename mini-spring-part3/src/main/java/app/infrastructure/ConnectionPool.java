package app.infrastructure;

import minispring.annotation.Component;
import minispring.annotation.PostConstruct;
import minispring.annotation.PreDestroy;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simulated database connection pool.
 *
 * Demonstrates the full lifecycle:
 *   - @PostConstruct: opens connections after the bean is constructed
 *   - @PreDestroy: closes all connections on container shutdown
 */
@Component
public class ConnectionPool {

    private final Deque<String> pool = new ArrayDeque<>();
    private static final int    POOL_SIZE = 5;

    @PostConstruct
    public void init() {
        System.out.println("[Pool] Initializing connection pool...");
        for (int i = 1; i <= POOL_SIZE; i++) {
            pool.push("jdbc:h2:mem:db?conn=" + i);
        }
        System.out.println("[Pool] Ready with " + POOL_SIZE + " connections");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("[Pool] Closing " + pool.size() + " connections...");
        pool.clear();
        System.out.println("[Pool] All connections closed");
    }

    public String acquire() {
        if (pool.isEmpty()) throw new RuntimeException("Connection pool exhausted");
        String conn = pool.pop();
        System.out.println("[Pool] Acquired: " + conn);
        return conn;
    }

    public void release(String conn) {
        pool.push(conn);
        System.out.println("[Pool] Released: " + conn);
    }

    public int available() { return pool.size(); }
}
