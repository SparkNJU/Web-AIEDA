package org.example.aiedabackend.service;

import org.example.aiedabackend.po.Account;

import java.util.Map;

public interface AccountService {
    String createUser(Account account);
    Account getUserDetail(String phone);
    String updateUser(Account account);
    Map<String,Object> login(Account account);
    String deleteUser(String phone);
}