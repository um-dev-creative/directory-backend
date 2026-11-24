# How Directory talks to mercury (user guide)

Directory signs in to **mercury** (MER-11) with an alias and a password that belong to Directory itself. mercury answers a token that only
serves verification codes and e-mails. People using Directory see no change, except that a busy or unavailable mercury now shows a clear
message (see below).

## Set-up for operators (once per environment)

1. In mercury, register Directory as a login client (`umdc.mercury.login-clients`) with an alias and a password.
2. Give Directory the same pair as `DIRECTORY_BACKEND_LOGIN_ALIAS` and `DIRECTORY_BACKEND_LOGIN_PASSWORD` (Vault or environment). Never write them in a file of the repository.
3. The old Backbone managed-client settings (`MERCURY_M2M_CLIENT_ID`, `MERCURY_M2M_CLIENT_SECRET`, `MERCURY_M2M_SCOPES`) are no longer read.

## What to expect

| Situation | Result |
| --- | --- |
| Everything configured | Confirming a verification code works as before |
| Alias or password missing | 503 with the message "Mercury is not available" |
| mercury refuses the alias or password | 503, and Directory does not try again for one minute (so mercury's own limit is not hit) |
| mercury asks to slow down (429) | 429 with `Retry-After`; Directory sends nothing to mercury until that time has passed |
| mercury is slow | The call fails after 3 s (connect) or 5 s (read) with 503 |
