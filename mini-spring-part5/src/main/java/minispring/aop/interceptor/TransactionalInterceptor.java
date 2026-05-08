package minispring.aop.interceptor;

import minispring.aop.InvocationChain;
import minispring.aop.MethodInterceptor;
import minispring.aop.annotation.Transactional;

public class TransactionalInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(InvocationChain chain) throws Throwable {
        Transactional tx = chain.getMethod().getAnnotation(Transactional.class);
        if (tx == null) return chain.proceed();

        String methodName = chain.getMethod().getName();
        System.out.println("[TX] BEGIN  — " + methodName);

        try {
            Object result = chain.proceed();
            System.out.println("[TX] COMMIT — " + methodName);
            return result;

        } catch (Throwable t) {
            boolean rollback = false;
            for (Class<? extends Throwable> rollbackType : tx.rollbackFor()) {
                if (rollbackType.isAssignableFrom(t.getClass())) {
                    rollback = true;
                    break;
                }
            }
            if (rollback) {
                System.out.println("[TX] ROLLBACK — " + methodName
                        + " (" + t.getClass().getSimpleName() + ")");
            } else {
                System.out.println("[TX] COMMIT (no-rollback exception) — " + methodName);
            }
            throw t; // always re-throw
        }
    }
}
