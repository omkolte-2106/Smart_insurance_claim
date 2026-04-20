import { useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import { Car, LayoutDashboard, Percent, Search as SearchIcon, ShieldAlert, UserCircle } from "lucide-react";
import api from "../api/client";
import { AppShell } from "../components/layout/AppShell";
import { Card } from "../components/ui/Card";
import { useAuth } from "../context/AuthContext";

export const SearchPage = () => {
  const { role } = useAuth();
  const [reg, setReg] = useState("");
  const [result, setResult] = useState<any>(null);

  const nav =
    role === "ROLE_ADMIN"
      ? [
          { to: "/admin", label: "Overview", icon: LayoutDashboard },
          { to: "/fraud", label: "Fraud desk", icon: ShieldAlert },
          { to: "/search", label: "Vehicle search", icon: SearchIcon },
          { to: "/discounts", label: "Discount analytics", icon: Percent },
          { to: "/profile", label: "Account", icon: UserCircle },
        ]
      : [
          { to: "/company", label: "Overview", icon: LayoutDashboard },
          { to: "/search", label: "Vehicle search", icon: SearchIcon },
          { to: "/discounts/company", label: "Discount insights", icon: Percent },
          { to: "/profile", label: "Account", icon: UserCircle },
        ];

  const search = async () => {
    try {
      const { data } = await api.get("/search/vehicle", { params: { registration: reg } });
      setResult(data);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Search failed");
    }
  };

  return (
    <AppShell
      nav={nav}
      title="Vehicle discovery"
      subtitle="Resolve registrations to policyholders for field investigations and servicing."
    >
      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="p-6 lg:col-span-2">
          <label className="si-label" htmlFor="reg">
            Registration number
          </label>
          <div className="mt-2 flex gap-2">
            <input
              id="reg"
              className="si-input"
              placeholder="e.g. KA03MN4455"
              value={reg}
              onChange={(e) => setReg(e.target.value.toUpperCase())}
            />
            <button type="button" className="si-btn-primary shrink-0 px-4" onClick={search}>
              <SearchIcon className="h-4 w-4" />
            </button>
          </div>
          <p className="mt-3 text-xs leading-relaxed text-ink-500">
            Company users only see vehicles linked to policies they have issued. Administrators see the full index.
          </p>
          <Link to={role === "ROLE_ADMIN" ? "/admin" : "/company"} className="mt-6 inline-flex text-xs font-semibold text-brand-700 hover:text-brand-600">
            ← Back to dashboard
          </Link>
        </Card>

        <Card className="p-6 lg:col-span-3">
          {!result && (
            <div className="flex h-full min-h-[220px] flex-col items-center justify-center text-center text-sm text-ink-500">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-ink-600">
                <Car className="h-7 w-7" strokeWidth={1.5} />
              </div>
              <div className="mt-4 max-w-sm">
                Enter a registration to pull the linked customer, insurer context, and active policy identifiers.
              </div>
            </div>
          )}
          {result && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Vehicle</div>
                  <div className="font-display text-2xl font-semibold tracking-tight text-ink-950">{result.registrationNumber}</div>
                </div>
                <span className="si-pill">
                  <Car className="h-3.5 w-3.5" />
                  Verified lookup
                </span>
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
                  <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Customer</div>
                  <div className="mt-1 font-semibold text-ink-950">{result.customerName}</div>
                  <div className="mt-1 text-xs text-ink-600">{result.customerEmail}</div>
                </div>
                <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
                  <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Insurer context</div>
                  <div className="mt-1 font-semibold text-ink-950">{result.insurerName}</div>
                  <div className="mt-1 text-xs text-ink-600">Active policies</div>
                  <div className="mt-2 text-xs font-medium text-ink-800">{result.activePolicyNumbers?.join(", ") || "—"}</div>
                </div>
              </div>
            </div>
          )}
        </Card>
      </div>
    </AppShell>
  );
};
