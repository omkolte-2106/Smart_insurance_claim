import { useEffect, useState } from "react";
import {
  Building2,
  LayoutDashboard,
  Percent,
  Search,
  ShieldAlert,
  UserCircle,
  Users,
} from "lucide-react";
import api from "../../api/client";
import { AppShell } from "../../components/layout/AppShell";
import { KpiCard } from "../../components/ui/KpiCard";
import { Card } from "../../components/ui/Card";
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from "recharts";

type Dashboard = {
  totalCustomers: number;
  totalCompanies: number;
  totalClaims: number;
  pendingCompanyApprovals: number;
  pendingClaims: number;
  fraudFlaggedClaims: number;
  bannedCustomers: number;
  bannedCompanies: number;
  discountEligibleCustomers: number;
};

export const AdminDashboard = () => {
  const [data, setData] = useState<Dashboard | null>(null);
  const [pending, setPending] = useState<any[]>([]);

  useEffect(() => {
    (async () => {
      const [dash, pend] = await Promise.all([api.get("/admin/dashboard"), api.get("/admin/companies/pending?size=20")]);
      setData(dash.data);
      setPending(pend.data.content ?? pend.data);
    })().catch(() => {});
  }, []);

  const chartData = data
    ? [
        { name: "Customers", value: data.totalCustomers },
        { name: "Claims", value: data.totalClaims },
        { name: "Fraud", value: data.fraudFlaggedClaims },
      ]
    : [];

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
      title="Administrator control centre"
      subtitle="Portfolio-wide KPIs, onboarding, and risk signals across the SmartInsure motor network."
    >
      {data && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <KpiCard title="Customers" value={data.totalCustomers} icon={Users} accent="brand" />
          <KpiCard title="Claims" value={data.totalClaims} hint={`${data.pendingClaims} awaiting review`} icon={ShieldAlert} accent="accent" />
          <KpiCard title="Fraud flagged" value={data.fraudFlaggedClaims} icon={ShieldAlert} accent="slate" />
          <KpiCard title="Pending insurers" value={data.pendingCompanyApprovals} icon={Building2} accent="brand" />
          <KpiCard title="Banned customers" value={data.bannedCustomers} icon={Users} accent="slate" />
          <KpiCard title="Discount cohort" value={data.discountEligibleCustomers} icon={Percent} accent="accent" />
        </div>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-5">
        <Card className="p-6 lg:col-span-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-sm font-semibold text-ink-950">Operational mix</div>
              <div className="text-xs text-ink-500">Indexed portfolio signals</div>
            </div>
          </div>
          <div className="mt-6 h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="name" stroke="#94a3b8" tickLine={false} axisLine={false} />
                <YAxis stroke="#94a3b8" tickLine={false} axisLine={false} />
                <Tooltip cursor={{ fill: "rgba(13,148,136,0.06)" }} />
                <Bar dataKey="value" fill="url(#barGradient)" radius={[10, 10, 0, 0]} />
                <defs>
                  <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#0d9488" />
                    <stop offset="100%" stopColor="#2563eb" stopOpacity={0.85} />
                  </linearGradient>
                </defs>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="p-6 lg:col-span-2">
          <div className="text-sm font-semibold text-ink-950">Pending insurer approvals</div>
          <p className="mt-1 text-xs text-ink-500">Approve only after KYC checks in your real process.</p>
          <div className="mt-4 max-h-[340px] space-y-3 overflow-y-auto pr-1">
            {pending?.length ? (
              pending.map((c: any) => (
                <div
                  key={c.id}
                  className="flex flex-col gap-3 rounded-xl border border-slate-200/80 bg-slate-50/50 p-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="min-w-0">
                    <div className="truncate font-semibold text-ink-950">{c.legalName}</div>
                    <div className="truncate text-xs text-ink-500">{c.contactEmail}</div>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <button
                      type="button"
                      className="si-btn-primary px-3 py-2 text-xs"
                      onClick={async () => {
                        await api.post(`/admin/companies/${c.id}/approve`);
                        window.location.reload();
                      }}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      className="si-btn-secondary px-3 py-2 text-xs"
                      onClick={async () => {
                        await api.post(`/admin/companies/${c.id}/reject`, null, { params: { reason: "Incomplete KYC" } });
                        window.location.reload();
                      }}
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))
            ) : (
              <div className="rounded-xl border border-dashed border-slate-200 bg-white/60 p-6 text-center text-sm text-ink-500">
                No pending applications.
              </div>
            )}
          </div>
        </Card>
      </div>
    </AppShell>
  );
};
