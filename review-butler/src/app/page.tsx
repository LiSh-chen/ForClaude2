import Link from "next/link";
import { redirect } from "next/navigation";
import { Button } from "@/components/ui/button";
import { getCurrentUser } from "@/lib/auth";

export default async function Home() {
  if (await getCurrentUser()) redirect("/dashboard");
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-3xl font-bold">好評管家</h1>
      <p className="text-muted-foreground">AI 幫您寫好 Google 評論的回覆，您只要確認、一鍵發布。</p>
      <Button asChild size="lg"><Link href="/login">登入</Link></Button>
    </main>
  );
}
