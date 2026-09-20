import { PageHeader } from "@/components/layout/page-header";
import { PageBackground } from "@/components/layout/page-background";
import { JoinRoomForm } from "@/components/rooms/join-room-form";

export default function JoinRoomPage() {
  return (
    <>
      <PageBackground variant="lobby" />
      <div className="mx-auto max-w-3xl animate-fade-up">
        <PageHeader
          eyebrow="Room Code"
          title="Join Room"
          subtitle="Enter a friend's room code or choose from active waiting rooms."
        />
        <JoinRoomForm />
      </div>
    </>
  );
}