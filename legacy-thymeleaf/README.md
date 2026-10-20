# Legacy Thymeleaf UI (archived)

This folder preserves an earlier server-rendered UI prototype that used
**Spring Boot + Thymeleaf** templates. It was the first UI demo for the
project, before the team adopted **Vite + React** for the new UI.

## Why it is here

- The new UI lives in [`../frontend/`](../frontend/) (Vite + React 19 +
  TypeScript + Tailwind v4) and is served by Spring Boot from
  `frontend/dist/`. See [`../src/main/java/com/pricewatch/web/WebConfig.java`](../src/main/java/com/pricewatch/web/WebConfig.java).
- The original Thymeleaf pages (`list.html`, `search.html`, …) and the
  matching `@Controller` would otherwise intercept the same paths the SPA
  needs (`/`, `/list`, `/search`, `/assistant`, `/alerts`, `/history`,
  `/more`), so they have been moved out of the build path.
- The files here are **git-tracked** and can be restored by copying them
  back into `src/main/`.

## Contents

| Path | What it was |
|------|-------------|
| `src/main/java/com/pricewatch/web/PageController.java` | Mapped `/`, `/list`, `/search`, `/assistant`, `/alerts`, `/history`, `/more` to Thymeleaf templates |
| `src/main/resources/templates/*.html` | The six page templates |
| `src/main/resources/templates/fragments/layout.html` | Shared header/footer fragment |
| `src/main/resources/static/css/app.css` | Stylesheet linked by the templates |
| `src/test/java/com/pricewatch/web/PageControllerTest.java` | MockMvc tests for the controller |

## Restoring (if the team ever wants to compare)

```powershell
# from the repo root
Copy-Item -Recurse legacy-thymeleaf\src\* src\
```

You would also need to:
1. Remove or comment out `WebConfig.java` (or scope its handler to
   `classpath:/static/` only).
2. Make sure `spring-boot-starter-thymeleaf` is still in `pom.xml` (it is).
3. Run `.\mvnw.cmd spring-boot:run` and visit the URLs above.
