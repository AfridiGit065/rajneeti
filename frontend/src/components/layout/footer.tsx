import { Brand } from "./brand";

export function Footer() {
  return (
    <footer className="border-t border-forest-500/15 py-8">
      <div className="mx-auto flex max-w-7xl flex-col items-center gap-4 px-4 sm:px-6 md:flex-row md:justify-between">
        <Brand size="sm" subtitle={false} />
        <p className="max-w-md text-center text-xs leading-relaxed text-muted md:text-right">
          রাজনীতি (RAJNEETI) একটি কাল্পনিক খেলা। এটি কোনো বাস্তব রাজনৈতিক দল, নেতা,
          প্রতিষ্ঠান বা ঘটনার প্রতিনিধিত্ব করে না।
        </p>
      </div>
    </footer>
  );
}