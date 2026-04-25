package app.service;

import minispring.annotation.Autowired;
import minispring.annotation.Component;

/**
 * Demonstrates FIELD INJECTION.
 *
 * Less boilerplate than constructor injection, but the dependency
 * (userService) is hidden — you cannot see it from the class signature.
 *
 * Use constructor injection in production. Field injection is acceptable
 * in test classes or small demo code.
 */
@Component
public class OrderService {

    @Autowired
    private UserService userService; // injected by the container after construction

    public String placeOrder(int userId, String item) {
        String user = userService.getUser(userId);
        return user + " ordered '" + item + "'";
    }
}
