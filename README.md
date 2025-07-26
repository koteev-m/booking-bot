## Security Checks
* `./gradlew dependencyUpdates` — список более новых версий.
* `./gradlew dependencyCheckAnalyze` — скан CVE (OWASP DC). При обнаружении High/CRITICAL сборка завершается.

## Observability
Start the monitoring stack and access metrics in Grafana.

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

Services expose Prometheus metrics at `/metrics` and logs in JSON. Grafana is available on `localhost:3000`.

## Secrets

1. Copy `.env.example` → `.env` and set real credentials.
2. For production:
   docker secret create tg_token <(echo "$TELEGRAM_BOT_TOKEN")
   docker compose --env-file .env up -d
