import { mkdirSync } from "node:fs";
import path from "node:path";
import type { PGlite as PGliteType } from "@electric-sql/pglite";
import type { PgDatabase, PgQueryResultHKT } from "drizzle-orm/pg-core";
import * as schema from "./schema";

export type Db = PgDatabase<PgQueryResultHKT, typeof schema>;

export const MIGRATIONS_FOLDER = path.join(process.cwd(), "src/lib/db/migrations");

/** PGlite（嵌入式 Postgres，WASM）。dataDir 省略時為記憶體資料庫（測試用）。 */
export async function createPgliteDb(dataDir?: string): Promise<{ db: Db; client: PGliteType }> {
  const { PGlite } = await import("@electric-sql/pglite");
  const { drizzle } = await import("drizzle-orm/pglite");
  const { migrate } = await import("drizzle-orm/pglite/migrator");
  if (dataDir) mkdirSync(dataDir, { recursive: true });
  const client = new PGlite(dataDir);
  const db = drizzle(client, { schema });
  await migrate(db, { migrationsFolder: MIGRATIONS_FOLDER });
  return { db: db as unknown as Db, client };
}

async function createPostgresDb(url: string): Promise<Db> {
  const postgres = (await import("postgres")).default;
  const { drizzle } = await import("drizzle-orm/postgres-js");
  return drizzle(postgres(url), { schema }) as unknown as Db;
}

// 以 globalThis 保留連線，避免 Next dev 熱重載重複建立。
const g = globalThis as unknown as { __db?: Promise<Db> };

/**
 * 有 DATABASE_URL → 連 PostgreSQL（需先 `npm run db:migrate`）。
 * 沒有 → 使用 .data/pglite 的本機嵌入式資料庫並自動套用 migration，零設定即可啟動。
 */
export function getDb(): Promise<Db> {
  if (!process.env.DATABASE_URL && process.env.NODE_ENV === "production") {
    throw new Error("DATABASE_URL must be set in production（內嵌 PGlite 只供本機開發）");
  }
  return (g.__db ??= process.env.DATABASE_URL
    ? createPostgresDb(process.env.DATABASE_URL)
    : createPgliteDb(path.join(process.cwd(), ".data/pglite")).then((r) => r.db));
}

export { schema };
