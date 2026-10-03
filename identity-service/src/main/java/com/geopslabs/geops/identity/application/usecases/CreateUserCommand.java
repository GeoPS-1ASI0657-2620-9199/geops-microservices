package com.geopslabs.geops.identity.application.usecases;

public record CreateUserCommand(
    String name,
    String email,
    String phone,
    String password,
    String role
) {
    public CreateUserCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be null or empty");
        }
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("Invalid phone format");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
    }

    private static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private static boolean isValidPhone(String phone) {
        String phoneRegex = "^[+]?[0-9]{1,4}?[-\\s]?[(]?[0-9]{1,4}[)]?[-\\s]?[0-9]{1,4}[-\\s]?[0-9]{1,9}$";
        return phone.matches(phoneRegex);
    }
}
