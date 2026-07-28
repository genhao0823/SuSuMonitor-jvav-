# 2026-07-29 ServerListView 分页按钮无响应 Bug 修复

**日期**: 2026-07-29
**操作人**: ZCode / 用户
**关联**: 用户反馈"服务器页面点击下方的切换页码的按钮没有任何反应"

## 一、Bug 现象

`/servers` 页面点击 el-pagination 的页码按钮(1/2/3)或 prev/next,**表格数据不切换、URL 不变**,但 el-pagination 内部按钮高亮状态可能短暂更新。

## 二、根因定位(两步)

### 根因 1:`buildQuery()` 写死 page=1

`ServerListView.vue:252-258` 原代码:
```ts
function buildQuery(): ServerQuery {
  const q: ServerQuery = {
    page: 1,                          // ← 写死为 1,从不读 page.value
    page_size: pageSize.value,
    sort_by: sortBy.value,
    sort_order: sortOrder.value
  }
  ...
}
```

`fetchList()` 调 `listServers(buildQuery())`,page 永远 = 1。

### 根因 2:ServerPagination 的 update:page 没有触发 reload

`ServerListView.vue:193-200` 原代码:
```vue
<ServerPagination
  :page="page"
  :page-size="pageSize"
  :total="totalCount"
  :page-size-options="pageSizeOptions"
  @update:page="(v: number) => { page = v }"   ← 只更新 page.value,没调 reload!
  @update:page-size="onPageSizeChange"          ← 显式 reload
/>
```

`@update:page` 收到页码后**只更新 page.value ref**,**没有触发 reload**。而 `onPageSizeChange` 是显式调 reload 的。

所以即使修好根因 1(page 字段用 page.value),用户点击页码**也不会重新拉数据** —— 因为没人触发 reload。

## 三、修复(2 处)

### 修复 1:`buildQuery` 用 page.value

```diff
 function buildQuery(): ServerQuery {
   const q: ServerQuery = {
-    page: 1,
+    page: page.value,
     page_size: pageSize.value,
     sort_by: sortBy.value,
     sort_order: sortOrder.value
   }
```

### 修复 2:加 `onPageChange` 触发 reload

`ServerListView.vue:198`:
```diff
-    @update:page="(v: number) => { page = v }"
+    @update:page="onPageChange"
```

新增函数:
```ts
/**
 * 切换页码:仅刷新 page.value,触发 reload 拉新数据。
 * buildQuery() 已读 page.value,无需重置其它状态。
 */
function onPageChange(nextPage: number): void {
  if (nextPage === page.value) {
    return
  }
  page.value = nextPage
  void reload()
}
```

## 四、自动化验证(全绿)

| 命令 | 结果 |
|---|---|
| `npm run typecheck` | ✅ 0 错 |
| `npm run lint` | ✅ 0 错 0 警 |
| `npm run openapi:check` | ✅ 5/5 文件、29 endpoint |
| `npm run test` | ✅ 14 files / 104 tests |

## 五、IAB 真实浏览器联调证据

### 5.1 后端 API 验证 3 页数据

```bash
GET /api/servers?page=1&page_size=10&sort_by=id&sort_order=desc
  → [{id:48, audit-srv-bom}, {id:47, audit-xss}, ...]
GET /api/servers?page=2&page_size=10&sort_by=id&sort_order=desc
  → [{id:28, log-elk-prod-01}, {id:27, cache-cdn-edge-01}, ...]
GET /api/servers?page=3&page_size=10&sort_by=id&sort_order=desc
  → [{id:18, web-prod-01}, {id:17, probe-m4-srv}, ...]
```

### 5.2 浏览器联调

登录 admin/1059412135 → 进入 /servers:

| 操作 | 期望 | 实际 |
|---|---|---|
| 点页码 2 | URL → `?page=2` + 表格第一条变 28(log-elk) | ✅ |
| 点页码 3 | URL → `?page=3` + 表格第一条变 18(web-prod-01) | ✅ |
| 直接访问 `/servers?page=3` | F5 恢复 page=3 数据 | ✅ (snapshot 含 `web-prod-01`,不含 `audit-srv-bom`) |
| 搜索 `audit` | URL → `?name=audit`(page 自动重置) | ✅ |

截图证据:page 2 表格显示 id 序列 27, 26, 25, 24, 23, 22(对比 page 1 是 48, 47, 45, ...)。

## 六、教训

1. **写死常量是隐蔽 Bug**: `page: 1` 看起来像"默认值",但调用方始终传入,实际是"忽略外部参数"。这种 Bug 在 mock 数据或简单场景下永远不会被发现,只有在用户点击真实页码时暴露。
2. **emit handler 不等于 reload**:`@update:page` 只更新 ref,**必须显式 reload**。`@update:page-size` 有 `onPageSizeChange` 显式 reload 是正确范式,但 page 路径漏了同样的处理。
3. **IAB 截图作为验证手段的局限**:el-table 在 IAB 视口内会渲染完整列表(不依赖 pageSize),所以光看截图无法区分"page 1"和"page 2"渲染出的中段行号。**必须配合 URL ?page=N + snapshot 字符串包含特定 ID 名字** 才能严谨判断。
4. **vite HMR 不会自动重载新逻辑**:第二个修复(新增函数)HMR 能识别,但 el-pagination 内部状态可能保持旧值,**刷新页面后才生效**(本次已 `tab.goto()` 强制刷新)。

## 七、Commit 拆分

1. `fix(web): ServerListView buildQuery 用 page.value 而非硬编码 1`
   - 仅 src/views/ServerListView.vue 第 254 行
2. `fix(web): ServerListView 页码切换触发 reload + onPageChange handler`
   - 仅 src/views/ServerListView.vue(新增 onPageChange + @update:page 改用 handler)
3. `docs(web): ServerListView 分页 Bug 修复 dev-log(IAB 真实联调证据)`
   - 仅本文件

## 八、关联

- 上游:`20260729-前端补齐4个未签契约封装.md`(B-038)
- 上游:`20260729-前端B-038真实浏览器联调收口.md`(IAB 验证机制)
- 后续:无(分页 Bug 已闭环)