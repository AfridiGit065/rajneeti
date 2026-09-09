import { PageHeader } from "./page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { FileQuestion } from "@/components/ui/icons";

interface ModulePlaceholderProps {
  title: string;
  eyebrow?: string;
  subtitle?: string;
  module: string;
  note: string;
}

/**
 * Temporary but on-brand placeholder for routes implemented in later modules.
 * Keeps the app compiling while keeping the surface intentional.
 */
export function ModulePlaceholder({
  title,
  eyebrow = "শীঘ্রই",
  subtitle,
  module,
  note,
}: ModulePlaceholderProps) {
  return (
    <div className="animate-fade-up">
      <PageHeader eyebrow={eyebrow} title={title} subtitle={subtitle} />
      <EmptyState
        icon={<FileQuestion className="size-6" aria-hidden />}
        title={`মডিউল ${module}`}
        description={note}
      />
    </div>
  );
}