import { eq } from "drizzle-orm";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { cache } from "react";
import { getDb } from "./db/client";
import { organizations, users } from "./db/schema";
import { SESSION_COOKIE, verifySessionToken } from "./session-token";

export const getCurrentUser = cache(async () => {
  const uid = verifySessionToken((await cookies()).get(SESSION_COOKIE)?.value);
  if (!uid) return null;
  const db = await getDb();
  const [row] = await db
    .select({ user: users, org: organizations })
    .from(users)
    .innerJoin(organizations, eq(users.organizationId, organizations.id))
    .where(eq(users.id, uid));
  return row ? { ...row, db } : null;
});

/** 未登入就導向登入頁。回傳目前使用者、其店家，以及資料庫連線。 */
export async function requireUser() {
  const u = await getCurrentUser();
  if (!u) redirect("/login");
  return u;
}
