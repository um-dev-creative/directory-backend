# How Directory talks to mercury (user guide)

Directory proves itself to **mercury** with the Backbone M2M token of its own managed client: it never sends an alias and a password to
mercury. mercury trades that token for its own token, which only serves verification codes and e-mails. People using Directory see no change,
except that a busy or unavailable mercury now shows a clear message (see below).

## Set-up for operators (once per environment)

1. In Backbone, create Directory's managed client, link it to the Directory application and give it the scopes `mercury:message:read` and
   `mercury:message:write`.
2. Give Directory its credentials as `MERCURY_M2M_CLIENT_ID` (a UUID) and `MERCURY_M2M_CLIENT_SECRET` (Vault or environment; never in a file of
   the repository). `MERCURY_M2M_SCOPES` is optional (default: the two scopes above).
3. Nothing about Directory needs to be registered by alias in mercury: `DIRECTORY_BACKEND_LOGIN_ALIAS` and `DIRECTORY_BACKEND_LOGIN_PASSWORD`
   are not read (they never worked against mercury).

## What to expect

| Situation | Result |
| --- | --- |
| Everything configured | Confirming a verification code works |
| Managed client id or secret missing | 503 with the message "Mercury is not available" |
| Backbone or mercury refuses the client | 503, and Directory does not try again for one minute |
| mercury asks to slow down (429) | 429 with `Retry-After`; Directory sends nothing to mercury until that time has passed |
| mercury is slow | The call fails after 3 s (connect) or 5 s (read) with 503 |
