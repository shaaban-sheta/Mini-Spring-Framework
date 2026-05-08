package minispring.aop.interceptor;

import minispring.aop.InvocationChain;
import minispring.aop.MethodInterceptor;
import minispring.aop.annotation.Cacheable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheInterceptor implements MethodInterceptor {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object invoke(InvocationChain chain) throws Throwable {
        Cacheable cacheable = chain.getMethod().getAnnotation(Cacheable.class);
        if (cacheable == null) return chain.proceed();

        String key = buildKey(chain, cacheable);

        Object cached = cache.get(key);
        if (cached != null) {
            System.out.println("[Cache] HIT  " + key);
            return cached;
        }

        System.out.println("[Cache] MISS " + key);
        Object result = chain.proceed();
        if (result != null) {
            cache.put(key, result);
        }
        return result;
    }

    private String buildKey(InvocationChain chain, Cacheable cacheable) {
        if (!cacheable.key().isBlank()) {
            return cacheable.key() + Arrays.toString(chain.getArgs());
        }
        return chain.getMethod().getDeclaringClass().getSimpleName()
                + "." + chain.getMethod().getName()
                + Arrays.toString(chain.getArgs());
    }

    public void evict(String pattern) {
        cache.keySet().removeIf(k -> k.contains(pattern));
        System.out.println("[Cache] Evicted entries matching: " + pattern);
    }

    public void clear() {
        cache.clear();
        System.out.println("[Cache] Cleared all entries");
    }

    public int size() { return cache.size(); }
}
