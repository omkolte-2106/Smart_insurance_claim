const tone: Record<string, string> = {
  SUBMITTED: "bg-slate-100 text-slate-700 ring-slate-200/70",
  DOCUMENTS_UPLOADED: "bg-sky-50 text-sky-800 ring-sky-200/70",
  AI_VERIFICATION_IN_PROGRESS: "bg-amber-50 text-amber-900 ring-amber-200/70",
  AI_VERIFIED: "bg-emerald-50 text-emerald-900 ring-emerald-200/70",
  MANUAL_REVIEW_PENDING: "bg-indigo-50 text-indigo-900 ring-indigo-200/70",
  ADDITIONAL_DOCUMENTS_REQUIRED: "bg-orange-50 text-orange-900 ring-orange-200/70",
  APPROVED: "bg-teal-50 text-teal-900 ring-teal-200/70",
  REJECTED: "bg-rose-50 text-rose-900 ring-rose-200/70",
  FRAUD_FLAGGED: "bg-red-50 text-red-900 ring-red-200/70",
  PAYOUT_CALCULATED: "bg-cyan-50 text-cyan-900 ring-cyan-200/70",
  SETTLED: "bg-ink-900 text-white ring-ink-800",
};

export const StatusBadge: React.FC<{ status: string }> = ({ status }) => (
  <span
    className={`inline-flex items-center rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-wide ring-1 ring-inset ${
      tone[status] || "bg-slate-100 text-slate-700 ring-slate-200/70"
    }`}
  >
    {status.replace(/_/g, " ")}
  </span>
);
