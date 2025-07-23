package org.example.aiedabackend.vo;

import org.example.aiedabackend.po.Account;

public class AccountVO {

    private String username;

    private String phone;

    private String password;

    private String description;

    private Integer role;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Account toPO() {
        Account account = new Account();
        account.setUsername(this.username);
        account.setPhone(this.phone);
        account.setPassword(this.password);
        account.setDescription(this.description);
        account.setRole(this.role);
        return account;
    }
}
