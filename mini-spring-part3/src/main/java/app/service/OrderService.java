package app.service;

import minispring.annotation.Autowired;
import minispring.annotation.Component;
import minispring.annotation.Scope;
import minispring.container.ObjectFactory;

/**
 * OrderService — demonstrates the ObjectFactory<T> pattern.
 *
 * RequestContext is prototype-scoped. If we injected it directly:
 *   @Autowired RequestContext context;   // WRONG — frozen at construction
 *
 * Instead we inject an ObjectFactory so each call gets a fresh instance.
 */
@Component
public class OrderService {

    private final UserService userService;

    // Inject factory, not the prototype itself
    @Autowired
    private ObjectFactory<RequestContext> requestContextFactory;

    @Autowired
    public OrderService(UserService userService) {
        this.userService = userService;
        System.out.println("[OrderService] Constructor called");
    }

    public void placeOrder(int userId, String item) {
        // Fresh RequestContext for every order — correct prototype behaviour
        RequestContext ctx = requestContextFactory.getObject();
        System.out.println("[OrderService] Order [" + ctx.getRequestId() + "] — "
                + userService.getUser(userId) + " ordered: " + item);
    }
}
