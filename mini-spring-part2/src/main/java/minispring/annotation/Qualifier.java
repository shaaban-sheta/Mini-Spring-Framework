package minispring.annotation;

import java.lang.annotation.*;

/**
 * Disambiguates injection when multiple beans match the requested type.
 *
 * Usage on fields:
 *   @Autowired
 *   @Qualifier("emailNotifier")
 *   private NotificationService notifier;
 *
 * Usage on constructor parameters:
 *   @Autowired
 *   public OrderService(@Qualifier("emailNotifier") NotificationService n) { ... }
 *
 * The value() must exactly match the bean name in the registry.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Qualifier {
    String value(); // required — no default
}
