# 2026-07-30 宽带运营商劫持 WebSocket(诊断记录,非代码修复)

**日期**: 2026-07-30
**操作人**: opencode / 用户
**关联**: 宽带网络下前端 `/ws/monitor` 连接失败,手机热点正常;本记录为网络层诊断,非代码 bug

## 一、现象

用户用宽带访问 `http://82.156.245.102`:
- 前端登录、dashboard 正常(普通 HTTP 请求通)
- 但 Web 终端 `/ws/monitor` WebSocket 连接 failed
- 浏览器 console: `WebSocket connection to 'ws://82.156.245.102/ws/monitor?ticket=...' failed:`

**关键对比**:同一浏览器,**手机热点网络下 /ws/monitor 正常连上(101)**,宽带网络下失败。换 IP(128→132)仍失败。

## 二、尝试的方法

1. **怀疑 nginx /ws/monitor 配置错误**:查 vhost,`location /ws/monitor` 有 `proxy_http_version 1.1` + `proxy_set_header Upgrade $http_upgrade` + `Connection "upgrade"`,配置正确。且 agent 的 `/ws/agent`(同样配置)在宽带下能连上(agent authenticated)——配置没问题。
2. **怀疑宝塔 WAF 拦截**:查 `nginx.conf`,`#include luawaf.conf;`(注释,未启用);`waf2monitor_data.conf` 只有空 map;无站点 WAF 配置——排除 WAF。
3. **curl 对比测试(关键)**:
   - 云服务器本地 `curl http://127.0.0.1/ws/monitor`(带 Upgrade,Host 头) → **401**(Upgrade 保留,握手过,ticket 无效)
   - 云服务器 `curl http://82.156.245.102/ws/monitor`(自己公网 IP) → **401**(直连 nginx,无中间层)
   - 浏览器(外部宽带 IP) → **400**(后端报 `invalid Upgrade header: null`)
   
   同一路径同配置,curl 保留 Upgrade,浏览器丢失——差异在浏览器→服务器的网络路径。
4. **tcpdump 抓浏览器原始请求**:在云服务器 `tcpdump -i any -A port 80` 抓浏览器到 nginx的包,发现:
   ```
   GET /ws/monitor?ticket=... HTTP/1.1
   Host: 82.156.245.102
   Connection: Upgrade
   Sec-WebSocket-Version: 13
   Sec-WebSocket-Key: ...
   (没有 Upgrade: websocket 头!)
   ```
   浏览器发的请求**缺 `Upgrade: websocket` 头**(Chrome WebSocket 一定发),到 nginx 时已丢失。
5. **curl / 返回 HTML 被注入(铁证)**:`curl http://82.156.245.102/` 返回的 index.html 被**注入了**:
   ```html
   <head><script async src="//ij.so9.cc/j/?t=fx&g=d8c8e936aa30&c=cc28aa157bd1&rv=1"></script>
   ```
   `ij.so9.cc` 是运营商广告/追踪注入脚本——**宽带运营商 HTTP 劫持铁证**。浏览器请求还带了 WAF cookie(`http_Path=%2Froot`、`8adb4395...`)。

## 三、根因

**宽带运营商劫持明文 HTTP 流量**:
1. **篡改 HTML**:在响应里注入 `ij.so9.cc` 脚本(广告/追踪)
2. **破坏 WebSocket 升级**:对浏览器的 WebSocket 升级请求,**去掉 `Upgrade: websocket` 头**,导致后端 `DefaultHandshakeHandler` 收到 `Upgrade=null`,握手失败 400

这**不是服务器/部署/代码问题**,是用户接入网络(宽带运营商)的网络层劫持。`curl` 和 agent(Go client)的请求特征不触发运营商的 WS 拦截(直传),所以它们能连;浏览器(Chrome + WS 升级头)被运营商去头。

## 四、修复

**非代码修复**。代码侧无 bug(手机热点验证终端功能正常)。解决方向:

1. **彻底方案:HTTPS + 域名 + WSS**(推荐)。HTTPS 流量加密,运营商无法劫持/注入/去头,WebSocket 走 `wss://` 正常。需域名(指向 82.156.245.102)+ 证书(Let's Encrypt 免费/宝塔一键)+ nginx 配 443 + 前端改 `https://`/`wss://`。
2. **临时方案:换网络**。用手机热点(已验证可用),或换不受劫持的宽带线路。
3. **投诉运营商**(效果通常有限)。

## 五、验证(诊断证据链)

| 测试 | 结果 | 说明 |
|---|---|---|
| 云服务器 curl 127.0.0.1 /ws/monitor(带 Upgrade) | 401(Upgrade 保留) | 直连 nginx,服务器侧正常 |
| 云服务器 curl 公网 IP /ws/monitor | 401(Upgrade 保留) | 服务器到自己无中间层 |
| 浏览器(宽带,tcpdump 抓包) | 缺 Upgrade 头 + WAF cookie | 宽带路径被中间层处理 |
| curl / 返回 HTML | 注入 ij.so9.cc 脚本 | 运营商 HTTP 劫持铁证 |
| agent(WSL Go client)/ws/agent | 101 连上 | Go client 不带浏览器特征,不被去头 |
| 浏览器(手机热点)/ws/monitor | 101 连上 | 手机热点无运营商劫持 |

## 六、教训

1. **明文 HTTP 部署的根本短板**:运营商可任意篡改明文 HTTP 流量(注入脚本、去头),WebSocket 升级尤其脆弱。生产环境**必须 HTTPS/WSS**。
2. **"curl 能连、浏览器不能"指向网络中间层**:同一路径 curl 保留 Upgrade、浏览器丢失,且 curl / 被注入脚本,基本可判定运营商劫持,不要在服务器配置上浪费排查时间。
3. **tcpdump 抓浏览器原始包是最强证据**:直接看浏览器到服务器的原始 HTTP 头,能立刻发现缺 Upgrade 头,比反复试配置可靠。
4. **换网络对比是快速判定**:手机热点能连、宽带不能,几乎可锁定接入网络(运营商)问题。
