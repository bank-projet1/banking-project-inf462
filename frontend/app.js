const SERVICES = [
  { key: "config", name: "Config Server", port: 8080, health: "/actuator/health", config: "/service-account/default" },
  { key: "registry", name: "Service Registry", port: 8081, health: "/actuator/health", config: "/" },
  { key: "gateway", name: "Gateway", port: 8083, health: "/actuator/health", config: "/" },
  { key: "auth", name: "Auth Service", port: 8084, health: "/", config: "/config" },
  { key: "account", name: "Account Service", port: 8082, health: "/actuator/health", config: "/accounts" },
  { key: "transaction", name: "Transaction Service", port: 8085, health: "/actuator/health", config: "/api/transactions/history/0" },
  { key: "loan", name: "Loan Service", port: 8086, health: "/actuator/health", config: "/api/loans" },
  { key: "notification", name: "Notification Service", port: 8087, health: "/actuator/health", config: "/api/notifications" }
];

const HOME_BY_ROLE = {
  ADMIN: "admin-home",
  CLIENT: "client-home",
  OPERATOR: "operator-home"
};

const ROLE_LABELS = {
  ADMIN: "Administrateur",
  CLIENT: "Client",
  OPERATOR: "Operateur financier"
};

const storedUser = localStorage.getItem("banking-current-user");

const state = {
  apiBase: localStorage.getItem("banking-api-base") || "/api",
  serviceBase: localStorage.getItem("banking-service-base") || "/services",
  currentUser: storedUser ? JSON.parse(storedUser) : null,
  token: localStorage.getItem("banking-auth-token") || "",
  loans: [],
  accounts: [],
  transactions: [],
  notifications: [],
  users: [],
  services: [],
  selectedStatus: "ALL",
  search: ""
};

const els = {
  loginScreen: document.querySelector("#loginScreen"),
  appShell: document.querySelector("#appShell"),
  loginAlert: document.querySelector("#loginAlert"),
  loginForm: document.querySelector("#loginForm"),
  clientRegisterForm: document.querySelector("#clientRegisterForm"),
  toggleClientRegisterButton: document.querySelector("#toggleClientRegisterButton"),
  authSwitchText: document.querySelector("#authSwitchText"),
  pageTitle: document.querySelector("#pageTitle"),
  pageEyebrow: document.querySelector("#pageEyebrow"),
  alertBox: document.querySelector("#alertBox"),
  statusDot: document.querySelector("#statusDot"),
  connectionLabel: document.querySelector("#connectionLabel"),
  connectionDetail: document.querySelector("#connectionDetail"),
  roleLabel: document.querySelector("#roleLabel"),
  currentUserName: document.querySelector("#currentUserName"),
  currentUserEmail: document.querySelector("#currentUserEmail"),
  metricGrid: document.querySelector("#metricGrid"),
  clientMetricGrid: document.querySelector("#clientMetricGrid"),
  operatorMetricGrid: document.querySelector("#operatorMetricGrid"),
  loanList: document.querySelector("#loanList"),
  accountGrid: document.querySelector("#accountGrid"),
  accountForm: document.querySelector("#accountForm"),
  transactionForm: document.querySelector("#transactionForm"),
  transactionAccountFilter: document.querySelector("#transactionAccountFilter"),
  transactionList: document.querySelector("#transactionList"),
  receiverAccountPreview: document.querySelector("#receiverAccountPreview"),
  notificationList: document.querySelector("#notificationList"),
  usersTable: document.querySelector("#usersTable"),
  serviceDetails: document.querySelector("#serviceDetails"),
  loanForm: document.querySelector("#loanForm"),
  registerForm: document.querySelector("#registerForm"),
  searchInput: document.querySelector("#searchInput"),
  statusFilter: document.querySelector("#statusFilter"),
  apiBaseInput: document.querySelector("#apiBaseInput"),
  serviceBaseInput: document.querySelector("#serviceBaseInput"),
  dialog: document.querySelector("#loanDialog"),
  dialogTitle: document.querySelector("#dialogTitle"),
  dialogContent: document.querySelector("#dialogContent")
};

function money(value, currency = "XAF") {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 0
  }).format(Number(value || 0));
}

function number(value) {
  return new Intl.NumberFormat("fr-FR").format(Number(value || 0));
}

function normalizeRole(role) {
  if (role === "AGENT") return "OPERATOR";
  if (role === "CUSTOMER") return "CLIENT";
  return role || "CLIENT";
}

