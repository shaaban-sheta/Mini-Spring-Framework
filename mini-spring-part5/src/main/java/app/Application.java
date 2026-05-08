package app;

import app.service.UserService;
import minispring.aop.interceptor.SecurityInterceptor;
import minispring.container.MiniApplicationContext;
import minispring.web.MiniWebServer;

import java.util.Set;

/**
 * Entry point for Mini Spring Part 5 — Complete Framework with AOP.
 *
 * Demonstrates:
 *   1. Container bootstraps all beans (IoC + DI + Lifecycle from Parts 1–3)
 *   2. Meta-annotation scanning picks up @Controller (Web layer from Part 4)
 *   3. AOP proxy wraps UserServiceImpl with @Timed, @Secured, @Transactional, @Cacheable
 *
 * Run:
 *   mvn compile exec:java -Dexec.mainClass="app.Application"
 *
 * Test:
 *   curl http://localhost:8080/health
 *   curl http://localhost:8080/users/1         (cached on 2nd call)
 *   curl http://localhost:8080/users/1         (cache HIT — no DB query printed)
 *   curl -X POST http://localhost:8080/users \
 *        -H "Content-Type:application/json" \
 *        -d '{"name":"Shaaban","email":"shaaban@example.com"}'
 *
 * Programmatic AOP demo (see main method):
 *   - Sets USER roles  → findById succeeds
 *   - Sets ADMIN roles → create and delete succeed
 *   - Sets no roles    → access denied exception
 */
public class Application {

    public static void main(String[] args) throws Exception {
        MiniApplicationContext ctx = new MiniApplicationContext("app");

        // ---- Programmatic AOP demo ----
        System.out.println("\n=== AOP Demo ===\n");

        UserService userService = ctx.getBean(UserService.class);

        // 1. Successful read with USER role (cache MISS first, then HIT)
        System.out.println("--- Call 1: findById(1) with USER role ---");
        SecurityInterceptor.setCurrentRoles(Set.of("USER"));
        var user1 = userService.findById(1);
        System.out.println("Result: " + user1);
        SecurityInterceptor.clearCurrentRoles();

        System.out.println("\n--- Call 2: findById(1) again (should be CACHE HIT) ---");
        SecurityInterceptor.setCurrentRoles(Set.of("USER"));
        var user1Again = userService.findById(1);
        System.out.println("Result: " + user1Again);
        SecurityInterceptor.clearCurrentRoles();

        // 2. Create with ADMIN role
        System.out.println("\n--- Call 3: create() with ADMIN role ---");
        SecurityInterceptor.setCurrentRoles(Set.of("ADMIN"));
        var newUser = userService.create("Shaaban", "shaaban@example.com");
        System.out.println("Created: " + newUser);
        SecurityInterceptor.clearCurrentRoles();

        // 3. Delete rolls back transaction (user not found)
        System.out.println("\n--- Call 4: deleteUser(999) — should ROLLBACK ---");
        SecurityInterceptor.setCurrentRoles(Set.of("ADMIN"));
        try {
            userService.deleteUser(999);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        } finally {
            SecurityInterceptor.clearCurrentRoles();
        }

        // 4. Security check failure
        System.out.println("\n--- Call 5: create() with USER role (insufficient) ---");
        SecurityInterceptor.setCurrentRoles(Set.of("USER"));
        try {
            userService.create("Anas", "anas@example.com");
        } catch (SecurityException e) {
            System.out.println("Expected security error: " + e.getMessage());
        } finally {
            SecurityInterceptor.clearCurrentRoles();
        }

        // ---- Start the HTTP server ----
        System.out.println("\n=== Starting HTTP Server ===\n");
        MiniWebServer server = new MiniWebServer(ctx);
        server.start(8080);

        System.out.println("\nPress Ctrl+C to stop\n");
        Thread.currentThread().join();
    }
}
