package org.example.aiedabackend.service.serviceimpl;

import org.example.aiedabackend.constant.RoleConstant;
import org.example.aiedabackend.dao.AccountRepository;
import org.example.aiedabackend.po.Account;
import org.example.aiedabackend.service.AccountService;
import org.example.aiedabackend.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public String createUser(Account account) {
        if (accountRepository.findByPhone(account.getPhone()) != null) {
            return "手机号已存在";
        }

        // 密码加密存储
        account.setPassword(bCryptPasswordEncoder.encode(account.getPassword()));

        // 确保有默认角色
        if (account.getRole() == null) {
            account.setRole(Integer.valueOf(RoleConstant.USER)); // 默认为普通用户
        }

        accountRepository.save(account);
        return "注册成功";
    }

    @Override
    public Account getUserDetail(String phone) {
        Account account = accountRepository.findByPhone(phone);
        if (account == null) {
            return null;
        }
        account.setPassword(null); // 返回用户信息时，密码字段设为 null，防止泄露
        return account;
    }

    @Override
    public String updateUser(Account account) {
        Account existingAccount = accountRepository.findByPhone(account.getPhone());
        if (existingAccount == null) {
            return "用户不存在";
        }
        // 更新用户信息，允许部分更新
        if (account.getDescription() != null) {
            existingAccount.setDescription(account.getDescription());
        }
        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            existingAccount.setPassword(bCryptPasswordEncoder.encode(account.getPassword()));
        }
        accountRepository.save(existingAccount);
        return "更新成功";
    }

    @Override
    public Map<String, Object> login(Account account) {
        if (account == null || account.getPassword() == null) {
            throw new IllegalArgumentException("手机号或密码不能为空");
        }
        Account userAccount = accountRepository.findByPhone(account.getPhone());

        // 先检查用户是否存在
        if (userAccount == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 校验密码 - 使用BCrypt对输入的密码和数据库中存储的加密密码进行比对
        if (!bCryptPasswordEncoder.matches(account.getPassword(), userAccount.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        // 生成JWT令牌
        String token = jwtUtil.generateToken(userAccount.getUid(), userAccount.getUsername());
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("username", userAccount.getUsername());
        res.put("userId", userAccount.getUid());
        return res;
    }

    @Override
    public String deleteUser(String phone) {
        Account account = accountRepository.findByPhone(phone);
        if (account == null) {
            return "用户不存在";
        }
        accountRepository.delete(account);
        return "删除成功";
    }
}