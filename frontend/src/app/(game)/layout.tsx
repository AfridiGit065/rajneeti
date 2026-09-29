import type { ReactNode } from "react";
import { RequireAuth } from "@/components/auth/require-auth";

export default function GameLayout({ children }: { children: ReactNode }) {
  // A live match page is not public: it renders private hand state and drives the
  // WebSocket session, so it carries the same guard as the rest of the app.
  return (
    <RequireAuth>
      <div className="min-h-screen relative overflow-hidden">{children}</div>
    </RequireAuth>
  );
}
