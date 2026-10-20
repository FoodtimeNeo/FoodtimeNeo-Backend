# AI 预检（分诊）

对 `pending` 的评论与图片投稿做异步风险分诊：打风险等级、置信度、标签和一句话说明，供人工审核时参考。分诊本身保存结论；评论、社区正文/评论与点评回复还会按下文阈值走自动决策，图片仅供人工参考。未检出、无结论的一律留在人工队列。

## 范围与触发

- 对象：状态为 `pending` 的 `dish_review_submissions`（评论）、`dish_images`（图片投稿）
  与 `community_posts`（社区动态，文本类型 `community-post`、配图逐张 `community-image`）、`community_comments`及`dish_review_replies`（点评回复，类型`dish-review-reply`）。菜品/档口投稿暂不送检。
- 即时：投稿创建事务提交后，`SubmissionDiagnoseEvent` 经有界线程池（2–4 线程、队列 200，满了丢弃）异步诊断，通常几秒内出结论；请求线程绝不等 AI，AI 再慢也不影响发帖返回。
- 兜底：定时扫批（默认每 5 分钟一批，最多 10 条）只处理漏网的（崩溃重启、线程池满丢弃）；每条投稿最多检一次（`uq_ai_moderation_result` 保证，即使并发也不重复）。
- 传输故障（超时、鉴权失败、返回无法解析）不写结论行，下个周期重试；日志只记ID与错误类型，不记原文和图片字节。
- 暂存文件已丢失的投稿（手动删除或存储卷丢失）无法诊断，记一条 HIGH/`文件缺失` 结论并附说明，人工看到后驳回即可，同样不重试。

## 送检内容与隐私

- 评论：评分数字 + 正文文本。
- 图片：480px 缩略图字节（复用 `?size=thumb` 的生成逻辑，不传原图），MIME 随原格式。
- 使用外部模型即把用户原文与图片发往第三方服务：生产启用前须在用户协议或公告中说明，并走正式审批；测试/开发用 `fake` 提供方，不产生真实外发。

## 提供方

`AiProvider` 接口，两种实现，按 `app.ai.triage.provider` 选择：

| 取值 | 行为 |
| --- | --- |
| `openai`（默认） | 调 OpenAI 兼容的 `POST {base-url}/chat/completions`（chat + vision）；temperature 0，max_tokens 400，要求模型只回 JSON。多 key 时按 `AI_TRIAGE_API_KEY` 的逗号分隔列表逐请求轮询，遇到 401/403/429 自动切下一个（全死则本轮记失败、扫批下轮重试） |
| `fake` | 确定性演示：正文含“违禁”判 HIGH；含“存疑”首次 LOW 0.5、复核 LOW 0.95（演示升级链路）；其余 LOW；图片一律 LOW。仅供本地开发与集成测试，绝不用于真实判定 |

模型回答的 JSON 形如 `{"risk":"HIGH","confidence":0.95,"labels":["广告"],"reason":"推广二维码"}`；`risk` 只认 LOW/MEDIUM/HIGH（大小写不敏感），`confidence` 钳到0–1，`labels` 最多取10个，`reason` 截断500字。解析失败按传输故障处理（重试，不写行）。

提示词固定在 `OpenAiCompatibleProvider` 的 `REVIEW_SYSTEM` / `IMAGE_SYSTEM` 常量中，随代码版本管理；调提示词即发版，效果变化以验收记录为准。

## 三模型链路

评论走快车道+升级+兜底三段链：`primary-model`（快模型，如 super-120b-nano）先筛全部；
首判为 LOW 且置信度低于 `escalation-below-confidence`（默认 0.9）、或首判为 MEDIUM 时，
用 `escalation-model`（强模型，如 super-vl）复核并采用复核结论；任一环节报传输故障
（超时、鉴权失败、无法解析）则用 `fallback-model`（如 glm-flash）重试一次原输入。
图片直接用 `photo-model`（视觉模型，如 super-vl）诊断，故障时同样走 `fallback-model`。
升级失败保留首判；兜底也失败则不写行，下个周期重试。阈值 0.9 来自百条离线评测的敏感度扫描：
仅升级低于该阈值的 LOW 首判即可拿回漏检的高风险项，同时不引入误伤。
注意升级阈值不得高于 0.9：评测显示强模型对产品标准允许的激烈措辞更严格，
0.95 会把复核权交给它，导致本该放行的正常差评被升级改判（离线仿真已复现 1 例误驳）。

## 自动拍板（评论、社区正文/评论及点评回复）

最终结论够硬时 AI 直接执行，无需人工点按钮，拿不准的才留给人：

- LOW 且置信度 ≥ `auto-approve-above`（默认 0.95）→ 自动通过，正式评论即时可见；
- HIGH 且置信度 ≥ `auto-reject-above`（默认 0.9）→ 自动驳回，原因取 AI 说明；
- MEDIUM，或置信度不够 → 保持 pending，进人工队列（管理端照常看到 AI 分诊参考）。

自动决定走与人工审核同一事务路径（发布正式记录、更新投稿状态、记审核日志），
只是日志记 `actor='ai'` + 模型名，与人工（`actor='human'` + 管理员）分开存放，
核验时在审核日志里一眼区分。图片投稿暂不自动拍板，只做分诊参考。
自动与人工撞单（人先点了）按 `ALREADY_REVIEWED` 吸收，不报错。

## 待调优（用户记录，策略未定）

- 2026-10-18 本地试跑：一条低俗评论首判为 MEDIUM/0.85（标签含低俗），按当时规则留人工。
  2026-10-19 起 MEDIUM 已送升级模型复核（`needsEscalation`），复核 HIGH 达线自动驳回，
  其余仍留人工；线上观察复核后的去向分布再定下一步。
- 候选方向（待用户定）：对含低俗/色情标签的高置信 MEDIUM 直接自动驳回；
  或收紧 `auto-approve-above`。改动前先补评测用例，确认 0 误伤再上线。

## 结论表与展示

结论存 `foodtime.ai_moderation_results`（`database-design.md#ai_moderation_resultsAI预检结论表`）：类型+投稿ID唯一、`risk_level`、`confidence`（0–1三位小数）、`labels` 数组、`reason`、`model`、`checked_at`。管理审核队列（`GET /api/v1/admin/moderation`）的每条目附带 `aiRiskLevel/aiConfidence/aiLabels/aiReason/aiCheckedAt`，未检出为null；前端展示风险与置信度，pending 的仍须人工点通过/驳回（自动拍板的不进队列）。

## 开关与配额

见[配置](configuration.md)：`AI_TRIAGE_ENABLED` 默认关闭；`openai` 提供方要求配齐地址、密钥与模型名，否则启动校验直接失败（fail-fast，不静默跳过）。`AI_TRIAGE_API_KEY` 等私密项只走环境变量或部署平台密钥，模板见 `.env.example`，绝不进仓库。图片走缩略图后单次请求通常在几十KB内；费用按调用方用量结算，建议先在小流量时段启用并观察 `checked_at` 覆盖率。

V17起点评回复沿用文字三模型链路、即时异步事件及五分钟漏检补扫；审核操作统一写入身份/时间/原因及审核日志。只有未删除pending回复送检；原点评删除后不再补扫，人工可驳回清理。回复批准和删除通过触发器更新楼层reply_count，不修改菜品评分。
