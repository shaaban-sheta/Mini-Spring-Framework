package app.repository;

import app.infrastructure.ConnectionPool;
import minispring.annotation.Autowired;
import minispring.annotation.Component;
import minispring.annotation.PostConstruct;

/**
 * User repository — uses @PostConstruct to validate the pool at startup.
 *
 * Demonstrates:
 *   - @Autowired constructor injection
 *   - @PostConstruct running AFTER injection (pool is non-null here)
 *   - Dependency lifetime: UserRepository is destroyed before ConnectionPool
 *     because UserRepository was created after it (reverse-order destruction)
 */
@Component
public class UserRepository {

    private final ConnectionPool pool;

    @Autowired
    public UserRepository(ConnectionPool pool) {
        this.pool = pool;
        System.out.println("[UserRepository] Constructor called (pool injected)");
    }

    @PostConstruct
    private void validate() {
        // Acquire and immediately release — proves the pool is operational at startup
        String conn = pool.acquire();
        pool.release(conn);
        System.out.println("[UserRepository] Pool connection validated ✓");
    }

    public String findById(int id) {
        String conn = pool.acquire();
        try {
            return "User #" + id + " (via " + conn + ")";
        } finally {
            pool.release(conn);
        }
    }

    public void save(String name) {
        String conn = pool.acquire();
        try {
            System.out.println("[UserRepository] Saved user: " + name + " (via " + conn + ")");
        } finally {
            pool.release(conn);
        }
    }
}
