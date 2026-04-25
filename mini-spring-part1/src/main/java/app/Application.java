package app;

import app.service.UserService;
import minispring.container.MiniApplicationContext;

/**
 * Entry point for the Part 1 demo.
 *
 * What to observe:
 *   1. Two packages are scanned → 2 beans registered automatically
 *   2. Beans are retrieved by both type and name
 *   3. No XML, no manual new(), no Spring on the classpath
 *
 * Try adding a new @Component class to app.service or app.repository
 * and re-running. It will appear in the count automatically.
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini Spring Part 1: IoC Container Demo ===");
        System.out.println();

        // Bootstrap: scan, register, instantiate
        MiniApplicationContext context =
                new MiniApplicationContext("app.service", "app.repository");

        System.out.println();

        // ── Retrieve by type (type-safe, compile-time verified) ──────────────
        UserService userService = context.getBean(UserService.class);
        System.out.println("getBean(UserService.class) → " + userService.getClass().getSimpleName());
        System.out.println("userService.findUser(42)   → " + userService.findUser(42));

        System.out.println();

        // ── Retrieve by name (uses explicit @Component("userRepo")) ──────────
        Object repo = context.getBean("userRepo");
        System.out.println("getBean(\"userRepo\")         → " + repo.getClass().getSimpleName());

        System.out.println();

        // ── Container info ───────────────────────────────────────────────────
        System.out.println("containsBean(\"userService\") → " + context.containsBean("userService"));
        System.out.println("containsBean(\"unknown\")     → " + context.containsBean("unknown"));
        System.out.println("getBeanCount()              → " + context.getBeanCount());

        System.out.println();
        System.out.println("=== Done ===");
    }
}
