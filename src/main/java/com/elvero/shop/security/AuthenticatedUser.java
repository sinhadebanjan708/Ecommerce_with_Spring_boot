package com.elvero.shop.security;

import java.util.List;

/** The logged-in user, rebuilt from the access-token claims (user_id, email, role). */
public record AuthenticatedUser(Long userId, String email, List<String> roles) {
}
