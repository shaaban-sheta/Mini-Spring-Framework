package app.service;

import app.dto.User;

/**
 * Interface required for JDK dynamic proxy.
 *
 * AOP (Proxy.newProxyInstance) can only proxy interfaces.
 * The container wraps UserServiceImpl in a proxy that implements this interface.
 */
public interface UserService {
    User findById(int id);
    User create(String name, String email);
    void deleteUser(int id);
}
