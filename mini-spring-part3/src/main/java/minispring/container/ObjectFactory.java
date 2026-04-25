package minispring.container;

/**
 * A factory that creates or looks up a bean on demand.
 *
 * Inject ObjectFactory<T> instead of T when a singleton needs a
 * fresh prototype instance on every call, avoiding the classic
 * "prototype-in-singleton" trap.
 *
 * Usage:
 *   @Autowired
 *   private ObjectFactory<RequestContext> ctxFactory;
 *
 *   public void handle() {
 *       RequestContext ctx = ctxFactory.getObject(); // fresh each time
 *   }
 */
@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject();
}