function showAlert(message, type = "warning", target = els.alertBox) {
  target.textContent = message;
  target.classList.remove("hidden");
  target.dataset.type = type;
}

function clearAlert(target = els.alertBox) {
  target.classList.add("hidden");
  target.textContent = "";
}

async function request(base, path, options = {}) {
  const headers = { "content-type": "application/json", ...(options.headers || {}) };
  if (state.token && options.auth !== false) {
    headers.authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${base}${path}`, { ...options, headers });
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    const message = typeof data === "string" ? data : data?.error || data?.message;
    throw new Error(message || `Erreur HTTP ${response.status}`);
  }
  return data;
}

function gatewayApi(path, options) {
  return request(state.apiBase, path, options);
}

function serviceApi(serviceKey, path, options) {
  return request(`${state.serviceBase}/${serviceKey}`, path, options);
}

async function apiWithServiceFallback(gatewayPath, serviceKey, servicePath, options) {
  try {
    return await gatewayApi(gatewayPath, options);
  } catch (gatewayError) {
    try {
      return await serviceApi(serviceKey, servicePath, options);
    } catch (serviceError) {
      throw new Error(`${gatewayError.message} / direct ${serviceKey}: ${serviceError.message}`);
    }
  }
}

function loanApi(path = "", options) {
  const suffix = path.startsWith("/") ? path : `/${path}`;
  const normalizedSuffix = suffix === "/" ? "" : suffix;
  return apiWithServiceFallback(
    `/loans${normalizedSuffix}`,
    "loan",
    `/api/loans${normalizedSuffix}`,
    options
  );
}

function transactionApi(path = "", options) {
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return serviceApi("transaction", `/api/transactions${suffix === "/" ? "" : suffix}`, options);
}

function accountApi(path = "", options) {
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return serviceApi("account", `/accounts${suffix === "/" ? "" : suffix}`, options);
}

function notificationApi(path = "", options) {
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return serviceApi("notification", `/api/notifications${suffix === "/" ? "" : suffix}`, options);
}

async function settle(label, task) {
  try {
    return { ok: true, label, data: await task() };
  } catch (error) {
    return { ok: false, label, error: error.message || String(error) };
  }
}

function showAuthenticatedApp() {
  const role = normalizeRole(state.currentUser?.role);
  els.loginScreen.classList.add("hidden");
  els.appShell.classList.remove("hidden");
  els.roleLabel.textContent = ROLE_LABELS[role] || "Utilisateur";
  els.currentUserName.textContent = state.currentUser?.fullName || "Utilisateur";
  els.currentUserEmail.textContent = state.currentUser?.email || "";

  document.querySelectorAll(".nav-tab").forEach((tab) => {
    const roles = (tab.dataset.roles || "").split(",");
    tab.hidden = !roles.includes(role);
  });

  setView(HOME_BY_ROLE[role] || "client-home");
  syncAccountFormDefaults();
  syncLoanFormDefaults();
}

function logout() {
  state.currentUser = null;
  state.token = "";
  localStorage.removeItem("banking-current-user");
  localStorage.removeItem("banking-auth-token");
  els.appShell.classList.add("hidden");
  els.loginScreen.classList.remove("hidden");
  clearAlert();
  clearAlert(els.loginAlert);
}

function setAuthMode(mode) {
  const registering = mode === "register";
  els.loginForm.classList.toggle("hidden", registering);
  els.clientRegisterForm.classList.toggle("hidden", !registering);
  els.authSwitchText.textContent = registering ? "Vous avez deja un compte ?" : "Pas encore de compte client ?";
  els.toggleClientRegisterButton.textContent = registering ? "Se connecter" : "S'inscrire";
  clearAlert(els.loginAlert);
}

async function loadAll() {
  clearAlert();
  els.connectionDetail.textContent = `${state.apiBase} + ${state.serviceBase}`;

  const role = normalizeRole(state.currentUser?.role);
  const tasks = [
    settle("credits", () => loanApi()),
    settle("comptes", () => serviceApi("account", "/accounts")),
    settle("notifications", () => role === "ADMIN" ? notificationApi() : notificationApi(`/user/${state.currentUser.id}`))
  ];

  if (role === "ADMIN") {
    tasks.push(settle("utilisateurs", () => serviceApi("auth", "/users")));
    tasks.push(settle("services", probeServices));
  }

  const results = await Promise.all(tasks);
  const byLabel = Object.fromEntries(results.map((result) => [result.label, result]));

  state.loans = Array.isArray(byLabel.credits?.data) ? byLabel.credits.data : [];
  state.accounts = Array.isArray(byLabel.comptes?.data) ? byLabel.comptes.data : [];
  state.notifications = Array.isArray(byLabel.notifications?.data) ? byLabel.notifications.data : [];
  state.users = Array.isArray(byLabel.utilisateurs?.data) ? byLabel.utilisateurs.data : [];
  state.services = byLabel.services?.data || state.services;
  await loadTransactionHistory();
  syncLoanFormDefaults();
  syncAccountFormDefaults();
  syncTransactionControls();

  const failures = results.filter((result) => !result.ok);
  els.statusDot.className = failures.length ? "status-dot warning" : "status-dot online";
  els.connectionLabel.textContent = failures.length ? "Partiel" : "Connecte";

  if (failures.length) {
    showAlert(`Chargement partiel: ${failures.map((item) => `${item.label} (${item.error})`).join(", ")}`);
  }

  render();
}

async function probeServices() {
  return Promise.all(SERVICES.map(async (service) => {
    const health = await settle("health", () => serviceApi(service.key, service.health, { auth: false }));
    const config = await settle("config", () => serviceApi(service.key, service.config, { auth: false }));
    return { ...service, status: health.ok || config.ok ? "UP" : "DOWN", health, config };
  }));
}

function setView(viewName) {
  document.querySelectorAll(".view").forEach((view) => {
    const isActive = view.id === `${viewName}View`;
    view.classList.toggle("active", isActive);
    if (isActive) {
      els.pageTitle.textContent = view.dataset.title || "Banking Project";
      els.pageEyebrow.textContent = view.dataset.eyebrow || "Systeme bancaire distribue";
    }
  });

  document.querySelectorAll(".nav-tab").forEach((tab) => {
    tab.classList.toggle("active", tab.dataset.view === viewName);
  });
}

function filteredLoans() {
  const search = state.search.trim().toLowerCase();
  return state.loans.filter((loan) => {
    const matchesStatus = state.selectedStatus === "ALL" || loan.status === state.selectedStatus;
    const haystack = [loan.id, loan.customerId, loan.accountId, loan.status].join(" ").toLowerCase();
    return matchesStatus && (!search || haystack.includes(search));
  });
}

function metricsHtml(metrics) {
  return metrics.map(([label, value]) => `
    <article class="metric">
      <span>${label}</span>
      <strong>${value}</strong>
    </article>
  `).join("");
}

function renderRoleMetrics() {
  const totalAmount = state.loans.reduce((sum, loan) => sum + Number(loan.amount || 0), 0);
  const remaining = state.loans.reduce((sum, loan) => sum + Number(loan.remainingAmount || 0), 0);
  const visibleAccountList = visibleAccounts();
  const totalBalance = visibleAccountList.reduce((sum, account) => sum + Number(account.balance || 0), 0);
  const pending = state.loans.filter((loan) => ["PENDING", "UNDER_REVIEW"].includes(loan.status)).length;
  const activeServices = state.services.filter((service) => service.status === "UP").length;

  els.metricGrid.innerHTML = metricsHtml([
    ["Services UP", `${activeServices}/${SERVICES.length}`],
    ["Utilisateurs", number(state.users.length)],
    ["Comptes", number(state.accounts.length)],
    ["Credits", number(state.loans.length)],
    ["Encours credits", money(remaining)],
    ["Montant decaisse", money(totalAmount)]
  ]);

  els.clientMetricGrid.innerHTML = metricsHtml([
    ["Mes comptes", number(visibleAccountList.length)],
    ["Solde disponible", money(totalBalance)],
    ["Mes credits", number(state.loans.length)],
    ["Reste a payer", money(remaining)]
  ]);

  els.operatorMetricGrid.innerHTML = metricsHtml([
    ["Dossiers ouverts", number(pending)],
    ["Credits actifs", number(state.loans.filter((loan) => ["APPROVED", "ACTIVE"].includes(loan.status)).length)],
    ["Comptes suivis", number(state.accounts.length)],
    ["Volume analyse", money(totalAmount)]
  ]);
}

function renderServices() {
  els.serviceDetails.innerHTML = state.services.length
    ? state.services.map((service) => `
      <article class="service-detail">
        <header>
          <div>
            <strong>${service.name}</strong>
            <span>${state.serviceBase}/${service.key}</span>
          </div>
          <span class="badge ${service.status}">${service.status}</span>
        </header>
        <div class="detail-grid">
          <div><span>Port</span><strong>${service.port}</strong></div>
          <div><span>Health</span><strong>${service.health.ok ? "OK" : service.health.error}</strong></div>
          <div><span>Config/API</span><strong>${service.config.ok ? "OK" : service.config.error}</strong></div>
        </div>
      </article>
    `).join("")
    : `<div class="empty-state">Etat des services reserve a l'administrateur.</div>`;
}

function renderAccounts() {
  const accounts = visibleAccounts();
  els.accountGrid.innerHTML = accounts.length
    ? accounts.map((account) => `
      <article class="account-card">
        <div class="account-icon" aria-hidden="true">▤</div>
        <div>
          <span>Compte #${account.id ?? "-"}</span>
          <strong>${account.owner || account.accountNumber || "Titulaire inconnu"}</strong>
          <span>Client ${account.customerId ?? "-"}</span>
        </div>
        <strong>${money(account.balance, account.currency || "XAF")}</strong>
      </article>
    `).join("")
    : `<div class="empty-state">Aucun compte charge.</div>`;
}

function renderTransactions() {
  if (!els.transactionList) return;

  els.transactionList.innerHTML = state.transactions.length
    ? state.transactions.map((transaction) => `
      <article class="transaction-item">
        <div>
          <strong>${transactionLabel(transaction)}</strong>
          <span>${transaction.timestamp ? new Date(transaction.timestamp).toLocaleString("fr-FR") : "Date indisponible"}</span>
        </div>
        <strong>${money(transaction.amount)}</strong>
      </article>
    `).join("")
    : `<div class="empty-state">Aucune transaction chargee pour ce compte.</div>`;
}

function renderNotifications() {
  if (!els.notificationList) return;

  els.notificationList.innerHTML = state.notifications.length
    ? state.notifications.map((notification) => `
      <article class="notification-item">
        <div>
          <strong>${notification.type || "Notification"}</strong>
          <p>${notification.message || "Message indisponible"}</p>
          <span>${notification.createdAt ? new Date(notification.createdAt).toLocaleString("fr-FR") : ""}</span>
        </div>
        <span class="badge ${notification.status || ""}">${notification.status || "INFO"}</span>
      </article>
    `).join("")
    : `<div class="empty-state">Aucune notification pour le moment.</div>`;
}

function renderUsers() {
  if (!state.users.length) {
    els.usersTable.innerHTML = `<div class="empty-state">Aucun utilisateur charge.</div>`;
    return;
  }

  els.usersTable.innerHTML = `
    <table>
      <thead><tr><th>ID</th><th>Nom</th><th>Email</th><th>Role</th><th>Etat</th><th>Creation</th></tr></thead>
      <tbody>
        ${state.users.map((user) => `
          <tr>
            <td>${user.id ?? "-"}</td>
            <td>${user.fullName || "-"}</td>
            <td>${user.email || "-"}</td>
            <td><span class="badge">${user.role || "-"}</span></td>
            <td>${user.enabled === false ? "Desactive" : "Actif"}</td>
            <td>${user.createdAt ? new Date(user.createdAt).toLocaleString("fr-FR") : "-"}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function workflowButtons(loan) {
  const role = normalizeRole(state.currentUser?.role);
  if (!["ADMIN", "OPERATOR"].includes(role)) return "";

  const id = loan.id;
  if (loan.status === "PENDING") {
    return `<button class="secondary-button" type="button" data-action="review" data-id="${id}">Mettre en revue</button>`;
  }
  if (loan.status === "UNDER_REVIEW") {
    return `
      <button class="primary-button" type="button" data-action="approve" data-id="${id}">Approuver</button>
      <button class="danger-button" type="button" data-action="reject" data-id="${id}">Rejeter</button>
    `;
  }
  return "";
}

function loanCard(loan) {
  const role = normalizeRole(state.currentUser?.role);
  return `
    <article class="loan-card">
      <div class="loan-main">
        <div class="loan-title">
          <strong>Credit #${loan.id ?? "-"}</strong>
          <span class="badge ${loan.status || ""}">${loan.status || "UNKNOWN"}</span>
        </div>
        <div class="loan-meta">
          <div><span>Client</span><strong>${loan.customerId ?? "-"}</strong></div>
          <div><span>Compte</span><strong>${loan.accountId ?? "-"}</strong></div>
          <div><span>Montant</span><strong>${money(loan.amount)}</strong></div>
          <div><span>Mensualite</span><strong>${money(loan.monthlyPayment)}</strong></div>
        </div>
      </div>
      <div class="loan-actions">
        ${workflowButtons(loan)}
        <button class="secondary-button" type="button" data-action="open" data-id="${loan.id}"><span aria-hidden="true">›</span> Detail</button>
        ${role === "CLIENT" && ["APPROVED", "ACTIVE"].includes(loan.status) ? `<button class="primary-button" type="button" data-action="open" data-id="${loan.id}">Rembourser</button>` : ""}
      </div>
    </article>
  `;
}

function renderLoans() {
  const loans = filteredLoans();
  els.loanList.innerHTML = loans.length
    ? loans.map((loan) => loanCard(loan)).join("")
    : `<div class="empty-state">Aucun credit ne correspond aux filtres.</div>`;
}

function render() {
  renderRoleMetrics();
  renderServices();
  renderAccounts();
  renderTransactions();
  renderNotifications();
  renderUsers();
  renderLoans();
}

function transactionLabel(transaction) {
  if (transaction.type === "DEPOSIT") return `Depot vers compte #${transaction.destinationAccountId ?? "-"}`;
  if (transaction.type === "WITHDRAWAL") return `Retrait du compte #${transaction.sourceAccountId ?? "-"}`;
  return `Virement #${transaction.sourceAccountId ?? "-"} vers #${transaction.destinationAccountId ?? "-"}`;
}

function syncLoanFormDefaults() {
  if (!els.loanForm || !state.currentUser) return;

  const customerInput = els.loanForm.elements.customerId;
  const accountInput = els.loanForm.elements.accountId;
  const role = normalizeRole(state.currentUser.role);
  const clientAccounts = accountsForCurrentUser();
  if (customerInput) {
    customerInput.value = state.currentUser.id || customerInput.value || 1;
    customerInput.readOnly = role === "CLIENT";
  }
  if (accountInput?.tagName === "SELECT") {
    const accounts = role === "CLIENT" ? clientAccounts : state.accounts;
    accountInput.innerHTML = accounts.length
      ? accounts.map((account) => `<option value="${account.id}">${accountLabel(account)}</option>`).join("")
      : `<option value="">Compte cree automatiquement si absent</option>`;
    accountInput.disabled = role === "CLIENT" && !accounts.length;
  } else if (accountInput && state.accounts.length) {
    const accountIds = state.accounts.map((account) => String(account.id));
    if (!accountIds.includes(String(accountInput.value))) {
      accountInput.value = state.accounts[0].id;
    }
  }
}

function accountsForCurrentUser() {
  const userId = String(state.currentUser?.id ?? "");
  return state.accounts.filter((account) => String(account.customerId ?? "") === userId);
}

function accountLabel(account) {
  const numberLabel = account.accountNumber ? `Compte ${account.accountNumber}` : `Compte #${account.id}`;
  return `${numberLabel} - ${money(account.balance, account.currency || "XAF")}`;
}

function syncAccountFormDefaults() {
  if (!els.accountForm || !state.currentUser) return;

  const role = normalizeRole(state.currentUser.role);
  const customerInput = els.accountForm.elements.customerId;
  if (customerInput) {
    customerInput.value = role === "CLIENT" ? state.currentUser.id : customerInput.value || state.currentUser.id || 1;
    customerInput.readOnly = role === "CLIENT";
  }
}

async function ensureClientAccount() {
  const existing = accountsForCurrentUser()[0];
  if (existing) return existing;

  const userId = state.currentUser?.id;
  if (!userId) {
    throw new Error("Utilisateur connecte introuvable.");
  }

  const account = await accountApi("", {
    method: "POST",
    body: JSON.stringify({
      balance: 0,
      customerId: userId,
      status: "ACTIVE",
      currency: "XAF",
      accountType: "CURRENT"
    })
  });
  state.accounts = [...state.accounts, account];
  syncLoanFormDefaults();
  renderAccounts();
  return account;
}

function visibleAccounts() {
  return normalizeRole(state.currentUser?.role) === "CLIENT" ? accountsForCurrentUser() : state.accounts;
}

function accountOptions(accounts, placeholder = "Aucun compte disponible") {
  return accounts.length
    ? accounts.map((account) => `<option value="${account.id}">${accountLabel(account)}</option>`).join("")
    : `<option value="">${placeholder}</option>`;
}

function syncTransactionControls() {
  if (!els.transactionForm) return;

  const accounts = visibleAccounts();
  const sourceInput = els.transactionForm.elements.accountId;
  const receiverInput = els.transactionForm.elements.receiverName;
  const filterInput = els.transactionAccountFilter;
  const selectedSource = sourceInput?.value;
  const selectedFilter = filterInput?.value;

  if (sourceInput) {
    sourceInput.innerHTML = accountOptions(accounts);
    if (selectedSource && accounts.some((account) => String(account.id) === String(selectedSource))) {
      sourceInput.value = selectedSource;
    }
    sourceInput.disabled = !accounts.length;
  }
  if (filterInput) {
    filterInput.innerHTML = accountOptions(accounts, "Aucun compte a consulter");
    if (selectedFilter && accounts.some((account) => String(account.id) === String(selectedFilter))) {
      filterInput.value = selectedFilter;
    }
    filterInput.disabled = !accounts.length;
  }
  if (receiverInput && normalizeRole(state.currentUser?.role) === "CLIENT") {
    receiverInput.placeholder = "Nom du beneficiaire";
  }

  syncTransactionMode();
}

function syncTransactionMode() {
  if (!els.transactionForm) return;

  const type = els.transactionForm.elements.type.value;
  const destinationField = els.transactionForm.querySelector(".destination-account-field");
  const receiverInput = els.transactionForm.elements.receiverName;
  const isTransfer = type === "transfer";
  destinationField?.classList.toggle("hidden", !isTransfer);
  els.receiverAccountPreview?.classList.toggle("hidden", !isTransfer);
  if (receiverInput) {
    receiverInput.required = isTransfer;
    receiverInput.disabled = !isTransfer;
    if (!isTransfer) {
      receiverInput.value = "";
      setReceiverPreview("");
    }
  }
}

async function loadTransactionHistory() {
  if (normalizeRole(state.currentUser?.role) === "ADMIN") {
    const result = await settle("transactions", () => transactionApi());
    state.transactions = result.ok && Array.isArray(result.data) ? result.data : [];
    return;
  }

  const accountId = els.transactionAccountFilter?.value || visibleAccounts()[0]?.id;
  if (!accountId) {
    state.transactions = [];
    return;
  }

  const result = await settle("transactions", () => transactionApi(`/history/${accountId}`));
  state.transactions = result.ok && Array.isArray(result.data) ? result.data : [];
}

function setReceiverPreview(message, type = "info") {
  if (!els.receiverAccountPreview) return;
  els.receiverAccountPreview.textContent = message || "Compte receveur non selectionne.";
  els.receiverAccountPreview.dataset.type = type;
}

async function resolveReceiverAccount() {
  const receiverInput = els.transactionForm?.elements.receiverName;
  const receiverName = receiverInput?.value?.trim();
  if (!receiverName) {
    setReceiverPreview("");
    return null;
  }

  try {
    const params = new URLSearchParams({ name: receiverName });
    const account = await transactionApi(`/receiver/account?${params.toString()}`);
    const numberLabel = account.accountNumber || `#${account.id}`;
    setReceiverPreview(`Compte receveur: ${numberLabel} - Client ${account.customerId}`, "success");
    return account;
  } catch (error) {
    setReceiverPreview(`Aucun compte actif trouve pour "${receiverName}".`, "warning");
    return null;
  }
}

async function performAction(action, id, body) {
  const actionMap = {
    review: { method: "PUT", path: `/loans/${id}/review` },
    approve: { method: "PUT", path: `/loans/${id}/approve` },
    reject: { method: "PUT", path: `/loans/${id}/reject` },
    repay: { method: "POST", path: `/loans/${id}/repay` }
  };

  const config = actionMap[action];
  if (!config) return;

  try {
    await loanApi(config.path.replace("/loans", ""), {
      method: config.method,
      body: body ? JSON.stringify(body) : undefined
    });
    await loadAll();
    clearAlert();
  } catch (error) {
    showAlert(error.message);
  }
}

async function openLoan(id) {
  try {
    const loan = await loanApi(`/${id}`);
    let schedule = [];
    try {
      schedule = await loanApi(`/${id}/schedule`);
    } catch {
      schedule = [];
    }

    els.dialogTitle.textContent = `Credit #${loan.id}`;
    els.dialogContent.innerHTML = `
      <div class="dialog-body">
        <div class="detail-grid">
          <div><span>Statut</span><strong><span class="badge ${loan.status}">${loan.status}</span></strong></div>
          <div><span>Client</span><strong>${loan.customerId ?? "-"}</strong></div>
          <div><span>Compte</span><strong>${loan.accountId ?? "-"}</strong></div>
          <div><span>Montant</span><strong>${money(loan.amount)}</strong></div>
          <div><span>Restant</span><strong>${money(loan.remainingAmount)}</strong></div>
          <div><span>Paye</span><strong>${money(loan.totalPaid)}</strong></div>
          <div><span>Taux</span><strong>${loan.interestRate ?? 0}%</strong></div>
          <div><span>Duree</span><strong>${loan.durationMonths ?? 0} mois</strong></div>
        </div>
        <div class="detail-actions">
          ${workflowButtons(loan)}
          ${["APPROVED", "ACTIVE"].includes(loan.status) ? `
            <form class="repay-form" data-repay-form data-id="${loan.id}">
              <input name="amount" type="number" min="1" step="1" placeholder="Montant" required />
              <button class="primary-button" type="submit">Rembourser</button>
            </form>
          ` : ""}
        </div>
        <section>
          <div class="panel-header">
            <div>
              <p class="eyebrow">Simulation</p>
              <h2>Echeancier</h2>
            </div>
          </div>
          ${scheduleTable(schedule)}
        </section>
      </div>
    `;

    if (!els.dialog.open) {
      els.dialog.showModal();
    }
  } catch (error) {
    showAlert(error.message);
  }
}

function scheduleTable(schedule) {
  if (!schedule.length) {
    return `<div class="empty-state">Echeancier non disponible.</div>`;
  }

  return `
    <div class="table-wrap">
      <table>
        <thead><tr><th>#</th><th>Date</th><th>Mensualite</th><th>Principal</th><th>Interet</th><th>Solde</th></tr></thead>
        <tbody>
          ${schedule.map((entry) => `
            <tr>
              <td>${entry.installment}</td>
              <td>${entry.paymentDate || "-"}</td>
              <td>${money(entry.paymentAmount)}</td>
              <td>${money(entry.principal)}</td>
              <td>${money(entry.interest)}</td>
              <td>${money(entry.remainingBalance)}</td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

document.querySelectorAll(".nav-tab").forEach((tab) => {
  tab.addEventListener("click", () => setView(tab.dataset.view));
});

document.body.addEventListener("click", (event) => {
  const jump = event.target.closest("[data-jump]");
  if (jump) {
    setView(jump.dataset.jump);
  }
});

document.querySelector("#refreshButton").addEventListener("click", loadAll);
document.querySelector("#logoutButton").addEventListener("click", logout);
document.querySelector("#closeDialogButton").addEventListener("click", () => els.dialog.close());

document.querySelector("#saveSettingsButton").addEventListener("click", () => {
  state.apiBase = els.apiBaseInput.value.trim().replace(/\/$/, "") || "/api";
  state.serviceBase = els.serviceBaseInput.value.trim().replace(/\/$/, "") || "/services";
  localStorage.setItem("banking-api-base", state.apiBase);
  localStorage.setItem("banking-service-base", state.serviceBase);
  loadAll();
});

els.searchInput.addEventListener("input", (event) => {
  state.search = event.target.value;
  renderLoans();
});

els.statusFilter.addEventListener("change", (event) => {
  state.selectedStatus = event.target.value;
  renderLoans();
});

els.transactionForm?.elements.type.addEventListener("change", syncTransactionMode);

els.transactionForm?.elements.accountId.addEventListener("change", async () => {
  syncTransactionControls();
  await loadTransactionHistory();
  renderTransactions();
});

els.transactionForm?.elements.receiverName?.addEventListener("input", () => {
  clearTimeout(els.transactionForm.receiverLookupTimer);
  els.transactionForm.receiverLookupTimer = setTimeout(resolveReceiverAccount, 350);
});

els.transactionForm?.elements.receiverName?.addEventListener("blur", resolveReceiverAccount);

els.transactionAccountFilter?.addEventListener("change", async () => {
  await loadTransactionHistory();
  renderTransactions();
});

els.loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearAlert(els.loginAlert);
  const payload = Object.fromEntries(new FormData(event.currentTarget).entries());

  try {
    const result = await serviceApi("auth", "/login", { method: "POST", body: JSON.stringify(payload), auth: false });
    state.token = result.token || "";
    state.currentUser = {
      id: result.userId,
      fullName: result.fullName,
      email: result.email,
      role: normalizeRole(result.role)
    };
    localStorage.setItem("banking-auth-token", state.token);
    localStorage.setItem("banking-current-user", JSON.stringify(state.currentUser));
    showAuthenticatedApp();
    await loadAll();
  } catch (error) {
    showAlert(error.message, "warning", els.loginAlert);
  }
});

els.toggleClientRegisterButton.addEventListener("click", () => {
  setAuthMode(els.clientRegisterForm.classList.contains("hidden") ? "register" : "login");
});

els.clientRegisterForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearAlert(els.loginAlert);
  const form = event.currentTarget;
  const payload = Object.fromEntries(new FormData(form).entries());
  payload.role = "CLIENT";

  try {
    await serviceApi("auth", "/register", { method: "POST", body: JSON.stringify(payload), auth: false });
    els.loginForm.elements.email.value = payload.email;
    els.loginForm.elements.password.value = "";
    form.reset();
    setAuthMode("login");
    showAlert("Compte cree avec succes. Connectez-vous avec votre mot de passe.", "success", els.loginAlert);
  } catch (error) {
    showAlert(error.message, "warning", els.loginAlert);
  }
});

