import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Building2, ClipboardList, LayoutDashboard, Percent, Search, UserCircle, Wallet } from "lucide-react";
import api from "../../api/client";
import { AppShell } from "../../components/layout/AppShell";
import { KpiCard } from "../../components/ui/KpiCard";
import { Card } from "../../components/ui/Card";

type CompanyDash = {
  policyholders: number;
  totalClaims: number;
  pendingManualReview: number;
  approvedClaims: number;
  rejectedClaims: number;
  estimatedPayoutExposure: number;
};

export const CompanyDashboard = () => {
  const [dash, setDash] = useState<CompanyDash | null>(null);
  const [claims, setClaims] = useState<any[]>([]);

  const [churnFile, setChurnFile] = useState<File | null>(null);
  const [churnLoading, setChurnLoading] = useState(false);
  const [churnResults, setChurnResults] = useState<any[]>([]);
  const [churnAnalyzed, setChurnAnalyzed] = useState(0);

  const [showPolicyForm, setShowPolicyForm] = useState(false);
  const [issuanceMode, setIssuanceMode] = useState<"individual" | "bulk">("individual");
  const [bulkFile, setBulkFile] = useState<File | null>(null);
  const [bulkProcessing, setBulkProcessing] = useState(false);
  const [bulkResult, setBulkResult] = useState<any>(null);

  const downloadCsv = async (prefill: boolean) => {
    try {
      const res = await api.get(`/company/policies/csv-template?prefill=${prefill}`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', prefill ? 'prospect_policies.csv' : 'policy_template.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (e: any) {
      alert("Failed to download template: " + (e.response?.data?.message || e.message));
    }
  };

  const [newPolicy, setNewPolicy] = useState({
    customerEmail: "",
    vehicleRegistration: "",
    policyNumber: "POL-" + Math.floor(Math.random() * 1000000),
    sumInsured: 500000,
    annualPremium: 15000,
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().split('T')[0],
  });

  const handleChurnUpload = async () => {
    if (!churnFile) return;
    setChurnLoading(true);
    try {
      const formData = new FormData();
      formData.append("file", churnFile);

      const res = await fetch("/ml/churn-prediction", {
        method: "POST",
        body: formData,
      });
      const data = await res.json();
      if (data.error) {
        alert("Error: " + data.error);
      } else {
        setChurnResults(data.top_churners || []);
        setChurnAnalyzed(data.total_analyzed || 0);
      }
    } catch (err: any) {
      alert("Failed to analyze churn: " + err.message);
    } finally {
      setChurnLoading(false);
    }
  };

  useEffect(() => {
    (async () => {
      const [d, c] = await Promise.all([api.get("/company/dashboard"), api.get("/claims?size=8")]);
      setDash(d.data);
      setClaims(c.data.content ?? c.data);
    })().catch(() => {});
  }, []);

  const nav = [
    { to: "/company", label: "Overview", icon: LayoutDashboard },
    { to: "/search", label: "Vehicle search", icon: Search },
    { to: "/discounts/company", label: "Discount insights", icon: Percent },
    { to: "/profile", label: "Account", icon: UserCircle },
  ];

  return (
    <AppShell
      nav={nav}
      title="Company operations"
      subtitle="Throughput, reserves, and manual review workload for your issued motor portfolio."
    >
      {dash && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <KpiCard title="Policyholders" value={dash.policyholders} icon={Building2} accent="brand" />
          <KpiCard title="Submitted claims" value={dash.totalClaims} icon={ClipboardList} accent="accent" />
          <KpiCard title="Manual review queue" value={dash.pendingManualReview} icon={ClipboardList} accent="slate" />
          <KpiCard title="Approved" value={dash.approvedClaims} icon={ClipboardList} accent="brand" />
          <KpiCard title="Rejected" value={dash.rejectedClaims} icon={ClipboardList} accent="slate" />
          <KpiCard
            title="Exposure (est.)"
            value={`₹${Number(dash.estimatedPayoutExposure || 0).toLocaleString("en-IN")}`}
            hint="Sum of AI payout recommendations"
            icon={Wallet}
            accent="accent"
          />
        </div>
      )}

      <Card className="mt-8 p-6">
        <div className="flex items-center justify-between gap-3">
          <div>
            <div className="text-sm font-semibold text-ink-950">Issue New Policy</div>
            <div className="text-xs text-ink-500">Record policies for onboarded customers</div>
          </div>
          <div className="flex items-center gap-2">
            {showPolicyForm && (
              <div className="flex gap-1 rounded-lg bg-slate-100 p-0.5">
                <button 
                  onClick={() => setIssuanceMode("individual")}
                  className={`px-3 py-1 text-[10px] font-medium rounded-md transition-all ${issuanceMode === "individual" ? "bg-white text-ink-950 shadow-sm" : "text-ink-500 hover:text-ink-700"}`}
                >Single</button>
                <button 
                  onClick={() => setIssuanceMode("bulk")}
                  className={`px-3 py-1 text-[10px] font-medium rounded-md transition-all ${issuanceMode === "bulk" ? "bg-white text-ink-950 shadow-sm" : "text-ink-500 hover:text-ink-700"}`}
                >Bulk (CSV)</button>
              </div>
            )}
            <button onClick={() => setShowPolicyForm(!showPolicyForm)} className="si-btn-secondary px-3 py-1 text-xs">
              {showPolicyForm ? "Cancel" : "Create"}
            </button>
          </div>
        </div>
        
        {showPolicyForm && issuanceMode === "individual" && (
          <div className="mt-4 space-y-3 rounded-xl bg-slate-50 p-4 border border-slate-200">
            <div className="grid grid-cols-2 gap-2">
              <input className="si-input text-xs" placeholder="Customer Email" value={newPolicy.customerEmail} onChange={e => setNewPolicy({...newPolicy, customerEmail: e.target.value})} />
              <input className="si-input text-xs" placeholder="Vehicle Reg (e.g. MH01AB1234)" value={newPolicy.vehicleRegistration} onChange={e => setNewPolicy({...newPolicy, vehicleRegistration: e.target.value})} />
            </div>
            <div className="grid grid-cols-3 gap-2">
              <input className="si-input text-xs" placeholder="Policy Num" value={newPolicy.policyNumber} onChange={e => setNewPolicy({...newPolicy, policyNumber: e.target.value})} />
              <input type="number" className="si-input text-xs" placeholder="Sum Insured" value={newPolicy.sumInsured} onChange={e => setNewPolicy({...newPolicy, sumInsured: Number(e.target.value)})} />
              <input type="number" className="si-input text-xs" placeholder="Annual Premium" value={newPolicy.annualPremium} onChange={e => setNewPolicy({...newPolicy, annualPremium: Number(e.target.value)})} />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <input type="date" className="si-input text-xs" value={newPolicy.startDate} onChange={e => setNewPolicy({...newPolicy, startDate: e.target.value})} />
              <input type="date" className="si-input text-xs" value={newPolicy.endDate} onChange={e => setNewPolicy({...newPolicy, endDate: e.target.value})} />
            </div>
            <button 
              className="si-btn-primary w-full py-2 text-xs"
              onClick={async () => {
                try {
                  await api.post("/company/policies", newPolicy);
                  alert("Policy created successfully!");
                  setShowPolicyForm(false);
                  setNewPolicy({...newPolicy, policyNumber: "POL-" + Math.floor(Math.random() * 1000000)});
                } catch (e: any) { alert("Error: " + (e.response?.data?.message || e.message)); }
              }}
            >Issue Policy</button>
          </div>
        )}

        {showPolicyForm && issuanceMode === "bulk" && (
          <div className="mt-4 space-y-4 rounded-xl bg-slate-50 p-4 border border-slate-200">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="text-xs font-semibold text-ink-950">Upload Policy CSV</div>
                <div className="mt-1 text-[10px] text-ink-500 leading-relaxed">
                  Required: customerEmail, vehicleRegistration, policyNumber, sumInsured, annualPremium, dates
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <button onClick={() => downloadCsv(false)} className="si-btn-secondary px-3 py-1.5 text-[10px] bg-white">
                  Download Template
                </button>
                <button onClick={() => downloadCsv(true)} className="si-btn-secondary px-3 py-1.5 text-[10px] bg-brand-50 border-brand-200 text-brand-700 hover:bg-brand-100">
                  Generate Prospects
                </button>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <input 
                type="file" 
                accept=".csv" 
                onChange={(e) => setBulkFile(e.target.files?.[0] || null)}
                className="text-xs flex-1 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-[10px] file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
              />
              <button 
                className="si-btn-primary py-1.5 px-4 text-xs whitespace-nowrap disabled:opacity-50"
                disabled={!bulkFile || bulkProcessing}
                onClick={async () => {
                  setBulkProcessing(true);
                  setBulkResult(null);
                  try {
                    const fd = new FormData();
                    fd.append("file", bulkFile!);
                    const res = await api.post("/company/policies/bulk", fd);
                    setBulkResult(res.data);
                    setBulkFile(null);
                    alert(`Processed: ${res.data.successCount} succeeded, ${res.data.failureCount} failed.`);
                  } catch (e: any) {
                    alert("Upload failed: " + (e.response?.data?.message || e.message));
                  } finally {
                    setBulkProcessing(false);
                  }
                }}
              >
                {bulkProcessing ? "Processing..." : "Upload & Issue"}
              </button>
            </div>

            {bulkResult && (
              <div className="rounded-lg bg-white border border-slate-200 p-3">
                <div className="flex justify-between text-[11px] font-semibold">
                  <span className="text-green-600">{bulkResult.successCount} Successful</span>
                  <span className="text-red-500">{bulkResult.failureCount} Failed</span>
                </div>
                {bulkResult.errors?.length > 0 && (
                  <div className="mt-2 max-h-32 overflow-y-auto text-[10px] text-ink-500 space-y-1 border-t pt-2">
                    {bulkResult.errors.map((err: string, idx: number) => (
                      <div key={idx} className="flex gap-2">
                        <span className="text-red-400">•</span>
                        <span>{err}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </Card>

      <Card className="mt-8 p-6">
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
          <div>
            <div className="text-sm font-semibold text-ink-950">Recent claims</div>
            <div className="text-xs text-ink-500">Latest submissions from your policyholders</div>
          </div>
          <Link to="/search" className="si-btn-secondary px-4 py-2 text-xs">
            <Search className="h-4 w-4" />
            Search vehicle
          </Link>
        </div>
        <div className="mt-5 divide-y divide-slate-100">
          {claims?.length ? (
            claims.map((cl) => (
              <div key={cl.claimPublicId} className="flex flex-wrap items-center justify-between gap-3 py-4">
                <div>
                  <div className="font-semibold text-ink-950">{cl.claimPublicId}</div>
                  <div className="text-xs text-ink-500">{cl.policyNumber}</div>
                </div>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-semibold uppercase tracking-wide text-ink-700 ring-1 ring-slate-200/70">
                  {cl.status?.replace(/_/g, " ")}
                </span>
              </div>
            ))
          ) : (
            <div className="py-10 text-center text-sm text-ink-500">No claims yet.</div>
          )}
        </div>
      </Card>

      <Card className="mt-8 p-6">
        <div className="mb-4">
          <div className="text-sm font-semibold text-ink-950">Customer Churn Prediction</div>
          <div className="text-xs text-ink-500">Upload a CSV to identify the top 10% customers likely to cancel their policy. Expected columns: INCOME, HAS_CHILDREN, LENGTH_OF_RESIDENCE, MARITAL_STATUS, HOME_MARKET_VALUE, HOME_OWNER, COLLEGE_DEGREE</div>
        </div>
        <div className="flex items-center gap-4 mb-6">
          <input 
            type="file" 
            accept=".csv" 
            onChange={(e) => setChurnFile(e.target.files?.[0] || null)} 
            className="text-sm text-ink-700 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
          />
          <button 
            disabled={!churnFile || churnLoading}
            onClick={handleChurnUpload}
            className="si-btn-primary px-4 py-2 text-sm disabled:opacity-50"
          >
            {churnLoading ? "Analyzing..." : "Predict Churn"}
          </button>
        </div>

        {churnResults.length > 0 && (
          <div className="mt-6 border-t border-slate-100 pt-6">
            <h4 className="text-sm font-semibold text-ink-950 mb-4">Top 10% At-Risk Customers (Out of {churnAnalyzed})</h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-ink-700">
                <thead className="bg-slate-50 text-xs uppercase text-ink-500">
                  <tr>
                    <th className="px-4 py-3">Income</th>
                    <th className="px-4 py-3">Residence</th>
                    <th className="px-4 py-3">Marital Status</th>
                    <th className="px-4 py-3">Home Owner</th>
                    <th className="px-4 py-3 text-right">Probability</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {churnResults.map((r, i) => (
                    <tr key={i} className="hover:bg-slate-50">
                      <td className="px-4 py-3">${r.INCOME?.toLocaleString()}</td>
                      <td className="px-4 py-3">{r.LENGTH_OF_RESIDENCE} yrs</td>
                      <td className="px-4 py-3">{r.MARITAL_STATUS}</td>
                      <td className="px-4 py-3">{r.HOME_OWNER}</td>
                      <td className="px-4 py-3 text-right font-semibold text-red-600">
                        {((r.churn_probability || 0) * 100).toFixed(1)}%
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Card>
    </AppShell>
  );
};
