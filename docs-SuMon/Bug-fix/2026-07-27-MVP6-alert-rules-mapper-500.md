# MVP6-alert-rules-mapper-500 `POST/PUT/DELETE /api/alerts/rules` 返回 500 空响应体（代码已修复，待真实环境复验）

**日期**: 2026-07-27
**发现方式**: opencode 在 MVP-6 告警前端联调期间触发真实 POST 暴露
**优先级**: 高（阻塞前端告警规则页创建/编辑/删除三大主路径）
**模块**: 告警规则 Mapper
**影响前端**: `AlertRulesView.vue` 创建/编辑/删除按钮、`AlertRuleDialog.vue` 提交

## 修复记录

- 2026-07-27：`AlertRuleMapper.insertRule` 已增加 `@Param("rule")`，使接口参数名与 XML 的 `#{rule.xxx}` 及 `keyProperty="rule.id"` 保持一致。
- `keyProperty="rule.id"` 保持不变。增加 `@Param("rule")` 后，MyBatis 参数对象为带 `rule` 键的参数映射，生成主键应回写到 `rule.id`；改为 `keyProperty="id"` 会破坏该回填路径。
- 新增 `AlertRuleMapperMybatisTests`，以 H2 真实执行生产 XML，已验证 INSERT 主键回填、UPDATE 和软删除路径。
- 已执行 `mvn -q test` 与 `mvn -q -DskipTests package`，均通过。真实 MySQL 和 HTTP 写链路仍需在具备管理员凭据的联调环境复验。

## 复现命令

```powershell
# 登录拿 token（密码为后端启动环境变量 ADMIN_PASSWORD 的值，由后端首次启动 bootstrap）
$login = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:18080/api/auth/login' `
    -ContentType 'application/json' `
    -Body '{"username":"admin","password":"<ADMIN_PASSWORD>"}' `
    -TimeoutSec 10
$tok = $login.data.token
$h = @{ Authorization = "Bearer $tok" }

# POST 创建
try {
    Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:18080/api/alerts/rules' `
        -Headers $h -ContentType 'application/json' `
        -Body '{"serverId":null,"metric":"cpu","operator":">","thresholdValue":80,"level":"warning"}' `
        -TimeoutSec 10
} catch {
    Write-Host ("STATUS=" + [int]$_.Exception.Response.StatusCode)
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host ("BODY='" + $sr.ReadToEnd() + "'")
}

