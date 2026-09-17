"use client";
import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { post } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { ErrorBox } from "@/components/ui/shared";
function Verify() {
  const query = useSearchParams();
  const [done, setDone] = useState(false);
  const [error, setError] = useState<unknown>();
  return (
    <main id="main" className="legal-page">
      <h1>Verify your email.</h1>
      <p>
        Confirm access to this email before joining your family’s workspace.
      </p>
      <ErrorBox error={error} />
      {done ? (
        <Link className="text-link" href="/login">
          Email verified. Continue to sign in.
        </Link>
      ) : (
        <Button
          onClick={async () => {
            try {
              await post("/auth/verify-email", { token: query.get("token") });
              setDone(true);
            } catch (e) {
              setError(e);
            }
          }}
        >
          Verify email
        </Button>
      )}
    </main>
  );
}
export default function Page() {
  return (
    <Suspense>
      <Verify />
    </Suspense>
  );
}
