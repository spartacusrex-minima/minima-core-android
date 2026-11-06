# Fix: H2 2.4.240 crashes the Minima node at startup on Android (VerifyError)

**One-line fix:** downgrade the H2 dependency in `app/build.gradle` from
`com.h2database:h2:2.4.240` to `com.h2database:h2:2.1.214`.

## Symptom

On a fresh install, creating or restoring a wallet gets stuck on **"Syncing Minima
Wallet.."** forever. Reproduced on a Samsung Galaxy S10+ across **Android 9, 10, 11 and
12**, on the **official build** — so it is independent of Android version, network, and seed.

The wallet UI is fine; it is waiting on a node that has already died.

## Root cause

`app/build.gradle` pulls **`com.h2database:h2:2.4.240`**. Android's ART bytecode verifier
(strict on some OEM/ART builds, e.g. Samsung) **rejects** H2 2.4.240's
`org.h2.security.SHA256.getPBKDF2`, so the class fails to load. The node opens its H2
database *with a password*, which calls `ConnectionInfo.hashPassword → SHA256.getPBKDF2`,
triggering the `VerifyError` and killing the node's main thread at boot:

```
FATAL EXCEPTION: Thread-5
Process: org.minimarex.minimacore
java.lang.VerifyError: Verifier rejected class org.h2.security.SHA256:
  byte[] org.h2.security.SHA256.getPBKDF2(byte[], byte[], int, int) failed to verify:
  [0x2C] register v4 has type Precise Reference: byte[]
         but expected Reference: java.lang.Object[]
  (declaration of 'org.h2.security.SHA256' appears in base.apk!classes14.dex)
    at org.h2.security.SHA256.getKeyPasswordHash(SHA256.java:57)
    at org.h2.engine.ConnectionInfo.hashPassword(ConnectionInfo.java:405)
    at org.h2.engine.ConnectionInfo.convertPasswords(ConnectionInfo.java:394)
    at org.h2.engine.ConnectionInfo.<init>(ConnectionInfo.java:106)
    at org.h2.jdbc.JdbcConnection.<init>(JdbcConnection.java:115)
    at org.h2.Driver.connect(Driver.java:59)
    at java.sql.DriverManager.getConnection(...)
    at org.minima.utils.SqlDB.loadDB(SqlDB.java:73)
    at org.minima.database.MinimaDB.loadAllDB(MinimaDB.java:295)
    at org.minima.system.Main.<init>(Main.java:302)
    at org.minima.Minima.main(Minima.java:182)
    at org.minima.Minima$1.run(Minima.java:40)
```

`minima.jar` does **not** bundle H2 (0 `org/h2/*` classes), so the offending class comes
solely from this gradle dependency — which is why swapping the version is a complete fix.

## Fix

```diff
 //H2 database
-implementation 'com.h2database:h2:2.4.240'
+implementation 'com.h2database:h2:2.1.214'
```

H2 **2.1.214** is the same H2 2.x line/API used by `org.minima.utils.SqlDB`, and its
`SHA256` bytecode verifies cleanly on ART.

## Verification (Galaxy S10+, Android 12)

**Before** — node dies on boot, wallet hangs on "Syncing…":
```
Welcome to Pure Minima 1.1.2.4 …
FATAL EXCEPTION: Thread-5   java.lang.VerifyError … SqlDB.loadDB
Minima-Core: Now wait for Minima to say..   (waits forever)
```

**After** (`h2:2.1.214`) — node boots, connects to peers, downloads the chain:
```
Welcome to Pure Minima 1.1.2.4 …
Daemon mode started..
Connected attempt success to 168.138.15.32:9001 … 38.172.51.111:9001 …
[+] Connected to the blockchain — Initial Block Download received. size:15.6 MB blocks:2079
```

## Notes

- Scope is a single dependency line; no source changes.
- If a newer H2 is desired later, verify it loads on strict ARTs (Samsung/older devices)
  before shipping — 2.1.214 is a known-good baseline.
- The restore screen's default MegaMMR host `spartacusrex.com:9001` is currently
  unreachable; the node's other default peers work, so sync still completes. Worth
  refreshing that default separately.
