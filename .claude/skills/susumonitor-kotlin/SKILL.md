---
name: susumonitor-kotlin
description: SuSuMonitor Kotlin 编码规范。当在 SuSuMonitor 项目 app-kt-SuMon/ 目录下编写 Android/Kotlin 代码时自动加载。触发场景：编写 Kotlin 代码、使用 Jetpack Compose 编写 UI、使用 Retrofit 网络请求、Android 前台 Service 开发、Android 推送通知。注意：app-kt-SuMon/ 当前为空目录（属增强阶段），本 skill 适用于未来 Android 客户端启动。
when_to_use: 编写 Kotlin 代码 / Jetpack Compose / Retrofit / Android 前台 Service 时
user-invocable: true
---

# SuSuMonitor Kotlin 编码规范

## 1. 命名规范

- 类名：PascalCase，如 `ServerListViewModel`
- 函数名：camelCase，如 `fetchServers()`
- 变量名：camelCase，如 `serverList`
- 常量：UPPER_SNAKE_CASE，如 `MAX_RETRY_COUNT`
- 包名：小写，用 `.` 分隔

## 2. Jetpack Compose

- Composable 函数命名：PascalCase，如 `ServerListScreen()`
- 使用 `remember` 和 `mutableStateOf` 管理局部状态
- 使用 `ViewModel` 管理业务逻辑和 UI 状态
- 使用 `LaunchedEffect` 处理副作用
- 预览函数使用 `@Preview` 注解
- 使用 `Modifier` 链式调用设置样式

## 3. 网络请求 (Retrofit + OkHttp)

- 接口定义放在 `api/` 包下
- 使用 `suspend` 函数定义异步请求
- 使用 `@Header("Authorization")` 传递 JWT Token
- 统一错误处理，使用 `sealed class` 封装结果：
  ```kotlin
  sealed class Result<out T> {
      data class Success<T>(val data: T) : Result<T>()
      data class Error(val message: String) : Result<Nothing>()
  }
  ```

## 4. WebSocket

- 使用 OkHttp 的 WebSocket 客户端
- 连接和断开在 `ViewModel` 中管理
- 使用 `StateFlow` 向外暴露数据

## 5. 前台 Service

- 继承 `Service` 类，在 `onStartCommand` 返回 `START_STICKY`
- 必需创建 `Notification` 并调用 `startForeground()`
- 在 `AndroidManifest.xml` 中声明 `FOREGROUND_SERVICE` 权限

## 6. 通知推送

- 使用 `NotificationManager` + `NotificationChannel`
- 创建告警通知渠道，优先级设为 `IMPORTANCE_HIGH`
- 点击通知跳转到告警详情页面

## 7. 依赖注入 (Hilt)

- 使用 `@HiltViewModel` 注解 ViewModel
- 使用 `@Inject` 构造函数注入依赖
- Repository 使用 `@Singleton` 作用域

## 8. 目录结构

```
app/src/main/java/com/susumonitor/
├── api/            # Retrofit 接口定义
├── data/           # 数据模型和 Repository
├── di/             # Hilt 依赖注入模块
├── service/        # 前台 Service
├── ui/             # Compose UI 页面
│   ├── login/
│   ├── dashboard/
│   ├── servers/
│   ├── alerts/
│   └── terminal/
├── util/           # 工具类
└── MainActivity.kt
```

## 9. 其他

- 优先使用 `val` 而非 `var`
- 使用 `when` 替代 `if-else if` 链
- 使用 `?.` 和 `?:` 处理空安全
- 使用扩展函数简化代码
- 避免使用 `!!` 强制解包
