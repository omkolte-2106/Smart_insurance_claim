import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Building2, ClipboardList, LayoutDashboard, Percent, Search, UserCircle, Wallet, X, ExternalLink, CheckCircle, AlertCircle, MessageSquare } from "lucide-react";
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
  
  const [selectedClaim, setSelectedClaim] = useState<any | null>(null);
  const [reviewRemarks, setReviewRemarks] = useState("");
  const [reviewLoading, setReviewLoading] = useState(false);

  const [dateFilter, setDateFilter] = useState({ start: "", end: "" });
  const [filteredClaims, setFilteredClaims] = useState<any[]>([]);

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
      const [d, c] = await Promise.all([api.get("/company/dashboard"), api.get("/claims?size=20&sort=createdAt,desc")]);
      setDash(d.data);
      const allClaims = c.data.content ?? c.data;
      setClaims(allClaims);
      setFilteredClaims(allClaims);
    })().catch(() => {});
  }, []);

  useEffect(() => {
    let result = [...claims];
    if (dateFilter.start) {
      result = result.filter(c => new Date(c.createdAt) >= new Date(dateFilter.start));
    }
    if (dateFilter.end) {
      // Add 23:59:59 to include the entire end day
      const end = new Date(dateFilter.end);
      end.setHours(23, 59, 59, 999);
      result = result.filter(c => new Date(c.createdAt) <= end);
    }
    setFilteredClaims(result);
  }, [dateFilter, claims]);

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
            hint="Sum of approved claim payouts"
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
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 bg-white">
                <span className="text-[10px] font-bold text-ink-400 uppercase">From</span>
                <input 
                  type="date" 
                  className="bg-transparent border-0 text-[10px] focus:ring-0 p-0" 
                  value={dateFilter.start}
                  onChange={e => setDateFilter({...dateFilter, start: e.target.value})}
                />
              </div>
              <div className="flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 bg-white">
                <span className="text-[10px] font-bold text-ink-400 uppercase">To</span>
                <input 
                  type="date" 
                  className="bg-transparent border-0 text-[10px] focus:ring-0 p-0" 
                  value={dateFilter.end}
                  onChange={e => setDateFilter({...dateFilter, end: e.target.value})}
                />
              </div>
              {claims?.length > 0 && (
                <button onClick={() => setClaims([])} className="text-[10px] font-bold text-ink-400 hover:text-ink-600 transition-colors uppercase tracking-wider ml-2">
                  Clear
                </button>
              )}
              <Link to="/search" className="si-btn-secondary px-3 py-1.5 text-xs ml-2">
                <Search className="h-3.5 w-3.5 mr-1" />
                Search
              </Link>
            </div>
          </div>
        <div className="mt-5 divide-y divide-slate-100">
          {filteredClaims?.length ? (
            filteredClaims.map((cl, idx) => {
              const isRecent = idx === 0 && !dateFilter.start && !dateFilter.end;
              return (
                <div key={cl.claimPublicId} className={`flex flex-wrap items-center justify-between gap-3 py-4 ${isRecent ? "bg-brand-50/30 -mx-6 px-6 border-y border-brand-100/50" : ""}`}>
                  <div className="flex items-center gap-4">
                    <div className={`p-2 rounded-lg ${isRecent ? "bg-brand-100 text-brand-700" : "bg-slate-50 text-slate-400"}`}>
                      <ClipboardList className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <div className="font-bold text-ink-950">{cl.claimPublicId}</div>
                        {isRecent && <span className="text-[8px] bg-brand-600 text-white px-1.5 py-0.5 rounded-full font-black uppercase tracking-tighter">Latest</span>}
                      </div>
                      <div className="text-[11px] font-medium text-ink-700 flex items-center gap-1">
                        <UserCircle className="h-3 w-3 inline" /> {cl.customerName || "Unknown Customer"}
                      </div>
                      <div className="text-[10px] text-ink-500 font-medium">Policy: {cl.policyNumber} • {new Date(cl.createdAt).toLocaleDateString()}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className={`rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-wide ring-1 ${
                      cl.status === 'SETTLED' ? 'bg-green-50 text-green-700 ring-green-200' :
                      cl.status === 'APPROVED' ? 'bg-brand-50 text-brand-700 ring-brand-200' :
                      cl.status === 'REJECTED' ? 'bg-red-50 text-red-700 ring-red-200' :
                      'bg-slate-50 text-ink-700 ring-slate-200/70'
                    }`}>
                      {cl.status?.replace(/_/g, " ")}
                    </span>
                    {(cl.status === "MANUAL_REVIEW_PENDING" || cl.status === "FRAUD_FLAGGED" || cl.status === "AI_VERIFIED" || cl.status === "DOCUMENTS_UPLOADED") && (
                      <button 
                        onClick={() => setSelectedClaim(cl)}
                        className="si-btn-primary px-4 py-1.5 text-[11px]"
                      >
                        Review
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          ) : (
            <div className="py-12 text-center">
              <div className="text-sm text-ink-500">No claims matching your filter.</div>
              {(dateFilter.start || dateFilter.end) && (
                <button onClick={() => setDateFilter({start:"", end:""})} className="mt-2 text-xs text-brand-600 font-bold hover:underline">
                  Clear Filters
                </button>
              )}
            </div>
          )}
        </div>
      </Card>

      <Card className="mt-8 p-6">
        <div className="mb-4">
          <div className="text-sm font-semibold text-ink-950">Renewal Intelligence</div>
          <div className="text-xs text-ink-500">Scan demographics to find your top 10% customers for renewal.</div>
        </div>
        <div className="flex items-center gap-4 mb-6">
          <input 
            type="file" 
            accept=".csv" 
            onChange={(e) => setChurnFile(e.target.files?.[0] || null)} 
            className="text-xs text-ink-700 file:mr-4 file:py-1.5 file:px-4 file:rounded-lg file:border-0 file:text-[10px] file:font-bold file:bg-slate-100 file:text-ink-700 hover:file:bg-slate-200 transition-all cursor-pointer"
          />
          <button 
            disabled={!churnFile || churnLoading}
            onClick={handleChurnUpload}
            className="si-btn-primary px-5 py-2 text-xs disabled:opacity-50 shadow-sm"
          >
            {churnLoading ? "Analyzing..." : "Analyze Batch"}
          </button>
        </div>

        {churnResults.length > 0 && (
          <div className="mt-6 border-t border-slate-100 pt-6">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-sm font-semibold text-ink-950">Renewal Recommendations</h4>
              <span className="text-[10px] font-bold text-brand-600 bg-brand-50 px-2 py-0.5 rounded-full uppercase tracking-wider">Top 10% Identified</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-ink-700">
                <thead className="bg-slate-50 text-xs uppercase text-ink-500">
                  <tr>
                    <th className="px-4 py-3">Income</th>
                    <th className="px-4 py-3">Residence</th>
                    <th className="px-4 py-3">Marital Status</th>
                    <th className="px-4 py-3">Home Owner</th>
                    <th className="px-4 py-3 text-right">Priority Score</th>
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

      {selectedClaim && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 backdrop-blur-sm p-4">
          <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto p-0 flex flex-col">
            <div className="sticky top-0 bg-white z-10 px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-ink-950">{selectedClaim.claimPublicId} Review</h3>
                <p className="text-xs text-ink-500">{selectedClaim.policyNumber} • {selectedClaim.vehicleRegistration}</p>
              </div>
              <button onClick={() => setSelectedClaim(null)} className="p-2 hover:bg-slate-100 rounded-full transition-colors">
                <X className="h-5 w-5 text-ink-500" />
              </button>
            </div>
            
            <div className="p-6 space-y-6">
              <div className="grid grid-cols-3 gap-4">
                <div className="rounded-xl bg-slate-50 p-3 border border-slate-100 text-center">
                  <div className="text-[10px] uppercase font-bold text-ink-400 mb-1">AI Severity</div>
                  <div className="text-xl font-black text-ink-900">{(selectedClaim.damageSeverityScore * 100).toFixed(0)}%</div>
                </div>
                <div className="rounded-xl bg-slate-50 p-3 border border-slate-100 text-center">
                  <div className="text-[10px] uppercase font-bold text-ink-400 mb-1">AI Fraud</div>
                  <div className="text-xl font-black text-ink-900">{(selectedClaim.fraudScore || 0).toFixed(0)}%</div>
                </div>
                <div className="rounded-xl bg-slate-50 p-3 border border-slate-100 text-center">
                  <div className="text-[10px] uppercase font-bold text-ink-400 mb-1">Est. Payout</div>
                  <div className="text-xl font-black text-brand-600">₹{Number(selectedClaim.estimatedPayoutAmount || 0).toLocaleString()}</div>
                </div>
              </div>

              <div>
                <h4 className="text-[10px] uppercase font-bold text-ink-400 mb-3 tracking-widest flex items-center gap-2">
                  <ClipboardList className="h-3 w-3" /> Submitted Documents
                </h4>
                <div className="space-y-2">
                  {selectedClaim.documents?.map((doc: any) => (
                    <div key={doc.id} className="flex items-center justify-between p-3 rounded-xl border border-slate-200 bg-white hover:border-brand-200 transition-colors group">
                      <div className="flex items-center gap-3">
                        <div className="p-2 bg-slate-50 rounded-lg group-hover:bg-brand-50 transition-colors">
                          <ClipboardList className="h-4 w-4 text-ink-600 group-hover:text-brand-600" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-ink-950 uppercase">{doc.documentType.replace(/_/g, " ")}</p>
                          <p className="text-[10px] text-ink-500">{doc.originalFilename}</p>
                        </div>
                      </div>
                      <a 
                        href={`${api.defaults.baseURL}/claims/${selectedClaim.claimPublicId}/documents/${doc.id}/content?token=${localStorage.getItem("si_token")}`}
                        target="_blank" 
                        rel="noreferrer"
                        className="flex items-center gap-1 text-[10px] font-bold text-brand-600 hover:text-brand-700 bg-brand-50 px-3 py-1.5 rounded-lg"
                      >
                        <ExternalLink className="h-3 w-3" /> View
                      </a>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <h4 className="text-[10px] uppercase font-bold text-ink-400 mb-3 tracking-widest flex items-center gap-2">
                  <MessageSquare className="h-3 w-3" /> Incident Details
                </h4>
                <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs text-ink-700 leading-relaxed italic">
                  "{selectedClaim.incidentDescription}"
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100">
                <label className="block text-[11px] font-bold text-ink-950 mb-2">Manual Review Decision & Remarks</label>
                <textarea 
                  className="si-input w-full min-h-[100px] text-xs py-3"
                  placeholder="Provide reasoning for your decision (e.g. Damage matches description, suspicious metadata found, etc.)"
                  value={reviewRemarks}
                  onChange={(e) => setReviewRemarks(e.target.value)}
                />
              </div>
            </div>

            <div className="sticky bottom-0 bg-white p-6 border-t border-slate-100 flex gap-3">
              <button 
                disabled={reviewLoading}
                onClick={async () => {
                  setReviewLoading(true);
                  try {
                    await api.post(`/company/claims/${selectedClaim.claimPublicId}/decision`, {
                      targetStatus: "REJECTED",
                      remarks: reviewRemarks
                    });
                    setSelectedClaim(null);
                    setReviewRemarks("");
                    // Refresh listing
                    const [d, c] = await Promise.all([api.get("/company/dashboard"), api.get("/claims?size=8")]);
                    setDash(d.data);
                    setClaims(c.data.content ?? c.data);
                  } catch (e: any) { alert("Action failed: " + (e.response?.data?.message || e.message)); }
                  finally { setReviewLoading(false); }
                }}
                className="si-btn-secondary flex-1 py-3 text-xs border-red-200 text-red-600 hover:bg-red-50"
              >
                <AlertCircle className="h-4 w-4" /> Reject Claim
              </button>
              <button 
                disabled={reviewLoading}
                onClick={async () => {
                  setReviewLoading(true);
                  try {
                    await api.post(`/company/claims/${selectedClaim.claimPublicId}/decision`, {
                      targetStatus: "APPROVED",
                      remarks: reviewRemarks
                    });
                    setSelectedClaim(null);
                    setReviewRemarks("");
                    // Refresh listing
                    const [d, c] = await Promise.all([api.get("/company/dashboard"), api.get("/claims?size=8")]);
                    setDash(d.data);
                    setClaims(c.data.content ?? c.data);
                  } catch (e: any) { alert("Action failed: " + (e.response?.data?.message || e.message)); }
                  finally { setReviewLoading(false); }
                }}
                className="si-btn-primary flex-1 py-3 text-xs"
              >
                <CheckCircle className="h-4 w-4" /> Approve Claim
              </button>
            </div>
          </Card>
        </div>
      )}
    </AppShell>
  );
};
