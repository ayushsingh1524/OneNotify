"use client";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import { api, post, recordCaseCreation } from "@/lib/api";
import { Button } from "./ui/button";
import { Heading, Field, ErrorBox } from "./ui/shared";
import {
  ArrowRight,
  ArrowLeft,
  Check,
  Save,
  Sprout,
  FileUp,
} from "lucide-react";
const schema = z.object({
  fullName: z.string().min(2, "Enter their full name").max(120),
  dateOfBirth: z.string().optional(),
  dateOfDeath: z.string().min(1, "Choose a date"),
  city: z.string().min(1, "Enter a city"),
  state: z.string().min(1, "Enter a state"),
  relationship: z.string().min(1, "Choose your relationship"),
  panLastFour: z
    .string()
    .regex(/^([A-Za-z0-9]{4})?$/, "Enter only the last four characters")
    .optional(),
  aadhaarLastFour: z
    .string()
    .regex(/^(\d{4})?$/, "Enter only the last four digits")
    .optional(),
  mobileNumbers: z.string().optional(),
  emailAddresses: z.string().optional(),
  knownServices: z.string().optional(),
});
type Values = z.infer<typeof schema>;
const steps = [
  "Who you’re helping",
  "A few details",
  "Documents",
  "Known services",
  "Ready to begin",
];
export function CaseWizard({ caseId }: { caseId?: string }) {
  const [step, setStep] = useState(0);
  const [error, setError] = useState<unknown>();
  const [saved, setSaved] = useState("");
  const [started] = useState(() => Date.now());
  const [file, setFile] = useState<File | null>(null);
  const router = useRouter();
  const client = useQueryClient();
  const {
    register,
    handleSubmit,
    trigger,
    getValues,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      fullName: "",
      dateOfBirth: "",
      dateOfDeath: "",
      city: "",
      state: "",
      relationship: "",
      panLastFour: "",
      aadhaarLastFour: "",
      mobileNumbers: "",
      emailAddresses: "",
      knownServices: "",
    },
  });
  useEffect(() => {
    api(caseId ? `/cases/${caseId}` : "/onboarding")
      .then((d) => {
        if (caseId) {
          reset({
            fullName: d.full_name,
            dateOfBirth: d.date_of_birth || "",
            dateOfDeath: d.date_of_death,
            city: d.city,
            state: d.state,
            relationship: d.relationship,
            panLastFour: d.pan_last_four || "",
            aadhaarLastFour: d.aadhaar_last_four || "",
            mobileNumbers: d.mobile_numbers,
            emailAddresses: d.email_addresses,
            knownServices: d.known_services,
          });
        } else if (d.values) {
          reset(d.values);
          setStep(Math.min(d.step || 0, 4));
        }
      })
      .catch(setError);
  }, [caseId, reset]);
  const fullName = useWatch({ control, name: "fullName" });
  const next = async () => {
    const keys: (keyof Values)[] =
      step === 0
        ? ["fullName", "relationship"]
        : step === 1
          ? ["dateOfDeath", "city", "state", "panLastFour", "aadhaarLastFour"]
          : [];
    if (await trigger(keys)) setStep(Math.min(step + 1, 4));
  };
  const submit = handleSubmit(async (data) => {
    setError(null);
    try {
      const payload = { ...data, dateOfBirth: data.dateOfBirth || null };
      const result = caseId
        ? await api(`/cases/${caseId}`, {
            method: "PUT",
            body: JSON.stringify(payload),
          })
        : await post("/cases", payload);
      if (!caseId) {
        await api("/onboarding", { method: "DELETE" });
        await recordCaseCreation(started);
      }
      let uploadError = "";
      if (file) {
        const form = new FormData();
        form.append("file", file);
        form.append("category", "DEATH_CERTIFICATE");
        try {
          await api(`/cases/${result.id}/documents`, {
            method: "POST",
            body: form,
          });
        } catch (e) {
          uploadError =
            e instanceof Error ? e.message : "Document upload failed";
        }
      }
      await client.invalidateQueries();
      if (uploadError) {
        setSaved(`Case created. Upload needs another try: ${uploadError}`);
        router.push(`/app/cases/${result.id}/documents`);
      } else router.push(`/app/cases/${result.id}`);
    } catch (e) {
      setError(e);
    }
  });
  return (
    <>
      <Heading
        eyebrow={caseId ? "FAMILY DETAILS" : "A PLACE TO BEGIN"}
        title={
          caseId
            ? "Your family member’s details."
            : "We’ll take this one step at a time."
        }
        description="Only share what you know. You can return to the rest later."
      />
      <div className="wizard-layout">
        <aside className="wizard-steps">
          {steps.map((s, i) => (
            <button
              key={s}
              className={step === i ? "current" : step > i ? "finished" : ""}
              onClick={() => i < step && setStep(i)}
              disabled={i > step}
            >
              <span>{step > i ? <Check size={15} /> : i + 1}</span>
              {s}
            </button>
          ))}
          <div className="wizard-note">
            <Sprout size={25} />
            <p>You don’t need to have all the answers today.</p>
          </div>
        </aside>
        <section className="panel wizard-panel">
          <span className="eyebrow">STEP {step + 1} OF 5</span>
          <form onSubmit={submit}>
            {step === 0 && (
              <>
                <h2>Who are you helping?</h2>
                <p>Tell us a little about your connection.</p>
                <Field label="Your family member’s full name">
                  <input
                    {...register("fullName")}
                    placeholder="As it appears on their documents"
                    aria-invalid={!!errors.fullName}
                  />
                  {errors.fullName && (
                    <small className="field-error">
                      {errors.fullName.message}
                    </small>
                  )}
                </Field>
                <Field label="Your relationship to them">
                  <select {...register("relationship")}>
                    <option value="">Choose your relationship</option>
                    {[
                      "Spouse",
                      "Child",
                      "Parent",
                      "Sibling",
                      "Nominee",
                      "Legal heir",
                      "Authorized representative",
                    ].map((x) => (
                      <option key={x}>{x}</option>
                    ))}
                  </select>
                  {errors.relationship && (
                    <small className="field-error">
                      {errors.relationship.message}
                    </small>
                  )}
                </Field>
              </>
            )}
            {step === 1 && (
              <>
                <h2>Tell us about your family member.</h2>
                <p>These details help prepare your organization requests.</p>
                <div className="form-grid">
                  <Field label="Date of death">
                    <input
                      {...register("dateOfDeath")}
                      type="date"
                      max={new Date().toISOString().slice(0, 10)}
                    />
                    {errors.dateOfDeath && (
                      <small className="field-error">
                        {errors.dateOfDeath.message}
                      </small>
                    )}
                  </Field>
                  <Field label="Date of birth (optional)">
                    <input
                      {...register("dateOfBirth")}
                      type="date"
                      max={new Date().toISOString().slice(0, 10)}
                    />
                  </Field>
                  <Field label="City">
                    <input {...register("city")} />
                    {errors.city && (
                      <small className="field-error">
                        {errors.city.message}
                      </small>
                    )}
                  </Field>
                  <Field label="State / union territory">
                    <input {...register("state")} />
                    {errors.state && (
                      <small className="field-error">
                        {errors.state.message}
                      </small>
                    )}
                  </Field>
                </div>
                <details>
                  <summary>Optional contact and identity references</summary>
                  <p className="muted">
                    Only the last four characters. Never enter a full Aadhaar or
                    PAN number.
                  </p>
                  <div className="form-grid">
                    <Field label="PAN last four">
                      <input {...register("panLastFour")} maxLength={4} />
                    </Field>
                    <Field label="Aadhaar last four">
                      <input
                        {...register("aadhaarLastFour")}
                        maxLength={4}
                        inputMode="numeric"
                      />
                    </Field>
                    <Field label="Known mobile numbers">
                      <input {...register("mobileNumbers")} maxLength={500} />
                    </Field>
                    <Field label="Known email addresses">
                      <input {...register("emailAddresses")} maxLength={500} />
                    </Field>
                  </div>
                </details>
              </>
            )}
            {step === 2 && (
              <>
                <h2>Upload the death certificate when you’re ready.</h2>
                <p>It’s okay to skip this step. We’ll keep a place for it.</p>
                <label className="upload-zone">
                  <FileUp size={32} />
                  <strong>
                    {file ? file.name : "Choose a death certificate"}
                  </strong>
                  <span>PDF, PNG, or JPG · up to 10 MB</span>
                  <input
                    type="file"
                    accept="application/pdf,image/png,image/jpeg"
                    onChange={(e) => setFile(e.target.files?.[0] || null)}
                  />
                </label>
                <p className="notice">
                  Local demo scanning is a placeholder. Use sample documents in
                  this demonstration.
                </p>
              </>
            )}
            {step === 3 && (
              <>
                <h2>Which services do you already know about?</h2>
                <p>
                  Banks, insurers, investments, mobile services, or an employer.
                  A rough list is enough.
                </p>
                <Field label="Known organizations (optional)">
                  <textarea
                    {...register("knownServices")}
                    rows={5}
                    maxLength={2000}
                    placeholder="For example: SBI, LIC, Airtel…"
                  />
                </Field>
                <div className="notice">
                  After creating the case, use Find accounts to add these
                  organizations to your checklist. We won’t assume account
                  ownership.
                </div>
              </>
            )}
            {step === 4 && (
              <>
                <h2>We’ll help you build the rest of the checklist.</h2>
                <p>
                  Your workspace for <strong>{fullName}</strong> is ready to
                  begin.
                </p>
                <div className="review-list">
                  <p>
                    <Check size={17} />A shared family case
                  </p>
                  <p>
                    <Check size={17} />A private document vault
                  </p>
                  <p>
                    <Check size={17} />A clear next step for each organization
                  </p>
                </div>
                <div className="notice">
                  OneNotify helps you organize and prepare. Providers decide
                  their own requirements and approvals. Nothing is sent without
                  your review.
                </div>
              </>
            )}
            <ErrorBox error={error} />
            {saved && (
              <p className="success" role="status">
                {saved}
              </p>
            )}
            <div className="wizard-actions">
              {step > 0 && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setStep(step - 1)}
                >
                  <ArrowLeft size={16} />
                  Back
                </Button>
              )}
              {!caseId && (
                <Button
                  type="button"
                  variant="ghost"
                  onClick={async () => {
                    try {
                      await api("/onboarding", {
                        method: "PUT",
                        body: JSON.stringify({ values: getValues(), step }),
                      });
                      setSaved(
                        "Draft saved securely. You can come back to it.",
                      );
                    } catch (e) {
                      setError(e);
                    }
                  }}
                >
                  <Save size={15} />
                  Save for later
                </Button>
              )}
              {step < 4 ? (
                <Button
                  key="wizard-next"
                  type="button"
                  onClick={(event) => {
                    event.preventDefault();
                    void next();
                  }}
                >
                  Continue <ArrowRight size={16} />
                </Button>
              ) : (
                <Button
                  key="wizard-submit"
                  type="submit"
                  disabled={isSubmitting}
                >
                  {isSubmitting
                    ? "Saving…"
                    : caseId
                      ? "Save changes"
                      : "Create your case"}
                  <ArrowRight size={16} />
                </Button>
              )}
            </div>
          </form>
        </section>
      </div>
    </>
  );
}
