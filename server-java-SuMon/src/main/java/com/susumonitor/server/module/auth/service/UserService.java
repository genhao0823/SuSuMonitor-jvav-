package com.susumonitor.server.module.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.auth.dto.RegisterRequest;
import com.susumonitor.server.module.auth.entity.UserEntity;
import com.susumonitor.server.module.auth.mapper.UserMapper;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 将当前类注册为 Spring Service Bean，承载用户相关业务逻辑。
@Service
// 自动生成包含 final 字段的构造方法，用于构造方法依赖注入。
@RequiredArgsConstructor
public class UserService {

    private static final String ADMIN_ROLE = "admin";

    private static final String USER_ROLE = "user";

    private static final String APPROVED_STATUS = "approved";

    private static final String PENDING_STATUS = "pending";

    private static final ZoneId APPLICATION_ZONE = ZoneId.systemDefault();

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    // 在事务中创建用户，并根据当前是否存在用户设置首个用户的管理员和审核状态。
    @Transactional
    public CurrentUserVo register(RegisterRequest request) {
        UserEntity existingUser = userMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT);
        }

        boolean firstUser = userMapper.selectCount(Wrappers.emptyWrapper()) == 0;
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.getUsername());
        userEntity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userEntity.setRole(firstUser ? ADMIN_ROLE : USER_ROLE);
        userEntity.setReviewStatus(firstUser ? APPROVED_STATUS : PENDING_STATUS);
        userEntity.setCreatedAt(LocalDateTime.now());

        if (userMapper.insert(userEntity) != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR);
        }
        return toCurrentUserVo(userEntity);
    }

    private CurrentUserVo toCurrentUserVo(UserEntity userEntity) {
        CurrentUserVo currentUserVo = new CurrentUserVo();
        currentUserVo.setId(userEntity.getId());
        currentUserVo.setUsername(userEntity.getUsername());
        currentUserVo.setRole(userEntity.getRole());
        currentUserVo.setReviewStatus(userEntity.getReviewStatus());
        currentUserVo.setReviewedAt(toOffsetDateTime(userEntity.getReviewedAt()));
        currentUserVo.setCreatedAt(toOffsetDateTime(userEntity.getCreatedAt()));
        return currentUserVo;
    }

    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(APPLICATION_ZONE).toOffsetDateTime();
    }
}
