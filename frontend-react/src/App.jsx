import { useEffect, useMemo, useState } from "react";
import {
  Link,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { apiRequest, API_BASE_URL } from "./api";

const USER_STORAGE_KEY = "bank_user_session";
const ADMIN_STORAGE_KEY = "bank_admin_session";

function App() {
  const [userSession, setUserSession] = useState(() => readStorage(USER_STORAGE_KEY));
  const [adminSession, setAdminSession] = useState(() => readStorage(ADMIN_STORAGE_KEY));

  return (
    <div className="app-shell">
      <Routes>
        <Route
          path="/"
          element={<HomePage userSession={userSession} adminSession={adminSession} />}
        />
        <Route
          path="/register"
          element={<RegisterPage onLogin={(account) => setAndStoreUserSession(setUserSession, account)} />}
        />
        <Route
          path="/login"
          element={<LoginPage onLogin={(account) => setAndStoreUserSession(setUserSession, account)} />}
        />
        <Route path="/forgot-password" element={<ForgotPasswordPage mode="user" />} />
        <Route path="/reset-password" element={<ResetPasswordPage mode="user" />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute isAllowed={Boolean(userSession)}>
              <UserDashboard
                userSession={userSession}
                onLogout={() => clearSession(USER_STORAGE_KEY, setUserSession)}
                onSessionUpdate={(account) => setAndStoreUserSession(setUserSession, account)}
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={<AdminLoginPage onLogin={(admin) => setAndStoreAdminSession(setAdminSession, admin)} />}
        />
        <Route path="/admin/forgot-password" element={<ForgotPasswordPage mode="admin" />} />
        <Route path="/admin/reset-password" element={<ResetPasswordPage mode="admin" />} />
        <Route
          path="/admin/dashboard"
          element={
            <ProtectedRoute isAllowed={Boolean(adminSession)}>
              <AdminDashboard
                adminSession={adminSession}
                onLogout={() => clearSession(ADMIN_STORAGE_KEY, setAdminSession)}
              />
            </ProtectedRoute>
          }
        />
      </Routes>
    </div>
  );
}

function HomePage({ userSession, adminSession }) {
  return (
    <main className="page">
      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow">Modern Banking Demo</span>
   
          <p>
         This interface adds OTP signup, password reset, customer dashboard,
            statement download, and admin account controls.
          </p>
          <div className="hero-actions">
            <Link className="button primary" to="/register">
              Register
            </Link>
            <Link className="button secondary" to="/login">
              User Login
            </Link>
            <Link className="button ghost" to="/admin">
              Admin Portal
            </Link>
          </div>
          {userSession ? (
            <p className="inline-note">
              Active user session: <strong>{userSession.holderName}</strong>
            </p>
          ) : null}
          {adminSession ? (
            <p className="inline-note">
              Active admin session: <strong>{adminSession.displayName}</strong>
            </p>
          ) : null}
        </div>
        <div className="hero-panel">
          <div className="metric-card">
            <span>Capabilities</span>
            <strong>OTP, reset links, statements</strong>
          </div>
          <div className="metric-card">
            <span>Admin tools</span>
            <strong>View, freeze, unfreeze accounts</strong>
          </div>
          <div className="metric-card">
            <span>Backend API</span>
            <strong>{API_BASE_URL}</strong>
          </div>
        </div>
      </section>

      <section className="feature-grid">
        <FeatureCard
          title="Secure onboarding"
          description="Registration sends an OTP to email and only unlocks final signup after successful verification."
        />
        <FeatureCard
          title="Customer self-service"
          description="Users can log in, deposit, withdraw, transfer funds, and download a statement from one dashboard."
        />
        <FeatureCard
          title="Admin operations"
          description="Managers can access a separate admin URL, review accounts, and freeze or unfreeze customers instantly."
        />
      </section>
    </main>
  );
}

function RegisterPage({ onLogin }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    depositAmount: "",
  });
  const [otp, setOtp] = useState("");
  const [otpRequested, setOtpRequested] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSendOtp(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await apiRequest("/auth/register/request-otp", {
        method: "POST",
        body: toRegisterPayload(form),
      });
      setOtpRequested(true);
      setOtpVerified(false);
      setMessage(response.message);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyOtp(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await apiRequest("/auth/register/verify-otp", {
        method: "POST",
        body: { email: form.email, otp },
      });
      setOtpVerified(true);
      setMessage(response.message);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleRegister(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await apiRequest("/auth/register", {
        method: "POST",
        body: toRegisterPayload(form),
      });
      onLogin(response.account);
      navigate("/dashboard");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      description="Register with your name, email, password, and opening deposit. OTP verification is required before signup."
      footer={<Link to="/login">Already have an account? Log in</Link>}
    >
      <form className="stack" onSubmit={handleRegister}>
        <InputField label="Full name" value={form.name} onChange={(value) => updateForm(setForm, "name", value)} />
        <InputField label="Email address" type="email" value={form.email} onChange={(value) => updateForm(setForm, "email", value)} />
        <InputField label="Password" type="password" value={form.password} onChange={(value) => updateForm(setForm, "password", value)} />
        <InputField label="Deposit amount" type="number" value={form.depositAmount} onChange={(value) => updateForm(setForm, "depositAmount", value)} />
        <div className="split-actions">
          <button className="button secondary" onClick={handleSendOtp} disabled={loading} type="button">
            {otpRequested ? "Resend OTP" : "Send OTP"}
          </button>
          <span className={`status-pill ${otpVerified ? "verified" : ""}`}>
            {otpVerified ? "Email verified" : "Verification pending"}
          </span>
        </div>

        {otpRequested ? (
          <div className="otp-panel">
            <InputField label="Enter OTP" value={otp} onChange={setOtp} />
            <button className="button ghost" onClick={handleVerifyOtp} disabled={loading} type="button">
              Verify OTP
            </button>
          </div>
        ) : null}

        {message ? <p className="message success">{message}</p> : null}
        {error ? <p className="message error">{error}</p> : null}

        <button className="button primary" type="submit" disabled={!otpVerified || loading}>
          Register account
        </button>
      </form>
    </AuthLayout>
  );
}

function LoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await apiRequest("/auth/login", {
        method: "POST",
        body: form,
      });
      onLogin(response.account);
      navigate("/dashboard");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      title="User login"
      description="Sign in with your email and password to access your dashboard."
      footer={
        <div className="auth-links">
          <Link to="/forgot-password">Forgot password?</Link>
          <Link to="/register">Create account</Link>
        </div>
      }
    >
      <form className="stack" onSubmit={handleSubmit}>
        <InputField label="Email address" type="email" value={form.email} onChange={(value) => updateForm(setForm, "email", value)} />
        <InputField label="Password" type="password" value={form.password} onChange={(value) => updateForm(setForm, "password", value)} />
        {error ? <p className="message error">{error}</p> : null}
        <button className="button primary" type="submit" disabled={loading}>
          Login
        </button>
      </form>
    </AuthLayout>
  );
}

