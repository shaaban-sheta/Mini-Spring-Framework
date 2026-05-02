package app.controller;

import app.dto.User;
import app.service.UserService;
import minispring.annotation.Autowired;
import minispring.web.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserController — demonstrates @Controller (meta-annotated with @Component),
 * @GetMapping with @PathVariable, @RequestParam with defaultValue, and @PostMapping
 * with @RequestBody.
 *
 * Note: no @Component needed — @Controller is meta-annotated with @Component,
 * so the scanner picks this class up automatically.
 */
@Controller
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
        System.out.println("[UserController] Initialized");
    }

    /** GET /users/{id} */
    @GetMapping("/users/{id}")
    public String getUserById(@PathVariable("id") int id) {
        User user = userService.findById(id);
        if (user == null) {
            return "{\"error\":\"User " + id + " not found\"}";
        }
        return user.toJson();
    }

    /** GET /users?limit=10 */
    @GetMapping("/users")
    public String getAllUsers(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<User> users = userService.findAll(limit);
        String json = users.stream()
                .map(User::toJson)
                .collect(Collectors.joining(",", "[", "]"));
        return "{\"count\":" + users.size() + ",\"users\":" + json + "}";
    }

    /** POST /users  body: {"name":"Alice","email":"alice@example.com"} */
    @PostMapping("/users")
    public String createUser(@RequestBody String body) {
        User created = userService.createFromJson(body);
        return "{\"status\":\"created\",\"user\":" + created.toJson() + "}";
    }

    /** GET /health */
    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\",\"framework\":\"MiniSpring Part 4\"}";
    }
}
