import { readdirSync } from "node:fs";
import path from "node:path";
import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { migrate } from "drizzle-orm/pglite/migrator";
import { beforeAll, describe, expect, it } from "vitest";
import * as schema from "@/lib/db/schema";

const migrationsFolder = path.resolve(__dirname, "../src/lib/db/migrations");

describe("db schema", () => {
  let db: ReturnType<typeof drizzle<typeof schema>>;

  beforeAll(async () => {
    db = drizzle(new PGlite(), { schema });
    await migrate(db, { migrationsFolder });
  }, 60_000);

  it("has a migration checked in", () => {
    expect(readdirSync(migrationsFolder).some((f) => f.endsWith(".sql"))).toBe(true);
  });

  async function seedReview(googleReviewId: string, starRating = 5) {
    const [org] = await db.insert(schema.organizations).values({ name: "o" }).returning();
    const [loc] = await db.insert(schema.locations).values({ organizationId: org!.id, googleLocationId: "g" + googleReviewId, name: "l" }).returning();
    const [rev] = await db
      .insert(schema.reviews)
      .values({ googleReviewId, locationId: loc!.id, starRating, createTime: new Date(), updateTime: new Date() })
      .returning();
    return { org: org!, loc: loc!, rev: rev! };
  }

  it("rejects star ratings outside 1–5", async () => {
    await expect(seedReview("bad", 6)).rejects.toThrow();
  });

  it("does not allow duplicate reviews within a location", async () => {
    const { loc } = await seedReview("dup");
    await expect(
      db.insert(schema.reviews).values({ googleReviewId: "dup", locationId: loc.id, starRating: 4, createTime: new Date(), updateTime: new Date() }),
    ).rejects.toThrow();
  });

  it("allows only one reply per review (publish idempotency guard)", async () => {
    const { rev } = await seedReview("once");
    await db.insert(schema.replies).values({ reviewId: rev.id, content: "a", publishedBy: "user" });
    await expect(db.insert(schema.replies).values({ reviewId: rev.id, content: "b", publishedBy: "line_user" })).rejects.toThrow();
  });

  it("applies defaults (status pending, usage month key unique per org)", async () => {
    const { org, rev } = await seedReview("defaults");
    expect(rev.status).toBe("pending");
    await db.insert(schema.usageCounters).values({ organizationId: org.id, month: "2026-10" });
    await expect(db.insert(schema.usageCounters).values({ organizationId: org.id, month: "2026-10" })).rejects.toThrow();
  });
});
