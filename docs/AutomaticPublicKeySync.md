# Automatic Public Key Synchronization

## Background

Setting up a Fiks-IO account traditionally required the account's public key to be registered in the catalog manually by Fiks Forvaltning. This manual step can lead to coordination issues, especially when vendors need to rotate keys or set up accounts independently.
To streamline this, the Fiks-IO Java client can **automatically upload the configured public key to the catalog when the client is built**.

With automatic public key synchronization, the client uploads the configured public key to the Fiks-IO catalog when `FiksIOKlientFactory.build()` runs — eliminating the need for manual coordination. The municipality employee can set up the account independently and share the account details with the vendor afterward. The first time the vendor builds their client, the key is registered automatically.

---

## Configuration

Pass the public certificate (PEM-encoded X.509) alongside the private key(s) in `KontoKonfigurasjon`:

```java
// Single private key
KontoKonfigurasjon.builder()
    .kontoId(new KontoId(kontoId))
    .privatNokkel(privateKey)
    .publicKey(publicKeyPem)
    .build();

// Multiple private keys (key rotation)
KontoKonfigurasjon.builder()
    .kontoId(new KontoId(kontoId))
    .privatNokkel(oldPrivateKey)
    .privatNokkel(newPrivateKey)
    .publicKey(newPublicKeyPem)
    .build();
```

`publicKey` is optional. Omitting it disables automatic upload entirely — the client neither reads nor writes the catalog key at build time. Note that uploading a key requires the account to have API-based configuration enabled at Fiks Forvaltning; without this, `uploadPublicKey` fails.

---

## Is the feature enabled?

Automatic upload runs **if and only if `publicKey` is set (non-null) on `KontoKonfigurasjon`**. There is no dedicated boolean flag or startup log line indicating enabled/disabled state — you can only tell by inspecting the configuration you built (`kontoKonfigurasjon.getPublicKey() != null`). Unlike a design where validation always runs regardless of the flag, in this client **no catalog read or private-key validation happens at all when `publicKey` is omitted** — the client simply skips straight to building the AMQP connection.

If you want to verify that a running client's key matches what is currently registered in the catalog (regardless of whether upload is configured), call the public method on the built client:

```java
Boolean matches = fiksIOKlient.validerOffentligNokkelMotPrivateKey();
```

This fetches the current catalog key and checks it against the configured private key(s) on demand — useful for health checks or diagnostics, but it is **not** invoked automatically unless `publicKey` is configured (see below).

---

## How It Works

`FiksIOKlientFactory.build()` runs synchronously (there is no async/await in this client) and performs the following steps, in order:

1. Sets up the Maskinporten client, dokumentlager client, and `FiksIOUtsendingKlient`.
2. Builds `AsicHandler`, `KatalogHandler`, `FiksIOHandler`, and `KeyValidatorHandler`.
3. Constructs `FiksIOKlientImpl`, which internally creates an `AmqpHandler` — **this already opens the RabbitMQ connection**.
4. Only after the client (and its AMQP connection) has been assembled, `build()` calls `lastOppOffentligNokkelHvisOppdatert(...)`, which performs the key synchronization described below.
5. Returns the built `FiksIOKlient`.

