# M4-server-list-sort-ignored `GET /api/servers` 排序参数被忽略（历史缺陷）

**日期**: 2026-07-21
**发现方式**: opencode 在 M4 服务器列表开发期间自动探测
**优先级**: 高
**模块**: 服务器管理 API
**影响前端**: `ServerListView` 排序列头无视觉反馈

## 复现命令

```powershell
# 登录拿 token
$login = Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/auth/login' `
    -Method Post -Body '{"username":"admin","password":"REDACTED"}' `
    -ContentType 'application/json' -UseBasicParsing
$token = ($login.Content | ConvertFrom-Json).data.token
$hdr = @{ Authorization = ('Bearer ' + $token) }

# 七种参数组合,期望看到不同排序结果
$cases = @(
    @{ label = '无排序参数';                    params = @{ page = 1; page_size = 18 } },
    @{ label = 'sort_by=id asc';                 params = @{ page = 1; page_size = 18; sort_by = 'id'; sort_order = 'asc' } },
    @{ label = 'sort_by=id desc';                params = @{ page = 1; page_size = 18; sort_by = 'id'; sort_order = 'desc' } },
    @{ label = 'sort_by=created_at asc';        params = @{ page = 1; page_size = 18; sort_by = 'created_at'; sort_order = 'asc' } },
    @{ label = 'sort_by=created_at desc';       params = @{ page = 1; page_size = 18; sort_by = 'created_at'; sort_order = 'desc' } },
    @{ label = 'sort_by=name asc';               params = @{ page = 1; page_size = 18; sort_by = 'name'; sort_order = 'asc' } },
    @{ label = 'sort_by=host asc';               params = @{ page = 1; page_size = 18; sort_by = 'host'; sort_order = 'asc' } }
)

foreach ($c in $cases) {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/servers' `
        -Method Get -Headers $hdr -Body $c.params -UseBasicParsing
    $ids = ($r.Content | ConvertFrom-Json).data.items | ForEach-Object { $_.id }
    Write-Host "$($c.label): $($ids -join ',')"
}
```

## 期望行为

不同排序参数应返回不同顺序,例如:

| 请求 | 期望首项 id |
|---|---|
| `sort_by=id asc` | 10 |
| `sort_by=id desc` | 32 |
| `sort_by=created_at asc` | 10 |
| `sort_by=created_at desc` | 32 |
| `sort_by=name asc` | (按字母/拼音) |
| `sort_by=host asc` | (按 IP) |

## 实际行为

**所有 7 种参数组合返回完全相同的 id desc 顺序**:`32,31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,13,10`

后端**完全忽略了** `sort_by` / `sort_order` 参数,返回固定顺序(看上去是 id desc,但无法确认是不是 SQL ORDER BY 的结果)。

## 探测证据

探测脚本输出(已实测):

```
无排序参数:                   32,31,30,...,10
sort_by=id asc:              32,31,30,...,10  ← 完全相同
sort_by=id desc:             32,31,30,...,10  ← 完全相同
sort_by=created_at asc:      32,31,30,...,10  ← 完全相同
sort_by=created_at desc:     32,31,30,...,10  ← 完全相同
sort_by=name asc:            32,31,30,...,10  ← 完全相同
sort_by=host asc:            32,31,30,...,10  ← 完全相同
```

## 影响范围

### 前端

- `ServerListView` 排序列头点击后 UI 箭头变化但顺序不变
- 用户体验差:用户以为点击无效,实际是后端没实现
- 当前用 **客户端排序** 绕过(见下方)

### 后端

- `ServerController.list` 没有读 `sort_by` / `sort_order` 查询参数
- 可能 controller 中压根没声明这些参数,或 MyBatis-Plus `QueryWrapper` 没传 `orderBy`

## 建议修复方向

1. **参数白名单**:`sort_by` 接受 `id` / `name` / `host` / `created_at` / `updated_at`;`sort_order` 接受 `asc` / `desc`
2. **默认值**:`sort_by=id`, `sort_order=desc`
3. **非法值**:不在白名单的 `sort_by` / `sort_order` → 返回 `40002` (invalid request parameter)
4. **实现位置**:`ServerController.list()` 读取 `@RequestParam`(可选),传给 service 层,在 MyBatis-Plus `QueryWrapper.orderBy(...)` 拼接
5. **不要** 直接拼接用户输入到 SQL,务必白名单过滤防 SQL 注入

## 前端绕过方案

`src/views/ServerListView.vue` 中:

- 数据流改为 `serverItems`(后端原顺序) → `sortedRows`(前端 `computed` 排序) → `pagedRows`(前端切片)
- 后端 `page_size=999` 一次拉全
- `onSortChange` / `onPageSizeChange` 仅更新本地 ref,不调网络
- `sortBy/sortOrder` 仍发给后端(无害且对齐契约)

## 后端修好后前端撤除

修好后,**撤除以下改动恢复原始实现**:

1. 删除 `sortedRows` / `pagedRows` / `totalCount` 三个 computed
2. 删除 `serverItems` ref,恢复 `rows` ref
3. `fetchList` 改为写 `rows.value` 和 `total.value`
4. `onSortChange` / `onPageSizeChange` 重新调用 `reload()`
5. `buildQuery` 的 `page_size` 从 999 改回原值
6. 模板 `:total="totalCount"` 改回 `:total="total"`
7. 模板 `:data="pagedRows"` 改回 `:data="rows"`
8. 删除 `README.md` 中"排序类说明"段(已不适用)

## 验收标准

后端修完后:

- [ ] `curl "http://127.0.0.1:18080/api/servers?sort_by=id&sort_order=asc"` 返回 id asc
- [ ] `curl "?sort_by=name&sort_order=asc"` 按 name 升序
- [ ] `curl "?sort_by=invalid"` 返回 `{"code":40002,"message":"invalid request parameter"}`
- [ ] 排序与分页同时存在时分页切片位置正确(每页内部按排序)
- [ ] 前端删掉客户端排序代码后,所有列头排序立即生效
- [ ] 双字段搜索 + 排序 + 分页三者组合无错位

## 当前实现与 J2 验证状态

原始复现记录保留为历史证据。当前 Java 后端源码已经实现排序参数链路：

- Controller 接收并校验 `sort_by`、`sort_order`。
- Service 使用 `id/name/host/status/created_at/updated_at` 白名单并透传排序参数。
- `ServerMapper.xml` 使用固定分支生成 `ORDER BY`，没有直接拼接用户输入。
- 默认排序为 `id desc`，相同主排序值使用同方向 `id` 作为稳定次级排序。

J2 自动化验证结果：

| 验证层级 | 命令/范围 | 实际结果 |
|---|---|---|
| Controller | 合法字段、默认值、非法方向 | 通过，24 项 Controller 测试整体通过 |
| Service | 默认值、非法方向、六字段双方向透传 | 通过，40 项 Service 测试整体通过 |
| Java 编译 | `mvn -DskipTests compile` | 通过 |
| Java 常规回归 | `mvn test` | 188/188 通过 |
| 真实 MySQL Mapper | `ServerMapperMySqlValidationIT` | 通过，3/3 |
| 真实 HTTP/Apifox | 当前版本排序矩阵 | 真实 HTTP 已通过；Apifox CLI 未执行 |

真实 MySQL 集成测试入口已创建：

```powershell
mvn -Pmysql-validation "-Dit.test=ServerMapperMySqlValidationIT" verify
```

该测试只允许连接本机且数据库名包含 `validation` 的隔离库，并覆盖六个排序字段、升降序、排序后分页、关键词组合和软删除排除。J2 已在 `susumonitor_server_sort_validation_20260723` 上通过。

真实 MySQL 运行结果：

```text
Flyway V1-V9：通过
MetricsCleanupMySqlValidationIT：2/2 通过
ServerMapperMySqlValidationIT：3/3 通过
```

真实 HTTP 结果：

```text
18080 /api/health：HTTP 200
18080 /api/ready：HTTP 200
POST /api/auth/login：admin / 运行时提供新凭据返回 HTTP 200、业务码 0
```

本轮仅对现有 `18080` 执行只读登录和 GET 列表请求，未在开发库创建、修改或删除测试服务器。

实际排序矩阵：

| 请求 | HTTP | 业务码 | total | 返回 ID | `X-Request-ID` |
|---|---:|---:|---:|---|---|
| `id asc` | 200 | 0 | 2 | `1,3` | 存在 |
| `id desc` | 200 | 0 | 2 | `3,1` | 存在 |
| `name asc` | 200 | 0 | 2 | `1,3` | 存在 |
| `name desc` | 200 | 0 | 2 | `3,1` | 存在 |
| `host asc` | 200 | 0 | 2 | `1,3` | 存在 |
| 非法 `sort_by` | 400 | `40002` | - | - | 存在 |
| 非法 `sort_order` | 400 | `40002` | - | - | 存在 |

当前 HTTP 实例样本由开发库 active 服务器提供；完整字段顺序由真实 MySQL IT 覆盖。J5 已在 Apifox AI 分支 `ai/20260723-from-main-server-sort-validation` 创建并执行 9 个排序用例，9 个 HTTP 请求、30 个断言全部通过，失败数为 0。报告未上传云端。

J2 独立提交：

```text
已提交：`4d95c5e test(server): 验证服务器列表排序行为`

真实 MySQL、HTTP 和 Apifox CLI 验收记录已在 Java 后端执行日志中留痕。
```
