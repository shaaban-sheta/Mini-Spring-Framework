package app.service;

import app.repository.UserRepository;
import minispring.annotation.Autowired;
import minispring.annotation.Component;

/**
 * Demonstrates CONSTRUCTOR INJECTION.
 *
 * The dependency is declared in the constructor — which gives us:
 *   - An immutable (final) field
 *   - All dependencies visible from the constructor signature
 *   - Easy to test: just pass a mock UserRepository to the constructor
 */
@Component
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getUser(int id) {
        if (!userRepository.exists(id)) return "User not found: " + id;
        return userRepository.findById(id);
    }
}
