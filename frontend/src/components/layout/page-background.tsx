import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type PageBackgroundVariant =
  | "lobby"     // Lobby, Join Room, Create Room, Room Lobby (~0.45 dark emerald overlay)
  | "dark"      // Profile, Leaderboard, History, How To Play, Characters (~0.68 dark overlay)
  | "minimal"   // Settings (~0.82 minimal overlay)
  | "game"      // Game Board (dedicated table background with felt glow)
  | "game-over"; // Game Over screen (dedicated table background with dark victory overlay)

interface PageBackgroundProps {
  variant?: PageBackgroundVariant;
  image?: string;
  overlayOpacity?: number;
  className?: string;
  children?: ReactNode;
}

const BG_ASSETS = {
  lobby: "/assets/game/rajneeti-lobby-bg.png.png",
  table: "/assets/game/rajneeti-table-bg.png.png",
} as const;

export function PageBackground({
  variant = "lobby",
  image,
  overlayOpacity,
  className,
  children,
}: PageBackgroundProps) {
  // Determine the background image asset
  const isTableBg = variant === "game" || variant === "game-over";
  const bgImage = image ?? (isTableBg ? BG_ASSETS.table : BG_ASSETS.lobby);

  return (
    <div
      className={cn(
        "pointer-events-none fixed inset-0 -z-10 select-none overflow-hidden",
        className,
      )}
      aria-hidden="true"
    >
      {/* ── Layer 0: Background Image ───────────────────────── */}
      <div
        className="absolute inset-0 bg-cover bg-center bg-no-repeat transition-opacity duration-500"
        style={{
          backgroundImage: `url("${bgImage}")`,
          backgroundSize: "cover",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
        }}
      />

      {/* ── Layer 1: Dark Emerald Atmosphere Overlay ─────────── */}
      {variant === "lobby" && (
        <div
          className="absolute inset-0"
          style={{
            backgroundColor:
              overlayOpacity !== undefined
                ? `rgba(2, 12, 9, ${overlayOpacity})`
                : "rgba(2, 12, 9, 0.45)",
            backgroundImage:
              "radial-gradient(ellipse 95% 75% at 50% 25%, transparent 20%, rgba(2, 12, 9, 0.55) 100%)," +
              "linear-gradient(180deg, rgba(3, 19, 14, 0.35) 0%, rgba(2, 12, 9, 0.55) 100%)",
          }}
        />
      )}

      {variant === "dark" && (
        <div
          className="absolute inset-0"
          style={{
            backgroundColor:
              overlayOpacity !== undefined
                ? `rgba(2, 12, 9, ${overlayOpacity})`
                : "rgba(2, 12, 9, 0.68)",
            backgroundImage:
              "radial-gradient(ellipse 85% 65% at 50% 30%, transparent 15%, rgba(2, 12, 9, 0.75) 100%)," +
              "linear-gradient(180deg, rgba(3, 19, 14, 0.5) 0%, rgba(2, 12, 9, 0.75) 100%)",
          }}
        />
      )}

      {variant === "minimal" && (
        <div
          className="absolute inset-0"
          style={{
            backgroundColor:
              overlayOpacity !== undefined
                ? `rgba(2, 12, 9, ${overlayOpacity})`
                : "rgba(2, 12, 9, 0.82)",
            backgroundImage:
              "linear-gradient(180deg, rgba(3, 19, 14, 0.7) 0%, rgba(2, 12, 9, 0.88) 100%)",
          }}
        />
      )}

      {variant === "game" && (
        <div
          className="absolute inset-0"
          style={{
            backgroundColor:
              overlayOpacity !== undefined
                ? `rgba(2, 12, 9, ${overlayOpacity})`
                : "rgba(2, 12, 9, 0.38)",
            backgroundImage:
              "radial-gradient(ellipse 85% 70% at 50% 48%, rgba(8, 58, 41, 0.2) 0%, rgba(3, 19, 14, 0.65) 100%)," +
              "linear-gradient(180deg, rgba(3, 19, 14, 0.35) 0%, rgba(2, 12, 9, 0.55) 100%)",
          }}
        />
      )}

      {variant === "game-over" && (
        <div
          className="absolute inset-0"
          style={{
            backgroundColor:
              overlayOpacity !== undefined
                ? `rgba(2, 12, 9, ${overlayOpacity})`
                : "rgba(2, 12, 9, 0.78)",
            backgroundImage:
              "radial-gradient(ellipse 70% 60% at 50% 35%, rgba(201, 165, 60, 0.08) 0%, rgba(2, 12, 9, 0.85) 100%)," +
              "linear-gradient(180deg, rgba(3, 19, 14, 0.65) 0%, rgba(2, 12, 9, 0.88) 100%)",
          }}
        />
      )}

      {/* ── Layer 2: Subtle Ambient Vignette & Texture ─────────── */}
      <div
        className="absolute inset-0"
        style={{
          boxShadow: "inset 0 0 140px 30px rgba(0, 0, 0, 0.55)",
        }}
      />

      {children}
    </div>
  );
}
