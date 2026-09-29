import { Cinzel, Inter, Noto_Sans_Bengali } from "next/font/google";

export const fontInter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

export const fontBengali = Noto_Sans_Bengali({
  variable: "--font-bengali-font",
  subsets: ["bengali", "latin"],
  display: "swap",
});

/**
 * Cinzel carries the display/headline voice of the design (titles, character
 * names, uppercase labels). It has no Bengali coverage, so it is only ever
 * applied to the Latin/English presentation layer; Bengali body copy stays on
 * Noto Sans Bengali.
 */
export const fontCinzel = Cinzel({
  variable: "--font-cinzel-font",
  subsets: ["latin"],
  display: "swap",
});
