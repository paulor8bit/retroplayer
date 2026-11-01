#!/usr/bin/env python3
"""Download pinned ARM64 cloudflared and adapt the Linux DNS path for Android."""
import hashlib
from pathlib import Path
from urllib.request import urlopen
VERSION = '2026.10.7'
SHA256 = '3df43aedc9ff291d1735b5cb576cb97e0b157488796db2e32b28aecabb445775'
ROOT = Path(__file__).resolve().parent.parent
url = f'https://github.com/cloudflare/cloudflared/releases/download/{VERSION}/cloudflared-linux-arm64'
print(f'Downloading cloudflared {VERSION} (ARM64)…')
with urlopen(url, timeout=120) as response:
    data = response.read()
if hashlib.sha256(data).hexdigest() != SHA256:
    raise SystemExit('SHA256 mismatch: refusing to package executable')
old, new = b'/etc/resolv.conf', b'./dns/resolv.cnf'
assert len(old) == len(new) and data.count(old) == 1
path = ROOT / 'app/src/main/jniLibs/arm64-v8a/libcloudflared.so'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(data.replace(old, new))
path.chmod(0o755)
print(f'Prepared {path.relative_to(ROOT)}')
