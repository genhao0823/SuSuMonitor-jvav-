package com.susumonitor.server.websocket;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.security.AuthenticatedUser;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 管理单 JVM 内存中的一次性 Monitor ticket，避免浏览器把长期 JWT 放入 WebSocket URL。
 */
@Service
public class MonitorTicketService {

    private static final Duration DEFAULT_TICKET_TTL = Duration.ofSeconds(30);
    private static final int TICKET_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, TicketEntry> tickets = new ConcurrentHashMap<>();
    private final Duration ticketTtl;

    /** 默认构造，Spring 组件扫描使用，ticket 有效期 30 秒。 */
    public MonitorTicketService() {
        this(DEFAULT_TICKET_TTL);
    }

    /** 包私有构造，允许测试注入短 TTL 以验证过期清理逻辑。 */
    MonitorTicketService(Duration ticketTtl) {
        this.ticketTtl = ticketTtl;
    }

    /** 为已认证用户签发一次性 ticket。 */
    public MonitorTicketVo issue(AuthenticatedUser user) {
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(ticketTtl);
        tickets.put(ticket, new TicketEntry(user, expiresAt));
        return new MonitorTicketVo(ticket, expiresAt.atOffset(ZoneOffset.UTC));
    }

    /** 原子消费 ticket，过期或重复使用均视为未认证。 */
    public AuthenticatedUser consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        TicketEntry entry = tickets.remove(ticket);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return entry.user();
    }

    /** 每分钟清理已过期但从未被消费的 ticket，防止内存无界累积。 */
    @Scheduled(fixedDelay = 60_000)
    public void purgeExpiredTickets() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record TicketEntry(AuthenticatedUser user, Instant expiresAt) {
    }
}
