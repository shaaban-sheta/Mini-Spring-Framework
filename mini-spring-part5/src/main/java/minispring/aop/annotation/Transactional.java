package minispring.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    Class<? extends Throwable>[] rollbackFor() default {RuntimeException.class};
}
