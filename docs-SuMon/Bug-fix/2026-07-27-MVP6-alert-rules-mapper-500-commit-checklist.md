# MVP6-alert-rules-mapper-500 提交与验收 checklist

**日期**: 2026-07-27
**适用对象**: 用户在本地环境手动执行（OpenCode 不触碰后端 Java 代码、不跑 Maven）

## 修复内容回顾

`AlertRuleMapper.insertRule` 接口已加 `@Param("rule")`，与 XML 的 `#{rule.xxx}` 及 `keyProperty="rule.id"` 对齐。新增 `AlertRuleMapperMybatisTests` 以 H2 跑 INSERT / UPDATE / 软删除。

## 当前待提交文件（4 个）

| 文件 | 状态 |
|---|---|
| `server-java-SuMon/src/main/java/com/susumonitor/server/module/alert/mapper/AlertRuleMapper.java` | modified |
| `server-java-SuMon/src/test/java/com/susumonitor/server/module/alert/mapper/AlertRuleMapperMybatisTests.java` | untracked |
| `docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md` | modified |
| `docs-SuMon/Develop-log/20260727-MVP6告警规则Mapper修复.md` | untracked |

## Step 1 — Maven 验证

在 `server-java-SuMon/` 下：

```powershell
cd "server-java-SuMon"
mvn -q test -Dtest=AlertRuleMapperMybatisTests
mvn -q test
mvn -q -DskipTests package
```

**期望**：3 个命令全部静默通过。

**如失败**：把 mvn 输出贴回 `docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md` 末尾的"修复记录"段，定位失败原因后再处理。

## Step 2 — Git 确认范围

仓库根目录：

```powershell
cd ".."
git status
```

**期望**：`git status` 只列出上述 4 个文件，且没有意外的 `target/`、`*.class`、`.env`、`.idea/` 等。

**如有多余文件**：先 `git checkout -- <误报文件>` 或 `git clean -n`（dry-run 先看），确认安全后再清理。

## Step 3 — Stage + Commit

```powershell
git add server-java-SuMon/src/main/java/com/susumonitor/server/module/alert/mapper/AlertRuleMapper.java
git add server-java-SuMon/src/test/java/com/susumonitor/server/module/alert/mapper/AlertRuleMapperMybatisTests.java
git add docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md
git add docs-SuMon/Develop-log/20260727-MVP6告警规则Mapper修复.md

git status   # 再确认一次：只 4 个文件 staged
```

**Commit 消息**（建议一次 commit，Husky pre-commit 钩子会跑 `npm run openapi:check`）：

```text
fix(server): AlertRuleMapper.insertRule @Param("rule") + H2 regression test

- add @Param("rule") to AlertRuleMapper.insertRule so the @Param name
  matches XML #{rule.*} and useGeneratedKeys keyProperty="rule.id"
- add AlertRuleMapperMybatisTests covering INSERT id backfill,
  UPDATE row mutation, and soft-delete visibility (real H2 + real XML)
- update bug-fix note and develop-log to record fix and verification
```

```powershell
git commit -m "fix(server): AlertRuleMapper.insertRule @Param(\"rule\") + H2 regression test" -m "- add @Param(\"rule\") to AlertRuleMapper.insertRule so the @Param name matches XML #{rule.*} and useGeneratedKeys keyProperty=\"rule.id\"" -m "- add AlertRuleMapperMybatisTests covering INSERT id backfill, UPDATE row mutation, and soft-delete visibility (real H2 + real XML)" -m "- update bug-fix note and develop-log to record fix and verification"
```

**期望**：husky pre-commit 钩子跑通，commit 成功。

## Step 4 — 真实环境复验（可选但是强烈建议）

复验的目的是把"修复前 500 空 body" / "修复后仅 H2 通过" 推进到 "真实 MySQL 真实 HTTP 全 200"。

1. **在 IDEA 中重启后端**（让 `target/classes` 包含新 mapper）—— 当前已在 IDEA 跑着，需要你 Stop 再 Run
2. 用合法 ADMIN_PASSWORD 登录拿 token
3. 复跑 `docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md` 中的"复现命令"段
4. **期望**：POST / PUT / DELETE 三路全 HTTP 200，`DELETE` 后 GET 列表行不在

把复验结果写回 `2026-07-27-MVP6-alert-rules-mapper-500.md` 末尾"修复记录"段。

## 通过标准

- [x] Step 1 三个 mvn 命令通过（用户执行后填入实际结果）
- [x] Step 2 git status 只剩 4 个文件
- [x] Step 3 commit 成功，hash 贴到 `20260727-MVP6告警规则Mapper修复.md`
- [ ] Step 4 真实 HTTP 4 路全 200（推荐但非强制）

## 不在本 checklist 范围

- 不修改任何后端 Java 代码
- 不跑 Maven 命令（除验证已完成 fix）
- 不触碰数据库
- 不启动 WSL Agent

## 关联

- 问题单：`docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md`
- 修复日志：`docs-SuMon/Develop-log/20260727-MVP6告警规则Mapper修复.md`
- 收口计划：`C:\Users\genhaosan\.claude\plans\iterative-bouncing-valley.md`
