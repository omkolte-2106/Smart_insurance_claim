import { Link } from "react-router-dom";
import { Shield } from "lucide-react";

export const MarketingLayout: React.FC<{ children: React.ReactNode; variant?: "default" | "auth" }> = ({
  children,
  variant = "default",
}) => {
  return (
    <div className="min-h-screen bg-mesh-light bg-hero-fade">
      <header className="sticky top-0 z-40 border-b border-white/60 bg-white/75 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 lg:px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-brand-600 to-brand-500 text-white shadow-glow">
              <Shield className="h-5 w-5" strokeWidth={1.75} />
            </span>
            <span>
              <span className="block font-display text-lg font-semibold leading-none text-ink-950">SmartInsure</span>
              <span className="text-[11px] font-medium uppercase tracking-wider text-ink-500">Motor claims cloud</span>
            </span>
          </Link>
          <nav className="hidden items-center gap-8 text-sm font-medium text-ink-700 md:flex">
            <Link to="/about" className="transition hover:text-brand-600">
              Platform
            </Link>
            <Link to="/login" className="transition hover:text-brand-600">
              Sign in
            </Link>
            <Link
              to="/register/customer"
              className="rounded-full bg-ink-950 px-5 py-2 text-sm font-semibold text-white shadow-soft transition hover:bg-ink-900"
            >
              Get started
            </Link>
          </nav>
          <Link to="/login" className="si-btn-secondary px-4 py-2 text-xs md:hidden">
            Sign in
          </Link>
        </div>
      </header>
      <main
        className={
          variant === "auth"
            ? "flex min-h-[calc(100vh-220px)] items-center justify-center px-4 py-12 lg:py-16"
            : "px-4 pb-16 pt-6 lg:px-6"
        }
      >
        {children}
      </main>
      <footer className="border-t border-slate-200/80 bg-white/60 py-10 text-center text-xs text-ink-500 backdrop-blur">
        © {new Date().getFullYear()} SmartInsure · Vehicle insurance claims demo stack
      </footer>
    </div>
  );
};
