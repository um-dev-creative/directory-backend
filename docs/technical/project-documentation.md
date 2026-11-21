# Project documentation layout (technical guide)

```mermaid
flowchart TD
    R[README.md] --> D[docs/DOCUMENTATION_INDEX.md]
    D --> P[docs/PROJECT_DOCUMENTATION.md]
    D --> T[docs/TECHNICAL_DOCUMENTATION.md]
    D --> A[docs/ARCHITECTURE_DIAGRAMS.md]
    D --> B[BA set: COMPLETE_..._FOR_BA, BA_QUICK_REFERENCE, BA_VISUAL_SUMMARY]
    D --> F[docs/user-guide/* and docs/technical/*: one pair per feature]
    G[.github/agents, skills, prompts, hooks, tools] --> W[AI-assistant workflows]
    I[issues/ and youtrack/] --> K[task backlog]
```

Conventions: guides are Markdown, diagrams are Mermaid, the CHANGELOG is in English, and no secret, key or token is ever committed
(`default.env`, `config/local.env`, keystores and `.env` files stay out of the repository).
The `.github` agent files are prompts for AI assistants; they do not affect the build or the runtime.
