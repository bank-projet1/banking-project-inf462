import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API = {
  auth: "/auth-api",
  customers: "/customer-api/customers",
  accounts: "/account-api/accounts",
  transactions: "/transaction-api/api/transactions",
  documents: "/document-api/api/documents"
};

const initialLogin = { email: "alice@example.com", password: "secret123" };
const initialRegister = { fullName: "Alice Client", email: "alice@example.com", password: "secret123", role: "CLIENT" };
const initialCustomer = { firstName: "Alice", lastName: "Talla", email: "alice.talla@example.com", phone: "+237690000001", address: "Yaounde" };
const initialAccount = { accountNumber: "ACC-001", balance: 10000, customerId: 1, status: "ACTIVE", currency: "XAF", accountType: "CURRENT" };

function useRequestLog() {
  const [log, setLog] = useState([]);
  const push = (title, payload, ok = true) => {
    setLog((items) => [{ title, ok, payload, at: new Date().toLocaleTimeString() }, ...items].slice(0, 8));
  };
  return { log, push };
}

async function runAction(push, title, action) {
  try {
    const data = await action();
    push(title, data);
    return data;
  } catch (error) {
    push("Erreur API", error, false);
    return null;
  }
}

async function jsonRequest(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw data || { status: response.status, message: response.statusText };
  }
  return data;
}

