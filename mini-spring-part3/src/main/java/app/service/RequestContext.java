package app.service;

import minispring.annotation.Component;
import minispring.annotation.PostConstruct;
import minispring.annotation.Scope;

import java.util.UUID;

/**
 * Prototype-scoped request context.
 *
 * @Scope("prototype") means a new instance is created on each getBean() call.
 * Compare with OrderService which injects this via ObjectFactory<RequestContext>
 * to get a fresh instance for each order.
 *
 * Note: @PreDestroy is NEVER called on prototype beans by the container.
 * If this bean held a resource, the caller would be responsible for closing it.
 */
@Component
@Scope("prototype")
public class RequestContext {

    private final String requestId;

    public RequestContext() {
        this.requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[RequestContext] Created new context: " + requestId);
    }

    @PostConstruct
    public void setup() {
        // @PostConstruct IS called for prototypes — every time a new instance is made
        System.out.println("[RequestContext] @PostConstruct for context: " + requestId);
    }

    public String getRequestId() { return requestId; }
}
