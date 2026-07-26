package com.susumonitor.server.module.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.auth.entity.UserEntity;
import com.susumonitor.server.module.auth.mapper.UserMapper;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证管理员审核状态机和条件更新失败分支。
 */
// 启用 Mockito 扩展，为测试创建 Mapper 替身。
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTests {

    // 隔离真实数据库，精确验证审核业务分支。
    @Mock
    private UserMapper userMapper;

    private AdminUserService adminUserService;

    // 在每个测试前创建管理员服务。
    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(userMapper);
    }

    // 验证待审核列表返回安全 VO。
    @Test
    void listPendingUsersShouldReturnUsers() {
        when(userMapper.selectPendingUsers()).thenReturn(List.of(user("pending", "user")));

        assertEquals(1, adminUserService.listPendingUsers().size());
    }

    // 验证批准用户记录审核状态和时间。
    @Test
    void approvePendingUserShouldSucceed() {
        when(userMapper.selectReviewUserById(2L)).thenReturn(user("pending", "user"));
        when(userMapper.updateReviewStatus(any(), anyString(), any(), any())).thenReturn(1);

        CurrentUserVo result = adminUserService.approveUser(2L, 1L);

        assertEquals("approved", result.getReviewStatus());
        assertNotNull(result.getReviewedAt());
    }

    // 验证拒绝用户返回 rejected。
    @Test
    void rejectPendingUserShouldSucceed() {
        when(userMapper.selectReviewUserById(2L)).thenReturn(user("pending", "user"));
        when(userMapper.updateReviewStatus(any(), anyString(), any(), any())).thenReturn(1);

        assertEquals("rejected", adminUserService.rejectUser(2L, 1L).getReviewStatus());
    }

    // 验证不存在目标返回 404。
    @Test
    void missingUserShouldReturnNotFound() {
        when(userMapper.selectReviewUserById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminUserService.approveUser(99L, 1L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    // 验证已审核用户返回冲突。
    @Test
    void reviewedUserShouldReturnConflict() {
        when(userMapper.selectReviewUserById(2L)).thenReturn(user("approved", "user"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminUserService.rejectUser(2L, 1L));

        assertEquals(ErrorCode.RESOURCE_CONFLICT, exception.getErrorCode());
    }

    // 验证管理员目标返回冲突。
    @Test
    void adminTargetShouldReturnConflict() {
        when(userMapper.selectReviewUserById(2L)).thenReturn(user("pending", "admin"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminUserService.approveUser(2L, 1L));

        assertEquals(ErrorCode.RESOURCE_CONFLICT, exception.getErrorCode());
    }

    // 验证并发条件更新失败返回冲突。
    @Test
    void concurrentUpdateShouldReturnConflict() {
        when(userMapper.selectReviewUserById(2L)).thenReturn(user("pending", "user"));
        when(userMapper.updateReviewStatus(any(), anyString(), any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminUserService.approveUser(2L, 1L));

        assertEquals(ErrorCode.RESOURCE_CONFLICT, exception.getErrorCode());
    }

    // 创建审核目标用户。
    private UserEntity user(String status, String role) {
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setUsername("review_target");
        user.setRole(role);
        user.setReviewStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
