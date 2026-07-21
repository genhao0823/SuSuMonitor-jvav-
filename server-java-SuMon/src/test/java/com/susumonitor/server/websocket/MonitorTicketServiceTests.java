package com.susumonitor.server.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.security.AuthenticatedUser;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** 验证 Monitor ticket 的短时、一次性消费和过期清理规则。 */
class MonitorTicketServiceTests {

    /** 验证同一 ticket 只能消费一次。 */
    @Test
    void ticketShouldBeSingleUse() {
        MonitorTicketService service = new MonitorTicketService();
        AuthenticatedUser user = new AuthenticatedUser(1L, "admin", "admin", "approved", null,
                OffsetDateTime.now());

        MonitorTicketVo ticket = service.issue(user);

        assertEquals(user, service.consume(ticket.ticket()));
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.consume(ticket.ticket()));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    /** 验证过期 ticket 消费被拒，且清理任务会移除过期未消费的 entry，避免内存泄漏。 */
    @Test
    void expiredTicketShouldBeRejectedAndPurged() throws Exception {
        // 注入 100 毫秒短 TTL，避免测试等待 30 秒。
        MonitorTicketService service = new MonitorTicketService(Duration.ofMillis(100));
        AuthenticatedUser user = new AuthenticatedUser(1L, "admin", "admin", "approved", null,
                OffsetDateTime.now());

        MonitorTicketVo ticket = service.issue(user);
        // 等待超过 TTL。
        Thread.sleep(300);

        // 过期 ticket 消费被拒。
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.consume(ticket.ticket()));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());

        // 清理任务应移除该过期 entry。
        service.purgeExpiredTickets();
        BusinessException purgeCheck = assertThrows(
                BusinessException.class, () -> service.consume(ticket.ticket()));
        assertEquals(ErrorCode.UNAUTHORIZED, purgeCheck.getErrorCode());
    }
}

