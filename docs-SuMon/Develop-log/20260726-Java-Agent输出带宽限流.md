# Java Agent 输出带宽限流

日期：2026-07-26
操作人：OpenCode

## 改动

- 新增 Java 单 JVM `TerminalOutputRateLimiter`，按服务端 `session_id` 对 Agent `terminal.output` 的 Base64 解码原始字节执行 Token Bucket 限流。
- 默认速率为 262144 bytes/s（256 KiB/s），突发容量为 524288 bytes（512 KiB），新桶令牌满额；会话关闭、Monitor 断开和当前 Agent 断开均释放桶。
- 超额输出不转发、不记录、不持久化输出内容；Java 发送服务端生成的 `terminal.close`，以 `closed/output_rate_exceeded` 收口会话、移除路由绑定并释放桶。
- 配置环境变量为 `TERMINAL_OUTPUT_RATE_BYTES_PER_SECOND` 与 `TERMINAL_OUTPUT_BURST_BYTES`；两者必须为正数，突发容量必须不小于协议单帧最大值 16384 bytes。

## 备份

- 使用任务开始前已存在的备份：`C:\Backup\SuSuMonitor\20260726-java-terminal-output-rate-limit.zip`。
- 本次未删除文件，未修改该备份。

## 验证边界

- 定向 Maven 单元测试覆盖令牌耗尽、固定时钟补充、会话隔离、桶释放、超额关闭链路、正常关闭释放和配置校验。
- 单元测试不等同于真实 Agent PTY、WebSocket 网络或多 JVM 部署验证；限流状态仅在当前 Java JVM 内存中有效。
