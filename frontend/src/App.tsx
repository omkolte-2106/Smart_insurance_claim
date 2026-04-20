import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import { LandingPage } from "./pages/LandingPage";
import { AboutPage } from "./pages/AboutPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterCustomerPage } from "./pages/RegisterCustomerPage";
import { RegisterCompanyPage } from "./pages/RegisterCompanyPage";
import { AdminDashboard } from "./pages/admin/AdminDashboard";
import { CompanyDashboard } from "./pages/company/CompanyDashboard";
import { CustomerDashboard } from "./pages/customer/CustomerDashboard";
import { ClaimFilingPage } from "./pages/claims/ClaimFilingPage";
import { ClaimTrackingPage } from "./pages/claims/ClaimTrackingPage";
import { FraudReviewPage } from "./pages/FraudReviewPage";
import { SearchPage } from "./pages/SearchPage";
import { DiscountAnalyticsPage } from "./pages/DiscountAnalyticsPage";
import { ProfilePage } from "./pages/ProfilePage";

const Protected: React.FC<{ role: string; children: React.ReactElement }> = ({ role, children }) => {
  const { token, role: r } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (r !== role) return <Navigate to="/" replace />;
  return children;
};

const ProtectedRoles: React.FC<{ roles: string[]; children: React.ReactElement }> = ({ roles, children }) => {
  const { token, role } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (!role || !roles.includes(role)) return <Navigate to="/" replace />;
  return children;
};

const App = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/about" element={<AboutPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register/customer" element={<RegisterCustomerPage />} />
      <Route path="/register/company" element={<RegisterCompanyPage />} />

      <Route
        path="/admin"
        element={
          <Protected role="ROLE_ADMIN">
            <AdminDashboard />
          </Protected>
        }
      />
      <Route
        path="/company"
        element={
          <Protected role="ROLE_COMPANY">
            <CompanyDashboard />
          </Protected>
        }
      />
      <Route
        path="/customer"
        element={
          <Protected role="ROLE_CUSTOMER">
            <CustomerDashboard />
          </Protected>
        }
      />

      <Route
        path="/claims/new"
        element={
          <Protected role="ROLE_CUSTOMER">
            <ClaimFilingPage />
          </Protected>
        }
      />
      <Route
        path="/claims/:id"
        element={
          <Protected role="ROLE_CUSTOMER">
            <ClaimTrackingPage />
          </Protected>
        }
      />

      <Route
        path="/fraud"
        element={
          <Protected role="ROLE_ADMIN">
            <FraudReviewPage />
          </Protected>
        }
      />
      <Route
        path="/search"
        element={
          <ProtectedRoles roles={["ROLE_ADMIN", "ROLE_COMPANY"]}>
            <SearchPage />
          </ProtectedRoles>
        }
      />

      <Route
        path="/discounts"
        element={
          <Protected role="ROLE_ADMIN">
            <DiscountAnalyticsPage mode="admin" />
          </Protected>
        }
      />
      <Route
        path="/discounts/company"
        element={
          <Protected role="ROLE_COMPANY">
            <DiscountAnalyticsPage mode="company" />
          </Protected>
        }
      />

      <Route path="/profile" element={<ProfilePage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
