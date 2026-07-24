# 开发日志: Polish 4 — Vitest 单元测试

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: Polish 4(5 项 polish 中的第 4 项,~70 分钟预算)
**关联需求**: `npm run test` 跑通 stores/utils/composables 单元测试

## 状态

| 项 | 结果 |
|---|---|
| vitest 装包 | ✅ vitest@1.6.1 + @vue/test-utils@2.4 + jsdom@24 |
| vitest.config.ts | ✅ 36 行,jsdom + @ 别名 + 覆盖率配置 |
| 5 spec 文件 | ✅ 28 测试全过 |
| typecheck | ✅ 0 错 |
| lint | ✅ 0 错 0 警 |
| openapi:check | ✅ 3/3 |

**`npm run test` 全过,5 Test Files / 28 Tests passed**。

## 本次落地的工程文件

### 新增 devDeps(package.json)

```json
"vitest": "^1.6.0",
"@vue/test-utils": "^2.4.0",
"jsdom": "^24.0.0"
```

### 新增 scripts(package.json)

```json
"test": "vitest run",
"test:watch": "vitest"
```

### 新增 config

- `web-vue-SuMon/vitest.config.ts`(36 行)
  - jsdom 环境
  - globals: true(describe / it / expect 无需 import)
  - include: `src/**/*.spec.ts`
  - coverage: v8 provider, text + html reporter
  - 路径别名 `@` → `./src`

### 新增 5 spec 文件

| 文件 | 行数 | 测试数 | 覆盖目标 |
|---|---|---|---|
| `src/stores/auth.spec.ts` | 151 | 9 | `useAuthStore` login/logout/refresh/clearLocal/isAdmin/isApproved |
| `src/stores/metrics.spec.ts` | 64 | 4 | `useMetricsStore` applyRealtime/reset/setConnected |
| `src/utils/format.spec.ts` | 53 | 8 | formatDateTime / serverStatusLabel / userRoleLabel / reviewStatusLabel |
| `src/utils/animate.spec.ts` | 43 | 4 | animateCounter 边界 + cancel |
| `src/composables/useRouterLoading.spec.ts` | 51 | 3 | installRouterLoading 注册 3 个 router hook |
| **总** | **362** | **28** | — |

## 关键设计要点

### vitest.config.ts
- 用 `vitest/config` 而非 `vite/config`(环境隔离)
- 复用 `vite.config.ts` 的 `@` 别名
- coverage include 限定为 stores/utils/composables(不测 components 渲染)

### auth.spec.ts 关键 mock 模式

```typescript
// Mock 整个 @/api/auth module(避免真调后端)
vi.mock('@/api/auth', () => ({
  loginUser: vi.fn(),
  registerUser: vi.fn(),
  getCurrentUser: vi.fn(),
  logoutUser: vi.fn()
}))

// 再 import(必须 hoist 后)
import * as mockApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

// 用 vi.mocked() 获取类型
vi.mocked(mockApi.loginUser).mockResolvedValueOnce({...})
```

**关键点**:`vi.mock` factory 不能 hoist 外部变量,必须内联。后续用 `import * as mockApi` 拿 mock。

### localStorage mock(Node 22+ 警告)

```typescript
const storage = new Map<string, string>()
Object.defineProperty(global, 'localStorage', {
  value: { /* Map-based impl */ },
  writable: true,
  configurable: true
})
```

**根因**:Node 22+ `localStorage` 默认不可用,jsdom 会出 ExperimentalWarning。**用 Map mock 避免 warning**。

### animate.spec.ts 用 vi.useFakeTimers

```typescript
beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())
```

RAF 在 jsdom 不可用,直接 mock 整个 timer 系统。

### useRouterLoading.spec.ts mock NProgress

```typescript
vi.mock('nprogress', () => ({
  default: { configure: vi.fn(), start: vi.fn(), done: vi.fn() }
}))
```

避免真调 NProgress 启动动画。

## 测试结果详情

```
✓ src/utils/format.spec.ts       (8 tests)  5ms
✓ src/utils/animate.spec.ts       (4 tests)  9ms
✓ src/composables/useRouterLoading.spec.ts  (3 tests)  4ms
✓ src/stores/auth.spec.ts         (9 tests)  1788ms
✓ src/stores/metrics.spec.ts      (4 tests)  13ms

Test Files  5 passed (5)
     Tests  28 passed (28)
  Duration  ~3s
```

**auth.spec.ts 跑得慢(1788ms)** 是因为 `vi.mocked(mockApi.xxx).mockReset()` 在 beforeEach 重置 mock,加上测试内有 await 网络调用(被 mock 拦截)。**可接受**。

## 验证清单

| 检查 | 命令 | 结果 |
|---|---|---|
| typecheck | `npm run typecheck` | ✅ 0 错 |
| lint | `npm run lint` | ✅ 0 错 0 警 |
| openapi:check | `npm run openapi:check` | ✅ 3/3 |
| audit:catchup | `npm run audit:catchup` | ✅ 0/0/1(DashboardView) |
| **测试** | **`npm run test`** | **✅ 28/28** |

## 备份(红线 #5)

```
C:\Backup\package.json.2026-07-22.pre-Polish4.bak   (1558 bytes)
```

## Commit

- `a3445b5 test(web): 装 vitest@1 + @vue/test-utils + jsdom + 写 vitest.config.ts`
- `8454863 test(web): 5 个单元测试 spec(stores/auth + metrics + utils + composables)`
- `xxx docs(web): Polish-4 Vitest dev-log + README 同步`(紧随其后)

## 风险与对策(回顾)

| 风险 | 处理 |
|---|---|
| vitest 与 Vite 版本不兼容 | vitest@^1 + vite@^5(实测兼容)|
| jsdom 缺 Element Plus | 不测组件(只测 stores/utils/composables)|
| Pinia 测试 | setActivePinia + createPinia(每个 beforeEach)|
| ES module + vitest | vitest 1.x 原生支持 |
| localStorage Node 22+ 警告 | Map-based mock 替代 |
| mock hoist | vi.mock factory 内联,后续用 `import * as mockApi` |
| typecheck 严格类型 | mock 数据加 `as any` 标注 |
| run tests 时 husky | 不依赖后端,不触发 openapi:check hook |

## 后续

- **不挂 pre-commit hook**(测试覆盖率刚开始,跑失败概率高,等稳定后挂)
- **不强制每次提交都跑测试**(本批 28 测试 3s 跑完,可以挂)
- **新功能添加时同步补 spec**(stores/utils 改动要带 spec)

## Polish 4 价值

✅ **stores/auth 9 个用例覆盖核心鉴权流** → 改 auth.ts 后能 1.8s 内发现回归
✅ **utils/format 8 个用例覆盖字符串处理** → 改 format.ts 后能 5ms 内发现回归
✅ **3 道防线 + 单元测试 = 完整质量门**:
- 静态(11 规则)
- HTTP(13 路径)
- UI(17 路径)
- **单元(28 用例)**
