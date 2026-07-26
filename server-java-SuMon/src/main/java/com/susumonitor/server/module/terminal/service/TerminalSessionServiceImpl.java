package com.susumonitor.server.module.terminal.service;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.config.AppProperties;
import com.susumonitor.server.module.auth.entity.UserEntity;
import com.susumonitor.server.module.auth.mapper.UserMapper;
import com.susumonitor.server.module.server.entity.ServerEntity;
import com.susumonitor.server.module.server.mapper.ServerMapper;
import com.susumonitor.server.module.terminal.entity.TerminalSessionEntity;
import com.susumonitor.server.module.terminal.enums.TerminalSessionStatus;
import com.susumonitor.server.module.terminal.mapper.TerminalSessionMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实现终端元数据的创建、状态转换和单 JVM 会话额度控制。 */
@Service
public class TerminalSessionServiceImpl implements TerminalSessionService {
    private final TerminalSessionMapper sessionMapper;
    private final UserMapper userMapper;
    private final ServerMapper serverMapper;
    private final AppProperties properties;
    private final Clock clock;

    public TerminalSessionServiceImpl(TerminalSessionMapper sessionMapper, UserMapper userMapper,
            ServerMapper serverMapper, AppProperties properties, Clock clock) {
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
        this.serverMapper = serverMapper;
        this.properties = properties;
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TerminalSessionEntity openSession(Long userId, Long serverId, String openMessageId) {
        TerminalSessionEntity existing = sessionMapper.selectByUserIdAndOpenMessageId(userId, openMessageId);
        if (existing != null) {
            return existing;
        }
        UserEntity user = userMapper.selectAuthenticationUserById(userId);
        if (user == null || !"approved".equals(user.getReviewStatus())) {
            throw new BusinessException(ErrorCode.TERMINAL_ACCESS_DENIED);
        }
        ServerEntity server = serverMapper.selectActiveServerStatusById(serverId);
        if (server == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!"online".equals(server.getAgentStatus())) {
            throw new BusinessException(ErrorCode.TERMINAL_AGENT_OFFLINE);
        }
        enforceLimits(userId, serverId);
        LocalDateTime now = LocalDateTime.now(clock);
        TerminalSessionEntity session = new TerminalSessionEntity();
        session.setSessionId(UUID.randomUUID().toString());
        session.setOpenMessageId(openMessageId);
        session.setUserId(userId);
        session.setServerId(serverId);
        session.setStatus(TerminalSessionStatus.OPENING.value());
        session.setLastActivityAt(now);
        sessionMapper.insertSession(session);
        return session;
    }

    /** {@inheritDoc} */
    @Override
    public void markOpened(String sessionId, String shellIdentifier) {
        if (sessionMapper.markOpened(sessionId, shellIdentifier, LocalDateTime.now(clock)) == 0) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_STATE_CONFLICT);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void touchSession(String sessionId) {
        if (sessionMapper.updateLastActivity(sessionId, LocalDateTime.now(clock)) == 0) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_STATE_CONFLICT);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void closeSession(String sessionId, String status, String reason) {
        sessionMapper.closeSession(sessionId, status, reason, LocalDateTime.now(clock));
    }

    /** {@inheritDoc} */
    @Override
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime idleDeadline = now.minus(properties.getTerminal().getIdleTimeoutMinutes(), ChronoUnit.MINUTES);
        LocalDateTime maximumDeadline = now.minus(properties.getTerminal().getMaxSessionHours(), ChronoUnit.HOURS);
        for (TerminalSessionEntity session : sessionMapper.selectAllActive()) {
            if (session.getLastActivityAt().isBefore(idleDeadline)) {
                closeSession(session.getSessionId(), TerminalSessionStatus.TIMEOUT.value(), "idle_timeout");
            } else if (session.getCreatedAt().isBefore(maximumDeadline)) {
                closeSession(session.getSessionId(), TerminalSessionStatus.TIMEOUT.value(), "max_session_duration");
            }
        }
    }

    private void enforceLimits(Long userId, Long serverId) {
        int userCount = sessionMapper.selectActiveByUserId(userId).size();
        int serverCount = sessionMapper.selectActiveByServerId(serverId).size();
        if (userCount >= properties.getTerminal().getMaxSessionsPerUser()
                || serverCount >= properties.getTerminal().getMaxSessionsPerServer()
                || sessionMapper.selectAllActive().size() >= properties.getTerminal().getMaxSessions()) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_LIMIT_REACHED);
        }
    }
}
