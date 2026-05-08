package minispring.aop;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Carries the chain of interceptors and the final real-method invocation.
 *
 * Each interceptor calls chain.proceed() to pass control to the next one.
 * After all interceptors run, the real target method is invoked via reflection.
 */
public class InvocationChain {

    private final Object                  target;
    private final Method                  method;
    private final Object[]                args;
    private final List<MethodInterceptor> interceptors;
    private int                           index = 0;

    public InvocationChain(Object target, Method method, Object[] args,
                           List<MethodInterceptor> interceptors) {
        this.target       = target;
        this.args         = args;
        this.interceptors = interceptors;
        // Resolve the *concrete* method on the target class so that annotations
        // placed on the implementation (not the interface) are visible to interceptors.
        Method resolved = method;
        try {
            resolved = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ignored) {}
        this.method = resolved;
    }

    public Object proceed() throws Throwable {
        if (index < interceptors.size()) {
            return interceptors.get(index++).invoke(this);
        }
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException(); // always unwrap!
        }
    }

    public Object   getTarget() { return target; }
    public Method   getMethod() { return method; }
    public Object[] getArgs()   { return args;   }
}
