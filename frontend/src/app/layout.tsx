import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { fontInter, fontBengali } from "@/lib/fonts";
import { AppProviders } from "@/components/providers/app-providers";
import "./globals.css";

const APP_TITLE = "রাজনীতি — RAJNEETI";

export const metadata: Metadata = {
  title: {
    default: `${APP_TITLE} | The Game of Power`,
    template: `%s | ${APP_TITLE}`,
  },
  description:
    "রাজনীতি — A real-time multiplayer bluff strategy game. ক্ষমতার খেলায় সত্য নয়, বুদ্ধিই শেষ কথা।",
  keywords: [
    "রাজনীতি",
    "rajneeti",
    "bluff game",
    "strategy game",
    "multiplayer card game",
  ],
};

export const viewport: Viewport = {
  themeColor: "#06120e",
  colorScheme: "dark",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html
      lang="en"
      className={`${fontInter.variable} ${fontBengali.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}