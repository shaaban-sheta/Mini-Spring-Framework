package app.repository;

import minispring.annotation.Component;

/** Simulated database repository — no real DB, just in-memory data. */
@Component
public class UserRepository {

    public String findById(int id) {
        return "User #" + id + " {Alice}";
    }

    public boolean exists(int id) {
        return id > 0 && id < 1000;
    }
}
