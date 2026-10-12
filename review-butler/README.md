# 好評管家（暫名）

用 AI 幫台灣在地店家回覆 Google 評論：抓新評論 → AI 產生個人化繁中草稿 → LINE 推播 → 一鍵發布。
完整需求見 [SPEC.md](./SPEC.md)（開發順序依第 9 節分階段進行）。

> 目前進度：**階段 0（專案骨架）**。尚無登入、評論流程等功能。

## 技術組合
Next.js 16（App Router）· TypeScript · Tailwind CSS v4 · shadcn/ui · Drizzle ORM（PostgreSQL）· Vitest

## 本機啟動
需求：Node.js 20+、PostgreSQL（Supabase / Neon 或本機皆可）。

```bash
cd review-butler
npm install
cp .env.example .env.local     # 填入 DATABASE_URL；其餘階段 0 不需要
npm run db:migrate             # 套用 src/lib/db/migrations
npm run dev                    # http://localhost:3000
```

`.env.local` 中 `USE_MOCK_ADAPTERS=true`（預設）時，所有外部服務（Google、LINE、LLM、金流）都走 mock，不需任何金鑰。

## 常用指令
| 指令 | 說明 |
|---|---|
| `npm test` | 執行測試（含用 PGlite 實際套用 migration 的 schema 測試，不需外部資料庫） |
| `npm run typecheck` | TypeScript 型別檢查 |
| `npm run db:generate` | 修改 `src/lib/db/schema.ts` 後產生新的 migration |
| `npm run build` | 正式建置 |

## 目錄
```
src/
  app/                 Next.js 路由
  components/ui/       shadcn/ui 元件（可用 `npx shadcn add <name>` 新增）
  config/              方案額度（plans.ts）、產業別與預設主題（industries.ts）
  lib/
    adapters/          外部服務抽象：google-business / line / llm / payment
                       每個都有 interface.ts + mock，real 實作留待對應階段
    db/                Drizzle schema、連線、migrations
    ai/ jobs/          之後階段使用
tests/
```

## Adapter 約定
- 一律透過 `src/lib/adapters/index.ts` 的 `getGoogleBusiness()` / `getLine()` / `getLlm()` / `getPayment()` 取得實例，不直接 new。
- `USE_MOCK_ADAPTERS` 未設定時視為 `true`，避免誤打正式 API。
- Real adapter 目前都只是會拋 `NotImplementedError` 的占位。
- Mock 提供測試鉤子：`MockLineAdapter.sent`、`MockGoogleBusinessAdapter.revoked / addReview()`、`MockLlmAdapter.calls` 等。
