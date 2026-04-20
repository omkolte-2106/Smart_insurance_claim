import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import { Activity, Download, FileText, Sparkles, Wrench, AlertTriangle, CheckCircle2, ShieldAlert } from "lucide-react";
import api from "../../api/client";
import { Card } from "../../components/ui/Card";
import { StatusBadge } from "../../components/ui/StatusBadge";

const steps = [
  "SUBMITTED",
  "DOCUMENTS_UPLOADED",
  "AI_VERIFICATION_IN_PROGRESS",
  "AI_VERIFIED",
  "PAYOUT_CALCULATED",
  "MANUAL_REVIEW_PENDING",
  "APPROVED",
  "SETTLED",
];

const getFraudSeverity = (score: number) => {
  if (score > 70) return { label: "High", color: "text-red-600 bg-red-50 border-red-200", icon: ShieldAlert };
  if (score > 30) return { label: "Medium", color: "text-amber-600 bg-amber-50 border-amber-200", icon: AlertTriangle };
  return { label: "Low", color: "text-emerald-600 bg-emerald-50 border-emerald-200", icon: CheckCircle2 };
};

export const ClaimTrackingPage = () => {
  const { id } = useParams();
  const [claim, setClaim] = useState<any>(null);

  useEffect(() => {
    if (!id) return;
    api.get(`/claims/${id}`).then((r) => setClaim(r.data));
  }, [id]);

  const downloadSummary = async () => {
    if (!claim) return;
    try {
      const res = await api.get(`/claims/${claim.claimPublicId}/export`, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const a = document.createElement("a");
      a.href = url;
      a.download = `${claim.claimPublicId}.txt`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error("Unable to download summary");
    }
  };

  if (!claim) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas text-sm text-ink-500">
        Loading claim…
      </div>
    );
  }

  const idx = steps.includes(claim.status) ? steps.indexOf(claim.status) : steps.indexOf("MANUAL_REVIEW_PENDING");

  return (
    <div className="min-h-screen bg-mesh-light bg-hero-fade">
      <div className="border-b border-white/60 bg-white/70 backdrop-blur-md">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-4">
          <Link to="/customer" className="text-sm font-semibold text-brand-700 hover:text-brand-600">
            ← Dashboard
          </Link>
          <button type="button" className="si-btn-secondary px-4 py-2 text-xs" onClick={downloadSummary}>
            <Download className="h-4 w-4" />
            Download summary
          </button>
        </div>
      </div>

      <div className="mx-auto max-w-4xl px-4 py-10">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Claim reference</div>
            <h1 className="mt-1 font-display text-3xl font-semibold tracking-tight text-ink-950">{claim.claimPublicId}</h1>
            <p className="mt-2 text-sm text-ink-600">{claim.companyName}</p>
          </div>
          <StatusBadge status={claim.status} />
        </div>

        <div className="mt-8 grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <Card className="p-6">
            <div className="flex items-center gap-2 text-sm font-semibold text-ink-950">
              <Activity className="h-4 w-4 text-brand-600" />
              Milestone tracker
            </div>
            <div className="relative mt-6 space-y-0">
              {steps.map((s, i) => {
                const active = i <= idx;
                return (
                  <div key={s} className="relative flex gap-4 pb-6 last:pb-0">
                    {i !== steps.length - 1 && (
                      <div
                        className={`absolute left-[15px] top-8 h-[calc(100%-8px)] w-px ${
                          active ? "bg-gradient-to-b from-brand-400 to-slate-200" : "bg-slate-200"
                        }`}
                      />
                    )}
                    <div
                      className={`relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
                        active ? "bg-brand-600 text-white shadow-glow" : "bg-slate-200 text-ink-500"
                      }`}
                    >
                      {i + 1}
                    </div>
                    <div className="min-w-0 pt-0.5">
                      <div className={`text-sm font-semibold ${active ? "text-ink-950" : "text-ink-400"}`}>
                        {s.replace(/_/g, " ")}
                      </div>
                      <div className="text-xs text-ink-500">{i === idx ? "You are here" : i < idx ? "Completed" : "Pending"}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>

          <div className="space-y-6">
            <Card className="p-6">
              <div className="flex items-center gap-2 text-sm font-semibold text-ink-950">
                <Sparkles className="h-4 w-4 text-accent-600" />
                AI signals
              </div>
              <dl className="mt-4 space-y-3 text-sm">
                <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-slate-50/50 px-3 py-2">
                  <dt className="text-ink-500">Fraud severity</dt>
                  <dd>
                    {claim.fraudScore != null ? (
                      (() => {
                        const sev = getFraudSeverity(claim.fraudScore);
                        return (
                          <div className={`flex items-center gap-1.5 px-2 py-0.5 rounded-full border text-[11px] font-bold uppercase tracking-wider ${sev.color}`}>
                            <sev.icon className="h-3 w-3" />
                            {sev.label}
                          </div>
                        );
                      })()
                    ) : "—"}
                  </dd>
                </div>
                <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-slate-50/50 px-3 py-2">
                  <dt className="text-ink-500">Damage severity</dt>
                  <dd className="font-semibold text-ink-950">{claim.damageSeverityScore != null ? claim.damageSeverityScore.toFixed(2) : "—"}</dd>
                </div>
                <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-slate-50/50 px-3 py-2">
                  <dt className="text-ink-500">Estimated payout</dt>
                  <dd className="font-semibold text-ink-950">
                    {claim.estimatedPayoutAmount != null ? `₹${Number(claim.estimatedPayoutAmount).toLocaleString("en-IN")}` : "—"}
                  </dd>
                </div>
              </dl>
            </Card>

            {claim.damagedParts && claim.damagedParts.length > 0 && (
              <Card className="p-6 border-brand-100 bg-brand-50/20 shadow-sm">
                <div className="flex items-center gap-2 text-sm font-semibold text-ink-950">
                  <Wrench className="h-4 w-4 text-brand-600" />
                  Detected damaged parts
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {claim.damagedParts.map((part: string) => (
                    <span key={part} className="si-badge-neutral px-3 py-1 ring-1 ring-slate-200">
                      {part}
                    </span>
                  ))}
                </div>
                <p className="mt-3 text-[10px] text-ink-400 italic">
                  * Automated detection via AI part-damage-model
                </p>
              </Card>
            )}

            <Card className="p-6">
              <div className="flex items-center gap-2 text-sm font-semibold text-ink-950">
                <FileText className="h-4 w-4 text-brand-600" />
                Documents
              </div>
              <ul className="mt-4 space-y-2 text-sm text-ink-700">
                {claim.documents?.map((d: any) => (
                  <li key={d.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-white/70 px-3 py-2">
                    <span className="font-medium">{d.documentType.replace(/_/g, " ")}</span>
                    <span className="text-xs text-ink-500">{d.verificationStatus}</span>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};
