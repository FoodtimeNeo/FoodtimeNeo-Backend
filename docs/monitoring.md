# 监控

后端暴露 Prometheus 指标与慢请求日志；告警规则跑在采集端（Prometheus + Alertmanager 或等价方案），本仓库只提供被采集端与建议阈值，不自建告警通道。

## 指标采集

- 地址：`GET /actuator/prometheus`（需采集端网络可达，见下节访问控制）。
- 关键指标：`http_server_requests_seconds`（分路径的请求量/延迟，含 `status` 标签）、`jvm_memory_used_bytes`、`hikaricp_connections`、`tomcat_threads_*`、`process_cpu_usage`、`disk_free_bytes`。
- 建议采集间隔 15s；`http_server_requests_seconds_count{status="500"}` 持续增长即需介入。

## 访问控制

指标端点默认只允许本机抓取（`MONITORING_SCRAPE_IP` 默认 `127.0.0.1`，支持单个IP或CIDR）。生产采集机不在本机时，将该变量设为采集机地址或网段；留空则拒绝一切抓取（fail-closed）。经过反向代理抓取时，需同步启用可信代理转发头（`FORWARD_HEADERS_STRATEGY=framework`），否则看到的来源IP是代理地址而非采集机。

## 慢请求日志

`/api/**` 响应超过 `SLOW_REQUEST_THRESHOLD`（默认2秒）记一条 warn，带方法、路径、耗时、状态码和 `requestId`，可直接按 `requestId` 串联全链路。阈值0到300秒的整秒数；测试可设0秒。

## 磁盘与错误盯防（无指标时的最小集合）

- 暂存目录：每次清理任务记录实际字节数，超过 `STORAGE_DISK_WARN_BYTES`（默认1GiB）记 warn；见 [图片文件](api/dish/uploads.md)。
- 错误率：`code != OK` 且 HTTP 5xx 的响应体统一带 `requestId`，按 `grep -c '"code":"INTERNAL_ERROR"\|DATA_UNAVAILABLE' 日志` 即可做最简巡检；429 激增说明正在被刷，对照限流命名空间的 Redis 键确认来源。
- 建议告警（采集端配置）：5xx 比例 > 1%（5分钟）、P95 延迟 > 2秒（10分钟）、`disk_free_bytes` 余量 < 20%、JVM堆使用 > 85%（10分钟）。

## 实现位置

`micrometer-registry-prometheus` 由 `pom.xml` 引入，版本跟随 Spring Boot；`common/web` 负责请求ID与慢请求日志；`config` 负责 `app.monitoring` 与抓取IP门禁；`media/service` 负责暂存清理与目录水位。新增或修改接口时无需手工埋点，HTTP指标自动按路径打标签；高基数路径变量（ID）由框架归一化为模板，不会打爆标签。
