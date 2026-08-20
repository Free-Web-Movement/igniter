# Free Web Proxy Client for Android

> Derived from [Igniter](https://github.com/trojan-gfw/igniter)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## Goals

This project is a first and important step towards the free web. It allows people to hide their web activities as much as possible by interacting with proxy servers supported by trojan servers and finally by free web movement servers.

## Name

The current name Igniter will be replace by `free web proxy` when most of the original source codes are migrated.

## Current Plans & Status

1. - [x] Separation of Trojan configuration from global settings []
2. - [ ] Enabling the ordered Trojan server list and default server selection.
3. - [ ] Enable real-time connection status
4. - [x] Minimize running modules to enable Android TV/Auto/Watch support
5. - [ ] Enable share servers with QRCODE and URI


## Architecture

### Proxy Chain

The app routes traffic through a local VPN interface (tun) into a SOCKS5
proxy backed by trojan-go. Two modes are supported:

#### Non-Clash mode (default)

```
App traffic
  → Android VPN (tun0)
    → go-tun2socks (captures all IP traffic from tun0)
      → trojan-go SOCKS5 listener (127.0.0.1:localPort, default 1080)
        → trojan protocol (TLS) → remote server → internet
```

All IP traffic (TCP + UDP via tun2socks) is forwarded to the single
trojan-go SOCKS5 port. DNS is handled by the system resolvers or
tun2socks depending on configuration.

#### Clash mode (optional)

```
App traffic
  → Android VPN (tun0)
    → go-tun2socks
      → Socks5Gate (SOCKS5 gateway, random port)
        → per-domain routing decision (direct / proxy / unreachable)
          → Clash SOCKS5 (rule-based routing, e.g. GeoIP, domain lists)
            → trojan-go SOCKS5 (localPort + 1)
              → trojan protocol → remote server → internet
```

Socks5Gate sits between tun2socks and Clash. It probes domains in
real-time to decide whether to route directly (for CN sites) or
through the proxy (for foreign sites). Clash handles rule-based routing
(GeoIP, domain lists, fallback chains).

### Port Management

| Port             | Role                                             | Default       |
|------------------|--------------------------------------------------|---------------|
| `localPort`      | trojan-go SOCKS5 listener                        | 1080          |
| `clashSocksPort` | Clash SOCKS5 listener (Clash mode only)          | 1080          |
| `localPort + 1`  | trojan-go when Clash is enabled (avoids conflict)| 1081          |
| tun2socks target | SOCKS5 address tun2socks forwards to             | = trojan port |

The port is stored in `config.json` and kept in sync by
`NetWorkConfig.applyTrojanPort()`. When `Server.local_port` is 0
(unconfigured), the default port (1080) is used. The port is **not**
randomized — `findFreePort()` is only used for the Clash SOCKS port to
avoid startup crashes when the port is busy.

### Connectivity Test (G button)

```
Main process (UI)
  → AIDL → ProxyService.testConnection() in :proxy process
    → TestConnection (AsyncTask, SOCKS5 proxy)
      → 127.0.0.1:localPort  (bypasses VPN tunnel)
        → trojan-go → remote server → https://www.google.com
```

The `:proxy` process is excluded from its own VPN tunnel via
`addDisallowedApplication(packageName)`, so the test socket reaches
trojan-go directly without looping through tun2socks.

### Process Architecture

| Process         | Role                                                    |
|-----------------|---------------------------------------------------------|
| Main process    | UI (Compose), AIDL client, server DB, settings          |
| `:proxy` process| VPN service, tun2socks, trojan-go (JNI), Clash, Socks5Gate |

The main process binds to `ProxyService` via AIDL to control the proxy
and receive state/test-result callbacks. The `:proxy` process owns all
native (Go) networking and must be recycled on configuration changes
because the Go runtime cannot re-initialize within the same process.

### VPN Routing

- **Non-Clash**: `addRoute("0.0.0.0", 0)` — all traffic through tun.
  System DNS servers are used.
- **Clash**: bypass-private routes + `addRoute("198.18.0.0", 16)` for
  fake-ip range. DNS is set to a fake-ip address so tun2socks can
  intercept and fake-ip-resolve for domain-based routing.
- The app's own package is always excluded from the VPN to avoid loops.

### Key Files

| File                          | Role                                         |
|-------------------------------|----------------------------------------------|
| `NetWorkConfig.kt`            | VPN setup, port management, tun2socks start  |
| `ProxyService.kt`            | VPN service, AIDL binder, state machine      |
| `TestConnection.kt`          | SOCKS5 connectivity test                     |
| `Socks5Gate.kt`              | Per-domain routing gateway (Clash mode)      |
| `TrojanConfig.kt`           | config.json read/write, port persistence     |
| `JNIHelper.kt`              | JNI bridge to trojan-go native               |
| `ClashConfig.kt`            | Clash YAML config management                 |
| `constants/Clash.kt`        | Default ports, mode names, Clash API keys    |
| `constants/Net.kt`          | VPN addresses, MTU, DNS servers              |

## Get Code

1. Clone the repo.
```
gh repo clone Free-Web-Movement/igniter -- --recurse-submodules
```
> if failed, run the following command:
> ```
> cd igniter
> git submodule update --init --recursive
> ```

## Versioning

This project follows [Effective Versioning](https://github.com/calidion/effective-versioning) to force compatibility check before upgrading.


## License

New code added by Free Web Movement Project is subject to GPL， which means you cannot close source of this project for your private purposes.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
