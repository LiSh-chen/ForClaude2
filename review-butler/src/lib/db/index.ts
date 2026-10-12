import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import * as schema from "./schema";

let _db: ReturnType<typeof createDb> | undefined;

function createDb() {
  const url = process.env.DATABASE_URL;
  if (!url) throw new Error("DATABASE_URL is not set");
  return drizzle(postgres(url), { schema });
}

/** 延遲建立連線，讓 mock／測試情境不需要資料庫。 */
export function getDb() {
  return (_db ??= createDb());
}

export { schema };
