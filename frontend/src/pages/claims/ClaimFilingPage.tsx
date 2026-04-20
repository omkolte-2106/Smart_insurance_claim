import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { Check, FileText, Sparkles, UploadCloud } from "lucide-react";
import api from "../../api/client";
import { Card } from "../../components/ui/Card";

const docTypes = ["AADHAAR", "DRIVING_LICENCE", "PUC_CERTIFICATE", "VEHICLE_DAMAGE_PHOTO"] as const;

const steps = ["Draft", "Documents", "AI review"];

export const ClaimFilingPage = () => {
  const navigate = useNavigate();
  const [policies, setPolicies] = useState<any[]>([]);
  const [policyId, setPolicyId] = useState<number | null>(null);
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [claimId, setClaimId] = useState<string | null>(null);
  const [files, setFiles] = useState<Record<string, File | undefined>>({});
  const [step, setStep] = useState(0);

  useEffect(() => {
    api.get("/customer/policies").then((r) => setPolicies(r.data));
  }, []);

  useEffect(() => {
    if (claimId) setStep(1);
  }, [claimId]);

  const create = async () => {
    if (!policyId) {
      toast.error("Select a policy");
      return;
    }
    const { data } = await api.post("/claims", {
      policyId,
      incidentDescription: description,
      incidentLocation: location,
    });
    setClaimId(data.claimPublicId);
    toast.success("Claim created");
  };

  const uploadAll = async () => {
    if (!claimId) return;
    try {
      for (const type of docTypes) {
        const file = files[type];
        if (!file) {
          toast.error(`Please attach ${type.replace(/_/g, " ").toLowerCase()}`);
          return;
        }
        const fd = new FormData();
        fd.append("file", file);
        await api.post(`/claims/${claimId}/documents?type=${type}`, fd, {
          headers: { "Content-Type": "multipart/form-data" },
        });
      }
      toast.success("Documents uploaded");
      setStep(2);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Failed to upload documents";
      toast.error(msg);
      console.error("Upload error:", err);
    }
  };

  const runAi = async () => {
    if (!claimId) return;
    try {
      await api.post(`/claims/${claimId}/submit-ai`);
      toast.success("AI pipeline triggered");
      navigate(`/claims/${claimId}`);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Failed to process AI review";
      toast.error(msg);
      console.error("AI review error:", err);
    }
  };

  return (
    <div className="min-h-screen bg-mesh-light bg-hero-fade">
      <div className="border-b border-white/60 bg-white/70 backdrop-blur-md">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-4">
          <Link to="/customer" className="text-sm font-semibold text-brand-700 hover:text-brand-600">
            ← Dashboard
          </Link>
          {claimId && <span className="si-pill font-mono text-[11px] text-ink-700">{claimId}</span>}
        </div>
      </div>

      <div className="mx-auto max-w-3xl px-4 py-10">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="font-display text-3xl font-semibold tracking-tight text-ink-950">File a motor claim</h1>
            <p className="mt-2 text-sm text-ink-600">Guided intake with the evidence pack insurers expect before AI scoring.</p>
          </div>
        </div>

        <div className="mt-8 flex items-center gap-3">
          {steps.map((label, i) => (
            <div key={label} className="flex flex-1 items-center gap-2">
              <div
                className={`flex h-9 w-9 items-center justify-center rounded-full text-xs font-bold ${
                  i <= step ? "bg-brand-600 text-white shadow-glow" : "bg-slate-200 text-ink-500"
                }`}
              >
                {i < step ? <Check className="h-4 w-4" /> : i + 1}
              </div>
              <div className="hidden min-w-0 sm:block">
                <div className={`text-xs font-semibold uppercase tracking-wide ${i <= step ? "text-brand-800" : "text-ink-400"}`}>
                  Step {i + 1}
                </div>
                <div className="truncate text-sm font-semibold text-ink-900">{label}</div>
              </div>
              {i < steps.length - 1 && <div className="mx-2 hidden h-px flex-1 bg-gradient-to-r from-slate-200 to-transparent sm:block" />}
            </div>
          ))}
        </div>

        <Card elevated className="mt-8 p-8 shadow-glow">
          {!claimId ? (
            <div className="space-y-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-ink-950">
                <FileText className="h-4 w-4 text-brand-600" />
                Incident details
              </div>
              <div>
                <label className="si-label" htmlFor="policy">
                  Policy
                </label>
                <select
                  id="policy"
                  className="si-input"
                  value={policyId ?? ""}
                  onChange={(e) => setPolicyId(e.target.value ? Number(e.target.value) : null)}
                >
                  <option value="">Select an active policy</option>
                  {policies.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.policyNumber} · sum insured ₹{Number(p.sumInsured).toLocaleString("en-IN")}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="si-label" htmlFor="desc">
                  What happened?
                </label>
                <textarea id="desc" className="si-input min-h-[120px]" value={description} onChange={(e) => setDescription(e.target.value)} required />
              </div>
              <div>
                <label className="si-label" htmlFor="loc">
                  Location (optional)
                </label>
                <input id="loc" className="si-input" value={location} onChange={(e) => setLocation(e.target.value)} />
              </div>
              <button type="button" className="si-btn-primary w-full py-3 text-sm" onClick={create}>
                Create draft claim
              </button>
            </div>
          ) : (
            <div className="space-y-6">
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-brand-100 bg-brand-50/60 p-4">
                <div className="text-sm text-brand-950">
                  <span className="font-semibold">Evidence pack</span> · Upload clear scans or photos (PDF/JPG/PNG, max 10MB each).
                </div>
                <UploadCloud className="h-5 w-5 text-brand-700" />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                {docTypes.map((type) => (
                  <div key={type} className="rounded-xl border border-slate-200/80 bg-slate-50/40 p-4">
                    <div className="text-xs font-semibold uppercase tracking-wide text-ink-600">{type.replace(/_/g, " ")}</div>
                    <label className="mt-3 flex cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-white/80 px-3 py-8 text-center text-xs text-ink-500 transition hover:border-brand-300 hover:bg-brand-50/30">
                      <input
                        type="file"
                        className="hidden"
                        onChange={(e) => setFiles({ ...files, [type]: e.target.files?.[0] })}
                      />
                      <UploadCloud className="mb-2 h-6 w-6 text-ink-400" />
                      <span className="font-semibold text-ink-800">Click to upload</span>
                      <span className="mt-1 text-[11px]">{files[type]?.name ?? "No file selected"}</span>
                    </label>
                  </div>
                ))}
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <button type="button" className="si-btn-secondary flex-1 justify-center py-3 text-sm" onClick={uploadAll}>
                  Upload documents
                </button>
                <button type="button" className="si-btn-primary flex-1 justify-center py-3 text-sm" onClick={runAi}>
                  <Sparkles className="h-4 w-4" />
                  Submit for AI review
                </button>
              </div>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};
