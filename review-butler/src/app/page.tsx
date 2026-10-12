import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-bold">好評管家</h1>
      <p className="text-muted-foreground">專案骨架（階段 0）已就緒。</p>
      <Button>開始使用</Button>
    </main>
  );
}
