import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { LayoutDashboard, Percent, Search, ShieldAlert, UserCircle } from "lucide-react";
import api from "../api/client";
import { AppShell } from "../components/layout/AppShell";
import { Card } from "../components/ui/Card";
import { StatusBadge } from "../components/ui/StatusBadge";

export const FraudReviewPage = () => {
  const [rows, setRows] = useState<any[]>([]);

  useEffect(() => {
    api.get("/admin/fraud/claims?size=50").then((r) => setRows(r.data.content ?? r.data));
  }, []);

  const nav = [
    { to: "/admin", label: "Overview", icon: LayoutDashboard },
    { to: "/fraud", label: "Fraud desk", icon: ShieldAlert },
    { to: "/search", label: "Vehicle search", icon: Search },
    { to: "/discounts", label: "Discount analytics", icon: Percent },
    { to: "/profile", label: "Account", icon: UserCircle },
  ];

  return (
    <AppShell
      nav={nav}
      title="Fraud intelligence"
      subtitle="Claims with elevated AI suspicion or manual escalations. Pair this view with your investigation runbooks."
    >
      <Card className="overflow-hidden p-0">
        <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/60 px-6 py-4">
          <div>
            <div className="text-sm font-semibold text-ink-950">Flagged portfolio</div>
            <div className="text-xs text-ink-500">Sorted by API default (paginate as you grow data)</div>
          </div>
          <Link to="/admin" className="si-btn-ghost text-xs">
            ← Admin home
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-white text-left text-[11px] font-semibold uppercase tracking-wide text-ink-500">
              <tr>
                <th className="px-6 py-3">Claim</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Fraud score</th>
                <th className="px-6 py-3">Insurer</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((c) => (
                <tr key={c.claimPublicId} className="bg-white/80 hover:bg-slate-50/60">
                  <td className="px-6 py-4 font-semibold text-ink-950">{c.claimPublicId}</td>
                  <td className="px-6 py-4">
                    <StatusBadge status={c.status} />
                  </td>
                  <td className="px-6 py-4 text-ink-700">{c.fraudScore != null ? c.fraudScore.toFixed(1) : "—"}</td>
                  <td className="px-6 py-4 text-ink-600">{c.companyName}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length && <div className="p-10 text-center text-sm text-ink-500">No fraud-flagged claims.</div>}
        </div>
      </Card>
    </AppShell>
  );
};
