import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import { LayoutDashboard, Percent, Search, ShieldAlert, Sparkles, UserCircle } from "lucide-react";
import api from "../api/client";
import { AppShell } from "../components/layout/AppShell";
import { Card } from "../components/ui/Card";
import { ResponsiveContainer, ScatterChart, Scatter, XAxis, YAxis, Tooltip, CartesianGrid, ZAxis } from "recharts";

export const DiscountAnalyticsPage: React.FC<{ mode: "admin" | "company" }> = ({ mode }) => {
  const [rows, setRows] = useState<any[]>([]);

  const nav =
    mode === "admin"
      ? [
          { to: "/admin", label: "Overview", icon: LayoutDashboard },
          { to: "/fraud", label: "Fraud desk", icon: ShieldAlert },
          { to: "/search", label: "Vehicle search", icon: Search },
          { to: "/discounts", label: "Discount analytics", icon: Percent },
          { to: "/profile", label: "Account", icon: UserCircle },
        ]
      : [
          { to: "/company", label: "Overview", icon: LayoutDashboard },
          { to: "/search", label: "Vehicle search", icon: Search },
          { to: "/discounts/company", label: "Discount insights", icon: Percent },
          { to: "/profile", label: "Account", icon: UserCircle },
        ];

  useEffect(() => {
    api.get("/discounts/analytics").then((r) => setRows(r.data));
  }, []);

  const chartData = rows.map((d) => ({
    x: d.percentileRank,
    y: Number(d.suggestedDiscountPercent),
    z: 200,
    name: d.customer?.fullName ?? "Customer",
  }));

  const recompute = async () => {
    try {
      await api.post("/admin/discounts/recompute", null, { params: { topFraction: 0.15 } });
      toast.success("Ranking recomputed");
      const refreshed = await api.get("/discounts/analytics");
      setRows(refreshed.data);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Failed");
    }
  };

  return (
    <AppShell
      nav={nav}
      title="Discount intelligence"
      subtitle="Identify the best 10–20% of policyholders for renewal incentives—ML-ready when your ranking model ships."
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        {mode === "admin" ? (
          <button type="button" className="si-btn-primary px-5 py-2.5 text-sm" onClick={recompute}>
            <Sparkles className="h-4 w-4" />
            Recompute cohort
          </button>
        ) : (
          <div className="text-sm text-ink-500">Showing cohort members linked to policies you have issued.</div>
        )}
        <Link to={mode === "admin" ? "/admin" : "/company"} className="si-btn-ghost text-xs">
          ← Dashboard
        </Link>
      </div>

      <Card className="mt-6 p-4">
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <ScatterChart margin={{ top: 16, right: 16, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis type="number" dataKey="x" name="Percentile" unit="%" stroke="#94a3b8" tickLine={false} axisLine={false} />
              <YAxis type="number" dataKey="y" name="Discount %" unit="%" stroke="#94a3b8" tickLine={false} axisLine={false} />
              <ZAxis type="number" dataKey="z" range={[60, 400]} />
              <Tooltip cursor={{ strokeDasharray: "3 3" }} />
              <Scatter name="Customers" data={chartData} fill="#0d9488" />
            </ScatterChart>
          </ResponsiveContainer>
        </div>
      </Card>

      <Card className="mt-6 overflow-hidden p-0">
        <div className="border-b border-slate-100 bg-slate-50/60 px-6 py-4 text-sm font-semibold text-ink-950">Ranked cohort</div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="text-left text-[11px] font-semibold uppercase tracking-wide text-ink-500">
              <tr>
                <th className="px-6 py-3">Customer</th>
                <th className="px-6 py-3">Percentile</th>
                <th className="px-6 py-3">Suggested %</th>
                <th className="px-6 py-3">Campaign</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((d) => (
                <tr key={d.id} className="bg-white/80 hover:bg-slate-50/60">
                  <td className="px-6 py-4 font-semibold text-ink-950">{d.customer?.fullName ?? "—"}</td>
                  <td className="px-6 py-4 text-ink-700">{d.percentileRank?.toFixed?.(1)}</td>
                  <td className="px-6 py-4 text-ink-700">{d.suggestedDiscountPercent}</td>
                  <td className="px-6 py-4 text-xs text-ink-500">{d.campaignCode}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length && <div className="p-10 text-center text-sm text-ink-500">No cohort rows yet. Run recompute (admin).</div>}
        </div>
      </Card>
    </AppShell>
  );
};
