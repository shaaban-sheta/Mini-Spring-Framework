package app;

import app.service.OrderService;
import app.service.UserService;
import minispring.container.MiniApplicationContext;

/**
 * Part 2 demo: Dependency Injection in action.
 *
 * The object graph:
 *   OrderService
 *     └─(field)──► UserService
 *                    └─(constructor)──► UserRepository
 *
 * We never call 'new' for any of these. The container builds the
 * entire graph automatically via reflection.
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini Spring Part 2: Dependency Injection Demo ===");
        System.out.println();

        MiniApplicationContext ctx =
                new MiniApplicationContext("app.service", "app.repository");

        System.out.println();

        // ── Constructor-injected service ─────────────────────────────────────
        UserService userService = ctx.getBean(UserService.class);
        System.out.println("getUser(42): " + userService.getUser(42));
        System.out.println("getUser(9999): " + userService.getUser(9999)); // not found

        System.out.println();

        // ── Field-injected service ────────────────────────────────────────────
        OrderService orderService = ctx.getBean(OrderService.class);
        System.out.println("placeOrder(42, \"Mechanical Keyboard\"): "
                + orderService.placeOrder(42, "Mechanical Keyboard"));

        System.out.println();
        System.out.println("Total beans: " + ctx.getBeanCount());
        System.out.println();
        System.out.println("=== Done ===");
    }
}
