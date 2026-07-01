import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API = {
  auth: "/auth-api",
  customer: "/customer-api",
  account: "/account-api",
  transaction: "/transaction-api",
  document: "/document-api",
  gateway: "/gateway-api"
};

const emptyCustomer = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  address: ""
};

const emptyAccount = {
  accountNumber: "",
  balance: 10000,
  customerId: 1,
  status: "ACTIVE",
  currency: "XAF",
  accountType: "CURRENT"
};

const emptyTransaction = {
  sourceAccountId: "",
  destinationAccountId: "",
  amount: 0,
  type: "DEPOSIT",
  timestamp: ""
};

function buildQuery(params) {
  return new URLSearchParams(params).toString();
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message = typeof body === "string" ? body : body.message || body.error || "Erreur API";
    throw new Error(message);
  }

  return body;
}

function App() {
  const [activeView, setActiveView] = useState("dashboard");
  const [token, setToken] = useState(() => localStorage.getItem("bankToken") || "");
  const [currentUser, setCurrentUser] = useState(() => localStorage.getItem("bankUser") || "");
  const [toast, setToast] = useState(null);
  const [loading, setLoading] = useState(false);

  const [users, setUsers] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [history, setHistory] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState("1");

  const notify = (type, message) => {
    setToast({ type, message });
    window.setTimeout(() => setToast(null), 4200);
  };

  const safeRun = async (action, successMessage) => {
    setLoading(true);
    try {
      const result = await action();
      if (successMessage) notify("success", successMessage);
      return result;
    } catch (error) {
      notify("error", error.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const refreshAll = async () => {
    await safeRun(async () => {
      const [nextUsers, nextCustomers, nextAccounts, nextTransactions] = await Promise.allSettled([
        request(`${API.auth}/users`),
        request(`${API.customer}/customers`),
        request(`${API.account}/accounts`),
        request(`${API.transaction}/api/transactions`)
      ]);

      if (nextUsers.status === "fulfilled") setUsers(Array.isArray(nextUsers.value) ? nextUsers.value : []);
      if (nextCustomers.status === "fulfilled") setCustomers(Array.isArray(nextCustomers.value) ? nextCustomers.value : []);
      if (nextAccounts.status === "fulfilled") setAccounts(Array.isArray(nextAccounts.value) ? nextAccounts.value : []);
      if (nextTransactions.status === "fulfilled") {
        setTransactions(Array.isArray(nextTransactions.value) ? nextTransactions.value : []);
      }
    });
  };

  useEffect(() => {
    refreshAll();
  }, []);

  const totals = useMemo(() => {
    const balance = accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0);
    const deposits = transactions.filter((item) => item.type === "DEPOSIT").length;
    const withdrawals = transactions.filter((item) => item.type === "WITHDRAWAL").length;
    const transfers = transactions.filter((item) => item.type === "TRANSFER").length;
    return { balance, deposits, withdrawals, transfers };
  }, [accounts, transactions]);

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">B</div>
          <div>
            <strong>Banking INF462</strong>
            <span>Plateforme microservices</span>
          </div>
        </div>

        <nav className="nav">
          {[
            ["dashboard", "Tableau de bord"],
            ["auth", "Connexion"],
            ["customers", "Clients"],
            ["accounts", "Comptes"],
            ["transactions", "Transactions"],
            ["history", "Historique"],
            ["documents", "Documents"],
            ["admin", "Administration"]
          ].map(([key, label]) => (
            <button
              className={activeView === key ? "active" : ""}
              key={key}
              onClick={() => setActiveView(key)}
            >
              <span>{label.charAt(0)}</span>
              {label}
            </button>
          ))}
        </nav>

        <div className="sidebar-note">
          <span>Gateway</span>
          <strong>localhost:8083</strong>
          <small>Services directs proxifies par Vite pour les tests.</small>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div>
            <p className="eyebrow">Systeme bancaire distribue</p>
            <h1>{titleFor(activeView)}</h1>
          </div>
          <div className="top-actions">
            <button className="ghost" onClick={refreshAll} disabled={loading}>
              Actualiser
            </button>
            <div className={token ? "session on" : "session"}>
              <span>{token ? "Connecte" : "Invite"}</span>
              <strong>{currentUser || "Aucune session"}</strong>
            </div>
          </div>
        </header>

        {toast && <div className={`toast ${toast.type}`}>{toast.message}</div>}

        {activeView === "dashboard" && (
          <Dashboard
            totals={totals}
            users={users}
            customers={customers}
            accounts={accounts}
            transactions={transactions}
          />
        )}

        {activeView === "auth" && (
          <AuthView
            setToken={setToken}
            setCurrentUser={setCurrentUser}
            notify={notify}
            safeRun={safeRun}
          />
        )}

        {activeView === "customers" && (
          <CustomerView
            customers={customers}
            setCustomers={setCustomers}
            notify={notify}
            safeRun={safeRun}
          />
        )}

        {activeView === "accounts" && (
          <AccountView
            accounts={accounts}
            setAccounts={setAccounts}
            safeRun={safeRun}
            notify={notify}
          />
        )}

        {activeView === "transactions" && (
          <TransactionView
            transactions={transactions}
            setTransactions={setTransactions}
            refreshAll={refreshAll}
            safeRun={safeRun}
          />
        )}

        {activeView === "history" && (
          <HistoryView
            history={history}
            setHistory={setHistory}
            selectedAccountId={selectedAccountId}
            setSelectedAccountId={setSelectedAccountId}
            safeRun={safeRun}
          />
        )}

        {activeView === "documents" && (
          <DocumentView safeRun={safeRun} />
        )}

        {activeView === "admin" && (
          <AdminView users={users} setUsers={setUsers} safeRun={safeRun} notify={notify} />
        )}
      </main>
    </div>
  );
}

function titleFor(view) {
  return {
    dashboard: "Dashboard d'accueil",
    auth: "Connexion et inscription",
    customers: "Gestion des clients",
    accounts: "Gestion des comptes",
    transactions: "Operations transactionnelles",
    history: "Historique des transactions",
    documents: "Analyse OCR et documents",
    admin: "Gestion des utilisateurs"
  }[view];
}

function Dashboard({ totals, users, customers, accounts, transactions }) {
  return (
    <section className="grid dashboard-grid">
      <Metric label="Solde total" value={`${formatMoney(totals.balance)} XAF`} tone="green" />
      <Metric label="Clients" value={customers.length} tone="amber" />
      <Metric label="Comptes" value={accounts.length} tone="coral" />
      <Metric label="Transactions" value={transactions.length} tone="blue" />

      <div className="panel wide">
        <div className="panel-head">
          <div>
            <h2>Activite bancaire</h2>
            <p>Vue synthetique des operations du systeme.</p>
          </div>
        </div>
        <div className="activity-strip">
          <Activity label="Depots" value={totals.deposits} />
          <Activity label="Retraits" value={totals.withdrawals} />
          <Activity label="Transferts" value={totals.transfers} />
          <Activity label="Utilisateurs" value={users.length} />
        </div>
      </div>

      <div className="panel">
        <h2>Derniers comptes</h2>
        <CompactList
          items={accounts.slice(-5).reverse()}
          render={(account) => (
            <>
              <strong>{account.accountNumber || `Compte ${account.id}`}</strong>
              <span>{formatMoney(account.balance)} {account.currency || "XAF"}</span>
            </>
          )}
        />
      </div>

      <div className="panel">
        <h2>Dernieres transactions</h2>
        <CompactList
          items={transactions.slice(-5).reverse()}
          render={(transaction) => (
            <>
              <strong>{transaction.type}</strong>
              <span>{formatMoney(transaction.amount)} XAF</span>
            </>
          )}
        />
      </div>
    </section>
  );
}

function AuthView({ setToken, setCurrentUser, notify, safeRun }) {
  const [login, setLogin] = useState({ email: "alice@example.com", password: "secret123" });
  const [register, setRegister] = useState({
    fullName: "Alice Client",
    email: "alice@example.com",
    password: "secret123",
    role: "CLIENT"
  });

  const submitRegister = async (event) => {
    event.preventDefault();
    await safeRun(
      () => request(`${API.auth}/register`, { method: "POST", body: JSON.stringify(register) }),
      "Utilisateur cree avec succes"
    );
  };

  const submitLogin = async (event) => {
    event.preventDefault();
    const result = await safeRun(() =>
      request(`${API.auth}/login`, { method: "POST", body: JSON.stringify(login) })
    );
    if (result) {
      const token = result.token || result.accessToken || JSON.stringify(result);
      localStorage.setItem("bankToken", token);
      localStorage.setItem("bankUser", login.email);
      setToken(token);
      setCurrentUser(login.email);
      notify("success", "Connexion reussie");
    }
  };

  return (
    <section className="two-col">
      <form className="panel form-panel" onSubmit={submitLogin}>
        <h2>Connexion</h2>
        <Field label="Email" value={login.email} onChange={(email) => setLogin({ ...login, email })} />
        <Field
          label="Mot de passe"
          type="password"
          value={login.password}
          onChange={(password) => setLogin({ ...login, password })}
        />
        <button className="primary">Se connecter</button>
      </form>

      <form className="panel form-panel" onSubmit={submitRegister}>
        <h2>Creation de compte utilisateur</h2>
        <Field label="Nom complet" value={register.fullName} onChange={(fullName) => setRegister({ ...register, fullName })} />
        <Field label="Email" value={register.email} onChange={(email) => setRegister({ ...register, email })} />
        <Field
          label="Mot de passe"
          type="password"
          value={register.password}
          onChange={(password) => setRegister({ ...register, password })}
        />
        <Select
          label="Role"
          value={register.role}
          options={["CLIENT", "ADMIN", "OPERATOR"]}
          onChange={(role) => setRegister({ ...register, role })}
        />
        <button className="primary">Creer l'utilisateur</button>
      </form>
    </section>
  );
}

function CustomerView({ customers, setCustomers, safeRun }) {
  const [form, setForm] = useState(emptyCustomer);
  const [editingId, setEditingId] = useState("");

  const load = async () => {
    const result = await safeRun(() => request(`${API.customer}/customers`));
    if (Array.isArray(result)) setCustomers(result);
  };

  const submit = async (event) => {
    event.preventDefault();
    const method = editingId ? "PUT" : "POST";
    const url = editingId ? `${API.customer}/customers/${editingId}` : `${API.customer}/customers`;
    await safeRun(() => request(url, { method, body: JSON.stringify(form) }), "Client enregistre");
    setEditingId("");
    setForm(emptyCustomer);
    load();
  };

  const remove = async (id) => {
    await safeRun(() => request(`${API.customer}/customers/${id}`, { method: "DELETE" }), "Client supprime");
    load();
  };

  return (
    <CrudLayout
      form={
        <form onSubmit={submit}>
          <h2>{editingId ? "Modifier le client" : "Nouveau client"}</h2>
          <Field label="Prenom" value={form.firstName} onChange={(firstName) => setForm({ ...form, firstName })} />
          <Field label="Nom" value={form.lastName} onChange={(lastName) => setForm({ ...form, lastName })} />
          <Field label="Email" value={form.email} onChange={(email) => setForm({ ...form, email })} />
          <Field label="Telephone" value={form.phone} onChange={(phone) => setForm({ ...form, phone })} />
          <Field label="Adresse" value={form.address} onChange={(address) => setForm({ ...form, address })} />
          <button className="primary">{editingId ? "Mettre a jour" : "Creer le client"}</button>
        </form>
      }
      table={
        <DataTable
          columns={["ID", "Client", "Email", "Telephone", "Adresse", "Actions"]}
          rows={customers}
          render={(customer) => [
            customer.id,
            `${customer.firstName || ""} ${customer.lastName || ""}`,
            customer.email,
            customer.phone,
            customer.address,
            <RowActions
              onEdit={() => {
                setEditingId(customer.id);
                setForm({ ...emptyCustomer, ...customer });
              }}
              onDelete={() => remove(customer.id)}
            />
          ]}
        />
      }
    />
  );
}

function AccountView({ accounts, setAccounts, safeRun, notify }) {
  const [form, setForm] = useState(emptyAccount);
  const [balanceForm, setBalanceForm] = useState({ accountId: 1, amount: 5000 });

  const load = async () => {
    const result = await safeRun(() => request(`${API.account}/accounts`));
    if (Array.isArray(result)) setAccounts(result);
  };

  const submit = async (event) => {
    event.preventDefault();
    await safeRun(
      () => request(`${API.account}/accounts`, { method: "POST", body: JSON.stringify(form) }),
      "Compte cree"
    );
    setForm({ ...emptyAccount, accountNumber: `ACC-${Date.now().toString().slice(-5)}` });
    load();
  };

  const updateBalance = async (event) => {
    event.preventDefault();
    const query = buildQuery(balanceForm);
    await safeRun(() => request(`${API.account}/accounts/update-balance?${query}`, { method: "PUT" }), "Solde mis a jour");
    load();
  };

  const remove = async (id) => {
    await safeRun(() => request(`${API.account}/accounts/${id}`, { method: "DELETE" }), "Compte supprime");
    load();
  };

  const getBalance = async (id) => {
    const result = await safeRun(() => request(`${API.account}/accounts/${id}/balance`));
    if (result !== null) notify("success", `Solde du compte ${id}: ${formatMoney(result)} XAF`);
  };

  return (
    <section className="grid account-grid">
      <div className="panel form-panel">
        <form onSubmit={submit}>
          <h2>Creer un compte</h2>
          <Field label="Numero" value={form.accountNumber} onChange={(accountNumber) => setForm({ ...form, accountNumber })} />
          <Field label="Solde initial" type="number" value={form.balance} onChange={(balance) => setForm({ ...form, balance: Number(balance) })} />
          <Field label="ID client" type="number" value={form.customerId} onChange={(customerId) => setForm({ ...form, customerId: Number(customerId) })} />
          <Select label="Statut" value={form.status} options={["ACTIVE", "SUSPENDED", "CLOSED"]} onChange={(status) => setForm({ ...form, status })} />
          <Select label="Type" value={form.accountType} options={["CURRENT", "SAVINGS"]} onChange={(accountType) => setForm({ ...form, accountType })} />
          <button className="primary">Creer le compte</button>
        </form>
      </div>

      <div className="panel form-panel">
        <form onSubmit={updateBalance}>
          <h2>Ajuster le solde</h2>
          <Field label="ID compte" type="number" value={balanceForm.accountId} onChange={(accountId) => setBalanceForm({ ...balanceForm, accountId })} />
          <Field label="Montant (+ depot, - retrait)" type="number" value={balanceForm.amount} onChange={(amount) => setBalanceForm({ ...balanceForm, amount })} />
          <button className="secondary">Mettre a jour</button>
        </form>
      </div>

      <div className="panel wide">
        <DataTable
          columns={["ID", "Numero", "Client", "Solde", "Statut", "Type", "Actions"]}
          rows={accounts}
          render={(account) => [
            account.id,
            account.accountNumber,
            account.customerId,
            `${formatMoney(account.balance)} ${account.currency || "XAF"}`,
            account.status,
            account.accountType,
            <div className="actions">
              <button onClick={() => getBalance(account.id)}>Solde</button>
              <button className="danger" onClick={() => remove(account.id)}>Supprimer</button>
            </div>
          ]}
        />
      </div>
    </section>
  );
}

function TransactionView({ transactions, setTransactions, refreshAll, safeRun }) {
  const [operation, setOperation] = useState({ accountId: 1, amount: 5000 });
  const [transfer, setTransfer] = useState({ sourceAccountId: 1, destinationAccountId: 2, amount: 3000 });
  const [edit, setEdit] = useState({ id: "", ...emptyTransaction });

  const load = async () => {
    const result = await safeRun(() => request(`${API.transaction}/api/transactions`));
    if (Array.isArray(result)) setTransactions(result);
  };

  const doOperation = async (type) => {
    const endpoint = type === "deposit" ? "deposit" : "withdrawal";
    const query = buildQuery(operation);
    await safeRun(
      () => request(`${API.transaction}/api/transactions/${endpoint}?${query}`, { method: "POST" }),
      type === "deposit" ? "Depot effectue" : "Retrait effectue"
    );
    refreshAll();
  };

  const doTransfer = async (event) => {
    event.preventDefault();
    const query = buildQuery(transfer);
    await safeRun(
      () => request(`${API.transaction}/api/transactions/transfer?${query}`, { method: "POST" }),
      "Transfert effectue"
    );
    refreshAll();
  };

  const updateTransaction = async (event) => {
    event.preventDefault();
    const payload = {
      sourceAccountId: edit.sourceAccountId === "" ? null : Number(edit.sourceAccountId),
      destinationAccountId: edit.destinationAccountId === "" ? null : Number(edit.destinationAccountId),
      amount: Number(edit.amount),
      type: edit.type,
      timestamp: edit.timestamp || null
    };
    await safeRun(
      () => request(`${API.transaction}/api/transactions/${edit.id}`, { method: "PUT", body: JSON.stringify(payload) }),
      "Transaction corrigee"
    );
    setEdit({ id: "", ...emptyTransaction });
    load();
  };

  const remove = async (id) => {
    await safeRun(() => request(`${API.transaction}/api/transactions/${id}`, { method: "DELETE" }), "Transaction supprimee");
    load();
  };

  return (
    <section className="grid transaction-grid">
      <div className="panel form-panel">
        <h2>Depot / retrait</h2>
        <Field label="ID compte" type="number" value={operation.accountId} onChange={(accountId) => setOperation({ ...operation, accountId })} />
        <Field label="Montant" type="number" value={operation.amount} onChange={(amount) => setOperation({ ...operation, amount })} />
        <div className="split-actions">
          <button className="primary" onClick={() => doOperation("deposit")}>Depot</button>
          <button className="secondary" onClick={() => doOperation("withdrawal")}>Retrait</button>
        </div>
      </div>

      <form className="panel form-panel" onSubmit={doTransfer}>
        <h2>Transfert</h2>
        <Field label="Compte source" type="number" value={transfer.sourceAccountId} onChange={(sourceAccountId) => setTransfer({ ...transfer, sourceAccountId })} />
        <Field label="Compte destination" type="number" value={transfer.destinationAccountId} onChange={(destinationAccountId) => setTransfer({ ...transfer, destinationAccountId })} />
        <Field label="Montant" type="number" value={transfer.amount} onChange={(amount) => setTransfer({ ...transfer, amount })} />
        <button className="primary">Executer le transfert</button>
      </form>

      <form className="panel form-panel" onSubmit={updateTransaction}>
        <h2>Correction administrative</h2>
        <Field label="ID transaction" type="number" value={edit.id} onChange={(id) => setEdit({ ...edit, id })} />
        <Field label="Compte source" type="number" value={edit.sourceAccountId} onChange={(sourceAccountId) => setEdit({ ...edit, sourceAccountId })} />
        <Field label="Compte destination" type="number" value={edit.destinationAccountId} onChange={(destinationAccountId) => setEdit({ ...edit, destinationAccountId })} />
        <Field label="Montant" type="number" value={edit.amount} onChange={(amount) => setEdit({ ...edit, amount })} />
        <Select label="Type" value={edit.type} options={["DEPOSIT", "WITHDRAWAL", "TRANSFER"]} onChange={(type) => setEdit({ ...edit, type })} />
        <button className="secondary">Corriger</button>
      </form>

      <div className="panel wide">
        <DataTable
          columns={["ID", "Type", "Source", "Destination", "Montant", "Date", "Actions"]}
          rows={transactions}
          render={(transaction) => [
            transaction.id,
            transaction.type,
            transaction.sourceAccountId || "-",
            transaction.destinationAccountId || "-",
            `${formatMoney(transaction.amount)} XAF`,
            formatDate(transaction.timestamp),
            <RowActions
              onEdit={() =>
                setEdit({
                  id: transaction.id,
                  sourceAccountId: transaction.sourceAccountId || "",
                  destinationAccountId: transaction.destinationAccountId || "",
                  amount: transaction.amount || 0,
                  type: transaction.type || "DEPOSIT",
                  timestamp: transaction.timestamp || ""
                })
              }
              onDelete={() => remove(transaction.id)}
            />
          ]}
        />
      </div>
    </section>
  );
}

function HistoryView({ history, setHistory, selectedAccountId, setSelectedAccountId, safeRun }) {
  const loadHistory = async (event) => {
    event.preventDefault();
    const result = await safeRun(() =>
      request(`${API.transaction}/api/transactions/history/${selectedAccountId}`)
    );
    if (Array.isArray(result)) setHistory(result);
  };

  return (
    <section className="grid">
      <form className="panel form-panel" onSubmit={loadHistory}>
        <h2>Consulter l'historique</h2>
        <Field label="ID compte" type="number" value={selectedAccountId} onChange={setSelectedAccountId} />
        <button className="primary">Afficher l'historique</button>
      </form>
      <div className="panel wide">
        <DataTable
          columns={["ID", "Type", "Source", "Destination", "Montant", "Date"]}
          rows={history}
          render={(transaction) => [
            transaction.id,
            transaction.type,
            transaction.sourceAccountId || "-",
            transaction.destinationAccountId || "-",
            `${formatMoney(transaction.amount)} XAF`,
            formatDate(transaction.timestamp)
          ]}
        />
      </div>
    </section>
  );
}

function AdminView({ users, setUsers, safeRun }) {
  const [edit, setEdit] = useState({
    id: "",
    fullName: "",
    email: "",
    password: "",
    role: "CLIENT"
  });

  const load = async () => {
    const result = await safeRun(() => request(`${API.auth}/users`));
    if (Array.isArray(result)) setUsers(result);
  };

  const update = async (event) => {
    event.preventDefault();
    const payload = { ...edit };
    delete payload.id;
    if (!payload.password) delete payload.password;
    await safeRun(
      () => request(`${API.auth}/users/${edit.id}`, { method: "PUT", body: JSON.stringify(payload) }),
      "Utilisateur mis a jour"
    );
    setEdit({ id: "", fullName: "", email: "", password: "", role: "CLIENT" });
    load();
  };

  const remove = async (id) => {
    await safeRun(() => request(`${API.auth}/users/${id}`, { method: "DELETE" }), "Utilisateur supprime");
    load();
  };

  return (
    <section className="grid">
      <form className="panel form-panel" onSubmit={update}>
        <h2>Modifier un utilisateur</h2>
        <Field label="ID" type="number" value={edit.id} onChange={(id) => setEdit({ ...edit, id })} />
        <Field label="Nom complet" value={edit.fullName} onChange={(fullName) => setEdit({ ...edit, fullName })} />
        <Field label="Email" value={edit.email} onChange={(email) => setEdit({ ...edit, email })} />
        <Field label="Nouveau mot de passe" type="password" value={edit.password} onChange={(password) => setEdit({ ...edit, password })} />
        <Select label="Role" value={edit.role} options={["CLIENT", "ADMIN", "OPERATOR"]} onChange={(role) => setEdit({ ...edit, role })} />
        <button className="primary">Mettre a jour</button>
      </form>
      <div className="panel wide">
        <DataTable
          columns={["ID", "Nom", "Email", "Role", "Actif", "Actions"]}
          rows={users}
          render={(user) => [
            user.id,
            user.fullName,
            user.email,
            user.role,
            user.enabled ? "Oui" : "Non",
            <RowActions
              onEdit={() =>
                setEdit({
                  id: user.id,
                  fullName: user.fullName || "",
                  email: user.email || "",
                  password: "",
                  role: user.role || "CLIENT"
                })
              }
              onDelete={() => remove(user.id)}
            />
          ]}
        />
      </div>
    </section>
  );
}

function DocumentView({ safeRun }) {
  const [file, setFile] = useState(null);
  const [documentType, setDocumentType] = useState("CAMEROON_CNI");
  const [analysis, setAnalysis] = useState(null);

  const analyze = async (event) => {
    event.preventDefault();
    if (!file) {
      await safeRun(() => Promise.reject(new Error("Selectionne d'abord une image ou un document.")));
      return;
    }

    const body = new FormData();
    body.append("file", file);
    if (documentType) body.append("documentType", documentType);

    const result = await safeRun(async () => {
      const response = await fetch(`${API.document}/api/documents/analyze`, {
        method: "POST",
        body
      });
      const contentType = response.headers.get("content-type") || "";
      const data = contentType.includes("application/json") ? await response.json() : await response.text();
      if (!response.ok) {
        const message = typeof data === "string" ? data : data.message || data.error || "Erreur OCR";
        throw new Error(message);
      }
      return data;
    }, "Document analyse avec succes");

    if (result) setAnalysis(result);
  };

  return (
    <section className="grid document-grid">
      <form className="panel form-panel" onSubmit={analyze}>
        <h2>Analyser un document</h2>
        <label className="field">
          <span>Fichier</span>
          <input type="file" accept="image/*,.pdf" onChange={(event) => setFile(event.target.files?.[0] || null)} />
        </label>
        <Select
          label="Type de document"
          value={documentType}
          options={["CAMEROON_CNI", "CAMEROON_PASSPORT", "PASSPORT", "IDENTITY_CARD", "BANK_STATEMENT", "UNKNOWN"]}
          onChange={setDocumentType}
        />
        <button className="primary">Lancer l'analyse OCR</button>
      </form>

      <div className="panel result-panel">
        <h2>Resultat de l'analyse</h2>
        <pre>{analysis ? JSON.stringify(analysis, null, 2) : "Aucun document analyse pour le moment."}</pre>
      </div>
    </section>
  );
}

function Metric({ label, value, tone }) {
  return (
    <div className={`metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Activity({ label, value }) {
  return (
    <div className="activity">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function CompactList({ items, render }) {
  if (!items.length) return <p className="empty">Aucune donnee disponible.</p>;
  return (
    <div className="compact-list">
      {items.map((item, index) => (
        <div key={item.id || index}>{render(item)}</div>
      ))}
    </div>
  );
}

function CrudLayout({ form, table }) {
  return (
    <section className="crud-layout">
      <div className="panel form-panel">{form}</div>
      <div className="panel">{table}</div>
    </section>
  );
}

function DataTable({ columns, rows, render }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((row, rowIndex) => (
              <tr key={row.id || rowIndex}>
                {render(row).map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length} className="empty">Aucune donnee trouvee.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function RowActions({ onEdit, onDelete }) {
  return (
    <div className="actions">
      <button onClick={onEdit}>Modifier</button>
      <button className="danger" onClick={onDelete}>Supprimer</button>
    </div>
  );
}

function Field({ label, value, onChange, type = "text" }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function Select({ label, value, options, onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </label>
  );
}

function formatMoney(value) {
  return new Intl.NumberFormat("fr-FR").format(Number(value || 0));
}

function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString("fr-FR");
}

createRoot(document.getElementById("root")).render(<App />);
