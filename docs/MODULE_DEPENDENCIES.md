# Sokybot Module Dependencies — Comprehensive Analysis

This document describes the dependency structure of all Sokybot application modules: internal (project) dependencies, external libraries, scopes, and how they fit into the OSGi/Karaf runtime.

**Project:** `org.sokybot:sokybot` 1.0-SNAPSHOT  
**Build:** Maven multi-module, Java 21  
**Runtime:** Apache Karaf 4.4.6, OSGi bundles (Felix)

---

## 1. Project Structure Overview

```
sokybot (root)
├── core/          — Engine, API, runtime
├── network/       — Proxy, security, packet sniffer
├── game/          — Game data, events, loader, model, navigation, PK2
├── ui/            — Webview, HTTP server, dev tools, machine pages
├── infra/         — Commons, persistence, settings, features, dist, build-tools, dev-shell
└── legacy/        — Reference only; not included in the Maven reactor (not built)
```

The `legacy/` directory is kept for reference only and is **not** included in the Maven build. Legacy modules previously used SwingX, MyDoggy, TableLayout, and Ehcache; these libraries are no longer managed in the root POM.

**Aggregator POMs (no direct dependencies):**  
`core`, `network`, `game`, `ui`, `infra` — they only declare `<modules>`.

---

## 2. Root POM: Dependency Management & Inheritance

The root `pom.xml` provides:

- **`dependencyManagement`** for versions of:
  - Reactor BOM, Jackson BOM
  - Internal: `sokybot-security`, `sokybot-pk2`, `sokybot-loader-api`, `sokybot-game-loader`, `sokybot-ui-api`
  - JNA, Commons (lang3, io, csv, math3), Logback, SLF4J, Jackson, Lombok
  - JUnit Jupiter, Mockito, Byte Buddy

- **Global dependencies** (inherited by all modules unless overridden):
  - **Provided:** JNA, Commons (io, lang3, csv, math3), OSGi (cm, metatype, framework, SCR, component annotations, log), Felix framework, Logback, SLF4J, Lombok
  - **Test:** JUnit Jupiter, Mockito (core, junit-jupiter)

- **Key version properties** (examples):  
  `jna.version`, `slf4j.version`, `logback.version`, `jackson.version`, `lombok.version`, `netty.version`, `reactor.version`, `reactor-netty.version`, `rsocket.version`, `karaf.version`, etc.

---

## 3. Module-by-Module Dependencies

### 3.1 Core

| Module | Packaging | Internal (org.sokybot) | External (compile/provided) | Test / Notes |
|--------|-----------|------------------------|-----------------------------|--------------|
| **sokybot-engine-api** | bundle | sokybot-proxy, sokybot-settings, sokybot-game-model | (inherited) | — |
| **sokybot-engine** | bundle | sokybot-engine-api, sokybot-proxy, sokybot-security, sokybot-commons, sokybot-persistence, sokybot-game-loader, sokybot-game-model, sokybot-game-asset, sokybot-packet-sniffer-api (optional) | Lombok, javax.annotation-api 1.3.2, osgi.core 8.0.0, org.osgi.service.component.annotations 1.5.1, Groovy 4.0.16, reactor-core 3.6.0; Jackson and Felix Atomos **provided** by sokybot-jackson / sokybot-platform features | Uses shared bundles (Jackson, Atomos) via Karaf features and Import-Package; embeds only Groovy (scripting). Optional packet-sniffer-api for scripted pages that use IPacketSnifferRegistry. |
| **sokybot-runtime** | bundle | sokybot-commons, sokybot-game-events-api, sokybot-loader-api, sokybot-proxy, sokybot-engine-api, sokybot-persistence, sokybot-game-model, sokybot-game-events, sokybot-game-navigation | org.osgi.service.component.annotations 1.5.1, org.osgi.service.event 1.4.1, slf4j-api, jackson-databind, jackson-datatype-jsr310, Lombok | JUnit 5, Mockito |

**Core dependency chain (conceptual):**  
engine-api ← engine ← runtime; engine-api → proxy, settings, game-model; engine → security, commons, persistence, game-*, reactor; runtime → commons, game-events*, loader-api, proxy, engine-api, persistence, game-model, game-events, game-navigation.

---

### 3.2 Network

