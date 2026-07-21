# scripts/

自动化与校验脚本。

## check-openapi.mjs

OpenAPI JSON 结构 lint 脚本。

### 用法

```bash
npm run openapi:check
```

扫描 `docs-SuMon/OpenApi-SuMon/*.json`,对每份契约做 4 项最低线校验:

1. 顶层必含 `openapi` / `info` / `paths`
2. `openapi` 版本必须是 `3.0.x`
3. `info.title` 与 `info.version` 必须非空字符串
4. `paths` 至少含一条 HTTP 端点(get/post/put/delete/patch/options/head)

### 设计原则

- 纯只读:**不会修改任何 JSON 文件**
- 离线:**不依赖网络**,纯本地文件 IO
- 零依赖:仅用 Node 内置 `node:fs` / `node:path`,不需要 `npm install`
- CI 友好:退出码 `0` 全通过,`1` 有失败

### 输出示例(成功)

```
OpenAPI 契约结构校验
================================

✓ openapi-auth.json     (8 endpoints)
✓ openapi-server.json   (15 endpoints)
✓ openapi-system.json   (3 endpoints)

================================
3 / 3 通过
```

### 输出示例(失败)

```
OpenAPI 契约结构校验
================================

✓ openapi-auth.json     (8 endpoints)
✗ openapi-system.json   失败:
    - info.title 缺失或为空
✓ openapi-server.json   (15 endpoints)

================================
2 / 3 通过
```

### 失败处理

修复契约 JSON → 重新执行 `npm run openapi:check`。
脚本不修改源文件,所有修复由人/后端工具完成。

### 故意不做的事

- **不校验 schemas / components**:业务契约由后端维护
- **不校验路径格式**:路径风格是 OpenAPI 设计问题,不在 lint 范围
- **不校验跨文件一致性**:三份 JSON 分属不同模块,跨文件校验通过 Apifox import 验证
- **不校验中文/特殊字符**:OpenAPI 3 允许 Unicode