# PUT 更新（先 GET 拿一条 id）
$rules = Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:18080/api/alerts/rules' -Headers $h -TimeoutSec 10
if ($rules.data.Count -gt 0) {
    $id = $rules.data[0].id
    try {
        Invoke-RestMethod -Method Put -Uri "http://127.0.0.1:18080/api/alerts/rules/$id" `
            -Headers $h -ContentType 'application/json' `
            -Body '{"thresholdValue":90,"level":"critical","enabled":true}' `
            -TimeoutSec 10
    } catch {
        Write-Host ("STATUS=" + [int]$_.Exception.Response.StatusCode)
        $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host ("BODY='" + $sr.ReadToEnd() + "'")
    }
}

# DELETE 软删
try {
    Invoke-RestMethod -Method Delete -Uri "http://127.0.0.1:18080/api/alerts/rules/$id" `
        -Headers $h -TimeoutSec 10
} catch {
    Write-Host ("STATUS=" + [int]$_.Exception.Response.StatusCode)
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host ("BODY='" + $sr.ReadToEnd() + "'")
}
```

## 期望行为

| 操作 | 期望响应 |
|---|---|
| `POST /api/alerts/rules` | HTTP 201/200 + `data` 为新建的 `AlertRuleVo`（含 `id`） |
| `PUT /api/alerts/rules/{id}` | HTTP 200 + `data` 为更新后的 `AlertRuleVo` |
| `DELETE /api/alerts/rules/{id}` | HTTP 200 + `data: null` + 业务码 0 |
| 规则不存在 | HTTP 404 + 业务码 `40400` `resource not found` |
| metric/operator/level 非法 | HTTP 400 + 业务码 `40001` `invalid request parameter` |
| 凭据缺失/非 admin | HTTP 401/403 + 对应业务码 |

## 实际行为

POST/PUT/DELETE 三种写操作都返回：

```text
HTTP/1.1 500
Content-Length: 0
```

**响应体完全为空**（不是 `{"code":50000,"message":"internal server error","data":null}`，而是 0 字节）。

GET `/api/alerts/rules` 正常工作（HTTP 200，返回空数组 `[]`），证明读路径未受影响。

## 探测证据

### 读路径正常

```powershell
PS> Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:18080/api/alerts/rules' -Headers $h
code message data
---- ------- -----
   0 success {}
```

### 写路径全 500 空 body

```powershell
# POST
PS> Invoke-RestMethod ... -Body '{...}'
... 远程服务器返回错误: (500) Internal Server Error。
# Catch 分支 ReadToEnd 输出: '' (空字符串)

# PUT 同上
# DELETE 同上
```

### 控制台静默

IDEA 控制台在请求处理期间 **未打印任何 `ERROR` 行**，意味着：

1. `GlobalExceptionHandler.handleException(Exception)` 的 `LOGGER.error("Unhandled exception", exception)` 没有触发，或者
2. 异常发生在响应提交之后被吞掉，或者
3. 日志级别过滤掉了 ERROR 行

具体需在真实环境抓 `Unhandled exception` 堆栈确认。

## 静态分析已锁定的两个 mapper bug

### Bug 1：`AlertRuleMapper.xml` 的 `keyProperty` 路径分析结论

**文件**：`server-java-SuMon/src/main/resources/mapper/alert/AlertRuleMapper.xml:20`

```xml
<insert id="insertRule" useGeneratedKeys="true" keyProperty="rule.id">
    INSERT INTO alert_rules (server_id, metric, operator, threshold_value, level, enabled, created_by)
    VALUES (#{rule.serverId}, #{rule.metric}, #{rule.operator}, #{rule.thresholdValue},
        #{rule.level}, #{rule.enabled}, #{rule.createdBy})
</insert>
```

`keyProperty="rule.id"` 表示"参数对象的 `rule` 字段下的 `id` 属性"，但 XML 的参数对象是 `AlertRuleEntity`（接口签名 `int insertRule(AlertRuleEntity rule)`），它没有 `rule` 子字段。

**结论**：在接口增加 `@Param("rule")` 后，正确写法是保留 `keyProperty="rule.id"`，以匹配 MyBatis 的参数映射键。

**未加 `@Param("rule")` 时的风险**：自增主键回填与 SQL 参数解析均依赖编译产物保留实际参数名；该不稳定依赖已移除。

```java
ruleMapper.insertRule(entity);
return toVo(ruleMapper.selectActiveRuleById(entity.getId()));
```

生成主键会回填至 `entity.id`，后续查询可使用真实主键。该路径已由真实 Mapper 测试覆盖。

### Bug 2：`AlertRuleMapper.insertRule` 接口缺 `@Param("rule")`（更可能直接触发 500）

**文件**：`server-java-SuMon/src/main/java/com/susumonitor/server/module/alert/mapper/AlertRuleMapper.java:18`

```java
int insertRule(AlertRuleEntity rule);  // 缺 @Param("rule")
```

XML 里所有参数都用 `#{rule.serverId}` / `#{rule.metric}` 等 —— MyBatis 必须按参数名 `rule` 解析。在**没有 `@Param("rule")` 注解**的情况下：

- 编译保留参数名（`-parameters` 编译器选项开启）：能解析为 `rule`，INSERT 实际可执行（但因 Bug 1 写不进 id）
- 编译不保留参数名（默认）：参数名变成 `arg0`/`param1`，**`#{rule.xxx}` 全部解析失败**，抛 `org.apache.ibatis.binding.BindingException: Parameter 'rule' not found`

Spring Boot 3.x 默认带 `-parameters` 编译选项，所以**这个项目里 Bug 2 可能不会触发 BindingException**；但只要项目任何一处编译参数变化，Bug 2 就会立刻爆。

UPDATE/DELETE 也用了参数名（`#{id}` / `#{thresholdValue}` / `#{level}` / `#{enabled}` / `#{deletedAt}`），这些参数都**正确加了 `@Param`**，所以 UPDATE/DELETE 500 **不是**这个原因——需要继续排查。

## 单元测试为何未发现

`AlertRuleServiceTests` / `AlertRuleControllerTests` 全 13 个用例通过（mockito+MockMvc），原因是：

| 测试层 | mock 对象 | SQL 真实执行？ |
|---|---|---|
| Controller Test | `@MockBean AlertRuleService` | ❌ |
| Service Test | `@Mock AlertRuleMapper` | ❌ |
| Mapper XML | 无集成测试 | ❌ |

`MyBatis` 参数解析只在 SQL 真正执行时才校验。**整条写路径在仓库内从未被真实 MySQL 走过**。

## 影响范围

### 前端

- `AlertRulesView.vue`：
  - "新建规则"按钮 → `AlertRuleDialog` 提交 → 500 → toast 显示 `internal server error`
  - 行内"编辑"按钮 → 同上
  - 行内"删除"按钮 → 二次确认后调用 `DELETE` → 500
  - 行内 `enabled` `el-switch` → `PUT` toggle → 500
- 前端 `api/alert.ts` 的 `createAlertRule` / `updateAlertRule` / `deleteRule` 三个函数均受影响
- `stores/alerts.ts` 的 `createRule` / `updateRule` / `deleteRule` 三个 action 均受影响

### 后端

- `AlertRuleServiceImpl.createRule` / `updateRule` / `deleteRule` 三个写方法
- `AlertRuleMapper` 全部 4 个写方法（`insertRule` / `updateRule` / `softDeleteRule` + 间接的 `selectActiveRuleById`）
- 涉及 controller：`AlertRuleController.createRule` / `updateRule` / `deleteRule`

## 建议修复方向（不写代码，只写思路）

1. **已完成：**`AlertRuleMapper.insertRule` 增加 `@Param("rule")`，消除编译参数名依赖。
2. **已完成：**新增真实 MyBatis/H2 回归测试，覆盖 INSERT 主键回填、UPDATE 与软删除。
3. **待联调：**在真实 MySQL 环境触发 POST、PUT、DELETE 并保留服务端异常堆栈，确认 500 空响应体不再出现。
4. **无需修改 OpenAPI：**端点及既有成功/错误响应契约未发生变化。

## 前端绕过方案

`AlertRulesView.vue` / `AlertRuleDialog.vue`：

- **不调用真实写 API**，直接给 `ElMessage.warning('告警规则后端写路径当前不可用，待后端修复')`
- 列表展示沿用 GET 读路径（GET 正常）
- 待后端修好后撤除绕过，恢复真实 `createRule` / `updateRule` / `deleteRule` 调用

## 后端修好后前端改动

1. `stores/alerts.ts` 的 `createRule` / `updateRule` / `deleteRule` 三个 action 的错误处理恢复正常（无需特判 500）
2. `AlertRuleDialog.vue` 移除临时绕过提示，恢复真实 `store.createRule` / `store.updateRule` 调用
3. `AlertRulesView.vue` 删除按钮恢复真实 `store.deleteRule` 调用
4. `api:e2e` / `ui:e2e` 补充 3 条写路径断言

## 验收标准

后端修完后：

- [ ] 真实 POST 返回 HTTP 200 + `data.id` 非空
- [ ] 真实 PUT 返回 HTTP 200 + `data.id` 与请求路径 id 一致
- [ ] 真实 DELETE 返回 HTTP 200 + `data: null` + DB 行 `deleted=1` `deleted_at` 非空
- [ ] 集成测试 `AlertRuleMapperIT.insertRule_generatesId` / `updateRule_modifiesRow` / `softDeleteRule_marksDeleted` 全绿
- [ ] IDEA 控制台无 `Unhandled exception` 日志
- [ ] 前端 `AlertRulesView` 创建/编辑/删除/启停四个交互真实可用
- [ ] 5 件套（typecheck / lint / test / openapi:check / audit:catchup）全绿
