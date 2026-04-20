import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import { Bell, LogOut, Menu, Shield, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext";

export type ShellNavItem = { to: string; label: string; icon: LucideIcon };

const navClass = ({ isActive }: { isActive: boolean }) =>
  `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
    isActive
      ? "bg-gradient-to-r from-brand-600/10 to-brand-500/5 text-brand-800 ring-1 ring-brand-200/60"
      : "text-ink-700 hover:bg-slate-100 hover:text-ink-900"
  }`;

type Props = {
  nav: ShellNavItem[];
  children: React.ReactNode;
  title: string;
  subtitle?: string;
};

export const AppShell: React.FC<Props> = ({ nav, children, title, subtitle }) => {
  const { logout, email } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  const Sidebar = (
    <div className="flex h-full flex-col border-r border-slate-200/80 bg-gradient-to-b from-white via-white to-slate-50/90">
      <div className="flex items-center gap-3 px-5 py-6">
        <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-brand-600 to-brand-500 text-white shadow-glow">
          <Shield className="h-5 w-5" strokeWidth={1.75} />
        </span>
        <div>
          <div className="font-display text-base font-semibold text-ink-950">SmartInsure</div>
          <div className="text-[11px] font-medium text-ink-500">Console</div>
        </div>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 px-3">
        {nav.map((item) => (
          <NavLink key={item.to} to={item.to} className={navClass} onClick={() => setOpen(false)}>
            <item.icon className="h-[18px] w-[18px] shrink-0 opacity-70 group-hover:opacity-100" strokeWidth={1.75} />
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="space-y-3 p-4">
        <div className="rounded-xl border border-slate-200/80 bg-white/80 p-3 text-xs text-ink-500 shadow-inner">
          <div className="text-[10px] font-semibold uppercase tracking-wide text-ink-400">Signed in</div>
          <div className="mt-1 truncate font-medium text-ink-900">{email}</div>
        </div>
        <button
          type="button"
          className="si-btn-secondary w-full justify-center border-slate-200 py-2.5 text-xs"
          onClick={() => {
            logout();
            navigate("/login");
          }}
        >
          <LogOut className="h-4 w-4" />
          Sign out
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-canvas">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 lg:block">{Sidebar}</aside>

      {/* Mobile overlay */}
      {open && (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-ink-950/40 backdrop-blur-sm lg:hidden"
          aria-label="Close menu"
          onClick={() => setOpen(false)}
        />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-[min(20rem,88vw)] transform transition duration-200 ease-out lg:hidden ${
          open ? "translate-x-0 shadow-2xl" : "-translate-x-full"
        }`}
      >
        <div className="absolute right-2 top-3">
          <button type="button" className="si-btn-ghost p-2" onClick={() => setOpen(false)} aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>
        {Sidebar}
      </aside>

      <div className="lg:pl-72">
        <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/80 backdrop-blur-md">
          <div className="mx-auto flex max-w-6xl items-start justify-between gap-4 px-4 py-4 lg:px-8">
            <div className="flex items-start gap-3">
              <button
                type="button"
                className="si-btn-ghost -ml-1 p-2 lg:hidden"
                onClick={() => setOpen(true)}
                aria-label="Open menu"
              >
                <Menu className="h-5 w-5" />
              </button>
              <div>
                <h1 className="font-display text-xl font-semibold tracking-tight text-ink-950 lg:text-2xl">{title}</h1>
                {subtitle && <p className="mt-1 max-w-2xl text-sm text-ink-500">{subtitle}</p>}
              </div>
            </div>
            <button
              type="button"
              className="si-btn-ghost hidden items-center gap-2 rounded-xl border border-slate-200/80 bg-white/80 px-3 py-2 text-xs font-semibold text-ink-600 shadow-sm sm:inline-flex"
              onClick={() => navigate("/profile")}
            >
              <Bell className="h-4 w-4" />
              Account
            </button>
          </div>
        </header>
        <div className="mx-auto max-w-6xl px-4 py-8 lg:px-8">{children}</div>
      </div>
    </div>
  );
};
