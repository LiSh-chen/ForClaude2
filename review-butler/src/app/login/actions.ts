"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { isMockGoogle } from "@/lib/adapters";
import { signInMock } from "@/lib/accounts";
import { getDb } from "@/lib/db/client";
import { createSessionToken, SESSION_COOKIE, SESSION_MAX_AGE_SEC } from "@/lib/session-token";

export async function loginAction(formData: FormData) {
  if (!isMockGoogle()) redirect("/login?error=real");
  const email = String(formData.get("email") ?? "").trim();
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) redirect("/login?error=email");
  const userId = await signInMock(await getDb(), email);
  (await cookies()).set(SESSION_COOKIE, createSessionToken(userId), {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: SESSION_MAX_AGE_SEC,
  });
  redirect("/dashboard");
}

export async function logoutAction() {
  (await cookies()).delete(SESSION_COOKIE);
  redirect("/login");
}
