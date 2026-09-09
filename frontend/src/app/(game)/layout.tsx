import type { ReactNode } from "react";

export default function GameLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-app">
      <div
        className="pointer-events-none fixed inset-0 bg-[radial-gradient(ellipse_60%_45%_at_50%_32%,rgb(46_110_82/0.14),transparent_60%)]"
        aria-hidden
      />
      <div className="relative">{children}</div>
    </div>
  );
}