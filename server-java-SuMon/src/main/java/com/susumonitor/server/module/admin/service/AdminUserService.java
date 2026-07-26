package com.susumonitor.server.module.admin.service;

import com.susumonitor.server.module.admin.vo.PendingUserVo;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import java.util.List;

/**
 * 定义管理员审核用户的业务契约，隔离 HTTP 调用方与具体持久化实现。
 */
public interface AdminUserService {

    /**
     * 查询待审核用户。
     *
     * @return 待审核用户列表
     */
    List<PendingUserVo> listPendingUsers();

    /**
     * 审核通过待审核用户。
     *
     * @param userId 待审核用户 ID
     * @param operatorUserId 审核管理员 ID
     * @return 审核后的用户公开信息
     */
    CurrentUserVo approveUser(Long userId, Long operatorUserId);

    /**
     * 拒绝待审核用户。
     *
     * @param userId 待审核用户 ID
     * @param operatorUserId 审核管理员 ID
     * @return 审核后的用户公开信息
     */
    CurrentUserVo rejectUser(Long userId, Long operatorUserId);
}
