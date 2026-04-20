import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { UserPlus } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { MarketingLayout } from "../components/layout/MarketingLayout";
import { Card } from "../components/ui/Card";

export const RegisterCustomerPage = () => {
  const { registerCustomer } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
    phone: "",
    address: "",
  });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await registerCustomer(form);
      toast.success("Account created");
      navigate("/customer");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Registration failed");
    }
  };

  const fields = [
    { key: "fullName" as const, label: "Full name", type: "text", required: true },
    { key: "email" as const, label: "Email", type: "email", required: true },
    { key: "phone" as const, label: "Phone", type: "text", required: false },
    { key: "address" as const, label: "Address", type: "text", required: false },
    { key: "password" as const, label: "Password", type: "password", required: true, minLength: 8 },
  ];

  return (
    <MarketingLayout variant="auth">
      <Card elevated className="w-full max-w-lg p-8 shadow-glow">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-700 ring-1 ring-brand-100">
            <UserPlus className="h-6 w-6" strokeWidth={1.5} />
          </div>
          <div>
            <h1 className="font-display text-2xl font-semibold text-ink-950">Create your profile</h1>
            <p className="text-sm text-ink-500">Secure access to policies, claims, and AI updates.</p>
          </div>
        </div>

        <form onSubmit={submit} className="mt-8 grid gap-4 sm:grid-cols-2">
          {fields.map((f) => (
            <div key={f.key} className={f.key === "address" || f.key === "password" ? "sm:col-span-2" : ""}>
              <label className="si-label" htmlFor={f.key}>
                {f.label}
              </label>
              <input
                id={f.key}
                className="si-input"
                type={f.type}
                required={f.required}
                minLength={f.minLength}
                value={form[f.key]}
                onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
              />
            </div>
          ))}
          <div className="sm:col-span-2">
            <button type="submit" className="si-btn-primary mt-2 w-full py-3 text-sm">
              Create account
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
