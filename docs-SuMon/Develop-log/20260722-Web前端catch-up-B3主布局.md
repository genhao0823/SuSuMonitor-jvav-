# 开发日志: 前端 catch-up(B3) — M3 主布局 + Dashboard

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: 历史 catch-up
**关联需求**: M3 主布局 + Dashboard 美化

## 状态

MainLayout + PageHeader + DashboardView 三文件全部已落库。
本次 catch-up 不引入新功能,仅回填未提交的文件。

## 本次落地的工程文件

### 新增文件(3 个 / 1350 行)

| 文件 | 行数 | 说明 |
|---|---|---|
| `web-vue-SuMon/src/layouts/MainLayout.vue` | 242 | 受保护路由的根布局:顶栏 + 侧栏 + 头像下拉 + 玻璃卡视觉 |
| `web-vue-SuMon/src/components/PageHeader.vue` | 73 | 页面头部通用组件,`title`/`subtitle`/`actions` slot |
| `web-vue-SuMon/src/views/DashboardView.vue` | 1035 | Dashboard 美化:健康/就绪卡 + 服务器总数/在线数/管理员审核队列卡 + 头像 + 涂山苏苏签名引言 |

## 关键设计要点

- **MainLayout 顶栏**:左侧 logo + 系统名,右侧 el-dropdown 头像 + 用户名 + 退出登录。
- **侧栏**:仅渲染受保护路由(`requiresAuth` 或 `requiresAdmin`),避免泄露 `/login`、`/register` 等。
- **Dashboard 涂山苏苏视觉**:
  - 玻璃卡背景 `rgba(255, 255, 255, 0.5) + backdrop-filter: blur(20px)`
  - 渐变数字滚动(easeOutCubic)
  - 涂山苏苏 avatar + 签名引言(从引言池随机抽一句)
- **数字滚动**:`requestAnimationFrame` 而非 setInterval,时间更准、与帧率同步。

## 后续

- B4: 服务器 CRUD(ServerListView, ServerDetailView, ServerFormDialog)
