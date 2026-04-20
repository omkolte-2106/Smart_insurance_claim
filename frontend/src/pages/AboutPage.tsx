import { Link } from "react-router-dom";
import { Layers, Server, Sparkles } from "lucide-react";
import { MarketingLayout } from "../components/layout/MarketingLayout";
import { Card } from "../components/ui/Card";

export const AboutPage = () => (
  <MarketingLayout>
    <div className="mx-auto max-w-3xl py-10">
      <Link to="/" className="text-sm font-semibold text-brand-700 hover:text-brand-600">
        ← Home
      </Link>
      <h1 className="mt-6 font-display text-4xl font-semibold tracking-tight text-ink-950">Platform notes</h1>
      <p className="mt-4 text-lg text-ink-600">
        SmartInsure is a deliberately layered reference implementation: a Spring Boot core with JWT and PostgreSQL, a
        FastAPI sidecar for ML contracts, and a React experience that mirrors what policyholders and operations teams see in
        leading Indian insurtech products.
      </p>

      <div className="mt-10 grid gap-5">
        <Card className="flex gap-4 p-6">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 ring-1 ring-brand-100">
            <Server className="h-5 w-5" />
          </div>
          <div>
            <h2 className="font-display text-lg font-semibold text-ink-950">Backend discipline</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink-600">
              Controllers stay thin; services own orchestration; repositories stay query-focused. Exceptions are mapped
              consistently so the UI can show trustworthy feedback.
            </p>
          </div>
        </Card>
        <Card className="flex gap-4 p-6">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-accent-500/10 text-accent-600 ring-1 ring-accent-500/20">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h2 className="font-display text-lg font-semibold text-ink-950">ML boundary</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink-600">
              Every ML capability is a versioned HTTP module. Replace placeholders with OpenCV pipelines or Torch models
              without rewriting claim state machines.
            </p>
          </div>
        </Card>
        <Card className="flex gap-4 p-6">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-800 ring-1 ring-slate-200/80">
            <Layers className="h-5 w-5" />
          </div>
          <div>
            <h2 className="font-display text-lg font-semibold text-ink-950">Experience layer</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink-600">
              The interface favours soft neutrals, confident typography, and dashboards that read well in demos and
              interviews—without resorting to gimmicky palettes.
            </p>
          </div>
        </Card>
      </div>
    </div>
  </MarketingLayout>
);
