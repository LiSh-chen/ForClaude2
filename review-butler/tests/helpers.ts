import { createPgliteDb, type Db } from "@/lib/db/client";
import { signInMock } from "@/lib/accounts";
import { eq } from "drizzle-orm";
import { users } from "@/lib/db/schema";

/** 每個測試檔建立一個記憶體內的 PGlite（自動套用 migration）。 */
export async function makeTestDb(): Promise<Db> {
  return (await createPgliteDb()).db;
}

export async function makeOrg(db: Db, email = "owner@example.com") {
  const userId = await signInMock(db, email);
  const [user] = await db.select().from(users).where(eq(users.id, userId));
  return { userId, orgId: user!.organizationId! };
}