els.registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
  try {
    await serviceApi("auth", "/register", { method: "POST", body: JSON.stringify(payload), auth: false });
    await loadAll();
    showAlert("Utilisateur cree.", "success");
  } catch (error) {
    showAlert(error.message);
  }
});

els.accountForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const formData = new FormData(form);
  const payload = Object.fromEntries(formData.entries());
  payload.customerId = Number(payload.customerId);
  payload.balance = Number(payload.balance || 0);
  payload.status = "ACTIVE";

  try {
    const account = await accountApi("", { method: "POST", body: JSON.stringify(payload) });
    state.accounts = [...state.accounts, account];
    syncAccountFormDefaults();
    syncLoanFormDefaults();
    syncTransactionControls();
    render();
    showAlert(`Compte bancaire cree: ${account.accountNumber || `#${account.id}`}.`, "success");
  } catch (error) {
    showAlert(error.message);
  }
});

els.loanForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const formData = new FormData(form);
  const payload = Object.fromEntries([...formData.entries()].map(([key, value]) => [key, Number(value)]));

  try {
    if (normalizeRole(state.currentUser?.role) === "CLIENT") {
      const account = await ensureClientAccount();
      payload.customerId = Number(state.currentUser.id);
      payload.accountId = Number(account.id);
    }
    await loanApi("", { method: "POST", body: JSON.stringify(payload) });
    form?.reset();
    await loadAll();
    syncLoanFormDefaults();
    setView("loans");
  } catch (error) {
    showAlert(error.message);
  }
});

