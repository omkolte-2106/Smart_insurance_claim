import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import api from "../api/client";

type Role = "ROLE_ADMIN" | "ROLE_COMPANY" | "ROLE_CUSTOMER" | null;

type AuthState = {
  token: string | null;
  role: Role;
  email: string | null;
  customerProfileId: number | null;
  companyProfileId: number | null;
  login: (
    email: string,
    password: string,
  ) => Promise<{ role: string; email: string; customerProfileId?: number | null; companyProfileId?: number | null }>;
  logout: () => void;
  registerCustomer: (payload: Record<string, unknown>) => Promise<void>;
  registerCompany: (payload: Record<string, unknown>) => Promise<void>;
};

const AuthContext = createContext<AuthState | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("si_token"));
  const [role, setRole] = useState<Role>(() => (localStorage.getItem("si_role") as Role) || null);
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem("si_email"));
  const [customerProfileId, setCustomerProfileId] = useState<number | null>(
    () => Number(localStorage.getItem("si_customer") || "") || null,
  );
  const [companyProfileId, setCompanyProfileId] = useState<number | null>(
    () => Number(localStorage.getItem("si_company") || "") || null,
  );

  useEffect(() => {
    if (token) localStorage.setItem("si_token", token);
    else localStorage.removeItem("si_token");
    if (role) localStorage.setItem("si_role", role);
    else localStorage.removeItem("si_role");
    if (email) localStorage.setItem("si_email", email);
    else localStorage.removeItem("si_email");
    if (customerProfileId) localStorage.setItem("si_customer", String(customerProfileId));
    else localStorage.removeItem("si_customer");
    if (companyProfileId) localStorage.setItem("si_company", String(companyProfileId));
    else localStorage.removeItem("si_company");
  }, [token, role, email, customerProfileId, companyProfileId]);

  const login = async (e: string, password: string) => {
    const { data } = await api.post("/auth/login", { email: e, password });
    localStorage.setItem("si_token", data.token);
    setToken(data.token);
    setRole(data.role);
    setEmail(data.email);
    setCustomerProfileId(data.customerProfileId ?? null);
    setCompanyProfileId(data.companyProfileId ?? null);
    return data;
  };

  const registerCustomer = async (payload: Record<string, unknown>) => {
    const { data } = await api.post("/auth/register/customer", payload);
    localStorage.setItem("si_token", data.token);
    setToken(data.token);
    setRole(data.role);
    setEmail(data.email);
    setCustomerProfileId(data.customerProfileId ?? null);
    setCompanyProfileId(null);
  };

  const registerCompany = async (payload: Record<string, unknown>) => {
    await api.post("/auth/register/company", payload);
  };

  const logout = () => {
    setToken(null);
    setRole(null);
    setEmail(null);
    setCustomerProfileId(null);
    setCompanyProfileId(null);
    localStorage.clear();
  };

  const value = useMemo(
    () => ({
      token,
      role,
      email,
      customerProfileId,
      companyProfileId,
      login,
      logout,
      registerCustomer,
      registerCompany,
    }),
    [token, role, email, customerProfileId, companyProfileId],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
