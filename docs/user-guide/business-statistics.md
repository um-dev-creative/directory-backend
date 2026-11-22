# Business statistics: views (user guide)

The owner of a business can see how many times its page was viewed.

## What is counted
- Each time someone opens the page of your business, **one view** is added to today's counter.
- **Nothing about the visitor is stored**: no name, account, IP address or device. Only a number per day.
- Your own visits are **not counted**.
- Days are UTC. A visitor who reloads the page is counted again, because visitors cannot be told apart without personal data.

## What you see (owner only)
`GET /api/v1/businesses/{businessId}/stats?days=30` (or the statistics panel of the app) returns:

| Figure | Meaning |
| --- | --- |
| Total | Views in the chosen window (1 to 365 days, default 30), ending today |
| Today | Views today |
| Last 7 days | Views in the last 7 days, whatever window you chose |
| Per day | One value per day of the window; days without views show 0 |

## What to expect
| Situation | Result |
| --- | --- |
| You are not the owner | 403 |
| No session | 401 |
| `days` is 0 or more than 365 | 400 |
| Business does not exist | 404 |
