package com.susumonitor.server.module.auth.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.module.auth.dto.RegisterRequest;
import com.susumonitor.server.module.auth.service.UserService;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 将当前类注册为 REST Controller，并将返回值写入 HTTP 响应体。
@RestController
// 为认证接口统一增加 /api/auth 路径前缀。
@RequestMapping("/api/auth")
// 自动生成包含 final 字段的构造方法，用于构造方法依赖注入。
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 接收注册请求并委托 UserService 完成用户创建业务。
    @PostMapping("/register")
    public ApiResponse<CurrentUserVo> register(
            // 触发 RegisterRequest 的 Bean Validation 校验。
            @Valid
            // 将 HTTP JSON 请求体反序列化为 RegisterRequest。
            @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }
}
