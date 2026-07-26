package com.susumonitor.server.module.server.service;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.server.entity.ServerEntity;
import com.susumonitor.server.module.server.mapper.ServerMapper;
import com.susumonitor.server.module.server.vo.AgentTokenVo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理服务器 Agent Token 的首次注册、显式轮换和撤销生命周期。
 */
@Service
public class AgentTokenServiceImpl implements AgentTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String HASH_PREFIX = "sha256:";
    private static final ZoneId APPLICATION_ZONE = ZoneOffset.UTC;

    private final ServerMapper serverMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 注入服务器 Mapper。 */
    public AgentTokenServiceImpl(ServerMapper serverMapper) {
        this.serverMapper = serverMapper;
    }

    /** 首次生成服务器 Agent Token。 */
    @Transactional
    public AgentTokenVo register(Long serverId) {
        return createToken(serverId, false);
    }

    /** 显式轮换已有服务器 Agent Token。 */
    @Transactional
    public AgentTokenVo rotate(Long serverId) {
        return createToken(serverId, true);
    }

    /** 撤销服务器当前 Agent Token。 */
    @Transactional
    public void revoke(Long serverId) {
        validateServerId(serverId);
        try {
            ServerEntity server = serverMapper.selectActiveServerAgentTokenById(serverId);
            if (server == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if (server.getAgentTokenHash() == null || server.getAgentTokenRevokedAt() != null
                    || serverMapper.revokeAgentToken(serverId, LocalDateTime.now(ZoneOffset.UTC)) != 1) {
                throw new BusinessException(ErrorCode.RESOURCE_CONFLICT);
            }
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, exception);
        }
    }

    private AgentTokenVo createToken(Long serverId, boolean rotation) {
        validateServerId(serverId);
        try {
            ServerEntity server = serverMapper.selectActiveServerAgentTokenById(serverId);
            if (server == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if (rotation != (server.getAgentTokenHash() != null)) {
                throw new BusinessException(ErrorCode.RESOURCE_CONFLICT);
            }
            String token = generateToken();
            String hash = hashToken(token);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            int updated = rotation
                    ? serverMapper.rotateAgentToken(serverId, hash, now)
                    : serverMapper.registerAgentToken(serverId, server.getAgentId() == null
                            ? generateAgentId() : server.getAgentId(), hash, now);
            if (updated != 1) {
                throw new BusinessException(ErrorCode.RESOURCE_CONFLICT);
            }
            return new AgentTokenVo(serverId, token, toOffsetDateTime(now));
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, exception);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateAgentId() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HASH_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateServerId(Long serverId) {
        if (serverId == null || serverId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime.atZone(APPLICATION_ZONE).toOffsetDateTime();
    }
}
