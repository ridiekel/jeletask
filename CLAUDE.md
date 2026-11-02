# CLAUDE.md

Conventions for anyone (human or agent) working on this repository.

## Language

- **Commit messages: English.** Use the conventional prefix style already in the history,
  e.g. `fix(client): ...`, `test(mqtt): ...`, `refactor(native): ...`, with a body that explains *why*.
- Code, code comments, README and log messages: English.

## Code quality (SonarQube)

- The project is analysed by SonarQube on every CI pipeline; aim for 0 open issues.
- Fix real findings. Only suppress a false positive, and then as narrowly as possible
  (`@SuppressWarnings("java:Sxxxx")` on the field or method) with an English comment explaining why.
  Avoid `// NOSONAR`: it silences every rule on that line, not just the intended one.

## Testing

- `./mvnw test` runs the suite. The `End2End*Test` classes and the Docker-based resilience tests need a
  running Docker daemon; without it they fail to start their containers, everything else must stay green.
