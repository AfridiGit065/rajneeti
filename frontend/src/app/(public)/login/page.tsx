import Link from "next/link";
import { LoginForm } from "@/components/auth/login-form";
import { AuthShell } from "@/components/auth/auth-shell";
import { ArrowRight } from "@/components/ui/icons";

export default function LoginPage() {
  return (
    <AuthShell
      eyebrow="স্বাগতম · Welcome Back"
      title="লগইন"
      subtitle="ক্ষমতার আসনে আবার ফিরে এসো।"
      footer={
        <>
          অ্যাকাউন্ট নেই?{" "}
          <Link
            href="/register"
            className="inline-flex items-center gap-1 font-semibold text-gold-400 transition-colors hover:text-gold-300"
          >
            নিবন্ধন করো
            <ArrowRight className="size-3.5" aria-hidden />
          </Link>
        </>
      }
    >
      <LoginForm />
    </AuthShell>
  );
}