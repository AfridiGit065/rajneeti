import Link from "next/link";
import { LoginForm } from "@/components/auth/login-form";
import { AuthShell } from "@/components/auth/auth-shell";
import { ArrowRight } from "@/components/ui/icons";

export default function LoginPage() {
  return (
    <AuthShell
      eyebrow="Welcome Back"
      title="Log In"
      subtitle="Reclaim your seat in the court of power."
      footer={
        <>
          Don&apos;t have an account?{" "}
          <Link
            href="/register"
            className="inline-flex items-center gap-1 font-semibold text-gold-400 transition-colors hover:text-gold-300"
          >
            Sign Up
            <ArrowRight className="size-3.5" aria-hidden />
          </Link>
        </>
      }
    >
      <LoginForm />
    </AuthShell>
  );
}