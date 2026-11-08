"""Pré-visualização somente de leitura do relatório do projeto Java."""
from html import escape
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


class Preview(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path not in ("/", "/README.md"):
            self.send_error(404)
            return
        report = (ROOT / "README.md").read_text(encoding="utf-8")
        if self.path == "/README.md":
            body = report.encode("utf-8")
            content_type = "text/plain; charset=utf-8"
        else:
            body = ("""<!doctype html><html lang="pt-BR"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ajuda Aí — Relatório AEDS III</title>
<meta name="description" content="Relatório do projeto Java Ajuda Aí e de seus índices de perguntas.">
<meta property="og:title" content="Ajuda Aí — Relatório AEDS III">
<meta property="og:description" content="Relatório do projeto Java Ajuda Aí e de seus índices de perguntas.">
<meta property="og:type" content="website"><meta name="twitter:card" content="summary">
<style>:root{--background:#f8faf9;--foreground:#242925;--border:#cdd6d0;--accent:#186449}
*{box-sizing:border-box}body{margin:0;background:var(--background);color:var(--foreground);font-family:Georgia,serif}
main{max-width:960px;margin:auto;padding:32px 24px}h1{font-size:32px;letter-spacing:0}
p{line-height:1.6}a{color:var(--accent)}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:14px/1.7 monospace;
border-top:1px solid var(--border);padding-top:24px}</style></head><body><main>
<h1>Ajuda Aí</h1><p>Trabalho de AEDS III em Java. Esta página apresenta o relatório;
o programa continua sendo executado pelo terminal.</p><a href="/README.md">Abrir relatório em texto</a>
<pre>""" + escape(report) + "</pre></main></body></html>").encode("utf-8")
            content_type = "text/html; charset=utf-8"
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    HTTPServer(("0.0.0.0", 8080), Preview).serve_forever()