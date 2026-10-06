package com.vault.service;

import com.vault.dao.UserDAO;
import com.vault.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    // In-memory token to User cache for fast session authentication
    private final Map<String, User> sessionTokens = new ConcurrentHashMap<>();

    public AuthService() {
        // Pre-create token mappings if needed or handle dynamically
    }

    public Map<String, Object> login(String email, String password) {
        User user = userDAO.authenticate(email, password);
        if (user != null) {
            String token = "vault_token_" + UUID.randomUUID().toString().replace("-", "");
            sessionTokens.put(token, user);
            return Map.of(
                "success", true,
                "token", token,
                "user", user
            );
        }
        return Map.of("success", false, "message", "Invalid email or password");
    }

    public Map<String, Object> register(String name, String email, String password, String role) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Map.of("success", false, "message", "Email and password are required");
        }
        User existing = userDAO.findByEmail(email.trim());
        if (existing != null) {
            return Map.of("success", false, "message", "An account with this email already exists");
        }

        User newUser = new User();
        newUser.setName(name != null && !name.trim().isEmpty() ? name.trim() : "Student");
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPassword(password.trim());
        newUser.setRole("ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "STUDENT");

        boolean created = userDAO.create(newUser);
        if (created) {
            String token = "vault_token_" + UUID.randomUUID().toString().replace("-", "");
            sessionTokens.put(token, newUser);
            return Map.of(
                "success", true,
                "token", token,
                "user", newUser
            );
        }
        return Map.of("success", false, "message", "Failed to create user account");
    }

    public User getUserByToken(String token) {
        if (token == null) return null;
        return sessionTokens.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            sessionTokens.remove(token);
        }
    }
}
