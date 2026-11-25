# Pricewatch v0.0.2 Release Summary

## Release details

- Release date: 25 November 2026
- Git tag: `v0.0.2`
- Source commit: `806b268`
- Project version: `0.0.1-SNAPSHOT`
- Java: Temurin OpenJDK 21
- Spring Boot: 3.4.5
- Artifact: `pricewatch-v0.0.2.jar`
- Artifact size: 55,305,377 bytes
- SHA-256: `60CE4A1E1A4D0D37502662440D9602039E4561A7B3C9BBBD154166637BDF046D`

## Fixes

This release includes the fixes recorded in the closed issues:

- **#22:** Make reminder actions clear and usable when the pinned retailer has no current price.
- **#27:** Remove the obsolete reminder subsystem and complete price-rule behavior, including drop-by-amount rules and daily checks that continue until disabled.
- **#29:** Respect English display mode by showing only the localized product name.
- **#30:** Preserve rounded corners and layout geometry in high-contrast mode.
- **#31:** Enforce the completed Cart row lock on server-side add, edit, and remove requests.
- **#32:** Make repeated demo-data loads stable without duplicating observations or target alerts.
- **#33:** Preserve Discover search state when returning from Compare.
- **#34:** Include the synthetic demo-price label in downloaded offline checklists.

## Build and tests

Built from commit `806b268` using:

```powershell
.\mvnw.cmd clean package
```

The Maven build succeeded. All 347 tests passed, with 0 failures and 0 errors.

## Run the application

In PowerShell, with the current directory already set to the project's
`release` folder, run:

```powershell
java -jar .\pricewatch-v0.0.2.jar
```

By default, the application is available at `http://localhost:8080`.
The app's local database is stored relative to the folder you launch it from,
so this command uses `release\data`.

To configure the Ask API key in the current PowerShell session before starting:

```powershell
$env:DEEPSEEK_API_KEY_SKU = "your-api-key"
java -jar .\pricewatch-v0.0.2.jar
```

Do not commit the key or place it in `application.properties`.
