import type { LucideIcon } from "lucide-react";

type Props = {
  title: string;
  value: string | number;
  hint?: string;
  icon: LucideIcon;
  accent?: "brand" | "accent" | "slate";
};

const accents = {
  brand: "from-brand-500/15 to-brand-600/5 text-brand-700 ring-brand-200/60",
  accent: "from-accent-500/15 to-accent-600/5 text-accent-600 ring-blue-200/60",
  slate: "from-slate-500/10 to-slate-600/5 text-slate-700 ring-slate-200/60",
};

export const KpiCard: React.FC<Props> = ({ title, value, hint, icon: Icon, accent = "brand" }) => (
  <div className="group relative overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-5 shadow-soft transition hover:-translate-y-0.5 hover:shadow-md">
    <div
      className={`pointer-events-none absolute -right-6 -top-6 h-24 w-24 rounded-full bg-gradient-to-br ${accents[accent]} opacity-90 ring-1`}
    />
    <div className="relative flex items-start justify-between gap-3">
      <div>
        <div className="text-[11px] font-semibold uppercase tracking-wider text-ink-500">{title}</div>
        <div className="mt-2 font-display text-3xl font-semibold tracking-tight text-ink-950">{value}</div>
        {hint && <div className="mt-2 text-sm text-ink-500">{hint}</div>}
      </div>
      <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-2.5 text-ink-700 shadow-inner">
        <Icon className="h-5 w-5" strokeWidth={1.75} />
      </div>
    </div>
  </div>
);
