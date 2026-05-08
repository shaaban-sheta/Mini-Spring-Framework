package minispring.aop;

import minispring.aop.annotation.*;
import minispring.aop.interceptor.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Creates JDK dynamic proxies for beans that have AOP annotations.
 *
 * Interceptor chain order (applied to every call on the proxy):
 *   Timed → Security → Transactional → Cache → real method
 *
 * A proxy is only created if the bean implements at least one interface
 * AND at least one of its methods has an AOP annotation.
 */
public class ProxyFactory {

    private final TimedInterceptor         timedInterceptor    = new TimedInterceptor();
    private final SecurityInterceptor      securityInterceptor = new SecurityInterceptor();
    private final TransactionalInterceptor txInterceptor       = new TransactionalInterceptor();
    private final CacheInterceptor         cacheInterceptor    = new CacheInterceptor();

    private final List<MethodInterceptor> interceptors = List.of(
            timedInterceptor,
            securityInterceptor,
            txInterceptor,
            cacheInterceptor
    );

    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, Class<?>[] interfaces) {
        if (interfaces.length == 0) return bean;
        if (!requiresProxy(bean.getClass())) return bean;

        System.out.println("[ProxyFactory] Creating AOP proxy for: "
                + bean.getClass().getSimpleName());

        return (T) Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                interfaces,
                (proxy, method, args) -> {
                    InvocationChain chain = new InvocationChain(
                            bean, method, args != null ? args : new Object[0], interceptors);
                    return chain.proceed();
                }
        );
    }

    private boolean requiresProxy(Class<?> beanClass) {
        Class<?> current = beanClass;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (hasAop(m)) return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean hasAop(Method m) {
        return m.isAnnotationPresent(Timed.class)
                || m.isAnnotationPresent(Secured.class)
                || m.isAnnotationPresent(Transactional.class)
                || m.isAnnotationPresent(Cacheable.class);
    }

    public CacheInterceptor    getCacheInterceptor()    { return cacheInterceptor; }
    public SecurityInterceptor getSecurityInterceptor() { return securityInterceptor; }
}
