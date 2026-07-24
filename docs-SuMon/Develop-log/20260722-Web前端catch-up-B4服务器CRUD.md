# 开发日志: 前端 catch-up(B4) — M4 服务器 CRUD

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: 历史 catch-up
**关联需求**: M4 服务器列表 / 详情 / 创建 / 编辑 / 软删除

## 状态

M4 服务器 CRUD 全部页面组件 + API 已落库(未含 M6 metrics 入口)。
**注意**:`ServerDetailView.vue` 此时存在一个路由参数 bug(`route.params.id` 应为 `route.params.serverId`),
将在 A1 commit 中单独修复。

## 本次落地的工程文件

### 新增文件(4 个 / 1582 行)

| 文件 | 行数 | 说明 |
|---|---|---|
| `web-vue-SuMon/src/api/server.ts` | 80 | `queryServers`/`getServer`/`createServer`/`updateServer`/`deleteServer`/`getServerStatus`/`testSsh` |
| `web-vue-SuMon/src/views/ServerListView.vue` | 587 | 列表 + 搜索 + 排序(spark line mock) + 创建按钮 + 行操作(详情 / 编辑 / 测试连接) |
| `web-vue-SuMon/src/views/ServerDetailView.vue` | 473 | 详情:基本信息卡片 + 状态快照卡片 + 编辑 / 删除 / 测试连接按钮 |
| `web-vue-SuMon/src/components/ServerFormDialog.vue` | 442 | 创建 / 编辑弹窗:基础字段 + 凭据(密码/私钥) + 描述 |

## 关键设计要点

- **客户端排序**(后端 `sort_by`/`sort_order` 被忽略,见 `docs-SuMon/Bug-fix/2026-07-21-M4-server-list-sort-ignored.md`):
  ServerListView 用 computed 排序,而不是依赖后端参数。
- **状态快照**:`getServerStatus(id).catch(() => null)` 忽略 5xx,允许详情页在状态服务不可达时仍展示基础信息。
- **删除二次确认**:`el-popconfirm`,`确定要删除 X 吗?` 文案带服务器名,避免误删。
- **凭据二选一**:`ssh_auth_type` 切换时显示对应输入框,空字符串或省略语义为"不修改"。

## 已知问题

- **BUG(ServerDetailView 路由参数不匹配)**:当前 `parseId()` 读 `route.params.id`,
  但 `router/index.ts` 路由定义的是 `:serverId`。这导致从 `ServerListView.vue:134`
  `<router-link :to="{ params: { id: row.id } }">` 跳转时也可以工作(Vue Router 4
  会接受 path 未定义的 params 键并忽略),但 `ServerListView.vue:426` 脚本里的
  `router.push({ params: { serverId: row.id } })` 跳转时,ServerDetailView 永远
  拿不到 id,直接 goBack。

  **修复时机**:在 A1 commit 中单独修复,本次 commit 不修改,保持每 commit 单一目的。

## 后续

- B5: 管理补充 + utils/format
- A1: 修 ServerDetailView 路由参数 bug
- A2+: M6 接通