els.transactionForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const formData = new FormData(form);
  const type = formData.get("type");
  const amount = formData.get("amount");

  try {
    if (!formData.get("accountId")) {
      throw new Error("Aucun compte disponible pour cette operation.");
    }

    const params = new URLSearchParams({ amount });
    if (type === "transfer") {
      const receiverName = String(formData.get("receiverName") || "").trim();
      if (!receiverName) {
        throw new Error("Saisissez le nom du beneficiaire.");
      }
      const receiverAccount = await resolveReceiverAccount();
      if (!receiverAccount) {
        throw new Error("Le receveur doit avoir un compte bancaire actif.");
      }
      params.set("sourceAccountId", formData.get("accountId"));
      params.set("receiverName", receiverName);
      await transactionApi(`/transfer/name?${params.toString()}`, { method: "POST" });
    } else {
      params.set("accountId", formData.get("accountId"));
      await transactionApi(`/${type}?${params.toString()}`, { method: "POST" });
    }

    await loadAll();
    showAlert("Transaction executee avec succes.", "success");
    setView("transactions");
  } catch (error) {
    showAlert(error.message);
  }
});

document.body.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action]");
  if (!button) return;

  const { action, id } = button.dataset;
  if (action === "open") {
    openLoan(id);
    return;
  }

  await performAction(action, id);
  if (els.dialog.open) {
    openLoan(id);
  }
});

document.body.addEventListener("submit", async (event) => {
  const form = event.target.closest("[data-repay-form]");
  if (!form) return;

  event.preventDefault();
  const id = form.dataset.id;
  const amount = Number(new FormData(form).get("amount"));
  await performAction("repay", id, { amount });
  if (els.dialog.open) {
    openLoan(id);
  }
});

els.apiBaseInput.value = state.apiBase;
els.serviceBaseInput.value = state.serviceBase;

if (state.currentUser && state.token) {
  state.currentUser.role = normalizeRole(state.currentUser.role);
  showAuthenticatedApp();
  loadAll();
}
