## Security Checks
* `./gradlew dependencyUpdates` — список более новых версий.
* `./gradlew dependencyCheckAnalyze` — скан CVE (OWASP DC). При обнаружении High/CRITICAL сборка завершается.

## Observability
Start the monitoring stack and access metrics in Grafana.

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

Services expose Prometheus metrics at `/metrics` and logs in JSON. Grafana is available on `localhost:3000`.
