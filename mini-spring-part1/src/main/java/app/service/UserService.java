package app.service;

import minispring.annotation.Component;

/**
 * Example service bean discovered automatically via @Component scanning.
 *
 * Notice:
 *   - No explicit bean name → generated as "userService"
 *   - No-arg constructor → used by the container to instantiate it
 *   - No @Autowired dependencies → Part 1 limitation, fixed in Part 2
 */
@Component
public class UserService {

    public String findUser(int id) {
        // Simulated: real version would query a database
        return "User #" + id + " (Alice)";
    }

    public boolean userExists(int id) {
        return id > 0 && id < 1000;
    }
}
