package org.example.aiedabackend.controller;

import org.example.aiedabackend.po.Account;
import org.example.aiedabackend.service.AccountService;
import org.example.aiedabackend.util.JwtUtil;
import org.example.aiedabackend.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    AccountService accountService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取用户详情
     */
    @GetMapping("/{phone}")
    public Response<Account> getUser(@PathVariable String phone) {
        if (phone == null || phone.isEmpty()) {
            return Response.buildFailure("手机号不能为空", "400");
        }
        Account account = accountService.getUserDetail(phone);
        if (account != null) {
            return Response.buildSuccess(account);
        }
        return Response.buildFailure("用户不存在", "400");
    }

    /**
     * 创建新的用户
     */
    @PostMapping()
    public Response<String> createUser(@RequestBody Account account) {
        String result = accountService.createUser(account);
        if (result.equals("注册成功")){
            return Response.buildSuccess(result);
        }

        return Response.buildFailure(result,"400");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Response<String> login(@RequestBody Account account, HttpServletResponse response) {
        String token;
        try {
            Map<String, Object> map = accountService.login(account);
            token = jwtUtil.generateToken((Integer) map.get("phone"), (String) map.get("username"));
            response.setHeader("token", token);
            //response.setHeader("username", (String) map.get("username"));
            response.setHeader("phone", String.valueOf(map.get("phone")));
            log.info("登录成功，生成的token: {}", token);
        } catch (IllegalArgumentException e) {
            return Response.buildFailure(e.getMessage(), "400");
        }
        return Response.buildSuccess(token);
    }

    /**
     * 删除用户
     */
    @DeleteMapping()
    public Response<String> deleteUser(@RequestParam String phone) {
        String result = accountService.deleteUser(phone);
        if (result.equals("删除成功")) {
            return Response.buildSuccess(result);
        }
        return Response.buildFailure(result, "400");
    }

    /**
     * 更新用户信息
     */
    @PutMapping()
    public Response<String> updateUser(@RequestBody Account account) {
        String result = accountService.updateUser(account);
        if (result.equals("更新成功")) {
            return Response.buildSuccess(result);
        }
        return Response.buildFailure(result, "400");
    }
}