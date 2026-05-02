package app.dto;

/**
 * Simple user DTO.
 */
public record User(int id, String name, String email) {
    public String toJson() {
        return "{\"id\":" + id
                + ",\"name\":\"" + name + "\""
                + ",\"email\":\"" + email + "\"}";
    }
}
