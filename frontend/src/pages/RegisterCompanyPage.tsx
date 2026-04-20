import { useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import { Building2 } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { MarketingLayout } from "../components/layout/MarketingLayout";
import { Card } from "../components/ui/Card";

export const RegisterCompanyPage = () => {
  const { registerCompany } = useAuth();
  const [form, setForm] = useState({
    email: "",
    password: "",
    legalName: "",
    gstNumber: "",
    contactEmail: "",
    contactPhone: "",
    address: "",
  });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await registerCompany(form);
      toast.success("Application submitted. Await admin approval before signing in.");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Registration failed");
    }
  };

  const fields = [
    { key: "legalName" as const, label: "Legal entity name", required: true },
    { key: "email" as const, label: "Login email", required: true, type: "email" },
    { key: "password" as const, label: "Password", required: true, type: "password", minLength: 3 },
    { key: "gstNumber" as const, label: "GST / tax ID", required: false, type: "text" },
    { key: "contactEmail" as const, label: "Operations email", required: false, type: "email" },
    { key: "contactPhone" as const, label: "Operations phone", required: false, type: "text" },
    { key: "address" as const, label: "Address", required: true, type: "text" },
  ];

  return (
    <MarketingLayout variant="auth">
      <Card elevated className="w-full max-w-lg p-8 shadow-glow">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-accent-500/10 text-accent-600 ring-1 ring-accent-500/20">
            <Building2 className="h-6 w-6" strokeWidth={1.5} />
          </div>
          <div>
            <h1 className="font-display text-2xl font-semibold text-ink-950">Insurer onboarding</h1>
            <p className="text-sm text-ink-500">Console access unlocks only after SmartInsure admin approval.</p>
          </div>
        </div>

        <form onSubmit={submit} className="mt-8 grid gap-4 sm:grid-cols-2">
          {fields.map((f) => (
            <div key={f.key} className={f.key === "legalName" || f.key === "password" || f.key === "address" ? "sm:col-span-2" : ""}>
              <label className="si-label" htmlFor={f.key}>
                {f.label}
              </label>
              <input
                id={f.key}
                className="si-input"
                type={f.type ?? "text"}
                required={f.required}
                minLength={f.minLength}
                value={form[f.key]}
                onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
              />
            </div>
          ))}
          <div className="sm:col-span-2">
            <button type="submit" className="si-btn-primary mt-2 w-full py-3 text-sm">
              Submit application
            </button>
          </div>
        </form>

        <p className="mt-6 text-center text-sm text-ink-500">
          <Link to="/" className="font-semibold text-brand-700 hover:text-brand-600">
            ← Back to home
          </Link>
        </p>
      </Card>
    </MarketingLayout>
  );
};
