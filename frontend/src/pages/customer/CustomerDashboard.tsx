import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bell, FilePlus2, Gift, Home, ShieldCheck, UserCircle } from "lucide-react";
import api from "../../api/client";
import { AppShell } from "../../components/layout/AppShell";
import { KpiCard } from "../../components/ui/KpiCard";
import { Card } from "../../components/ui/Card";
import { StatusBadge } from "../../components/ui/StatusBadge";

export const CustomerDashboard = () => {
  const [policies, setPolicies] = useState<any[]>([]);
  const [claims, setClaims] = useState<any[]>([]);
  const [discount, setDiscount] = useState<any>(null);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [showVehicleForm, setShowVehicleForm] = useState(false);
  const [newVehicle, setNewVehicle] = useState({ registrationNumber: "", make: "", model: "", yearOfManufacture: 2024 });

  useEffect(() => {
    (async () => {
      const [p, c, d, v] = await Promise.all([
        api.get("/customer/policies"),
        api.get("/claims?size=8"),
        api.get("/customer/discount"),
        api.get("/customer/vehicles"),
      ]);
      setPolicies(p.data);
      setClaims(c.data.content ?? c.data);
      setDiscount(d.data);
      setVehicles(v.data);
    })().catch(() => {});
  }, []);

  const openClaims = claims.filter((c) => c.status !== "SETTLED" && c.status !== "REJECTED").length;

  const nav = [
    { to: "/customer", label: "Home", icon: Home },
    { to: "/claims/new", label: "File claim", icon: FilePlus2 },
    { to: "/profile", label: "Account", icon: UserCircle },
  ];

  return (
    <AppShell
      nav={nav}
      title="Your motor workspace"
      subtitle="Policies, AI-assisted verification, and settlement progress in one calm view."
    >
      <div className="grid gap-4 md:grid-cols-3">
        <KpiCard title="Active policies" value={policies.length} icon={ShieldCheck} accent="brand" />
        <KpiCard title="Open claims" value={openClaims} icon={Bell} accent="accent" />
        <KpiCard title="Loyalty" value={discount?.eligible ? "Eligible" : "Standard"} icon={Gift} accent="slate" hint={discount?.eligible ? `Up to ${discount.suggestedDiscountPercent}% renewal suggestion` : "Run admin cohort job to populate"} />
      </div>

      {discount?.eligible && (
        <div className="mt-6 rounded-2xl border border-emerald-200/80 bg-gradient-to-r from-emerald-50 to-white p-5 shadow-inner">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="text-xs font-semibold uppercase tracking-wide text-emerald-800">Renewal advantage</div>
              <div className="mt-1 font-display text-xl font-semibold text-emerald-950">
                Suggested discount {discount.suggestedDiscountPercent}%
              </div>
              <div className="mt-1 text-sm text-emerald-900/80">Percentile {discount.percentileRank?.toFixed?.(1)} · {discount.campaignCode}</div>
            </div>
            <div className="rounded-full bg-white/80 px-4 py-2 text-xs font-semibold text-emerald-900 ring-1 ring-emerald-200/70">
              SmartInsure loyalty signal
            </div>
          </div>
        </div>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        <Card className="p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-sm font-semibold text-ink-950">Active policies</div>
              <div className="text-xs text-ink-500">Tap a claim to view the full AI + review timeline</div>
            </div>
            <Link to="/claims/new" className="si-btn-primary px-4 py-2 text-xs">
              <FilePlus2 className="h-4 w-4" />
              File claim
            </Link>
          </div>
          <div className="mt-5 space-y-3">
            {policies.map((p) => (
              <div key={p.id} className="rounded-xl border border-slate-200/80 bg-slate-50/40 p-4">
                <div className="font-semibold text-ink-950">{p.policyNumber}</div>
                <div className="mt-1 text-xs text-ink-500">Sum insured ₹{Number(p.sumInsured).toLocaleString("en-IN")}</div>
              </div>
            ))}
            {!policies.length && <div className="text-sm text-ink-500">No policies linked yet.</div>}
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-sm font-semibold text-ink-950">Your Vehicles</div>
              <div className="text-xs text-ink-500">Manage registered vehicles</div>
            </div>
            <button onClick={() => setShowVehicleForm(!showVehicleForm)} className="si-btn-secondary px-3 py-1 text-xs">
              Add
            </button>
          </div>
          {showVehicleForm && (
            <div className="mt-4 space-y-3 rounded-xl bg-slate-50 p-4 border border-slate-200">
              <input className="si-input text-xs" placeholder="Registration (e.g. MH01AB1234)" value={newVehicle.registrationNumber} onChange={e => setNewVehicle({...newVehicle, registrationNumber: e.target.value})} />
              <div className="grid grid-cols-2 gap-2">
                <input className="si-input text-xs" placeholder="Make" value={newVehicle.make} onChange={e => setNewVehicle({...newVehicle, make: e.target.value})} />
                <input className="si-input text-xs" placeholder="Model" value={newVehicle.model} onChange={e => setNewVehicle({...newVehicle, model: e.target.value})} />
              </div>
              <input type="number" className="si-input text-xs" placeholder="Year" value={newVehicle.yearOfManufacture} onChange={e => setNewVehicle({...newVehicle, yearOfManufacture: Number(e.target.value)})} />
              <button 
                className="si-btn-primary w-full py-2 text-xs"
                onClick={async () => {
                  try {
                    await api.post("/customer/vehicles", newVehicle);
                    const v = await api.get("/customer/vehicles");
                    setVehicles(v.data);
                    setShowVehicleForm(false);
                    setNewVehicle({ registrationNumber: "", make: "", model: "", yearOfManufacture: 2024 });
                  } catch (e: any) { alert("Error: " + e.message); }
                }}
              >Save Vehicle</button>
            </div>
          )}
          <div className="mt-5 space-y-3">
            {vehicles.map((v) => (
              <div key={v.id} className="rounded-xl border border-slate-200/80 bg-slate-50/40 p-4">
                <div className="font-semibold text-ink-950">{v.registrationNumber}</div>
                <div className="mt-1 text-xs text-ink-500">{v.make} {v.model} ({v.yearOfManufacture})</div>
              </div>
            ))}
            {!vehicles.length && !showVehicleForm && <div className="text-sm text-ink-500">No vehicles registered.</div>}
          </div>
        </Card>


        <Card className="p-6">
          <div className="text-sm font-semibold text-ink-950">Recent claims</div>
          <div className="mt-4 space-y-2">
            {claims.map((c) => (
              <Link
                key={c.claimPublicId}
                to={`/claims/${c.claimPublicId}`}
                className="flex items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-white/70 p-4 transition hover:border-brand-200/80 hover:shadow-sm"
              >
                <div className="min-w-0">
                  <div className="truncate font-semibold text-ink-950">{c.claimPublicId}</div>
                  <div className="truncate text-xs text-ink-500">{c.companyName}</div>
                </div>
                <StatusBadge status={c.status} />
              </Link>
            ))}
            {!claims.length && <div className="text-sm text-ink-500">No claims yet.</div>}
          </div>
        </Card>
      </div>
    </AppShell>
  );
};
