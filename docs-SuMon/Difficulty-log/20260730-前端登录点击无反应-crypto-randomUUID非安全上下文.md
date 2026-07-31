# 2026-07-30 前端登录点击无反应(crypto.randomUUID 非安全上下文)

**日期**: 2026-07-30
**操作人**: opencode / 用户
**关联**: 云端部署后,浏览器登录页输入账号密码点击登录,页面不登录

## 一、Bug 现象

浏览器访问 `http://82.156.245.102` 登录页,输入账号密码点击"登录"按钮:
- 页面不跳转、不登录
- **浏览器 console 面板无任何输出**
- **浏览器 network 面板无任何请求发出**(连失败的请求都没有)

## 二、尝试的方法(含错误尝试)

1. **怀疑 LoginView 的表单校验拦截**:对比 `LoginView.vue.bak`(旧版,有 `formRef.validate()` 校验失败静默 return)与新版(无校验直接 login)。一度以为是浏览器缓存了旧版 validate-gated 代码导致静默失败——**错误**,后证实浏览器加载的是新版(无 validate 拦截)。
2. **怀疑浏览器缓存旧构建**:给 nginx `index.html` 加 `Cache-Control: no-cache, must-revalidate`,清浏览器缓存,问题依旧——**错误**,用户硬刷新后仍无反应,访问日志显示浏览器全量加载了新资源(全 200)。
3. **查 nginx 访问日志**:发现浏览器发了 0 次 `POST /api/auth/login`(只有测试时 curl 发的 3 次)——**登录请求根本没发出**,定位到客户端请求发出前被阻断。
4. **grep 部署的 main bundle**:发现 `function BO(){return crypto.randomUUID()}`(无兜底),定位到请求拦截器的 `newCorrelationId()`。

## 三、根因

站点以**明文 HTTP**(`http://82.156.245.102`)提供服务,不是安全上下文(HTTPS/localhost)。

`crypto.randomUUID()` 是 **Web Crypto API 的安全上下文专属方法**,在非安全上下文(明文 HTTP)下 `window.crypto.randomUUID` 是 `undefined`。

前端 `src/api/client.ts` 的请求拦截器每次发请求前调 `newCorrelationId()` 生成 `X-Correlation-ID` 头:
```ts
// 修复前
export function newCorrelationId(): string {
  return crypto.randomUUID()  // ← HTTP 下 crypto.randomUUID 是 undefined,调用抛 TypeError
}
```

`crypto.randomUUID()` 抛 `TypeError: crypto.randomUUID is not a function`,异常在 axios 请求拦截器内同步抛出,**请求在 dispatch 前被 reject**,network 面板不出现任何请求记录,console 也无输出(异常被 axios 内部 catch 后 reject promise,不冒泡到控制台)。

## 四、修复

`src/api/client.ts` 的 `newCorrelationId()` 加安全上下文降级(沿用项目 `services/terminal-ws.ts` 已有的兜底模式):
```ts
export function newCorrelationId(): string {
  return typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
}
```

同时 `src/services/websocket.ts` 两处裸 `crypto.randomUUID()`(metrics.subscribe 等)改复用 `newCorrelationId()`。

## 五、验证

重新构建前端部署后,浏览器硬刷新登录页,点击登录:
- network 面板出现 `POST /api/auth/login` 请求
- 后端正常响应(账号密码正确返回 200+token,错误返回 40001)
- 登录成功跳转 dashboard

## 六、教训

1. **明文 HTTP 部署是安全上下文 API 的雷区**:`crypto.randomUUID`、`crypto.subtle`、`navigator.clipboard` 等都仅安全上下文可用,明文 HTTP 下全部失效。本项目此前只在 `services/terminal-ws.ts` 做了兜底,`api/client.ts` 漏了——同类坑要全局排查,不能只修一处。
2. **"network 无请求 + console 无错"不一定是缓存**:请求拦截器在 dispatch 前同步抛异常,axios 会 reject promise 但不冒泡到控制台,表现为"什么都没发生"。遇到此类现象优先查请求拦截器的同步代码。
3. **访问日志是判定"请求是否发出"的铁证**:浏览器是否加载了新构建、是否发了登录请求,看 nginx access log 比猜缓存可靠得多。