| Module | Packaging | Internal (org.sokybot) | External (compile/provided) | Test / Notes |
|--------|-----------|------------------------|-----------------------------|--------------|
| **sokybot-security** | bundle | — | (inherited only) | Leaf; exports org.sokybot.security.* |
| **sokybot-proxy** | bundle | sokybot-security | Netty transport 4.1.85, Netty handler 4.1.85, osgi.core 6.0.0, osgi.cmpn 6.0.0 | — |
| **sokybot-packet-sniffer-api** | bundle | — | reactor-core 3.5.11 | API only; exports org.sokybot.packetsniffer.api (IPacketSnifferPage, IPacketSnifferRegistry). Consumed by packet-sniffer, machine-pages (optional), engine (optional). |
| **sokybot-packet-sniffer** | bundle | sokybot-packet-sniffer-api, sokybot-runtime, sokybot-proxy | jackson-databind 2.15.2, reactor-core 3.5.11, org.osgi.framework 1.9.0, org.osgi.service.component.annotations 1.5.0, org.osgi.service.event 1.4.1 | No longer depends on webview; registers IPacketSnifferRegistry; scripted pages (PacketSniffer, PacketAnalyzer) in scripts/pages/ use the API. |

**Network chain:** security (leaf) ← proxy ← packet-sniffer-api (leaf) ← packet-sniffer; packet-sniffer also depends on runtime.

---

### 3.3 Game

| Module | Packaging | Internal (org.sokybot) | External (compile/provided) | Test / Notes |
|--------|-----------|------------------------|-----------------------------|--------------|
| **sokybot-loader-api** | bundle | — | (inherited) | API only; no deps |
| **sokybot-pk2** | bundle | sokybot-security (via parent ref) | (inherited) | junit-jupiter-params (test) |
| **sokybot-pk2-extractor** | bundle | sokybot-pk2, sokybot-security | Lombok, commons-csv 1.10.0, org.osgi.service.component.annotations 1.4.0, osgi.core 6.0.0, commons-io 2.16.1 (compile, embedded) | — |
| **sokybot-game-events-api** | bundle | sokybot-proxy, sokybot-persistence | (inherited) | — |
| **sokybot-game-events** | bundle | sokybot-game-events-api, sokybot-commons, sokybot-persistence, sokybot-settings, sokybot-proxy | org.osgi.service.event 1.4.1, org.osgi.service.component.annotations 1.5.1, slf4j 1.7.36, Lombok | JUnit, Mockito, AssertJ |
| **sokybot-game-model** | bundle | sokybot-game-events, sokybot-commons | Lombok, org.osgi.service.component.annotations 1.4.0, org.osgi.service.event 1.4.0, reactor-core (provided) | Note: game-model depends on game-events (API/impl) |
| **sokybot-game-loader** | bundle | sokybot-loader-api, sokybot-commons | pecoff4j 0.3.2 | — |
| **sokybot-game-asset** | bundle | sokybot-pk2, sokybot-persistence | org.osgi.service.component.annotations 1.5.1, slf4j-api | — |
| **sokybot-game-navigation** | bundle | sokybot-persistence, sokybot-commons | javax.annotation-api 1.3.2, org.osgi.service.component.annotations 1.5.1, slf4j-api, poly2tri-core 0.1.2 | — |

**Game dependency order (for loading):**  
loader-api, pk2 (+ security) → pk2-extractor; proxy, persistence → game-events-api → game-events; game-events + commons → game-model; persistence, commons → game-asset, game-navigation; loader-api, commons → game-loader.

---

### 3.4 UI

