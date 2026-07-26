package com.susumonitor.server.module.auth.service;

import com.susumonitor.server.module.auth.dto.LoginRequest;
import com.susumonitor.server.module.auth.dto.RegisterRequest;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import com.susumonitor.server.module.auth.vo.LoginVo;

/**
 * 定义认证用例的业务契约，供 HTTP 适配层依赖而不耦合具体实现。
 */
public interface UserService {

    /**
     * 注册用户，并在首用户场景完成管理员初始化。
     *
     * @param request 注册请求
     * @return 当前用户公开信息
     */
    CurrentUserVo register(RegisterRequest request);

    /**
     * 校验用户凭据并签发访问令牌。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    LoginVo login(LoginRequest request);
}
