import Link from "next/link";
import { PageHeader } from "@/components/layout/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { RoomRulesSummary } from "@/components/rooms/room-rules-summary";
import { CreateRoomForm } from "@/components/rooms/create-room-form";
import { ArrowLeft } from "@/components/ui/icons";

export default function CreateRoomPage() {
  return (
    <div className="animate-fade-up">
      <Link
        href="/lobby"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-muted transition-colors hover:text-gold-300"
      >
        <ArrowLeft className="size-4" aria-hidden />
        লবিতে ফিরে যাও
      </Link>

      <PageHeader
        eyebrow="নতুন রুম"
        title="রুম তৈরি করো"
        subtitle="আকার ঠিক করে বসো — বাকি যুদ্ধ একটু পরেই।"
        actions={
          <Link href="/rooms/join">
            <Button variant="outline">
              <ArrowLeft className="size-4 rotate-180" aria-hidden />
              কোড দিয়ে যোগ দাও
            </Button>
          </Link>
        }
      />

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CreateRoomForm />
        </Card>
        <div className="lg:col-span-2">
          <RoomRulesSummary />
        </div>
      </div>
    </div>
  );
}