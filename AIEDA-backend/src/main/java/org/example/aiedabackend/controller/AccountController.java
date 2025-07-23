package org.example.aiedabackend.controller;

import org.example.aiedabackend.po.Account;
import org.example.aiedabackend.service.AccountService;
import org.example.aiedabackend.util.JwtUtil;
import org.example.aiedabackend.vo.AccountVO;
import org.example.aiedabackend.vo.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@RestController
@Tag(name = "账户管理", description = "账户相关接口")
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    AccountService accountService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据手机号获取用户详细信息")
    @GetMapping("/{phone}")
    public Response<AccountVO> getUser(
            @Parameter(description = "用户手机号", required = true, in = ParameterIn.PATH)
            @PathVariable String phone) {
        if (phone == null || phone.isEmpty()) {
            return Response.buildFailure("手机号不能为空", "400");
        }
        Account accountPO = accountService.getUserDetail(phone);
        AccountVO accountVO = accountPO.toVO();
        if (accountVO != null) {
            return Response.buildSuccess(accountVO);
        }
        return Response.buildFailure("用户不存在", "400");
    }

    /**
     * 创建新的用户
     */
    @Operation(summary = "用户注册", description = "注册一个新用户")
    @PostMapping()
    public Response<String> createUser(
            @Parameter(description = "用户信息对象", required = true)
            @RequestBody AccountVO accountVO) {
        String result = accountService.createUser(accountVO.toPO());
        if (result.equals("注册成功")){
            return Response.buildSuccess(result);
        }

        return Response.buildFailure(result,"400");
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录接口")
    @PostMapping("/login")
    public Response<String> login(
            @Parameter(description = "用户登录信息", required = true)
            @RequestBody AccountVO accountVO,
            HttpServletResponse response) {
        Account accountPO = accountVO.toPO();
        String token;
        try {
            Map<String, Object> map = accountService.login(accountPO);
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
    @Operation(summary = "删除用户", description = "根据手机号删除用户")
    @DeleteMapping()
    public Response<String> deleteUser(
            @Parameter(description = "用户手机号", required = true, in = ParameterIn.QUERY)
            @RequestParam String phone) {
        String result = accountService.deleteUser(phone);
        if (result.equals("删除成功")) {
            return Response.buildSuccess(result);
        }
        return Response.buildFailure(result, "400");
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新用户的详细信息")
    @PutMapping()
    public Response<String> updateUser(
            @Parameter(description = "用户信息对象", required = true)
            @RequestBody Account account) {
        String result = accountService.updateUser(account);
        if (result.equals("更新成功")) {
            return Response.buildSuccess(result);
        }
        return Response.buildFailure(result, "400");
    }
}