function Field({ label, value, onChange, type = "text" }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function AuthPanel({ push }) {
  const [login, setLogin] = useState(initialLogin);
  const [register, setRegister] = useState(initialRegister);
  const [token, setToken] = useState("");

  const submitRegister = () => runAction(
    push,
    "Inscription auth-service",
    () => jsonRequest(`${API.auth}/register`, { method: "POST", body: JSON.stringify(register) })
  );

  const submitLogin = () => runAction(push, "Connexion auth-service", async () => {
    const data = await jsonRequest(`${API.auth}/login`, { method: "POST", body: JSON.stringify(login) });
    setToken(data?.token || JSON.stringify(data));
    return data;
  });

  return (
    <section className="panel">
      <div className="panel-title">
        <h2>Authentification</h2>
        <span>auth-service : 8084</span>
      </div>
      <div className="grid two">
        <div className="form-card">
          <h3>Sign-up</h3>
          <Field label="Nom complet" value={register.fullName} onChange={(v) => setRegister({ ...register, fullName: v })} />
          <Field label="Email" value={register.email} onChange={(v) => setRegister({ ...register, email: v })} />
          <Field label="Mot de passe" value={register.password} onChange={(v) => setRegister({ ...register, password: v })} type="password" />
          <Field label="Role" value={register.role} onChange={(v) => setRegister({ ...register, role: v })} />
          <button onClick={submitRegister}>Creer le compte</button>
        </div>
        <div className="form-card">
          <h3>Login</h3>
          <Field label="Email" value={login.email} onChange={(v) => setLogin({ ...login, email: v })} />
          <Field label="Mot de passe" value={login.password} onChange={(v) => setLogin({ ...login, password: v })} type="password" />
          <button onClick={submitLogin}>Se connecter</button>
          {token && <pre className="token">{token}</pre>}
        </div>
      </div>
    </section>
  );
}

function CustomersPanel({ push }) {
  const [customer, setCustomer] = useState(initialCustomer);
  const [items, setItems] = useState([]);

  const create = () => runAction(
    push,
    "Creation client",
    () => jsonRequest(API.customers, { method: "POST", body: JSON.stringify(customer) })
  );
  const load = async () => {
    await runAction(push, "Liste clients", async () => {
      const data = await jsonRequest(API.customers);
      setItems(Array.isArray(data) ? data : []);
      return data;
    });
  };

  return (
    <section className="panel">
      <div className="panel-title">
        <h2>Clients</h2>
        <span>service-customer : 8085</span>
      </div>
      <div className="grid two">
        <div className="form-card">
          <Field label="Prenom" value={customer.firstName} onChange={(v) => setCustomer({ ...customer, firstName: v })} />
          <Field label="Nom" value={customer.lastName} onChange={(v) => setCustomer({ ...customer, lastName: v })} />
          <Field label="Email" value={customer.email} onChange={(v) => setCustomer({ ...customer, email: v })} />
          <Field label="Telephone" value={customer.phone} onChange={(v) => setCustomer({ ...customer, phone: v })} />
          <Field label="Adresse" value={customer.address} onChange={(v) => setCustomer({ ...customer, address: v })} />
          <button onClick={create}>Ajouter client</button>
        </div>
        <ListBox title="Clients enregistres" items={items} onRefresh={load} />
      </div>
    </section>
  );
}

function AccountsPanel({ push }) {
  const [account, setAccount] = useState(initialAccount);
  const [items, setItems] = useState([]);
  const create = () => runAction(
    push,
    "Creation compte",
    () => jsonRequest(API.accounts, { method: "POST", body: JSON.stringify(account) })
  );
  const load = async () => {
    await runAction(push, "Liste comptes", async () => {
      const data = await jsonRequest(API.accounts);
      setItems(Array.isArray(data) ? data : []);
      return data;
    });
  };

  return (
    <section className="panel">
      <div className="panel-title">
        <h2>Comptes</h2>
        <span>service-account : 8082</span>
      </div>
      <div className="grid two">
        <div className="form-card">
          <Field label="Numero" value={account.accountNumber} onChange={(v) => setAccount({ ...account, accountNumber: v })} />
          <Field label="Solde" value={account.balance} onChange={(v) => setAccount({ ...account, balance: Number(v) })} type="number" />
          <Field label="Client ID" value={account.customerId} onChange={(v) => setAccount({ ...account, customerId: Number(v) })} type="number" />
          <Field label="Devise" value={account.currency} onChange={(v) => setAccount({ ...account, currency: v })} />
          <Field label="Type" value={account.accountType} onChange={(v) => setAccount({ ...account, accountType: v })} />
          <button onClick={create}>Creer compte</button>
        </div>
        <ListBox title="Comptes disponibles" items={items} onRefresh={load} />
      </div>
    </section>
  );
}

function TransactionsPanel({ push }) {
  const [accountId, setAccountId] = useState(1);
  const [destinationAccountId, setDestinationAccountId] = useState(2);
  const [amount, setAmount] = useState(5000);
  const [history, setHistory] = useState([]);

  const deposit = () => runAction(
    push,
    "Depot",
    () => jsonRequest(`${API.transactions}/deposit?accountId=${accountId}&amount=${amount}`, { method: "POST" })
  );
  const withdrawal = () => runAction(
    push,
    "Retrait",
    () => jsonRequest(`${API.transactions}/withdrawal?accountId=${accountId}&amount=${amount}`, { method: "POST" })
  );
  const transfer = () => runAction(
    push,
    "Transfert",
    () => jsonRequest(`${API.transactions}/transfer?sourceAccountId=${accountId}&destinationAccountId=${destinationAccountId}&amount=${amount}`, { method: "POST" })
  );
  const loadHistory = async () => {
    await runAction(push, "Historique transactions", async () => {
      const data = await jsonRequest(`${API.transactions}/history/${accountId}`);
      setHistory(Array.isArray(data) ? data : []);
      return data;
    });
  };

  return (
    <section className="panel">
      <div className="panel-title">
        <h2>Transactions</h2>
        <span>transaction-service : 8086</span>
      </div>
      <div className="grid two">
        <div className="form-card">
          <Field label="Compte source" value={accountId} onChange={(v) => setAccountId(Number(v))} type="number" />
          <Field label="Compte destination" value={destinationAccountId} onChange={(v) => setDestinationAccountId(Number(v))} type="number" />
          <Field label="Montant" value={amount} onChange={(v) => setAmount(Number(v))} type="number" />
          <div className="button-row">
            <button onClick={deposit}>Depot</button>
            <button onClick={withdrawal}>Retrait</button>
            <button onClick={transfer}>Transfert</button>
          </div>
        </div>
        <ListBox title="Historique" items={history} onRefresh={loadHistory} />
      </div>
    </section>
  );
}

function DocumentsPanel({ push }) {
  const [file, setFile] = useState(null);
  const [documentType, setDocumentType] = useState("CAMEROON_CNI");
  const [analysis, setAnalysis] = useState(null);

  const analyze = () => runAction(push, "Analyse OCR document", async () => {
    if (!file) {
      throw { message: "Selectionne d'abord un fichier." };
    }
    const body = new FormData();
    body.append("file", file);
    if (documentType) {
      body.append("documentType", documentType);
    }
    const response = await fetch(`${API.documents}/analyze`, { method: "POST", body });
    const data = await response.json();
    if (!response.ok) {
      throw data;
    }
    setAnalysis(data);
    return data;
  });

  return (
    <section className="panel">
      <div className="panel-title">
        <h2>Documents OCR / IA</h2>
        <span>document-intelligence-service : 8087</span>
      </div>
      <div className="grid two">
        <div className="form-card">
          <label className="field">
            <span>Fichier</span>
            <input type="file" onChange={(event) => setFile(event.target.files?.[0] || null)} />
          </label>
          <Field label="Type document" value={documentType} onChange={setDocumentType} />
          <button onClick={analyze}>Analyser le document</button>
        </div>
        <div className="result-card">
          <h3>Resultat OCR</h3>
          <pre>{analysis ? JSON.stringify(analysis, null, 2) : "Aucune analyse lancee."}</pre>
        </div>
      </div>
    </section>
  );
}

function ListBox({ title, items, onRefresh }) {
  return (
    <div className="result-card">
      <div className="list-title">
        <h3>{title}</h3>
        <button className="ghost" onClick={onRefresh}>Actualiser</button>
      </div>
      <pre>{items.length ? JSON.stringify(items, null, 2) : "Aucune donnee chargee."}</pre>
    </div>
  );
}

function App() {
  const { log, push } = useRequestLog();
  const [view, setView] = useState("dashboard");
  const pages = useMemo(() => [
    ["dashboard", "Dashboard"],
    ["auth", "Login"],
    ["customers", "Clients"],
    ["accounts", "Comptes"],
    ["transactions", "Transactions"],
    ["documents", "Documents"]
  ], []);

  return (
    <main>
      <aside className="sidebar">
        <div className="brand">Banking INF462</div>
        <nav>
          {pages.map(([key, label]) => (
            <button className={view === key ? "active" : ""} key={key} onClick={() => setView(key)}>{label}</button>
          ))}
        </nav>
      </aside>
      <section className="content">
        <header>
          <div>
            <p>Plateforme bancaire distribuee</p>
            <h1>Console de demonstration microservices</h1>
          </div>
          <span className="status">Spring Cloud + Eureka + Gateway</span>
        </header>

        {view === "dashboard" && (
          <section className="panel dashboard">
            <h2>Etat de demonstration</h2>
            <div className="stats">
              <div><strong>5</strong><span>Services metier</span></div>
              <div><strong>8083</strong><span>API Gateway</span></div>
              <div><strong>8087</strong><span>OCR documents</span></div>
            </div>
            <p>Demarre Docker, service-config, service-registry, puis les microservices. Les actions ci-dessous appellent directement les routes REST documentees dans API_SWAGGER_TESTS.md.</p>
          </section>
        )}
        {view === "auth" && <AuthPanel push={push} />}
        {view === "customers" && <CustomersPanel push={push} />}
        {view === "accounts" && <AccountsPanel push={push} />}
        {view === "transactions" && <TransactionsPanel push={push} />}
        {view === "documents" && <DocumentsPanel push={push} />}

        <section className="panel log">
          <h2>Journal des appels</h2>
          {log.map((entry, index) => (
            <div className={entry.ok ? "log-item" : "log-item error"} key={`${entry.at}-${index}`}>
              <span>{entry.at}</span>
              <strong>{entry.title}</strong>
              <pre>{JSON.stringify(entry.payload, null, 2)}</pre>
            </div>
          ))}
        </section>
      </section>
    </main>
  );
}

createRoot(document.getElementById("root")).render(<App />);
