package app.controller;

import app.dto.User;
import app.service.UserService;
import minispring.annotation.Autowired;
import minispring.aop.interceptor.SecurityInterceptor;
import minispring.web.annotation.*;

import java.util.Set;

/**
 * UserController for Part 5.
 *
 * Sets the security context before delegating to UserService.
 * In a production system, the security context would be set by an authentication
 * filter reading a JWT or session cookie — here we set it directly for demo.
 */
@Controller
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
        System.out.println("[UserController] Initialized");
    }

    @GetMapping("/users/{id}")
    public String getUserById(@PathVariable("id") int id) {
        // Simulate: every GET request has USER role
        SecurityInterceptor.setCurrentRoles(Set.of("USER"));
        try {
            User user = userService.findById(id);
            return user == null
                    ? "{\"error\":\"User " + id + " not found\"}"
                    : user.toJson();
        } finally {
            SecurityInterceptor.clearCurrentRoles();
        }
    }

    @PostMapping("/users")
    public String createUser(@RequestBody String body) {
        // Simulate: POST requests require ADMIN role
        SecurityInterceptor.setCurrentRoles(Set.of("ADMIN"));
        try {
            String name  = extract(body, "name");
            String email = extract(body, "email");
            User created = userService.create(name, email);
            return "{\"status\":\"created\",\"user\":" + created.toJson() + "}";
        } finally {
            SecurityInterceptor.clearCurrentRoles();
        }
    }

    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\",\"framework\":\"MiniSpring Part 5\"}";
    }

    private String extract(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "unknown";
        start += search.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "unknown" : json.substring(start, end);
    }
}
