import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { ArrowRight, KeyRound } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { MarketingLayout } from "../components/layout/MarketingLayout";
import { Card } from "../components/ui/Card";

export const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = await login(email, password);
      toast.success("Signed in");
      if (data.role === "ROLE_ADMIN") navigate("/admin");
      else if (data.role === "ROLE_COMPANY") navigate("/company");
      else navigate("/customer");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Login failed");
    }
  };

  return (
    <MarketingLayout variant="auth">
      <Card elevated className="w-full max-w-md p-8 shadow-glow">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-700 ring-1 ring-brand-100">
            <KeyRound className="h-6 w-6" strokeWidth={1.5} />
          </div>
          <div>
            <h1 className="font-display text-2xl font-semibold text-ink-950">Welcome back</h1>
            <p className="text-sm text-ink-500">Use the seeded accounts from the README.</p>
          </div>
        </div>

        <form onSubmit={submit} className="mt-8 space-y-5">
          <div>
            <label className="si-label" htmlFor="email">
              Work email
            </label>
            <input id="email" className="si-input" value={email} onChange={(e) => setEmail(e.target.value)} type="email" required autoComplete="email" />
          </div>
          <div>
            <label className="si-label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              className="si-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              required
              autoComplete="current-password"
            />
          </div>
          <button type="submit" className="si-btn-primary mt-2 w-full py-3 text-sm">
            Continue to console
            <ArrowRight className="h-4 w-4" />
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-ink-500">
          New policyholder?{" "}
          <Link className="font-semibold text-brand-700 hover:text-brand-600" to="/register/customer">
            Create an account
          </Link>
        </p>
      </Card>
    </MarketingLayout>
  );
};
