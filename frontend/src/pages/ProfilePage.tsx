import { Link, Navigate } from "react-router-dom";
import { KeyRound, Shield } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Card } from "../components/ui/Card";

export const ProfilePage = () => {
  const { token, email, role, customerProfileId, companyProfileId, logout } = useAuth();
  if (!token) return <Navigate to="/login" replace />;

  const home =
    role === "ROLE_ADMIN" ? "/admin" : role === "ROLE_COMPANY" ? "/company" : role === "ROLE_CUSTOMER" ? "/customer" : "/";

  return (
    <div className="min-h-screen bg-mesh-light bg-hero-fade px-4 py-12">
      <div className="mx-auto max-w-lg">
        <Link to={home} className="text-sm font-semibold text-brand-700 hover:text-brand-600">
          ← Back to console
        </Link>

        <Card elevated className="mt-6 p-8 shadow-glow">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-600 to-brand-500 text-white shadow-glow">
              <Shield className="h-6 w-6" strokeWidth={1.5} />
            </div>
            <div>
              <h1 className="font-display text-2xl font-semibold text-ink-950">Session</h1>
              <p className="text-sm text-ink-500">Signed in credentials and role context.</p>
            </div>
          </div>

          <div className="mt-8 space-y-4">
            <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Email</div>
              <div className="mt-1 font-semibold text-ink-950">{email}</div>
            </div>
            <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Role</div>
              <div className="mt-1 font-mono text-sm text-ink-900">{role}</div>
            </div>
            {customerProfileId && (
              <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Customer profile</div>
                <div className="mt-1 font-semibold text-ink-950">#{customerProfileId}</div>
              </div>
            )}
            {companyProfileId && (
              <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Company profile</div>
                <div className="mt-1 font-semibold text-ink-950">#{companyProfileId}</div>
              </div>
            )}
          </div>

          <button type="button" className="si-btn-secondary mt-8 w-full justify-center py-3 text-sm" onClick={logout}>
            <KeyRound className="h-4 w-4" />
            Sign out everywhere on this browser
          </button>
        </Card>
      </div>
    </div>
  );
};
