# 每日推荐接口

`GET /api/v1/recommendations/daily`

需要现有登录会话；按项目规则登录的访客账号也可读取。请求无业务参数，服务端使用 Asia/Shanghai 的当天日期，不读取用户偏好或 userId。同一天、相同菜品可见状态下，所有用户读取相同业务数据。生成完成后变更名称、评分或可见性会体现在后续读取中。

返回现有 ApiResponse。以下为仅包含一道菜的说明性示例，UUID、名称、分数和 URL 均非真实业务数据：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "date": "2026-11-03",
    "algorithmVersion": "FT-HybridRec-v2",
    "total": 1,
    "items": [{
      "dishId": "00000000-0000-0000-0000-000000000001",
      "dishName": "示例菜品",
      "diningHallId": "00000000-0000-0000-0000-000000000002",
      "diningHallName": "示例食堂",
      "stallId": "00000000-0000-0000-0000-000000000003",
      "stallName": "示例档口",
      "averageRating": null,
      "reviewCount": 0,
      "imageUrl": null,
      "recommendationType": "EXPLORE",
      "recommendationScore": 0.81,
      "reasonCode": "LESS_EXPOSED",
      "reason": "发现更多菜品"
    }]
  },
  "timestamp": "2026-11-03T03:00:00Z",
  "requestId": "example-request"
}
```

| 字段 | 说明 |
|---|---|
| date / algorithmVersion | 推荐批次业务日期和算法版本 |
| total / items | 当前实际可见数量及按原始快照位置排序的列表，最多默认10条 |
| dishId / dishName | 当前菜品标识与名称 |
| diningHallId / diningHallName | 当前所属食堂 |
| stallId / stallName | 当前所属档口 |
| averageRating / reviewCount | 当前有效正式评论均分与数量；无评论均分为null |
| imageUrl | 最新 approved 图片，无合格图片为null；pending/rejected不返回 |
| recommendationType | QUALITY口碑阶段或EXPLORE探索阶段 |
| recommendationScore | 生成时归一化综合分，[0,1]；不是1～5的原始评分 |
| reasonCode / reason | 生成依据的理由码及中文说明 |

理由映射：GOOD_REPUTATION=口碑不错；RECENTLY_POPULAR=近期受到关注；DAILY_DISCOVERY=今日尝鲜推荐；LESS_EXPOSED=发现更多菜品。不使用“必吃”等无法验证的结论。口碑理由若已失去当前评分支持，会降为今日尝鲜推荐。

Redis故障或缓存损坏时查PostgreSQL；有成功批次不重跑算法，缺少成功批次时受数据库日期行锁保护同步生成。菜品下架、档口或食堂停用后过滤，允许 total 少于10，不覆盖当天推荐历史。空菜谱返回200、total=0、items=[]。

错误沿用现有包装：未登录401；数据库不可用503/DATA_UNAVAILABLE。GET不改变客户端认证或CSRF约定。认证请求见 [auth.http](auth.http)，算法与运行参数见 [模块文档](../modules/daily-recommendation.md)。

本地已有有效Cookie时示例：

```powershell
curl.exe --cookie cookies.txt http://localhost:8080/api/v1/recommendations/daily
```
