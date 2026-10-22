// Tiny website + receiver. No dependencies. Needs Node 18+.
// Set the SECRET environment variable to the same value as in the plugin's config.yml.
const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = process.env.PORT || 3000;
const SECRET = process.env.SECRET;
if (!SECRET) {
  console.error("Set the SECRET environment variable first.");
  process.exit(1);
}

let state = { players: [], updatedAt: 0 };

const server = http.createServer((req, res) => {
  if (req.method === "POST" && req.url === "/update") {
    if (req.headers["x-secret"] !== SECRET) {
      res.writeHead(401).end("Bad secret");
      return;
    }
    let body = "";
    req.on("data", (chunk) => {
      body += chunk;
      if (body.length > 100_000) req.destroy();
    });
    req.on("end", () => {
      try {
        const data = JSON.parse(body);
        if (!Array.isArray(data.players)) throw new Error("no players");
        state = { players: data.players, updatedAt: Date.now() };
        res.writeHead(204).end();
      } catch {
        res.writeHead(400).end("Bad JSON");
      }
    });
    return;
  }

  if (req.method === "GET" && req.url === "/api/players") {
    res.writeHead(200, { "Content-Type": "application/json", "Cache-Control": "no-store" });
    res.end(JSON.stringify({ ...state, now: Date.now() }));
    return;
  }

  if (req.method === "GET" && (req.url === "/" || req.url === "/index.html")) {
    res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
    res.end(fs.readFileSync(path.join(__dirname, "public", "index.html")));
    return;
  }

  res.writeHead(404).end("Not found");
});

server.listen(PORT, () => console.log("Listening on " + PORT));
