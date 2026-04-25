package minispring.annotation;

import java.lang.annotation.*;

/**
 * Marks a constructor or field for automatic dependency injection.
 *
 * Constructor injection (recommended):
 *   @Autowired
 *   public UserService(UserRepository repo) { this.repo = repo; }
 *
 * Field injection (convenient, but hides dependencies):
 *   @Autowired
 *   private UserRepository userRepository;
 *
 * Rules:
 *   - At most ONE constructor may be @Autowired
 *   - Cannot be used on final fields (use constructor injection instead)
 *   - If a class has exactly one constructor and no @Autowired, that constructor
 *     is used implicitly (Spring 4.3+ behavior)
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
}
