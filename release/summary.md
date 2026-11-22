# Pricewatch Release Summary

## Release details

- Release date: 22 November 2026
- Project version: `0.0.1-SNAPSHOT`
- Commit: `be8c622`
- Java: Temurin OpenJDK 21.0.12.1
- Spring Boot: 3.4.5
- Artifact: `release/pricewatch-0.0.1-SNAPSHOT.jar`
- Artifact size: 55,302,511 bytes

## Artifact checksum

SHA-256:

```text
BDA6DC3D48512235C89F4C0D6CFB6FC4E17F95253DBD710AE33169BDF7FD0FA2
```

## Build and test result

The release was built with:

```powershell
.\mvnw.cmd clean package
```

The Maven build completed successfully. The test suite passed with 164 tests, 0 failures, and 0 errors.

## Smoke test

The packaged JAR was started on port 18080 with:

```powershell
java "-Dserver.port=18080" -jar .\release\pricewatch-0.0.1-SNAPSHOT.jar
```

Startup completed successfully, the Tomcat server started on port 18080, and `http://127.0.0.1:18080/` returned HTTP 200. The smoke-test server was then stopped.

## Run the release

From the project root:

```powershell
java -jar .\release\pricewatch-0.0.1-SNAPSHOT.jar
```

By default, the application is available at:

```text
http://localhost:8080
```

## Configure the Ask API key

Set `DEEPSEEK_API_KEY_SKU` in the current PowerShell session before starting the JAR:

```powershell
$env:DEEPSEEK_API_KEY_SKU = "your-api-key"
java -jar .\release\pricewatch-0.0.1-SNAPSHOT.jar
```

To save the key for future terminal sessions on the current Windows user:

```powershell
setx DEEPSEEK_API_KEY_SKU "your-api-key"
```

Close and reopen PowerShell or VS Code after using `setx`, then start the JAR normally:

```powershell
java -jar .\release\pricewatch-0.0.1-SNAPSHOT.jar
```

Do not commit the key or place it in `application.properties`. The key remains server-side and is not stored in the JAR.
