import { PageHeader } from "@/components/layout/page-header";
import { JoinRoomForm } from "@/components/rooms/join-room-form";

export default function JoinRoomPage() {
  return (
    <div className="mx-auto max-w-3xl animate-fade-up">
      <PageHeader
        eyebrow="রুম কোড"
        title="ঘরে যোগ দিন"
        subtitle="বন্ধুর রুম কোড লিখে ঢুকে পড়ো — বা সাম্প্রতিক রুম থেকে বেছে নাও।"
      />
      <JoinRoomForm />
    </div>
  );
}