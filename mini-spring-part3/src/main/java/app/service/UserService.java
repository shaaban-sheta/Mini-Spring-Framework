package app.service;

import app.repository.UserRepository;
import minispring.annotation.Autowired;
import minispring.annotation.Component;
import minispring.annotation.PostConstruct;
import minispring.annotation.PreDestroy;

/**
 * UserService with full lifecycle callbacks.
 *
 * Demonstrates:
 *   - @PostConstruct running after constructor + field injection
 *   - @PreDestroy called before ConnectionPool is shut down
 *     (because UserService was created after ConnectionPool)
 */
@Component
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("[UserService] Constructor called");
    }

    @PostConstruct
    private void init() {
        System.out.println("[UserService] @PostConstruct — warming up service cache...");
        // Simulate pre-loading frequently accessed data
        userRepository.findById(1);
        System.out.println("[UserService] Cache ready ✓");
    }

    @PreDestroy
    private void cleanup() {
        System.out.println("[UserService] @PreDestroy — flushing pending writes...");
        // Flush any pending writes before the pool closes
        System.out.println("[UserService] Flush complete");
    }

    public String getUser(int id) {
        return userRepository.findById(id);
    }

    public void createUser(String name) {
        userRepository.save(name);
        System.out.println("[UserService] Created user: " + name);
    }
}
