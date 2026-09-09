"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/cn";
import { Brand } from "./brand";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Menu, X } from "@/components/ui/icons";
import { useAuthStore } from "@/store/auth-store";
import { useState } from "react";

const PUBLIC_LINKS = [
  { href: "/", label: "Home" },
  { href: "/how-to-play", label: "How To Play" },
  { href: "/characters", label: "Characters" },
];

const APP_LINKS = [
  { href: "/lobby", label: "লবি" },
  { href: "/rooms/join", label: "রুম যোগ দিন" },
  { href: "/leaderboard", label: "লিডারবোর্ড" },
  { href: "/history", label: "ইতিহাস" },
  { href: "/profile", label: "প্রোফাইল" },
  { href: "/settings", label: "সেটিংস" },
];

export function TopNav({ mode = "public" }: { mode?: "public" | "app" }) {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const user = useAuthStore((s) => s.user);

  const links = mode === "app" ? APP_LINKS : PUBLIC_LINKS;

  return (
    <header className="sticky top-0 z-40 border-b border-forest-500/20 bg-deep-950/85 backdrop-blur-lg">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
        <Brand size="sm" />

        <nav className="hidden items-center gap-1 lg:flex" aria-label="প্রধান নেভিগেশন">
          {links.map((link) => {
            const active = pathname === link.href || (link.href !== "/" && pathname.startsWith(`${link.href}/`));
            return (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  "rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
                  active
                    ? "text-gold-300 bg-gold-500/10"
                    : "text-muted hover:text-ivory hover:bg-deep-700/50",
                )}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="hidden items-center gap-3 lg:flex">
          {mode === "public" ? (
            <>
              <Link href="/login">
                <Button variant="ghost" size="sm">
                  Login
                </Button>
              </Link>
              <Link href="/register">
                <Button variant="premium" size="sm">
                  Register
                </Button>
              </Link>
            </>
          ) : user ? (
            <Link
              href="/profile"
              className="flex items-center gap-2 rounded-full border border-gold-500/35 bg-deep-800/80 py-1 pl-1 pr-3 transition-colors hover:border-gold-400"
            >
              <span className="flex size-7 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-xs font-bold text-deep-950">
                {user.avatarInitial}
              </span>
              <span className="text-sm font-medium text-ivory">{user.displayName}</span>
              <Badge tone="gold">লেভেল {user.level}</Badge>
            </Link>
          ) : null}
        </div>

        <button
          type="button"
          className="rounded-lg p-2 text-muted transition-colors hover:text-ivory focus-visible:outline-2 focus-visible:outline-gold-400 lg:hidden"
          aria-label={mobileOpen ? "মেনু বন্ধ" : "মেনু খুলুন"}
          aria-expanded={mobileOpen}
          onClick={() => setMobileOpen((v) => !v)}
        >
          {mobileOpen ? <X className="size-6" aria-hidden /> : <Menu className="size-6" aria-hidden />}
        </button>
      </div>

      {mobileOpen ? (
        <nav
          className="border-t border-forest-500/20 px-4 pb-4 pt-2 lg:hidden animate-fade-in"
          aria-label="মোবাইল নেভিগেশন"
        >
          <ul className="flex flex-col gap-1">
            {mode === "app" && user ? (
              <li className="mb-2 border-b border-forest-500/20 pb-2">
                <Link
                  href="/profile"
                  onClick={() => setMobileOpen(false)}
                  className="flex items-center gap-3 rounded-lg p-2 hover:bg-deep-700/50"
                >
                  <span className="flex size-8 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-xs font-bold text-deep-950">
                    {user.avatarInitial}
                  </span>
                  <div className="flex flex-col leading-tight">
                    <span className="text-sm font-semibold text-ivory">{user.displayName}</span>
                    <span className="text-xs text-muted">@{user.username}</span>
                  </div>
                  <Badge tone="gold" className="ml-auto text-[10px]">লেভেল {user.level}</Badge>
                </Link>
              </li>
            ) : null}
            {links.map((link) => {
              const active = pathname === link.href || (link.href !== "/" && pathname.startsWith(`${link.href}/`));
              return (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    onClick={() => setMobileOpen(false)}
                    className={cn(
                      "block rounded-lg px-3 py-2.5 text-sm font-medium",
                      active ? "text-gold-300 bg-gold-500/10" : "text-muted hover:bg-deep-700/50",
                    )}
                  >
                    {link.label}
                  </Link>
                </li>
              );
            })}
            {mode === "public" ? (
              <li className="mt-2 flex gap-2">
                <Link href="/login" className="flex-1" onClick={() => setMobileOpen(false)}>
                  <Button variant="outline" fullWidth>
                    Login
                  </Button>
                </Link>
                <Link href="/register" className="flex-1" onClick={() => setMobileOpen(false)}>
                  <Button variant="premium" fullWidth>
                    Register
                  </Button>
                </Link>
              </li>
            ) : null}
          </ul>
        </nav>
      ) : null}
    </header>
  );
}