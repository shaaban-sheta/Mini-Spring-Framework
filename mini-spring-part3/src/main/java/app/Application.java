package app;

import app.service.OrderService;
import app.service.UserService;
import minispring.container.MiniApplicationContext;

/**
 * Entry point for the Mini Spring Part 3 demo.
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass="app.Application"
 *
 * Expected startup output:
 *   [MiniSpring] Registered N bean definition(s)
 *   [Pool] Initializing connection pool...
 *   [Pool] Ready with 5 connections
 *   [UserRepository] Constructor called (pool injected)
 *   [Pool] Acquired: ...  (validation acquire)
 *   [Pool] Released: ...
 *   [UserRepository] Pool connection validated ✓
 *   [UserService] Constructor called
 *   [Pool] Acquired: ...  (cache warmup)
 *   [Pool] Released: ...
 *   [UserService] Cache ready ✓
 *   ... etc.
 *
 * Expected shutdown output (on Ctrl+C or normal exit):
 *   [MiniSpring] Shutting down...
 *   [MiniSpring] @PreDestroy called on: userService    (first — created last)
 *   [MiniSpring] @PreDestroy called on: connectionPool (last — created first)
 *   [MiniSpring] Shutdown complete
 */
public class Application {

    public static void main(String[] args) {
        MiniApplicationContext ctx = new MiniApplicationContext("app");

        System.out.println("\n--- Application running ---\n");

        UserService  userService  = ctx.getBean(UserService.class);
        OrderService orderService = ctx.getBean(OrderService.class);

        System.out.println("\n[App] Look up user 42: " + userService.getUser(42));
        userService.createUser("Alice");

        System.out.println("\n[App] Place two orders (each gets a fresh RequestContext):");
        orderService.placeOrder(1, "Laptop");
        orderService.placeOrder(2, "Phone");

        System.out.println("\n--- Triggering shutdown ---");
        ctx.close(); // also called automatically by the JVM shutdown hook
    }
}
