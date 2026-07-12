package com.susumonitor.server.module.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.module.auth.dto.RegisterRequest;
import com.susumonitor.server.module.auth.entity.UserEntity;
import com.susumonitor.server.module.auth.mapper.UserMapper;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

// 启用 Mockito 扩展，为测试初始化字段上的 Mock 对象。
@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    // 创建 UserMapper 的模拟对象，隔离 UserService 对真实数据库的依赖。
    @Mock
    private UserMapper userMapper;

    // 创建 PasswordEncoder 的模拟对象，隔离密码编码器的具体实现。
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    // 在每个测试方法执行前创建待测试的 UserService 实例。
    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordEncoder);
    }

    // 验证第一个用户注册后成为已审核的管理员。
    @Test
    void registerFirstUserShouldCreateApprovedAdmin() {
        RegisterRequest request = registerRequest("admin", "Password123");
        when(userMapper.selectByUsername("admin")).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Password123")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity userEntity = invocation.getArgument(0);
            userEntity.setId(1L);
            userEntity.setCreatedAt(LocalDateTime.now());
            return 1;
        });

        CurrentUserVo result = userService.register(request);

        assertEquals(1L, result.getId());
        assertEquals("admin", result.getRole());
        assertEquals("approved", result.getReviewStatus());
        assertNotNull(result.getCreatedAt());
        verify(passwordEncoder).encode("Password123");
    }

    // 验证后续用户注册后成为待审核的普通用户。
    @Test
    void registerFollowingUserShouldCreatePendingUser() {
        RegisterRequest request = registerRequest("user", "Password123");
        when(userMapper.selectByUsername("user")).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(passwordEncoder.encode("Password123")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        CurrentUserVo result = userService.register(request);

        assertEquals("user", result.getRole());
        assertEquals("pending", result.getReviewStatus());
    }

    // 验证重复用户名返回资源冲突异常。
    @Test
    void registerExistingUsernameShouldThrowConflict() {
        RegisterRequest request = registerRequest("admin", "Password123");
        when(userMapper.selectByUsername("admin")).thenReturn(new UserEntity());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.register(request));

        assertEquals(40900, exception.getErrorCode().getCode());
    }

    // 验证数据库保存密码哈希，响应对象不暴露密码哈希。
    @Test
    void registerShouldNotReturnPasswordHash() {
        RegisterRequest request = registerRequest("admin", "Password123");
        when(userMapper.selectByUsername(anyString())).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        CurrentUserVo result = userService.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("bcrypt-hash", captor.getValue().getPasswordHash());
        assertEquals("admin", result.getRole());
    }

    private RegisterRequest registerRequest(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
