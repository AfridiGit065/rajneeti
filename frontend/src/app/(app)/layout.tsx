import type { ReactNode } from "react";
import { TopNav } from "@/components/layout/top-nav";
import { Footer } from "@/components/layout/footer";
import { RequireAuth } from "@/components/auth/require-auth";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <RequireAuth><div className="flex min-h-screen flex-col bg-app">
      <TopNav mode="app" />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6">
        {children}
      </main>
      <Footer />
    </div></RequireAuth>
  );
}
