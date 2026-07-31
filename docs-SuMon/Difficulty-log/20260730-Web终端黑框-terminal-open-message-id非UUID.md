# 2026-07-30 Web 终端黑框无响应(terminal.open message_id 非 UUID + cols 极小)

**日期**: 2026-07-30
**操作人**: opencode / 用户
**关联**: 手机热点下 /ws/monitor 连上,但 Web 终端页面黑框、输入 ls 无反应,右上角一直"建链中"

## 一、Bug 现象

手机热点网络下(已绕过宽带运营商劫持),打开 `/terminal/4`:
- /ws/monitor 连上了(不再"正在连接")
- 终端区域是**黑框,无光标**
- 输入 `ls` 不显示输入、不显示输出
- 右上角状态标签一直显示**建链中**(`phase=connecting`)

## 二、尝试的方法(多次走弯路)

1. **怀疑 `term.value === null`(xterm 没初始化)**:`TerminalView.onSocketReady` 里 `if (term.value === null) return`,若 xterm 没挂载则不创建 terminalWs、不发 terminal.open。但终端页 F12 console **无任何报错**,且能看到黑框(xterm 容器 div),初步排除——但无法 100% 确认 term.value 是否非 null。
2. **tcpdump 抓 terminal.open**(反复多次,12s/20s/30s/40s/60s):**全部抓不到** terminal/metrics.subscribe 帧。一度怀疑前端根本没发 terminal.open,或 onSocketReady 没触发。
3. **给 agent 加 terminal.open 日志**:改 `terminal_agent.go` 在 handle terminal.open 加 Info 日志,交叉编译重新部署,刷新后 agent.log **无任何 terminal 事件**——证明 terminal.open 没到 agent。但仍无法确认是前端没发还是后端没中继。
4. **后端日志无 terminal relay**:`journalctl` 查后端 17:39:39(terminal.open 时间)无任何 terminal/relay/error 日志(relay 成功不记日志,失败 sendError 不记),无法判定。
5. **换用浏览器 DevTools Network → Messages 直接看 WS 帧**(关键突破):发现:
   - 发出(↑):`terminal.open` message_id = `1785407006966-e8ea7aa4ae2bc`(非 UUID),payload `{server_id:4, cols:2, rows:35}`
   - 收到(↓):`error` `{code:40003, message:"terminal invalid payload"}`
   - 收到(↓):多条 `metrics.update`(说明 onopen 触发了,metrics.subscribe 通了)

## 三、根因(两个)

### 根因 1:message_id 非 UUID(主因)

浏览器 WebSocket 用 `permessage-deflate` 压缩 WS 帧,导致 **tcpdump 抓包 grep 文本匹配不到**(压缩后是二进制)——这是 tcpdump 反复抓不到 terminal 帧的原因,严重误导了排查方向。

`terminal-ws.ts` 的 `wrapFrame` 生成 message_id:
```ts
// 修复前
message_id:
  typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`,
```

站点明文 HTTP(非安全上下文),`crypto.randomUUID` 不可用,降级成 `${Date.now()}-${random}` —— **不是 UUID 格式**。

后端 `TerminalProtocolValidator.validateCommonMessage` 校验:
```java
if (message == null || !allowedTypes.contains(message.type()) || !isUuid(message.messageId()) || ...) {
    throw invalidPayload();  // 40003
}
```
`isUuid("1785407006966-e8ea7aa4ae2bc")` → false(不是 8-4-4-4-12 hex)→ 拒绝 40003。

**和"前端登录点击无反应"是同一根因**(HTTP 非安全上下文下 crypto.randomUUID 不可用),只是之前修了 `api/client.ts` 的 HTTP 请求拦截器,漏了 `terminal-ws.ts` 的 WS 帧 message_id。

### 根因 2:cols=2(次要,影响体验)

`TerminalView.mountXterm` 在 `t.open(termHost)` 后立即 `f.fit()`:
```ts
t.open(termHost.value)
f.fit()  // ← 容器 CSS 布局可能未完成
```
onMounted `await nextTick` + mountXterm,nextTick 后 DOM 更新但 CSS layout 可能未完成,容器宽度≈0,fit 算出 `cols=2`(极小)。

虽然 validator 的 `MIN_COLUMNS=1` 允许 cols=2(不被 40003 拒绝),但终端 2 列无法正常使用。

## 四、修复

### 修复 1:`terminal-ws.ts` message_id 生成 UUID v4

```ts
function newTerminalMessageId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return uuidv4Fallback()  // getRandomValues 生成标准 UUID v4(RFC 4122)
}

function uuidv4Fallback(): string {
  // crypto.getRandomValues 在非安全上下文也可用,手动设置 version/variant 位
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const b = new Uint8Array(16)
    crypto.getRandomValues(b)
    b[6] = (b[6] & 0x0f) | 0x40
    b[8] = (b[8] & 0x3f) | 0x80
    // ...拼成 8-4-4-4-12 hex
  }
  // 极端降级 Math.random 生成 UUID v4 格式
}
```
`wrapFrame` 的 message_id 改用 `newTerminalMessageId()`。

### 修复 2:`TerminalView.vue` onSocketReady 前重新 fit

```ts
(socket) => {
  if (term.value === null) return
  // 重新 fit:mountXterm 初始 fit 时容器 CSS 布局未完成,cols 会极小;
  // socket 就绪时容器已布局,重算确保 terminal.open 携带合理 cols。
  try { fitAddon.value?.fit() } catch {}
  terminalWs = new TerminalWebSocket({ socket, ..., cols: term.value.cols, rows: term.value.rows, ... })
  terminalWs.open()
}
```

## 五、验证

重新构建前端 + 部署,硬刷新 `/terminal/4`:
- 右上角从"建链中"变"已连接"
- agent.log 出现 `terminal.open received` + `terminal.opened sending`
- 输入 `ls` 显示输入 + 目录回显

## 六、教训

1. **tcpdump 抓 WebSocket 不可靠**:浏览器 WS 默认 `permessage-deflate` 压缩,tcpdump -A 抓到的 payload 是压缩二进制,grep 文本匹配不到。本次 tcpdump 反复失败(12/20/30/40/60s 全空)严重误导排查。**遇到 WS 帧内容排查,优先用浏览器 DevTools Network → Messages 直接看,比 tcpdump 可靠**。
2. **同一根因会多处复发**:HTTP 非安全上下文下 `crypto.randomUUID` 不可用,第一次在 `api/client.ts`(登录)踩,第二次在 `terminal-ws.ts`(终端)踩。修第一处时没全局 grep 所有 `crypto.randomUUID` 裸调用,导致复发。**修一个根因后要全局排查同类调用点**。
3. **"agent.log 无 terminal 事件"不能区分前端没发 vs 后端没中继**:agent terminal handle 默认不记日志(即使 debug),无事件可能是没收到也可能是没记。加临时日志(本次给 terminal_agent.go 加 Info/Warn)才能区分。
4. **xterm fit 时机依赖容器布局**:`t.open` 后立即 fit 时容器宽度可能 0,要在容器布局完成后 fit(onSocketReady 时,或用 requestAnimationFrame/ResizeObserver)。
5. **后端协议校验要求 UUID 时,前端 fallback 必须生成真 UUID**(符合 RFC 4122),不能用 `Date.now()-random` 这种非 UUID 格式。
