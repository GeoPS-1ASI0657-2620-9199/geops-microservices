package com.geopslabs.geops.identity.domain.models;

import java.util.Date;

public class User {
    private static final String ADMIN_ROLE = "ADMIN";

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String role;
    private Date createdAt;
    private Date updatedAt;

    public User(String name, String email, String phone, String password, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    public User(Long id, User data, Date createdAt, Date updatedAt) {
        this(data.name, data.email, data.phone, data.password, data.role);
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateUser(String name, String email, String phone, String role) {
        this.name = valueOrCurrent(name, this.name);
        this.email = valueOrCurrent(email, this.email);
        this.phone = valueOrCurrent(phone, this.phone);
        this.role = valueOrCurrent(role, this.role);
    }

    public void updatePassword(String encryptedPassword) {
        this.password = valueOrCurrent(encryptedPassword, this.password);
    }

    public boolean isAdmin() {
        return ADMIN_ROLE.equalsIgnoreCase(role);
    }

    private static String valueOrCurrent(String candidate, String current) {
        return candidate != null && !candidate.isBlank() ? candidate : current;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
