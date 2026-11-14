# How Directory talks to mercury (user guide)

Directory sends the verification e-mail checks to **mercury** as an *application*, with the credentials of its own Backbone **managed client**.
It no longer has an alias and a password in mercury. People using Directory see no change.

## Set-up for operators (once per environment)

1. In Backbone, create Directory's managed client, tied to the Directory application, with the scopes `mercury:message:read` and `mercury:message:write` (one client per application).
2. Put its id and secret in Vault as `MERCURY_M2M_CLIENT_ID` and `MERCURY_M2M_CLIENT_SECRET`. `MERCURY_M2M_SCOPES` is optional (default: both scopes).
3. Deploy backbone-rest, `security-oauth` and mercury (with the Backbone-token exchange) **before** this version of Directory.

## What to expect

| Situation | Result |
| --- | --- |
| Everything configured | Confirming a verification code works as before |
| `MERCURY_M2M_CLIENT_ID`/`_SECRET` missing | Confirming a code fails with `Directory's Backbone managed client is not configured` (HTTP 500) |
| Backbone refuses the client (wrong secret, scope not granted) | The call fails with the status Backbone returned |
| The client has no application in Backbone, or lacks the scopes | mercury answers 403 |
| mercury has not been upgraded yet | mercury answers 401/403 to the exchange |

Remove from Vault what is no longer used for mercury's login: nothing else in Directory sends an alias or password to mercury.