| Module | Packaging | Internal (org.sokybot) | External (compile/provided) | Test / Notes |
|--------|-----------|------------------------|-----------------------------|--------------|
| **sokybot-http-server** | bundle | — | reactor-netty-http, reactor-core (provided by `sokybot-reactor` feature), Jackson (core, databind, annotations) provided, org.osgi.framework 1.9.0, org.osgi.service.component.annotations 1.5.0, org.osgi.service.metatype.annotations 1.4.1, slf4j-api | Uses shared bundles via Karaf features and Import-Package |
| **sokybot-webview** | bundle | sokybot-http-server, sokybot-game-events-api, sokybot-game-events, sokybot-runtime, sokybot-game-model, sokybot-engine-api, sokybot-persistence, sokybot-settings, sokybot-commons, sokybot-engine, sokybot-proxy | rsocket-core 1.1.4, rsocket-transport-netty 1.1.4 (provided by `sokybot-rsocket` feature), reactor-core & reactor-netty-http (provided), reactive-streams 1.0.4, jackson-databind (provided), org.osgi.*, slf4j-api, jsr305 3.0.2 | JUnit, Mockito, reactor-test. Frontend build (Node/npm) |
| **sokybot-machine-pages** | bundle | sokybot-webview, sokybot-engine-api, sokybot-runtime, sokybot-proxy, sokybot-game-model, sokybot-settings, sokybot-game-events, sokybot-logging, sokybot-packet-sniffer-api (optional) | jackson-databind 2.15.2, reactor-core 3.5.11, org.osgi.*, slf4j-api, Groovy 4.0.16, groovy-json 4.0.16 | Optional packet-sniffer-api for PacketSniffer/PacketAnalyzer scripted page wiring. |
| **sokybot-dev-tools** | bundle | sokybot-http-server, sokybot-persistence, sokybot-runtime | rsocket-core/rsocket-transport-netty 1.1.4 (provided by `sokybot-rsocket` feature), reactor-netty-http & reactive-streams (provided), jackson-databind (provided), org.osgi.*, slf4j-api, javax.persistence-api 2.2, hibernate-core 5.6.15 (provided) | Copies bundle to plugins dir; frontend build |

**UI chain:** http-server ← webview, dev-tools; webview ← machine-pages; machine-pages optionally uses packet-sniffer-api for Packet Sniffer/Analyzer pages; packet-sniffer (network) registers IPacketSnifferRegistry only, no webview dependency.

---

### 3.5 Infrastructure

| Module | Packaging | Internal (org.sokybot) | External (compile/provided) | Test / Notes |
|--------|-----------|------------------------|-----------------------------|--------------|
| **sokybot-commons** | bundle | — | Lombok, slf4j-api, JNA, jna-platform, reactor-core, org.osgi.service.component.annotations 1.5.0, org.osgi.service.event 1.4.0 | Shared utilities, JNA, reactor, OSGi |
| **sokybot-persistence** | bundle | sokybot-pk2-extractor, sokybot-pk2, sokybot-security, sokybot-commons | JPA 2.2, Hibernate 5.6.15 (core, osgi) and related deps (provided by `sokybot-hibernate` feature), H2 2.1.214 (provided by `sokybot-platform` feature), OSGi (component, cm, core), Lombok, slf4j, Spring (context, orm, tx) 5.3.27, Spring Data JPA 2.7.10, javax.transaction-api | Uses shared bundles via Karaf features and Import-Package; test deps |
| **sokybot-settings** | bundle | sokybot-persistence | Lombok, JAXB (jaxb-api 2.3.1, jaxb-runtime 2.3.1, javax.activation-api 1.2.0), slf4j 1.7.36, jackson-databind 2.15.2, org.osgi.service.component.annotations 1.5.1 | — |
| **sokybot-features** | feature | loader-api, engine-api, game-events-api, commons, http-server, webview, dev-tools, dev-shell | JNA, jna-platform, Jackson (annotations, core, databind) | Karaf feature descriptors only |
| **sokybot-dist** | karaf-assembly | sokybot-features | Karaf framework 4.4.6, Karaf features “standard”, sokybot-features (XML) | Boot: eventadmin, log; boot features: standard, scr, http, sokybot-jackson, sokybot-full |
| **sokybot-build-tools** | jar | — | — | Checkstyle/versions rules, no runtime deps |
| **sokybot-dev-shell** | bundle | sokybot-runtime, sokybot-engine-api, sokybot-engine, sokybot-machine-pages, sokybot-commons, sokybot-game-model, sokybot-game-events, sokybot-settings, sokybot-proxy | Karaf Shell core 4.3.0, reactor-core 3.6.0 | Karaf CLI commands |

**Infra chain:** commons (leaf for app libs) ← persistence, many others; persistence ← settings; settings used by engine-api, game-events, machine-pages, dev-shell; features references many bundles; dist assembles Karaf with features.

---

## 4. Cross-Module Dependency Graph (Internal Only)

