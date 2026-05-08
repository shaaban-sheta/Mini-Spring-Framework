package minispring.aop;

/**
 * Contract for a single piece of cross-cutting behaviour.
 *
 * Implementations inspect the method/args, run pre-logic,
 * call chain.proceed() to continue, then run post-logic.
 */
public interface MethodInterceptor {
    Object invoke(InvocationChain chain) throws Throwable;
}
