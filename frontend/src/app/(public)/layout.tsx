import type { ReactNode } from "react";
import { TopNav } from "@/components/layout/top-nav";

export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-app">
      <TopNav mode="public" />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6">
        {children}
      </main>
    </div>
  );
}