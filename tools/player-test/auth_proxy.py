#!/usr/bin/env python3
"""Local test-only forward proxy: adds Proxy-Authorization for the VM egress proxy.
Binds 127.0.0.1 only. No request contents or credentials are logged."""
import base64, os, re, socket, socketserver, threading

raw = os.environ.get("https_proxy") or os.environ.get("http_proxy") or ""
m = re.match(r"^[a-z]+://([^@]+)@([^:/]+):(\d+)", raw)
assert m, "no authed egress proxy in env"
AUTH = "Basic " + base64.b64encode(m.group(1).encode()).decode()
UPSTREAM = (m.group(2), int(m.group(3)))

def pipe(src, dst):
    try:
        while True:
            chunk = src.recv(65536)
            if not chunk:
                break
            dst.sendall(chunk)
    except OSError:
        pass
    finally:
        try: dst.shutdown(socket.SHUT_WR)
        except OSError: pass

class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            self._handle()
        except Exception:
            pass
    def _handle(self):
        data = b""
        while b"\r\n\r\n" not in data and len(data) < 65536:
            chunk = self.request.recv(4096)
            if not chunk:
                return
            data += chunk
        head, _, rest = data.partition(b"\r\n\r\n")
        lines = head.split(b"\r\n")
        method, target, _ver = lines[0].split(b" ", 2)
        method = method.decode()
        if method == "CONNECT":
            up = socket.create_connection(UPSTREAM, timeout=30)
            try:
                up.sendall(b"CONNECT " + target + b" HTTP/1.1\r\nHost: " + target +
                           b"\r\nProxy-Authorization: " + AUTH.encode() + b"\r\n\r\n")
                resp = b""
                while b"\r\n\r\n" not in resp and len(resp) < 65536:
                    c = up.recv(4096)
                    if not c: break
                    resp += c
                if not resp.startswith(b"HTTP/1.1 200") and not resp.startswith(b"HTTP/1.0 200"):
                    self.request.sendall(b"HTTP/1.1 502 Bad Gateway\r\n\r\n")
                    return
                self.request.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")
                t = threading.Thread(target=pipe, args=(up, self.request), daemon=True); t.start()
                pipe(self.request, up); t.join(timeout=5)
            finally:
                up.close()
            return
        # plain HTTP: forward absolute-form request with auth added
        up = socket.create_connection(UPSTREAM, timeout=30)
        try:
            out = [lines[0]]
            for ln in lines[1:]:
                if ln.lower().startswith(b"proxy-authorization:") or ln.lower().startswith(b"proxy-connection:"):
                    continue
                out.append(ln)
            out.append(b"Proxy-Authorization: " + AUTH.encode())
            up.sendall(b"\r\n".join(out) + b"\r\n\r\n" + rest)
            t = threading.Thread(target=pipe, args=(up, self.request), daemon=True); t.start()
            pipe(self.request, up); t.join(timeout=10)
        finally:
            up.close()

class Server(socketserver.ThreadingMixIn, socketserver.TCPServer):
    daemon_threads = True
    allow_reuse_address = True

if __name__ == "__main__":
    print("auth proxy on 127.0.0.1:8898", flush=True)
    Server(("127.0.0.1", 8898), Handler).serve_forever()
