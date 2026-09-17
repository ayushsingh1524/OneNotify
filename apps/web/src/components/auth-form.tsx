"use client";
import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { post } from "@/lib/api";
import { useSession } from "./providers";
import { Button } from "./ui/button";
import { Field, ErrorBox } from "./ui/shared";
import { Sprout, ArrowLeft, ArrowRight, ShieldCheck } from "lucide-react";
const schema = z.object({
  email: z.email("Enter a valid email address"),
  password: z.string().min(1, "Enter your password"),
  name: z.string().optional(),
});
export function AuthForm({
  mode,
}: {
  mode: "login" | "register" | "forgot-password" | "reset-password";
}) {
  const { signIn } = useSession();
  const router = useRouter();
  const search = useSearchParams();
  const [error, setError] = useState<unknown>();
  const [message, setMessage] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: mode === "reset-password" ? "reset@example.test" : "",
      password: mode === "forgot-password" ? "unused" : "",
      name: "",
    },
  });
  const submit = handleSubmit(async (data) => {
    setError(null);
    try {
      if (mode === "forgot-password") {
        const r = await post("/auth/forgot-password", { email: data.email });
        setMessage(r.message);
        return;
      }
      if (mode === "reset-password") {
        await post("/auth/reset-password", {
          token: search.get("token"),
          password: data.password,
        });
        setMessage("Your password is updated. You can now sign in.");
        return;
      }
      if (mode === "register" && data.password.length < 12)
        throw new Error("Use at least 12 characters for your password.");
      const result = await post(`/auth/${mode}`, data);
      if (result.user?.verificationRequired) {
        setMessage(
          "Check your email for a verification link before signing in. Local development mail is in Mailpit.",
        );
        return;
      }
      signIn(result);
      router.push("/app");
    } catch (e) {
      setError(e);
    }
  });
  return (
    <div className="auth-layout">
      <aside className="auth-aside">
        <Link className="brand" href="/">
          <span className="brand-icon">
            <Sprout />
          </span>
          OneNotify.
        </Link>
        <div>
          <span className="eyebrow">A LITTLE LESS TO CARRY</span>
          <h2>
            Space to breathe.
            <br />A place to begin.
          </h2>
          <p>
            We’ll help you organize what comes next, at a pace that feels right
            for you.
          </p>
          <div className="auth-plant">
            <Sprout size={150} strokeWidth={0.8} />
          </div>
        </div>
        <p className="auth-security">
          <ShieldCheck size={18} />
          Your information stays in your control.
        </p>
      </aside>
      <main id="main" className="auth-main">
        <Link className="back-link" href="/">
          <ArrowLeft size={16} />
          Back to OneNotify
        </Link>
        <div className="auth-form">
          <p className="eyebrow">WELCOME TO ONENOTIFY</p>
          <h1>
            {mode === "login"
              ? "Welcome back."
              : mode === "register"
                ? "Let’s begin, gently."
                : mode === "forgot-password"
                  ? "Reset your password."
                  : "Choose a new password."}
          </h1>
          <p>
            {mode === "login"
              ? "Your family workspace is right where you left it."
              : mode === "register"
                ? "Create your private workspace. You can add details later."
                : "We’ll help you get back into your workspace."}
          </p>
          <form onSubmit={submit}>
            {mode === "register" && (
              <Field label="Your name">
                <input
                  {...register("name")}
                  required
                  autoComplete="name"
                  maxLength={120}
                />
              </Field>
            )}
            {mode !== "reset-password" && (
              <Field label="Email address">
                <input
                  {...register("email")}
                  type="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                />
                {errors.email && (
                  <small className="field-error">{errors.email.message}</small>
                )}
              </Field>
            )}
            {mode !== "forgot-password" && (
              <Field
                label="Password"
                hint={mode === "register" ? "At least 12 characters." : ""}
              >
                <input
                  {...register("password")}
                  type="password"
                  minLength={mode === "login" ? 1 : 12}
                  maxLength={72}
                  autoComplete={
                    mode === "login" ? "current-password" : "new-password"
                  }
                />
                {errors.password && (
                  <small className="field-error">
                    {errors.password.message}
                  </small>
                )}
              </Field>
            )}
            {mode === "login" && (
              <Link className="text-link forgot" href="/forgot-password">
                Forgot password?
              </Link>
            )}
            <ErrorBox error={error} />
            {message && (
              <p className="success" role="status">
                {message}
              </p>
            )}
            <Button className="full" disabled={isSubmitting}>
              {isSubmitting
                ? "Please wait…"
                : mode === "login"
                  ? "Sign in"
                  : mode === "register"
                    ? "Create your account"
                    : "Continue"}
              <ArrowRight size={16} />
            </Button>
          </form>
          {mode === "login" && (
            <>
              <p className="auth-switch">
                New here? <Link href="/register">Create an account</Link>
              </p>
              <div className="demo-credentials">
                <strong>Just exploring?</strong>
                <p>Use the local demo account</p>
                <code>family@example.test</code>
                <code>Family-demo-2026!</code>
              </div>
            </>
          )}
          {mode !== "login" && (
            <p className="auth-switch">
              Already have an account? <Link href="/login">Sign in</Link>
            </p>
          )}
          <p className="fine-print">
            By continuing, you acknowledge our{" "}
            <Link href="/privacy">privacy information</Link>. Never share
            banking passwords, OTPs, or PINs.
          </p>
        </div>
      </main>
    </div>
  );
}