> **Important difference from a "validate before connecting" design:** in this client, the AMQP connection is already open by the time the public key check runs. If the key check fails and `build()` throws, the already-open AMQP connection is **not** explicitly closed by the surrounding `catch` block (only the dokumentlager and utsending HTTP clients are). See [Known Limitations](#known-limitations).

`lastOppOffentligNokkelHvisOppdatert` (in `FiksIOKlientFactory`):

1. Reads `publicKey` from `KontoKonfigurasjon`. If it is `null`, returns immediately — no catalog read, no write.
2. Otherwise calls `KatalogHandler.getPublicKey(kontoId)` against the **unauthenticated** public catalog endpoint:
   - If the catalog has no key registered, the underlying HTTP call throws `FeignException.NotFound` — but `getPublicKey` catches this internally and simply **returns `null`**; the exception never reaches the caller.
   - If the catalog returns an invalid/unparseable certificate, `getPublicKey` throws a `RuntimeException`.
   - Any other HTTP failure (5xx, timeout, etc.) is **not** caught by `getPublicKey` and propagates out of it as an unchecked `FeignException`.
3. Compares the catalog result with the configured `publicKey`:
   - If the catalog key is `null` (nothing registered) → treated as **different**.
   - Otherwise, the Base64-encoded DER bytes of the catalog certificate are checked as a **substring** of the configured PEM (with newlines stripped). This is a raw text/byte comparison — not a `SubjectPublicKeyInfo`-based comparison.
4. If the keys are the same → nothing happens, `build()` proceeds.
5. If the keys differ → `KeyValidatorHandler.validerOffentligNokkelMotPrivateKey(publicKey)` is called: it parses the configured `publicKey` as an `X509Certificate`, encrypts 256 random bytes (CMS, via `CMSKrypteringImpl`) with it, and tries decrypting with each configured private key in turn.
   - If **any** configured private key decrypts successfully → the key is ours → `KatalogHandler.uploadPublicKey(kontoId, publicKey)` is called, which invokes `FiksIoKontoApi.settOffentligNokkel(...)`. `FiksIOKlientFactory` always constructs a non-null `FiksIoKontoApi` for this call (the `kontoApi == null` guard inside `KatalogHandler` is unreachable via `build()`); the account must still have API-based configuration enabled for the upload call itself to succeed. Any failure during the upload (network error, API rejection, etc.) is caught by `lastOppOffentligNokkel` and rethrown as `RuntimeException("Feil med opplasting av public key", e)`, with the original failure as the cause.
   - If **no** configured private key decrypts it → throws `RuntimeException("Offentlignøkkel kan ikke valideres opp mot konfigurerte private nøkler")`, and `build()` fails.

Two things to note that differ from a design with independent, always-on validation:

- **Nothing is validated when `publicKey` is not configured.** There is no fail-fast startup check against whatever key currently sits in the catalog unless you opt in by setting `publicKey`, or by explicitly calling `fiksIOKlient.validerOffentligNokkelMotPrivateKey()` yourself after the client is built.
- **A transient catalog failure during the check is fatal whenever `publicKey` is configured** — any exception other than "not found" propagates out of `build()` (caught by the outer `catch (Exception e)`, which closes the dokumentlager/utsending clients and rethrows). There is no "log a warning and continue" fallback path for feature-enabled clients.

---

## Flow Diagram

```mermaid
flowchart TD
    classDef error   fill:#b91c1c,stroke:#b91c1c,color:#fff
    classDef warn    fill:#b45309,stroke:#b45309,color:#fff
    classDef info    fill:#1d4ed8,stroke:#1d4ed8,color:#fff
    classDef success fill:#15803d,stroke:#15803d,color:#fff

    START(["FiksIOKlientFactory.build()"]) --> AMQP
    AMQP["Construct FiksIOKlientImpl\n(AmqpHandler connects to RabbitMQ)"]:::info --> A

    subgraph SYNC ["lastOppOffentligNokkelHvisOppdatert (only when publicKey is set)"]
        A{"KontoKonfigurasjon\n.getPublicKey() != null?"}
        A -->|no| SKIP["return immediately\n(no catalog read/write)"]:::info
        A -->|yes| C["KatalogHandler.getPublicKey(kontoId)"]

        C -->|404: NotFound caught internally, returns null| NULLKEY["catalog key = null"]:::info
        C -->|invalid certificate| ERR0(["RuntimeException:\ncertificate generation failed"]):::error
        C -->|other FeignException, not caught| ERR3(["propagates out of build():\ncatalog read failed"]):::error
        C -->|key returned| D

        NULLKEY --> DIFF["treated as different"]
        D{"Base64(DER) of catalog cert\nis substring of configured PEM?"}
        D -->|yes| SAME["same key, no action"]:::info
        D -->|no| DIFF

        DIFF --> V["KeyValidatorHandler\n.validerOffentligNokkelMotPrivateKey(publicKey)"]
        V -->|no private key decrypts| ERR1(["RuntimeException:\nkey does not match configured private keys"]):::error
        V -->|a private key decrypts| U["KatalogHandler.uploadPublicKey(kontoId, publicKey)\nvia FiksIoKontoApi.settOffentligNokkel"]

        U -->|upload succeeds| OK["key uploaded"]:::success
        U -->|upload fails\n(network error, API rejection, etc.)| ERR2(["RuntimeException:\nFeil med opplasting av public key\n(cause: original error)"]):::error
    end

    SKIP --> DONE
    SAME --> DONE
    OK   --> DONE
    DONE(["return FiksIOKlient ✓"]):::success
```

---

## Scenarios

### Scenario 1 — First-time setup: no key in catalog

**Precondition:** A new account has been created. No public key has been registered yet. `publicKey` is configured.

**Flow:**
1. The catalog returns 404; `KatalogHandler.getPublicKey` catches the underlying `FeignException.NotFound` internally and returns `null` — no exception reaches `lastOppOffentligNokkelHvisOppdatert`
2. Comparison treats this as different → validation runs
3. `KeyValidatorHandler.validerOffentligNokkelMotPrivateKey(publicKey)` confirms one of the configured private keys matches
4. `KatalogHandler.uploadPublicKey(kontoId, publicKey)` uploads the key via `FiksIoKontoApi.settOffentligNokkel`
5. `build()` returns the client

**Result:** Key is registered. Subsequent senders will encrypt messages using this key.

---

### Scenario 2 — Key already current

**Precondition:** The public key in the catalog is identical to the configured `publicKey`.

**Flow:**
1. `KatalogHandler.getPublicKey` returns the existing certificate
2. The Base64(DER) substring check matches
3. No upload; `build()` returns immediately

**Result:** Nothing happens on the catalog side.

---

### Scenario 3 — Key rotation: replacing our own old key

**Precondition:** The catalog has an existing key that one of the configured private keys can still decrypt. The vendor generated a new key pair and wants to rotate.

**Configuration:**
```java
KontoKonfigurasjon.builder()
    .kontoId(new KontoId(kontoId))
    .privatNokkel(oldPrivateKey)
    .privatNokkel(newPrivateKey)
    .publicKey(newPublicKeyPem)
    .build();
```

**Flow:**
1. Catalog returns the old certificate; comparison against `newPublicKeyPem` finds it different
2. `validerOffentligNokkelMotPrivateKey(newPublicKeyPem)` succeeds because `newPrivateKey` is in the configured list
3. `uploadPublicKey` replaces the catalog key with the new certificate

**Message decryption during rotation:**

Messages already on the queue were encrypted with the old public key. The AMQP consumer (via `AsicHandler`, configured with all `privatNokler`) tries each private key until one succeeds:

```
Old message arrives (encrypted with oldPubCert)
  → try newPrivateKey → fails
  → try oldPrivateKey → succeeds ✅

New message arrives (encrypted with newPubCert)
  → try newPrivateKey → succeeds ✅
```

**When is it safe to remove the old private key?** When you are confident the queue no longer contains messages encrypted with the old key. There is no built-in indicator for this — it is an operational decision.

---

### Scenario 4 — Catalog has an unrelated key

**Precondition:** The catalog contains a public key that does not match any of the configured private keys (e.g. the account was set up by someone else, or the wrong key was configured).

**Flow:**
1. Catalog returns a certificate that differs from the configured `publicKey`
2. `validerOffentligNokkelMotPrivateKey` finds no configured private key can decrypt data encrypted with the configured `publicKey`'s certificate
3. `build()` throws `RuntimeException("Offentlignøkkel kan ikke valideres opp mot konfigurerte private nøkler")`

**Result:** `build()` fails and no `FiksIOKlient` is returned. Because the AMQP connection was already opened as part of constructing `FiksIOKlientImpl` (step 3 above), this connection is **not** explicitly closed in the failure path — only the dokumentlager and utsending HTTP clients are cleaned up.

---

### Scenario 5 — Catalog unavailable during build()

**Precondition:** `publicKey` is configured and the Fiks-IO catalog API is temporarily unreachable (network issue, 5xx — **not** a 404).

**Flow:**
1. `KatalogHandler.getPublicKey` lets the underlying `FeignException` (not `FeignException.NotFound`) propagate — it is only caught for the 404 case
2. The exception propagates out of `lastOppOffentligNokkelHvisOppdatert` and out of `build()`
3. The outer `try`/`catch` in `build()` closes the dokumentlager and utsending clients, then rethrows

**Result:** `build()` throws. Retry on the next attempt. As noted above, there is no "log and continue" fallback here — a transient catalog outage is fatal whenever `publicKey` is configured.

---

### Scenario 6 — Feature not configured

**Precondition:** `KontoKonfigurasjon` is built without `.publicKey(...)`.

```java
KontoKonfigurasjon.builder()
    .kontoId(new KontoId(kontoId))
    .privatNokkel(privateKey)
    .build();
```

**Flow:**
1. `getPublicKey()` on the configuration is `null`
2. `lastOppOffentligNokkelHvisOppdatert` returns immediately — no catalog read, no write, no validation
3. `build()` proceeds straight to returning the client

**Result:** No key upload or validation is performed at build time. If the catalog's registered key does not match any configured private key, this will only surface later — either when an incoming message cannot be decrypted, or if you explicitly call `fiksIOKlient.validerOffentligNokkelMotPrivateKey()` yourself.

---

## Key Rotation Step-by-Step Guide

1. **Generate a new key pair** (e.g. with OpenSSL or a PKI tool). Private keys must be in PKCS#8 format (convert PKCS#1 keys with `openssl pkcs8 -topk8 -nocrypt -in <pkcs1> -out <pkcs8>`).
2. **Update configuration** — add the new private key alongside the old one, and set the new public certificate as `publicKey`:
   ```java
   KontoKonfigurasjon.builder()
       .kontoId(new KontoId(kontoId))
       .privatNokkel(oldPrivateKey)
       .privatNokkel(newPrivateKey)
       .publicKey(newPublicKeyPem)
       .build();
   ```
3. **Deploy and rebuild the client** — `FiksIOKlientFactory.build()` uploads the new public key to the catalog.
4. **Wait** until you are confident the queue is drained of messages encrypted with the old key.
5. **Remove the old private key** from the configuration and redeploy:
   ```java
   KontoKonfigurasjon.builder()
       .kontoId(new KontoId(kontoId))
       .privatNokkel(newPrivateKey)
       .publicKey(newPublicKeyPem)
       .build();
   ```

---

## Known Limitations

- **No expiry awareness:** Certificate validity dates are not checked. An expired certificate in the catalog will not be replaced automatically unless the configured `publicKey` differs from it.
- **No rotation drain indicator:** There is no built-in signal for when it is safe to remove an old private key. This is left to the operator.
- **Sync is build()-time only, and opt-in:** Synchronization runs once, synchronously, inside `FiksIOKlientFactory.build()`, and **only** when `publicKey` is configured. If the catalog key changes afterward, nothing detects it until the client is rebuilt, and nothing detects it at all if `publicKey` was never configured — unless you call `validerOffentligNokkelMotPrivateKey()` yourself.
- **Comparison is a raw string/byte check, not `SubjectPublicKeyInfo`-based:** The client checks whether the Base64(DER) of the catalog certificate appears as a substring of the configured PEM text, rather than parsing and comparing public key material structurally.
- **AMQP connection may be left open on failure:** The RabbitMQ connection is established when `FiksIOKlientImpl`/`AmqpHandler` is constructed, which happens *before* the key check runs. If the key check subsequently throws, `build()`'s `catch` block closes the dokumentlager and utsending HTTP clients but does not close this AMQP connection.
- **Requires API-based account configuration to upload:** `uploadPublicKey` calls the authenticated `FiksIoKontoApi`, which `FiksIOKlientFactory` always provides when building via `build()`. If the account does not have API-based configuration enabled at Fiks Forvaltning, the upload call itself fails, and `lastOppOffentligNokkel` rewraps that failure as `RuntimeException("Feil med opplasting av public key", e)`.
- **No dedicated exception types:** All failures in this flow surface as plain `RuntimeException` (or the underlying `FeignException`/`CertificateException`), not a dedicated exception type for "key not found" vs. "misconfigured" vs. "catalog unavailable".
- **Concurrent builds during rotation:** If several client instances are built simultaneously with different `publicKey` values during a rollout, they can repeatedly overwrite each other's catalog key until the rollout converges. Roll out a key change to all instances together.
