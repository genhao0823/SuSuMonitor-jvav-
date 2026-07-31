package com.susumonitor.server.module.alert.consume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证消费幂等记录 Mapper 的真实参数绑定、主键回填与唯一键幂等语义。
 */
class ConsumeRecordMapperMybatisTests {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        PooledDataSource dataSource = new PooledDataSource(
                "org.h2.Driver", "jdbc:h2:mem:consume_record_mapper;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS message_consume_records");
            statement.execute("""
                    CREATE TABLE message_consume_records (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        consumer VARCHAR(64) NOT NULL,
                        event_id VARCHAR(36) NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'consumed',
                        attempts INT NOT NULL DEFAULT 0,
                        last_error VARCHAR(500),
                        consumed_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_consume_event (consumer, event_id)
                    )
                    """);
        }

        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(ConsumeRecordMapper.class);
        try (var mapperXml = Resources.getResourceAsReader("mapper/alert/ConsumeRecordMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(mapperXml, configuration, "mapper/alert/ConsumeRecordMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    /** INSERT 应回填主键。 */
    @Test
    void insertShouldGenerateId() {
        ConsumeRecordEntity record = newRecord();

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            assertEquals(1, session.getMapper(ConsumeRecordMapper.class).insert(record));
        }

        assertNotNull(record.getId());
    }

    /** 同一 consumer+event_id 重复插入触发唯一键冲突（消费幂等语义）。 */
    @Test
    void duplicateConsumerEventShouldConflict() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ConsumeRecordMapper mapper = session.getMapper(ConsumeRecordMapper.class);
            mapper.insert(newRecord());

            // 纯 MyBatis 环境抛 PersistenceException；Spring 环境由 mybatis-spring 翻译为 DuplicateKeyException。
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.apache.ibatis.exceptions.PersistenceException.class, () -> mapper.insert(newRecord()));
        }
    }

    /** existsConsumed 在已消费后命中、未消费时不命中。 */
    @Test
    void existsConsumedShouldReflectInsertedRows() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ConsumeRecordMapper mapper = session.getMapper(ConsumeRecordMapper.class);
            assertFalse(mapper.existsConsumed("alert-evaluator", "event-1"));

            ConsumeRecordEntity record = newRecord();
            mapper.insert(record);

            assertTrue(mapper.existsConsumed("alert-evaluator", "event-1"));
            assertFalse(mapper.existsConsumed("alert-evaluator", "event-other"));
            assertFalse(mapper.existsConsumed("other-consumer", "event-1"));
        }
    }

    /** markFailed 回写失败状态与原因。 */
    @Test
    void markFailedShouldWriteStatusAndError() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ConsumeRecordMapper mapper = session.getMapper(ConsumeRecordMapper.class);
            ConsumeRecordEntity record = newRecord();
            mapper.insert(record);

            assertEquals(1, mapper.markFailed(record.getId(), 3, "evaluation failed"));

            assertTrue(mapper.existsConsumed("alert-evaluator", "event-1"));
        }
    }

    private ConsumeRecordEntity newRecord() {
        ConsumeRecordEntity record = new ConsumeRecordEntity();
        record.setConsumer("alert-evaluator");
        record.setEventId("event-1");
        record.setStatus(ConsumeStatus.CONSUMED.ruleValue());
        record.setAttempts(0);
        record.setConsumedAt(LocalDateTime.now());
        return record;
    }
}
