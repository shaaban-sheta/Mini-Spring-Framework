package minispring.aop.interceptor;

import minispring.aop.InvocationChain;
import minispring.aop.MethodInterceptor;
import minispring.aop.annotation.Secured;

import java.util.Arrays;
import java.util.Set;

/**
 * Checks that the current thread's security context contains at least one
 * of the roles required by @Secured before proceeding.
 *
 * SecurityContext is stored in a ThreadLocal so each HTTP request thread
 * has its own independent security state.
 */
public class SecurityInterceptor implements MethodInterceptor {

    private static final ThreadLocal<Set<String>> currentRoles =
            ThreadLocal.withInitial(Set::of);

    public static void setCurrentRoles(Set<String> roles) {
        currentRoles.set(roles);
    }

    public static void clearCurrentRoles() {
        currentRoles.remove(); // prevent thread-pool leak
    }

    public static Set<String> getCurrentRoles() {
        return currentRoles.get();
    }

    @Override
    public Object invoke(InvocationChain chain) throws Throwable {
        Secured secured = chain.getMethod().getAnnotation(Secured.class);
        if (secured == null) return chain.proceed();

        Set<String> roles    = currentRoles.get();
        String[]    required = secured.value();

        boolean ok = Arrays.stream(required).anyMatch(roles::contains);
        if (!ok) {
            throw new SecurityException(
                    "Access denied. '" + chain.getMethod().getName()
                    + "' requires one of: " + Arrays.toString(required)
                    + ". Have: " + roles);
        }
        return chain.proceed();
    }
}
