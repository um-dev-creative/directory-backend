# Startup banner — user guide

**For whom:** anyone who starts `directory-backend` and reads its console or logs.

## What you see

When the application starts, it prints the same banner as `backbone-rest`: the "UM DC Directory" lettering in yellow, blue and red, then two lines:

```
Directory Backend Service::0.0.1
Powered by Spring Boot <version> | Spring Cloud <version> | UMDC libraries <version>
```

The second line tells you, at a glance, which platform versions the running jar was built with. Use it when you report a problem or check that a deployment is the one you expect.

## Notes

- The versions come from the build (`pom.xml`), so they always match the jar; nobody edits them by hand.
- If a version cannot be read, the banner shows `n/a` instead of failing; the application starts normally.
- To hide the banner, start with `--spring.main.banner-mode=off`.
