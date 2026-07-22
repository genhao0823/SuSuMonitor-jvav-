package com.susumonitor.server.module.auth.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.module.auth.dto.LoginRequest;
import com.susumonitor.server.module.auth.dto.RegisterRequest;
import com.susumonitor.server.module.auth.service.UserService;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import com.susumonitor.server.module.auth.vo.LoginVo;
import com.susumonitor.server.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * 校验用户凭据并为已审核用户签发 JWT。
     *
     * @param request 登录请求
     * @param response HTTP 响应，用于禁止缓存敏感 Token
     * @return 登录结果
     */
    // 将 POST /api/auth/login 映射到当前方法。
    @PostMapping("/login")
    public ApiResponse<LoginVo> login(
            // 触发 LoginRequest 的 Bean Validation 校验。
            @Valid
            // 将 HTTP JSON 请求体反序列化为 LoginRequest。
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        return ApiResponse.success(userService.login(request));
    }

    /**
     * 返回 Bearer 过滤器从数据库加载的最新安全用户快照。
     *
     * @param authenticatedUser 当前认证用户
     * @return 当前用户信息
     */
    // 将 GET /api/auth/me 映射到当前方法。
    @GetMapping("/me")
    public ApiResponse<CurrentUserVo> me(
            // 从 Spring SecurityContext 注入安全用户 Principal。
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.success(authenticatedUser.toCurrentUserVo());
    }

    /**
     * 确认无状态退出，客户端收到响应后负责删除本地 JWT。
     *
     * @return data 为 null 的统一成功响应
     */
    // 将 POST /api/auth/logout 映射到当前方法。
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null);
    }
}