```
sokybot-security
  ↑
  ├── sokybot-pk2
  ├── sokybot-proxy
  ├── sokybot-pk2-extractor
  └── sokybot-persistence

sokybot-loader-api (no deps)
  ↑
  └── sokybot-game-loader ← sokybot-commons

sokybot-commons
  ↑
  ├── sokybot-engine, sokybot-runtime, sokybot-game-events, sokybot-game-model,
  ├── sokybot-game-loader, sokybot-game-navigation, sokybot-persistence,
  ├── sokybot-dev-shell
  └── (inherited by many)

sokybot-proxy
  ↑
  ├── sokybot-engine-api, sokybot-engine
  ├── sokybot-game-events-api, sokybot-game-events
  ├── sokybot-runtime, sokybot-packet-sniffer, sokybot-dev-shell
  └── sokybot-machine-pages

sokybot-persistence
  ↑
  ├── sokybot-settings
  ├── sokybot-game-events-api, sokybot-game-asset, sokybot-game-navigation
  ├── sokybot-engine, sokybot-runtime
  ├── sokybot-webview, sokybot-dev-tools
  └── sokybot-pk2-extractor (via persistence’s use of pk2/pk2-extractor)

sokybot-settings
  ↑
  ├── sokybot-engine-api
  ├── sokybot-game-events
  ├── sokybot-machine-pages, sokybot-dev-shell
  └── sokybot-webview

sokybot-game-events-api → sokybot-game-events (impl)
sokybot-game-model → sokybot-game-events
sokybot-engine-api → sokybot-engine (engine has optional import of sokybot.packetsniffer.api for script classloading)
sokybot-http-server (no internal) → sokybot-webview, sokybot-dev-tools
sokybot-webview → sokybot-machine-pages
sokybot-packet-sniffer-api (no internal) → sokybot-packet-sniffer; optionally imported by sokybot-engine, sokybot-machine-pages
sokybot-runtime → sokybot-engine-api, sokybot-engine (via dev-shell)
sokybot-machine-pages → sokybot-dev-shell
```

---

## 5. External Technology Stack (Summary)

| Category | Libraries | Used By (examples) |
|----------|-----------|---------------------|
| **OSGi / Karaf** | Felix Framework, osgi.core, osgi.cmpn, org.osgi.service.* (component, event, cm, metatype), Felix SCR, Felix Atomos | All bundles, dist |
| **Networking** | Netty 4.1.x, Reactor Netty, Reactor Core, RSocket 1.1.4 | proxy, http-server, webview, dev-tools, packet-sniffer |
| **Serialization** | Jackson 2.15.x (core, databind, annotations, dataformat-yaml, datatype-jsr310) | engine, runtime, webview, settings, machine-pages, persistence, features |
| **Persistence** | JPA 2.2, Hibernate 5.6.x, H2 2.1.x, Spring Context/ORM/Tx, Spring Data JPA | persistence, settings, dev-tools |
| **Game / PK2** | pecoff4j, poly2tri-core, commons-csv, commons-io | game-loader, game-navigation, pk2-extractor |
| **Scripting** | Groovy 4.0.16 (+ groovy-json) | engine, machine-pages |
| **Logging** | SLF4J, Logback | All (inherited or direct) |
| **Utilities** | Lombok, JNA, Commons (lang3, io, csv, math3), javax.annotation-api, jsr305 | commons, engine, many modules |
| **Testing** | JUnit Jupiter, Mockito, AssertJ, reactor-test | runtime, game-events, webview, persistence |
| **Build / Runtime** | Karaf 4.4.6, maven-bundle-plugin (Felix), frontend-maven-plugin (Node/npm) | dist, webview, dev-tools |

---

## 6. Scopes and Embedding

- **provided:** OSGi/Karaf, JNA, Jackson, Reactor/Netty, RSocket, Hibernate/JPA stack (when supplied by dedicated features), Spring, Lombok, SLF4J/Logback. These are not packaged inside the bundle.
- **compile (default):** Packaged unless bundle instructions exclude/embed explicitly. Some bundles still embed non-OSGi libraries when necessary for classloading (e.g. Groovy, poly2tri, pecoff4j, commons-io in pk2-extractor).
- **test:** JUnit, Mockito, AssertJ, reactor-test — not deployed.

