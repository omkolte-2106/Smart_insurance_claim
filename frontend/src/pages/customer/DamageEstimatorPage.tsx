import React, { useState, useRef } from "react";
import { UploadCloud, CheckCircle2, AlertTriangle, Info, Image as ImageIcon } from "lucide-react";
import api from "../../api/client";

interface FileEstimate {
  fileName: string;
  severityScore: number;
  severityLabel: string;
  detectedParts: string[];
}

interface EstimateResponse {
  averageSeverityScore: number;
  overallSeverityLabel: string;
  estimatedPrice: number;
  currency: string;
  overallDetectedParts: string[];
  details: FileEstimate[];
}

export const DamageEstimatorPage: React.FC = () => {
  const [files, setFiles] = useState<FileList | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<EstimateResponse | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setFiles(e.dataTransfer.files);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!files || files.length === 0) {
      setError("Please select at least one image.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    try {
      const formData = new FormData();
      Array.from(files).forEach((file) => {
        formData.append("files", file);
      });

      const res = await api.post("/customer/estimate-damage", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setResult(res.data);
    } catch (err: any) {
      setError(err.message || "Failed to get estimate");
    } finally {
      setLoading(false);
    }
  };

  const getSeverityColor = (label: string) => {
    switch (label.toUpperCase()) {
      case "MINOR": return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "MODERATE": return "bg-amber-100 text-amber-800 border-amber-200";
      case "SEVERE": return "bg-rose-100 text-rose-800 border-rose-200";
      default: return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-5xl mx-auto space-y-12">
        {/* Header Section */}
        <div className="text-center space-y-4">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-slate-900 drop-shadow-sm">
            AI Damage Estimator
          </h1>
          <p className="text-lg text-slate-600 max-w-2xl mx-auto">
            Upload photos of your vehicle's damage to receive an instant, AI-powered repair estimate and parts analysis, without filing a claim.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
          {/* Upload Section */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white/70 backdrop-blur-xl p-8 rounded-3xl shadow-xl border border-white/50 relative overflow-hidden transition-all duration-300 hover:shadow-2xl">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500" />
              
              <form onSubmit={handleSubmit} className="space-y-6 relative z-10">
                <div 
                  className={`relative flex flex-col items-center justify-center w-full h-64 border-2 border-dashed rounded-2xl transition-all duration-300 ease-in-out cursor-pointer ${
                    dragActive ? "border-indigo-500 bg-indigo-50" : "border-slate-300 bg-slate-50 hover:bg-slate-100 hover:border-slate-400"
                  }`}
                  onDragEnter={handleDrag}
                  onDragLeave={handleDrag}
                  onDragOver={handleDrag}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={(e) => setFiles(e.target.files)}
                    className="hidden"
                  />
                  <UploadCloud className={`w-16 h-16 mb-4 ${dragActive ? "text-indigo-600" : "text-slate-400"}`} />
                  <p className="mb-2 text-sm text-slate-500 font-medium">
                    <span className="font-semibold text-indigo-600">Click to upload</span> or drag and drop
                  </p>
                  <p className="text-xs text-slate-400">PNG, JPG, JPEG up to 10MB</p>
                  
                  {files && files.length > 0 && (
                    <div className="absolute bottom-4 inset-x-4 bg-white/90 backdrop-blur rounded-lg p-2 flex items-center justify-center shadow-sm border border-slate-200">
                      <ImageIcon className="w-4 h-4 text-indigo-500 mr-2" />
                      <span className="text-sm font-semibold text-slate-700 truncate">{files.length} file(s) selected</span>
                    </div>
                  )}
                </div>

                {error && (
                  <div className="flex items-center p-3 text-sm text-rose-800 rounded-lg bg-rose-50 border border-rose-200">
                    <AlertTriangle className="w-4 h-4 mr-2 flex-shrink-0" />
                    <span>{error}</span>
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full relative overflow-hidden group bg-slate-900 text-white font-semibold py-4 px-6 rounded-xl transition-all hover:shadow-lg disabled:opacity-70 disabled:cursor-not-allowed"
                >
                  <div className="absolute inset-0 w-full h-full bg-gradient-to-r from-blue-600 to-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                  <span className="relative z-10 flex items-center justify-center">
                    {loading ? (
                      <>
                        <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        Analyzing Images...
                      </>
                    ) : (
                      "Generate Estimate"
                    )}
                  </span>
                </button>
              </form>
            </div>
          </div>

          {/* Results Section */}
          <div className="lg:col-span-3">
            {result ? (
              <div className="bg-white/80 backdrop-blur-2xl rounded-3xl p-8 shadow-2xl border border-white animate-in fade-in slide-in-from-bottom-4 duration-500">
                <div className="flex items-center justify-between mb-8 pb-4 border-b border-slate-100">
                  <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                    <CheckCircle2 className="w-6 h-6 text-emerald-500 mr-2" />
                    Analysis Complete
                  </h2>
                  <span className={`px-4 py-1.5 rounded-full text-sm font-bold tracking-wide border uppercase ${getSeverityColor(result.overallSeverityLabel)}`}>
                    {result.overallSeverityLabel} SEVERITY
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                  <div className="bg-gradient-to-br from-indigo-50 to-blue-50 p-6 rounded-2xl border border-indigo-100/50 relative overflow-hidden">
                    <div className="absolute top-0 right-0 p-4 opacity-10">
                      <svg className="w-16 h-16 text-indigo-900" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>
                    </div>
                    <p className="text-sm font-semibold text-indigo-800/70 uppercase tracking-wider mb-1">Estimated Payout</p>
                    <p className="text-4xl font-black text-indigo-950">
                      {result.currency} {result.estimatedPrice.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
                    </p>
                  </div>

                  <div className="bg-slate-50 p-6 rounded-2xl border border-slate-100 flex flex-col justify-center">
                    <p className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-1">Damage Severity Score</p>
                    <div className="flex items-baseline gap-2">
                      <p className="text-4xl font-black text-slate-800">{(result.averageSeverityScore * 100).toFixed(1)}<span className="text-2xl text-slate-500">%</span></p>
                    </div>
                  </div>
                </div>

                <div className="mb-8">
                  <h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center">
                    <Info className="w-5 h-5 text-slate-400 mr-2" />
                    Detected Damaged Parts
                  </h3>
                  {result.overallDetectedParts && result.overallDetectedParts.length > 0 ? (
                    <div className="flex flex-wrap gap-2">
                      {result.overallDetectedParts.map((part, idx) => (
                        <span key={idx} className="bg-white hover:bg-slate-50 shadow-sm transition-colors text-slate-700 px-3 py-1.5 rounded-lg text-sm font-semibold border border-slate-200/60">
                          {part}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-slate-500 text-sm italic bg-slate-50 p-4 rounded-xl border border-slate-100">No specific parts could be confidently identified.</p>
                  )}
                </div>

                <div>
                  <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4">Per-Image Breakdown</h3>
                  <div className="space-y-3">
                    {result.details.map((detail, idx) => (
                      <div key={idx} className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl bg-slate-50/50 border border-slate-100 hover:border-indigo-100 hover:bg-white transition-all duration-200">
                        <div className="flex-1 min-w-0 mb-2 sm:mb-0 sm:mr-4">
                          <p className="text-sm font-semibold text-slate-700 truncate" title={detail.fileName}>{detail.fileName}</p>
                          {detail.detectedParts && detail.detectedParts.length > 0 ? (
                            <p className="text-xs text-slate-500 truncate mt-1 font-medium">Parts: {detail.detectedParts.join(", ")}</p>
                          ) : (
                            <p className="text-xs text-slate-400 truncate mt-1 italic">No parts detected</p>
                          )}
                        </div>
                        <div className="flex items-center space-x-3 shrink-0">
                          <span className={`px-2.5 py-1 rounded-md text-xs font-bold uppercase tracking-wider ${getSeverityColor(detail.severityLabel)}`}>
                            {detail.severityLabel}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

              </div>
            ) : (
              <div className="h-full min-h-[400px] flex flex-col items-center justify-center border-2 border-dashed border-slate-200 rounded-3xl bg-white/40 p-8 text-center transition-all duration-500">
                <div className="w-20 h-20 bg-slate-100 rounded-2xl flex items-center justify-center mb-6 shadow-inner border border-white">
                  <ImageIcon className="w-10 h-10 text-slate-400" />
                </div>
                <h3 className="text-xl font-bold text-slate-700 mb-2">Awaiting Images</h3>
                <p className="text-slate-500 max-w-sm">Upload one or more photos of the damage to see the AI analysis results and parts breakdown here.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
