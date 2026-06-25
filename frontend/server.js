import { createServer } from "node:http";
import { createReadStream, existsSync, statSync } from "node:fs";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = fileURLToPath(new URL(".", import.meta.url));
const preferredPort = Number(process.env.FRONTEND_PORT || 5173);
const gatewayUrl = process.env.API_PROXY_TARGET || "http://localhost:8083";

const serviceTargets = {
  config: process.env.CONFIG_SERVICE_URL || "http://localhost:8080",
  registry: process.env.REGISTRY_SERVICE_URL || "http://localhost:8081",
  account: process.env.ACCOUNT_SERVICE_URL || "http://localhost:8082",
  gateway: process.env.GATEWAY_SERVICE_URL || gatewayUrl,
  auth: process.env.AUTH_SERVICE_URL || "http://localhost:8084",
  loan: process.env.LOAN_SERVICE_URL || "http://localhost:8086"
};

const mimeTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg"
};

function sendJson(res, status, payload) {
  res.writeHead(status, { "content-type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(payload));
}

async function proxy(req, res, targetBase, stripPrefix = "") {
  const sourceUrl = new URL(req.url, "http://127.0.0.1");
  const nextPath = stripPrefix && sourceUrl.pathname.startsWith(stripPrefix)
    ? sourceUrl.pathname.slice(stripPrefix.length) || "/"
    : sourceUrl.pathname;
  const target = new URL(`${nextPath}${sourceUrl.search}`, targetBase);
  const headers = { ...req.headers, host: target.host };

  try {
    const upstream = await fetch(target, {
      method: req.method,
      headers,
      body: ["GET", "HEAD"].includes(req.method || "GET") ? undefined : req,
      duplex: "half"
    });

    res.writeHead(upstream.status, Object.fromEntries(upstream.headers));
    if (upstream.body) {
      for await (const chunk of upstream.body) {
        res.write(chunk);
      }
    }
    res.end();
  } catch (error) {
    sendJson(res, 502, {
      error: "Service indisponible",
      target: targetBase,
      detail: error instanceof Error ? error.message : String(error)
    });
  }
}

function proxyService(req, res) {
  const match = req.url?.match(/^\/services\/([^/?#]+)(.*)?$/);
  const key = match?.[1];
  const target = key ? serviceTargets[key] : null;

  if (!key || !target) {
    sendJson(res, 404, { error: "Service inconnu", services: Object.keys(serviceTargets) });
    return;
  }

  proxy(req, res, target, `/services/${key}`);
}

function serveStatic(req, res) {
  const rawPath = decodeURIComponent(new URL(req.url, "http://127.0.0.1").pathname);
  const safePath = normalize(rawPath).replace(/^(\.\.[/\\])+/, "");
  let filePath = join(__dirname, safePath === "/" ? "index.html" : safePath);

  if (!filePath.startsWith(__dirname)) {
    sendJson(res, 403, { error: "Acces refuse" });
    return;
  }

  if (!existsSync(filePath) || statSync(filePath).isDirectory()) {
    filePath = join(__dirname, "index.html");
  }

  const ext = extname(filePath);
  res.writeHead(200, { "content-type": mimeTypes[ext] || "application/octet-stream" });
  createReadStream(filePath).pipe(res);
}

function listen(port, retries = 4) {
  const server = createServer((req, res) => {
    if (req.url?.startsWith("/api/")) {
      proxy(req, res, gatewayUrl);
      return;
    }
    if (req.url?.startsWith("/services/")) {
      proxyService(req, res);
      return;
    }
    serveStatic(req, res);
  });

  server.once("error", (error) => {
    if (error.code === "EADDRINUSE" && retries > 0) {
      listen(port + 1, retries - 1);
      return;
    }
    throw error;
  });

  server.listen(port, "127.0.0.1", () => {
    const address = server.address();
    const activePort = typeof address === "object" && address ? address.port : port;
    console.log(`Frontend: http://127.0.0.1:${activePort}`);
    console.log(`Gateway API: ${gatewayUrl}`);
    console.log("Direct services:", serviceTargets);
  });
}

listen(preferredPort);