function ForgotPasswordPage({ mode }) {
  const [email, setEmail] = useState(
    mode === "admin" ? "chaitanyag1718@gmail.com" : "",
  );
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    setError("");

    try {
      const endpoint =
        mode === "admin" ? "/admin/forgot-password" : "/auth/forgot-password";
      const response = await apiRequest(endpoint, {
        method: "POST",
        body: { email },
      });
      setMessage(response.message);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      title={mode === "admin" ? "Admin password reset" : "Reset your password"}
      description="A reset link will be sent to the email you provide."
      footer={
        <Link to={mode === "admin" ? "/admin" : "/login"}>
          Back to {mode === "admin" ? "admin login" : "user login"}
        </Link>
      }
    >
      <form className="stack" onSubmit={handleSubmit}>
        <InputField label="Email address" type="email" value={email} onChange={setEmail} />
        {message ? <p className="message success">{message}</p> : null}
        {error ? <p className="message error">{error}</p> : null}
        <button className="button primary" type="submit" disabled={loading}>
          Send reset link
        </button>
      </form>
    </AuthLayout>
  );
}

function ResetPasswordPage({ mode }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const [newPassword, setNewPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    setError("");

    try {
      const endpoint =
        mode === "admin" ? "/admin/reset-password" : "/auth/reset-password";
      const response = await apiRequest(endpoint, {
        method: "POST",
        body: { token, newPassword },
      });
      setMessage(response.message);
      setTimeout(() => navigate(mode === "admin" ? "/admin" : "/login"), 1200);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      title={mode === "admin" ? "Set new admin password" : "Set new password"}
      description="Enter a new password to complete the reset flow."
      footer={
        <Link to={mode === "admin" ? "/admin" : "/login"}>
          Back to {mode === "admin" ? "admin login" : "user login"}
        </Link>
      }
    >
      <form className="stack" onSubmit={handleSubmit}>
        <InputField label="Reset token" value={token} onChange={() => {}} disabled />
        <InputField label="New password" type="password" value={newPassword} onChange={setNewPassword} />
        {message ? <p className="message success">{message}</p> : null}
        {error ? <p className="message error">{error}</p> : null}
        <button className="button primary" type="submit" disabled={!token || loading}>
          Update password
        </button>
      </form>
    </AuthLayout>
  );
}

function UserDashboard({ userSession, onLogout, onSessionUpdate }) {
  const navigate = useNavigate();
  const [account, setAccount] = useState(userSession);
  const [action, setAction] = useState("deposit");
  const [amount, setAmount] = useState("");
  const [targetAccount, setTargetAccount] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [transactionState, setTransactionState] = useState("idle");

  useEffect(() => {
    setAccount(userSession);
  }, [userSession]);

  const greeting = useMemo(
    () => `Welcome back, ${account?.holderName || "Customer"}`,
    [account],
  );

  const selectedAction = ACTION_OPTIONS.find((option) => option.id === action) || ACTION_OPTIONS[0];

  async function handleAction(event) {
    event.preventDefault();
    setBusy(true);
    setTransactionState("processing");
    setMessage("");
    setError("");

    try {
      if (action === "deposit") {
        const [response] = await Promise.all([
          apiRequest("/transactions/deposit", {
            method: "POST",
            body: { accNo: account.accountNumber, amount: Number(amount) },
          }),
          wait(900),
        ]);
        updateAccount(response.account);
        setMessage(response.message);
      }

      if (action === "withdraw") {
        const [response] = await Promise.all([
          apiRequest("/transactions/withdraw", {
            method: "POST",
            body: { accNo: account.accountNumber, amount: Number(amount) },
          }),
          wait(900),
        ]);
        updateAccount(response.account);
        setMessage(response.message);
      }

      if (action === "transfer") {
        const [response] = await Promise.all([
          apiRequest("/transactions/transfer", {
            method: "POST",
            body: {
              fromAcc: account.accountNumber,
              toAcc: targetAccount,
              amount: Number(amount),
            },
          }),
          wait(900),
        ]);
        updateAccount(response.fromAccount);
        setMessage(response.message);
      }

      setTransactionState("success");
      await wait(1200);
      setTransactionState("idle");
      setAmount("");
      setTargetAccount("");
    } catch (requestError) {
      setTransactionState("idle");
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleDownloadStatement() {
    setBusy(true);
    setMessage("");
    setError("");

    try {
      const blob = await apiRequest(`/accounts/${account.accountNumber}/statement`, {
        responseType: "blob",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `statement-${account.accountNumber}.csv`;
      link.click();
      window.URL.revokeObjectURL(url);
      setMessage("Statement download started.");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  function updateAccount(nextAccount) {
    setAccount(nextAccount);
    onSessionUpdate(nextAccount);
  }

  return (
    <main className="page dashboard-page">
      <section className="dashboard-header">
        <div>
          <span className="eyebrow">Customer dashboard</span>
          <h1>{greeting}</h1>
          <p>
            Account number: <strong>{account.accountNumber}</strong>
          </p>
        </div>
        <div className="hero-actions">
          <button className="button ghost" onClick={handleDownloadStatement} disabled={busy}>
            Download statement
          </button>
          <button
            className="button secondary"
            onClick={() => {
              onLogout();
              navigate("/login");
            }}
          >
            Logout
          </button>
        </div>
      </section>

      <section className="dashboard-grid">
        <div className="panel">
          <h2>Account details</h2>
          <div className="detail-list">
            <Detail label="Name" value={account.holderName} />
            <Detail label="Email" value={account.email} />
            <Detail label="Balance" value={`Rs. ${Number(account.openingBalance).toFixed(2)}`} />
            <Detail label="Status" value={account.status} />
          </div>
        </div>

        <div className="panel actions-panel">
          <h2>Banking actions</h2>
          <div className="action-card-grid">
            {ACTION_OPTIONS.map((option) => (
              <button
                key={option.id}
                className={`action-card ${action === option.id ? "active" : ""}`}
                onClick={() => setAction(option.id)}
                type="button"
              >
                <span className="action-icon" aria-hidden="true">
                  <ActionIcon type={option.id} />
                </span>
                <span className="action-copy">
                  <strong>{option.label}</strong>
                  <small>{option.description}</small>
                </span>
              </button>
            ))}
          </div>
          <form className="stack compact action-form-card" onSubmit={handleAction}>
            <div className="action-form-heading">
              <span className="action-icon large" aria-hidden="true">
                <ActionIcon type={selectedAction.id} />
              </span>
              <div>
                <strong>{selectedAction.label}</strong>
                <p>{selectedAction.helperText}</p>
              </div>
            </div>
            <InputField label="Amount" type="number" value={amount} onChange={setAmount} disabled={busy} />
            {action === "transfer" ? (
              <InputField
                label="Receiver account number"
                value={targetAccount}
                onChange={setTargetAccount}
                disabled={busy}
              />
            ) : null}
            {message ? <p className="message success">{message}</p> : null}
            {error ? <p className="message error">{error}</p> : null}
            <button className="button primary action-submit" type="submit" disabled={busy}>
              {selectedAction.ctaLabel}
            </button>
          </form>
        </div>
      </section>

      {transactionState !== "idle" ? (
        <div className="transaction-overlay" role="status" aria-live="polite">
          <div className={`transaction-modal ${transactionState}`}>
            <div className={`transaction-loader ${transactionState}`}>
              {transactionState === "success" ? <SuccessCheckIcon /> : <span className="loader-ring" />}
            </div>
            <strong>
              {transactionState === "processing"
                ? `${selectedAction.label} in progress`
                : `${selectedAction.label} successful`}
            </strong>
            <p>
              {transactionState === "processing"
                ? "Please wait while we securely process your banking request."
                : "Your account has been updated successfully."}
            </p>
          </div>
        </div>
      ) : null}
    </main>
  );
}

function AdminLoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: "chaitanyag1718@gmail.com",
    password: "Admin@bank123",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await apiRequest("/admin/login", {
        method: "POST",
        body: form,
      });
      onLogin(response.admin);
      navigate("/admin/dashboard");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      title="Admin login"
      description="Use the manager credentials to access the operations dashboard."
      footer={
        <div className="auth-links">
          <Link to="/admin/forgot-password">Forgot password?</Link>
          <Link to="/">Back home</Link>
        </div>
      }
    >
      <form className="stack" onSubmit={handleSubmit}>
        <InputField label="Admin email" type="email" value={form.email} onChange={(value) => updateForm(setForm, "email", value)} />
        <InputField label="Password" type="password" value={form.password} onChange={(value) => updateForm(setForm, "password", value)} />
        {error ? <p className="message error">{error}</p> : null}
        <button className="button primary" type="submit" disabled={loading}>
          Login as admin
        </button>
      </form>
    </AuthLayout>
  );
}

function AdminDashboard({ adminSession, onLogout }) {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [balanceFilter, setBalanceFilter] = useState("all");
  const [lowBalanceLimit, setLowBalanceLimit] = useState("1000");
  const [currentPage, setCurrentPage] = useState(1);

  const accountsPerPage = 10;

  useEffect(() => {
    loadAccounts();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, balanceFilter, lowBalanceLimit]);

  const filteredAccounts = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();
    const threshold = Number(lowBalanceLimit);

    return accounts.filter((account) => {
      const matchesSearch =
        !normalizedSearch ||
        account.accountNumber?.toString().toLowerCase().includes(normalizedSearch) ||
        account.holderName?.toLowerCase().includes(normalizedSearch) ||
        account.email?.toLowerCase().includes(normalizedSearch);

      const balance = Number(account.openingBalance) || 0;
      const matchesBalance =
        balanceFilter !== "low" || Number.isNaN(threshold) ? true : balance <= threshold;

      return matchesSearch && matchesBalance;
    });
  }, [accounts, balanceFilter, lowBalanceLimit, searchTerm]);

  const totalPages = Math.max(1, Math.ceil(filteredAccounts.length / accountsPerPage));
  const paginatedAccounts = filteredAccounts.slice(
    (currentPage - 1) * accountsPerPage,
    currentPage * accountsPerPage,
  );

  useEffect(() => {
    setCurrentPage((page) => Math.min(page, totalPages));
  }, [totalPages]);

  async function loadAccounts() {
    setLoading(true);
    setError("");
    try {
      const response = await apiRequest("/admin/accounts");
      setAccounts(response);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusChange(accountNumber, status) {
    setMessage("");
    setError("");

    try {
      const response = await apiRequest(`/admin/accounts/${accountNumber}/status`, {
        method: "POST",
        body: { status },
      });
      setAccounts((current) =>
        current.map((account) =>
          account.accountNumber === accountNumber ? response.account : account,
        ),
      );
      setMessage(response.message);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <main className="page dashboard-page">
      <section className="dashboard-header">
        <div>
          <span className="eyebrow">Admin console</span>
          <h1>Welcome, {adminSession.displayName}</h1>
          <p>Manage customer account visibility and transaction availability.</p>
        </div>
        <div className="hero-actions">
          <button className="button secondary" onClick={loadAccounts}>
            Refresh accounts
          </button>
          <button
            className="button ghost"
            onClick={() => {
              onLogout();
              navigate("/admin");
            }}
          >
            Logout
          </button>
        </div>
      </section>

      {message ? <p className="message success">{message}</p> : null}
      {error ? <p className="message error">{error}</p> : null}

      <section className="panel">
        <h2>View accounts</h2>
        {loading ? <p className="inline-note">Loading accounts...</p> : null}
        <div className="accounts-toolbar">
          <label className="field">
            <span>Search accounts</span>
            <input
              placeholder="Search by name, account ID, or email"
              type="text"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>

          <label className="field field-compact">
            <span>Balance filter</span>
            <select value={balanceFilter} onChange={(event) => setBalanceFilter(event.target.value)}>
              <option value="all">All accounts</option>
              <option value="low">Low balance only</option>
            </select>
          </label>

          <label className="field field-compact">
            <span>Low balance threshold</span>
            <input
              min="0"
              step="100"
              type="number"
              value={lowBalanceLimit}
              onChange={(event) => setLowBalanceLimit(event.target.value)}
            />
          </label>
        </div>

        <div className="accounts-toolbar accounts-toolbar-summary">
          <p className="inline-note">
            Showing {paginatedAccounts.length} of {filteredAccounts.length} matching accounts
          </p>
          <p className="inline-note">
            Page {currentPage} of {totalPages}
          </p>
        </div>

        <div className="accounts-table">
          <div className="table-row table-head">
            <span>Account</span>
            <span>Customer</span>
            <span>Email</span>
            <span>Balance</span>
            <span>Status</span>
            <span>Manage</span>
          </div>
          {paginatedAccounts.map((account) => (
            <div className="table-row" key={account.accountNumber}>
              <span>{account.accountNumber}</span>
              <span>{account.holderName}</span>
              <span>{account.email}</span>
              <span>Rs. {Number(account.openingBalance).toFixed(2)}</span>
              <span>
                <span className={`status-pill ${account.status === "ACTIVE" ? "verified" : ""}`}>
                  {account.status}
                </span>
              </span>
              <span className="manage-actions">
                <button
                  className="button small secondary"
                  disabled={account.status === "FROZEN"}
                  onClick={() => handleStatusChange(account.accountNumber, "FROZEN")}
                >
                  Freeze
                </button>
                <button
                  className="button small ghost"
                  disabled={account.status === "ACTIVE"}
                  onClick={() => handleStatusChange(account.accountNumber, "ACTIVE")}
                >
                  Unfreeze
                </button>
              </span>
            </div>
          ))}
        </div>

        {!loading && !paginatedAccounts.length ? (
          <p className="inline-note">No accounts matched the current search and filter settings.</p>
        ) : null}

        <div className="pagination-bar">
          <button
            className="button small secondary"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
          >
            Previous
          </button>
          <button
            className="button small secondary"
            disabled={currentPage === totalPages}
            onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
          >
            Next
          </button>
        </div>
      </section>
    </main>
  );
}

function AuthLayout({ title, description, footer, children }) {
  return (
    <main className="page centered-page">
      <section className="auth-card">
        <div className="auth-heading">
          <Link className="back-link" to="/">
            Home
          </Link>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        {children}
        <div className="auth-footer">{footer}</div>
      </section>
    </main>
  );
}

function ProtectedRoute({ isAllowed, children }) {
  const location = useLocation();
  if (!isAllowed) {
    return <Navigate to={location.pathname.startsWith("/admin") ? "/admin" : "/login"} replace />;
  }
  return children;
}

function InputField({ label, type = "text", value, onChange, disabled = false }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled}
        required={!disabled}
      />
    </label>
  );
}

function FeatureCard({ title, description }) {
  return (
    <article className="feature-card">
      <h2>{title}</h2>
      <p>{description}</p>
    </article>
  );
}

function Detail({ label, value }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function toRegisterPayload(form) {
  return {
    ...form,
    depositAmount: Number(form.depositAmount),
  };
}

function updateForm(setter, key, value) {
  setter((current) => ({ ...current, [key]: value }));
}

function persistSession(key, value) {
  window.localStorage.setItem(key, JSON.stringify(value));
}

function readStorage(key) {
  const stored = window.localStorage.getItem(key);
  return stored ? JSON.parse(stored) : null;
}

function clearSession(key, setter) {
  window.localStorage.removeItem(key);
  setter(null);
}

function setAndStoreUserSession(setter, account) {
  persistSession(USER_STORAGE_KEY, account);
  setter(account);
}

function setAndStoreAdminSession(setter, admin) {
  persistSession(ADMIN_STORAGE_KEY, admin);
  setter(admin);
}

const ACTION_OPTIONS = [
  {
    id: "deposit",
    label: "Deposit",
    description: "Add funds",
    helperText: "Top up this account securely and reflect the new balance instantly.",
    ctaLabel: "Confirm deposit",
  },
  {
    id: "withdraw",
    label: "Withdraw",
    description: "Take money out",
    helperText: "Withdraw funds from the current account after verifying the amount.",
    ctaLabel: "Confirm withdrawal",
  },
  {
    id: "transfer",
    label: "Transfer",
    description: "Send to another account",
    helperText: "Move money to another account number with secure processing.",
    ctaLabel: "Confirm transfer",
  },
];

function ActionIcon({ type }) {
  if (type === "deposit") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M12 4v16" />
        <path d="M6 10l6-6 6 6" />
        <rect x="4" y="14" width="16" height="6" rx="2" />
      </svg>
    );
  }

  if (type === "withdraw") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M12 20V4" />
        <path d="M18 14l-6 6-6-6" />
        <rect x="4" y="4" width="16" height="6" rx="2" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M4 8h11a5 5 0 0 1 0 10H8" />
      <path d="M11 5 8 8l3 3" />
      <path d="M20 16l-3 3-3-3" />
    </svg>
  );
}

function SuccessCheckIcon() {
  return (
    <svg viewBox="0 0 52 52" fill="none">
      <circle cx="26" cy="26" r="24" stroke="currentColor" strokeWidth="3" opacity="0.25" />
      <path d="M15 27.5 22.5 35 37 19.5" stroke="currentColor" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function wait(duration) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, duration);
  });
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

export default App;
