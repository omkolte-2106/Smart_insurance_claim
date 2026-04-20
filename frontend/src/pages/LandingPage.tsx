import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Building2,
  Cpu,
  FileCheck2,
  Lock,
  Sparkles,
  TrendingUp,
} from "lucide-react";
import api from "../api/client";
import { MarketingLayout } from "../components/layout/MarketingLayout";
import { Card } from "../components/ui/Card";

export const LandingPage = () => {
  const [stats, setStats] = useState<{ customers: number; approvedInsurers: number; claimsProcessed: number } | null>(
    null,
  );

  useEffect(() => {
    api
      .get("/public/stats")
      .then((r) => setStats(r.data))
      .catch(() => setStats({ customers: 0, approvedInsurers: 0, claimsProcessed: 0 }));
  }, []);

  return (
    <MarketingLayout>
      <div className="mx-auto max-w-6xl">
        <section className="grid items-center gap-12 py-10 lg:grid-cols-[1.1fr_0.9fr] lg:py-16">
          <div className="animate-fade-in">
            <div className="si-pill w-fit border-brand-200/60 bg-brand-50/80 text-brand-800">
              <Sparkles className="h-3.5 w-3.5" />
              Vehicle insurance · AI-ready claims
            </div>
            <h1 className="mt-6 font-display text-4xl font-semibold leading-[1.1] tracking-tight text-ink-950 sm:text-5xl">
              The calm, modern way to run{" "}
              <span className="bg-gradient-to-r from-brand-600 to-accent-500 bg-clip-text text-transparent">
                motor claims
              </span>
              .
            </h1>
            <p className="mt-6 max-w-xl text-lg text-ink-600">
              SmartInsure unifies intake, document intelligence, fraud analytics, and insurer review in one polished
              experience—built so you can drop in your own OpenCV and ML weights later.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/register/customer" className="si-btn-primary px-6 py-3 text-sm">
                Start as policyholder
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link to="/register/company" className="si-btn-secondary px-6 py-3 text-sm">
                Register insurer
              </Link>
            </div>
            <div className="mt-10 grid max-w-lg grid-cols-3 gap-4 text-center sm:text-left">
              <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 shadow-inner">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Customers</div>
                <div className="mt-1 font-display text-2xl font-semibold text-ink-950">{stats?.customers ?? "—"}</div>
              </div>
              <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 shadow-inner">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Live insurers</div>
                <div className="mt-1 font-display text-2xl font-semibold text-ink-950">
                  {stats?.approvedInsurers ?? "—"}
                </div>
              </div>
              <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 shadow-inner">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">Claims</div>
                <div className="mt-1 font-display text-2xl font-semibold text-ink-950">
                  {stats?.claimsProcessed ?? "—"}
                </div>
              </div>
            </div>
          </div>

          <Card elevated className="relative overflow-hidden p-8 animate-fade-in">
            <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-gradient-to-br from-brand-400/25 to-accent-500/15 blur-2xl" />
            <div className="relative space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Live journey</div>
                  <div className="mt-1 font-display text-lg font-semibold text-ink-950">Claim CLM-2026-…</div>
                </div>
                <span className="rounded-full bg-emerald-50 px-3 py-1 text-[11px] font-semibold text-emerald-800 ring-1 ring-emerald-200/70">
                  On track
                </span>
              </div>
              <div className="space-y-3 text-sm text-ink-600">
                {["Documents verified", "AI severity scored", "Awaiting insurer"].map((step, i) => (
                  <div key={step} className="flex items-center gap-3">
                    <span className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-ink-700">
                      {i + 1}
                    </span>
                    <span className={i === 2 ? "font-semibold text-ink-900" : ""}>{step}</span>
                  </div>
                ))}
              </div>
              <div className="rounded-xl border border-slate-200/80 bg-slate-50/80 p-4 text-xs text-ink-500">
                <div className="flex items-center gap-2 font-semibold text-ink-800">
                  <TrendingUp className="h-4 w-4 text-brand-600" />
                  Payout recommendation ready for review
                </div>
                <div className="mt-2 leading-relaxed">
                  Spring Boot orchestrates ML modules; React surfaces every milestone with insurer-grade clarity.
                </div>
              </div>
            </div>
          </Card>
        </section>

        <section className="grid gap-6 py-6 md:grid-cols-3">
          {[
            {
              title: "Guided intake",
              body: "Structured capture for Aadhaar, licence, PUC, and damage imagery with validation and storage abstraction.",
              icon: FileCheck2,
            },
            {
              title: "ML orchestration",
              body: "Document, fraud, severity, and payout services are modular HTTP contracts—swap in your Python stack.",
              icon: Cpu,
            },
            {
              title: "Insurer control",
              body: "Manual review, remarks, settlement, and audit trails keep compliance teams confidently in charge.",
              icon: Building2,
            },
          ].map((f) => (
            <Card key={f.title} className="p-6 transition hover:-translate-y-0.5 hover:shadow-md">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700 ring-1 ring-brand-100">
                <f.icon className="h-5 w-5" strokeWidth={1.75} />
              </div>
              <h3 className="mt-4 font-display text-lg font-semibold text-ink-950">{f.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-600">{f.body}</p>
            </Card>
          ))}
        </section>

        <section className="mt-4 rounded-3xl border border-slate-200/80 bg-gradient-to-r from-ink-950 to-slate-900 p-8 text-white shadow-soft lg:flex lg:items-center lg:justify-between lg:gap-10 lg:p-10">
          <div className="max-w-xl">
            <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-white/70">
              <Lock className="h-4 w-4" />
              Security-first
            </div>
            <h3 className="mt-3 font-display text-2xl font-semibold tracking-tight">JWT, RBAC, and field-level discipline</h3>
            <p className="mt-2 text-sm leading-relaxed text-white/75">
              Role-gated APIs, BCrypt credentials, banned-account handling, and protected claim access mirror what you would
              expect in a regulated motor portfolio.
            </p>
          </div>
          <div className="mt-6 flex flex-wrap gap-3 lg:mt-0">
            <Link to="/login" className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-ink-950 shadow-soft">
              Sign in to console
            </Link>
            <Link
              to="/about"
              className="rounded-full border border-white/25 px-6 py-3 text-sm font-semibold text-white hover:bg-white/10"
            >
              Read platform notes
            </Link>
          </div>
        </section>
      </div>
    </MarketingLayout>
  );
};
