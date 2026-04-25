package app.repository;

import minispring.annotation.Component;

/**
 * Example repository bean with an EXPLICIT bean name.
 *
 * @Component("userRepo") → registered as "userRepo", not "userRepository"
 *
 * This demonstrates the optional value() element of @Component.
 */
@Component("userRepo")
public class UserRepository {

    public boolean exists(int id) {
        return id > 0 && id < 1000;
    }

    public String findById(int id) {
        return "MockUser{id=" + id + ", name='Alice', email='alice@example.com'}";
    }
}