**Notable embedding:**
- **sokybot-engine:** Relies on **sokybot-jackson** and **sokybot-platform** features for Jackson YAML and Felix Atomos (Import-Package); embeds only **Groovy** (scripting) where no feature provides it.
- **sokybot-pk2-extractor:** commons-io.

---

## 7. Karaf Features (Runtime Assembly)

Defined in `infra/sokybot-features` and used by `sokybot-dist`. **Third-party bundles live in dedicated features only;** sokybot-core references these features and lists only org.sokybot bundles.

- **sokybot-jackson:** Jackson bundles only (annotations, core, databind, dataformat-yaml, datatype-jsr310). No sokybot modules.
- **sokybot-platform:** Third-party platform bundles only (JNA, jna-platform, H2, Felix Atomos). No sokybot modules.
- **sokybot-reactor:** Reactor + Netty + reactive-streams (wrapped). No sokybot modules.
- **sokybot-rsocket:** RSocket (wrapped). Depends on sokybot-reactor. No sokybot modules.
- **sokybot-hibernate:** Hibernate/JPA stack (wrapped). No sokybot modules.
- **sokybot-core:** References features sokybot-jackson, sokybot-platform, sokybot-reactor, sokybot-rsocket, sokybot-hibernate; then only org.sokybot bundles (loader-api, engine-api, game-events-api, ui-api, commons, security, settings, persistence, pk2, pk2-extractor, game-loader, game-model, game-asset, game-navigation, proxy, game-events, engine, runtime, http-server, webview, machine-pages, packet-sniffer, dev-shell).
- **sokybot-dev:** sokybot-core + Karaf shell + SSH (optional dev-tools/bundle-manager commented out).
- **sokybot-full:** sokybot-core only (production).

**Note:** `sokybot-ui-api` is referenced in the root BOM and in feature `sokybot-core`; if that artifact is not built in the reactor, the feature or BOM may need to be updated or the module added.

---

## 8. Dependency Best Practices and Notes

1. **Version alignment:** Use root `dependencyManagement` and BOMs (Reactor, Jackson) to avoid version clashes; some modules still pin versions (e.g. reactor 3.5.11 vs 3.6.0) — consider aligning.
2. **Provided vs embed:** Keep “provided” for platform/framework and shared third-party bundles delivered by Karaf features; embed only when necessary for classloading (e.g. Groovy scripting).
3. **Circular risk:** game-model depends on game-events; persistence depends on pk2-extractor and pk2 — keep APIs (e.g. game-events-api, loader-api) thin to avoid cycles.
4. **sokybot-ui-api:** Referenced in root BOM and feature.xml; ensure the module exists and is built or remove/replace the reference.

---

## 9. Quick Reference: Which Module Depends on Which (Internal)

| Consumer | Depends on (internal) |
|----------|------------------------|
| sokybot-engine-api | proxy, settings, game-model |
| sokybot-engine | engine-api, proxy, security, commons, persistence, game-loader, game-model, game-asset |
| sokybot-runtime | commons, game-events-api, loader-api, proxy, engine-api, persistence, game-model, game-events, game-navigation |
| sokybot-proxy | security |
| sokybot-packet-sniffer | webview, runtime, proxy |
| sokybot-game-events-api | proxy, persistence |
| sokybot-game-events | game-events-api, commons, persistence, settings, proxy |
| sokybot-game-model | game-events, commons |
| sokybot-game-loader | loader-api, commons |
| sokybot-game-asset | pk2, persistence |
| sokybot-game-navigation | persistence, commons |
| sokybot-pk2-extractor | pk2, security |
| sokybot-pk2 | security |
| sokybot-webview | http-server, game-events-api, game-events, runtime, game-model, engine-api, persistence, settings, commons, engine, proxy |
| sokybot-machine-pages | webview, engine-api, runtime, proxy, game-model, settings, game-events |
| sokybot-dev-tools | http-server, persistence, runtime |
| sokybot-http-server | (none) |
| sokybot-commons | (none) |
| sokybot-persistence | pk2-extractor, pk2, security, commons |
| sokybot-settings | persistence |
| sokybot-dev-shell | runtime, engine-api, engine, machine-pages, commons, game-model, game-events, settings, proxy |

This document reflects the state of the POMs and feature.xml at the time of analysis. For exact versions and new modules, refer to the root and module `pom.xml` files and `infra/sokybot-features/src/main/feature/feature.xml`.
