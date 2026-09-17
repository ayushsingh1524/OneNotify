"use client";
/* Vault originals must be displayed as authenticated blob URLs, without an image optimizer. */
/* eslint-disable @next/next/no-img-element */
import { useEffect, useState, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, post, categories, date, label, download } from "@/lib/api";
import { Button } from "./ui/button";
import { Heading, Field, ErrorBox, Loading, Empty } from "./ui/shared";
import {
  FileUp,
  FileText,
  ShieldCheck,
  Eye,
  Download,
  ScanSearch,
  Trash2,
  X,
} from "lucide-react";
export function Documents({ caseId }: { caseId: string }) {
  const client = useQueryClient();
  const fileInput = useRef<HTMLInputElement>(null);
  const previewDialog = useRef<HTMLDialogElement>(null);
  const [expiresOn, setExpiresOn] = useState("");
  const [category, setCategory] = useState("DEATH_CERTIFICATE");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState("");
  const [message, setMessage] = useState("");
  const [preview, setPreview] = useState<{
    url: string;
    mime: string;
    name: string;
  } | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  useEffect(() => {
    if (preview) previewDialog.current?.showModal();
  }, [preview]);
  const q = useQuery({
    queryKey: ["documents", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/documents`),
  });
  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview.url);
    },
    [preview],
  );
  const run = async (key: string, fn: () => Promise<void>) => {
    setError(null);
    setMessage("");
    setBusy(key);
    try {
      await fn();
      await client.invalidateQueries();
    } catch (e) {
      setError(e);
    } finally {
      setBusy("");
    }
  };
  const read = async (d: any, view: boolean) => {
    const { ticket } = await post(`/documents/${d.id}/access`);
    const blob = await api<Blob>(`/documents/${d.id}/content?ticket=${ticket}`);
    if (view)
      setPreview({
        url: URL.createObjectURL(blob),
        mime: d.mime_type,
        name: d.filename,
      });
    else download(blob, d.filename);
  };
  return (
    <>
      <Heading
        eyebrow="UPLOAD ONCE, USE WHEN NEEDED"
        title="Your document vault."
        description="A private home for the documents your family needs. Originals are kept unchanged."
      />
      <div className="notice">
        <ShieldCheck size={18} />
        Files are encrypted before storage. Opening or downloading a document
        creates an access record. Demo scanning is not production antivirus; use
        sample files.
      </div>
      <ErrorBox error={error || q.error} />
      {message && (
        <p className="success" role="status">
          {message}
        </p>
      )}
      <section className="panel upload-panel">
        <div>
          <h2>Add a document</h2>
          <p>PDF, PNG, or JPG · up to 10 MB</p>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            run("upload", async () => {
              if (!file) throw new Error("Choose a file first.");
              const data = new FormData();
              data.append("file", file);
              data.append("category", category);
              if (expiresOn) data.append("expiresOn", expiresOn);
              await api(`/cases/${caseId}/documents`, {
                method: "POST",
                body: data,
              });
              setFile(null);
              if (fileInput.current) fileInput.current.value = "";
              setMessage("Document added to your encrypted vault.");
            });
          }}
        >
          <Field label="Document type">
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              {categories.map((c) => (
                <option key={c} value={c}>
                  {label(c)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Choose a document">
            <input
              ref={fileInput}
              type="file"
              required
              accept="application/pdf,image/png,image/jpeg"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </Field>
          <Field label="Expiry date (if applicable)">
            <input
              type="date"
              value={expiresOn}
              min={new Date().toISOString().slice(0, 10)}
              onChange={(e) => setExpiresOn(e.target.value)}
            />
          </Field>
          <Button disabled={!!busy}>
            <FileUp size={17} />
            {busy === "upload" ? "Uploading…" : "Upload document"}
          </Button>
        </form>
      </section>
      {q.isLoading ? (
        <Loading />
      ) : q.data?.length ? (
        <div className="panel">
          <div className="panel-heading">
            <h2>Saved documents</h2>
            <span className="muted">
              {q.data.filter((d) => d.status === "AVAILABLE").length} available
            </span>
          </div>
          {q.data.map((d) => (
            <div className="document-row" key={d.id}>
              <span className="document-icon">
                <FileText size={24} />
              </span>
              <div className="document-description">
                <strong>{d.filename}</strong>
                <p>
                  {label(d.category)} · Version {d.version} ·{" "}
                  {(d.size_bytes / 1024).toFixed(1)} KB
                </p>
                <small>
                  {date(d.uploaded_at)} ·{" "}
                  {d.is_original ? "Original" : "Derived"} · {label(d.status)}
                </small>
                <details>
                  <summary>Integrity & storage details</summary>
                  <p className="checksum">SHA-256: {d.checksum}</p>
                  <p>
                    Encrypted with AES-256-GCM. Demo signature check only; OCR
                    is not configured.
                  </p>
                </details>
              </div>
              {d.status === "AVAILABLE" && (
                <div className="document-actions">
                  <button
                    aria-label={`Preview ${d.filename}`}
                    className="icon-button"
                    disabled={!!busy}
                    onClick={() => run(d.id, () => read(d, true))}
                  >
                    <Eye size={17} />
                  </button>
                  <button
                    aria-label={`Download ${d.filename}`}
                    className="icon-button"
                    disabled={!!busy}
                    onClick={() => run(d.id, () => read(d, false))}
                  >
                    <Download size={17} />
                  </button>
                  <button
                    aria-label={`Analyze ${d.filename}`}
                    className="icon-button"
                    disabled={!!busy}
                    onClick={() =>
                      run(d.id, async () => {
                        const result = await post(`/documents/${d.id}/analyze`);
                        setMessage(
                          `Suggested type: ${label(result.classification.suggestedCategory)}. Review required. ${result.metadata.message}`,
                        );
                      })
                    }
                  >
                    <ScanSearch size={17} />
                  </button>
                  <button
                    aria-label={`Request deletion of ${d.filename}`}
                    className="icon-button"
                    onClick={() => setDeleting(d.id)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              )}
              {deleting === d.id && (
                <div className="inline-confirm">
                  <p>
                    Request deletion? This removes the document from future
                    request packages. Retention review is required before
                    permanent erasure.
                  </p>
                  <Button
                    variant="destructive"
                    disabled={!!busy}
                    onClick={() =>
                      run(d.id, async () => {
                        await api(`/documents/${d.id}`, { method: "DELETE" });
                        setDeleting(null);
                      })
                    }
                  >
                    Request deletion
                  </Button>
                  <Button variant="ghost" onClick={() => setDeleting(null)}>
                    Keep document
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <Empty
          title="A safe place, ready when you are."
          body="Add your first sample document above. You can reuse it across organization requests."
        />
      )}
      {preview && (
        <dialog
          ref={previewDialog}
          className="preview-modal"
          aria-modal="true"
          aria-label="Document preview"
          onClose={() => setPreview(null)}
          onKeyDown={(e) => {
            if (e.key === "Escape") setPreview(null);
          }}
        >
          <header>
            <h2>{preview.name}</h2>
            <Button
              variant="secondary"
              autoFocus
              onClick={() => setPreview(null)}
            >
              <X size={17} />
              Close
            </Button>
          </header>
          {preview.mime === "application/pdf" ? (
            <iframe sandbox="" title="PDF document preview" src={preview.url} />
          ) : (
            <img src={preview.url} alt={preview.name} />
          )}
        </dialog>
      )}
    </>
  );
}
