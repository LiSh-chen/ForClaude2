import { NextResponse } from "next/server";
import { getLine } from "@/lib/adapters";
import { getCurrentUser } from "@/lib/auth";
import { createLoginState, LINE_NONCE_COOKIE } from "@/lib/line/login-state";
import { lineCallbackUrl } from "@/lib/line/links";

/** 開始 LINE Login 綁定：必須已登入後台。導向 LINE 授權頁（會一併提示加官方帳號好友）。 */
export async function GET(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.redirect(new URL("/login", req.url));
  const { state, nonce } = createLoginState(me.org.id);
  const target = getLine().buildLoginUrl({ state, redirectUri: lineCallbackUrl() });
  const res = NextResponse.redirect(new URL(target, req.url));
  res.cookies.set(LINE_NONCE_COOKIE, nonce, { httpOnly: true, sameSite: "lax", secure: process.env.NODE_ENV === "production", path: "/api/auth/line", maxAge: 600 });
  return res;
}
