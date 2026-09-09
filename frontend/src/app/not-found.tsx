import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Landmark } from "@/components/ui/icons";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-app px-4 text-center">
      <span className="mb-6 flex size-16 items-center justify-center rounded-2xl border border-gold-500/40 bg-deep-800 text-gold-300">
        <Landmark className="size-8" aria-hidden />
      </span>
      <p className="font-bengali text-6xl font-bold text-gold-gradient">৪০৪</p>
      <h1 className="mt-3 font-bengali text-2xl font-bold text-ivory">পাতা খুঁজে পাওয়া যায়নি</h1>
      <p className="mt-2 max-w-md text-sm text-muted">
        এই ঠিকানায় কিছু নেই — হয়তো গোপন টানা-পোড়েনের অংশ। লবিতে ফিরে যান।
      </p>
      <div className="mt-8 flex gap-3">
        <Link href="/lobby">
          <Button variant="premium">লবিতে যান</Button>
        </Link>
        <Link href="/">
          <Button variant="ghost">হোম</Button>
        </Link>
      </div>
    </div>
  );
}