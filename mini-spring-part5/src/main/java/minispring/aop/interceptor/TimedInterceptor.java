package minispring.aop.interceptor;

import minispring.aop.InvocationChain;
import minispring.aop.MethodInterceptor;
import minispring.aop.annotation.Timed;

public class TimedInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(InvocationChain chain) throws Throwable {
        Timed timed = chain.getMethod().getAnnotation(Timed.class);
        if (timed == null) return chain.proceed();

        String name = timed.value().isBlank()
                ? chain.getMethod().getDeclaringClass().getSimpleName()
                  + "." + chain.getMethod().getName()
                : timed.value();

        long start = System.nanoTime();
        try {
            return chain.proceed();
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[Timed] %-40s %d ms%n", name, ms);
        }
    }
}
