# Monitoring with Prometheus and Grafana

This project includes Prometheus and Grafana for monitoring and visualizing metrics from the Spring Boot applications.

## Overview

- **Prometheus** - Time-series database that scrapes metrics from the applications
- **Grafana** - Visualization and dashboards for metrics
- **Micrometer** - Metrics instrumentation library integrated into Spring Boot

## Quick Start

### 1. Start the Monitoring Stack

```bash
make monitoring-up
```

This starts both Prometheus and Grafana in Docker containers.

### 2. Start Your Applications

Make sure your Spring Boot applications are running:

```bash
make infra-up      # Start infrastructure (databases and LDAP)
make run-auth-ldap # Terminal 1: Start auth service (LDAP variant)
# OR
make run-auth-db   # Terminal 1: Start auth service (Database variant)
make run-mvc       # Terminal 2: Start MVC API
make run-webflux   # Terminal 3: Start WebFlux API
```

### 3. Access the UIs

**Prometheus:**

```bash
make prometheus-ui
```

Or visit: <http://localhost:9090>

**Grafana:**

```bash
make grafana-ui
```

Or visit: <http://localhost:3000> (login: admin/admin)

## Viewing Metrics

### Prometheus Queries

In the Prometheus UI (<http://localhost:9090>), try these queries:

#### JVM Metrics

```promql
# JVM memory usage
jvm_memory_used_bytes{application="acme-api-mvc"}
jvm_memory_used_bytes{application="acme-api-webflux"}

# Garbage collection time
rate(jvm_gc_pause_seconds_sum[1m])

# Thread count
jvm_threads_live_threads
```

#### HTTP Metrics

```promql
# Request count
http_server_requests_seconds_count{application="acme-api-mvc"}
http_server_requests_seconds_count{application="acme-api-webflux"}

# Request rate (requests per second)
rate(http_server_requests_seconds_count[1m])

# Average response time
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])

# P95 response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))
```

#### Cache Metrics

```promql
# Cache hit rate
rate(cache_gets_total{result="hit"}[1m]) / rate(cache_gets_total[1m])

# Cache size
cache_size{cache="users"}

# Cache evictions
rate(cache_evictions_total[1m])
```

#### Database Connection Pool Metrics

**MVC (HikariCP):**

```promql
# Active connections
hikaricp_connections_active{application="acme-api-mvc"}

# Idle connections
hikaricp_connections_idle{application="acme-api-mvc"}

# Connection acquisition time
hikaricp_connections_acquire_seconds
```

**WebFlux (R2DBC):**

```promql
# Acquired connections
r2dbc_pool_acquired_connections{application="acme-api-webflux"}

# Idle connections
r2dbc_pool_idle_connections{application="acme-api-webflux"}
```

#### Comparing MVC vs WebFlux

```promql
# Compare request rates
rate(http_server_requests_seconds_count{uri="/api/books"}[1m])

# Compare memory usage
jvm_memory_used_bytes{area="heap"}

# Compare response times
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))
```

### Grafana Dashboards

1. Log in to Grafana (<http://localhost:3000>) with `admin/admin`
2. Click **+** → **Import dashboard**
3. Import one of these popular dashboards:

#### Recommended Dashboards

##### JVM (Micrometer) - ID: 4701

- JVM memory, CPU, threads
- Garbage collection metrics
- System metrics

##### Spring Boot 2.1 Statistics - ID: 11378

- HTTP metrics
- Tomcat/Netty metrics
- Cache statistics
- Database connection pools

##### Spring Boot Statistics - ID: 6756

- Alternative Spring Boot dashboard
- Application health
- Custom application metrics

#### Importing a Dashboard

1. Enter the dashboard ID (e.g., `4701`)
2. Click **Load**
3. Select **Prometheus** as the data source
4. Click **Import**

### Creating Custom Dashboards

1. In Grafana, click **+** → **Dashboard** → **Add visualization**
2. Select **Prometheus** as the data source
3. Enter a PromQL query (see examples above)
4. Configure visualization type (Graph, Gauge, Table, etc.)
5. Click **Save**

## Metrics Endpoints

Each application exposes metrics at:

- **MVC API:** <http://localhost:8080/actuator/prometheus>
- **WebFlux API:** <http://localhost:8081/actuator/prometheus>
- **Auth Service:** <http://localhost:8082/actuator/prometheus>

These endpoints are publicly accessible (no authentication required) and return metrics in Prometheus text format.

## Configuration

### Prometheus Configuration

Location: `monitoring/prometheus.yml`

Scrape interval: 15 seconds

Targets:

- `acme-api-mvc` - <http://host.docker.internal:8080/actuator/prometheus>
- `acme-api-webflux` - <http://host.docker.internal:8081/actuator/prometheus>
- `acme-auth-service` - <http://host.docker.internal:8082/actuator/prometheus>

### Grafana Configuration

Location: `monitoring/grafana/provisioning/datasources/prometheus.yml`

Prometheus datasource is automatically provisioned and configured.

### Application Configuration

Actuator endpoints are configured in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

Cache statistics are enabled with:

```yaml
spring:
  cache:
    caffeine:
      spec: >
        expireAfterWrite=5m,
        maximumSize=1000,
        recordStats
```

## Management Commands

```bash
# Start monitoring stack
make monitoring-up

# Stop monitoring stack
make monitoring-down

# View logs
make monitoring-logs

# Open Prometheus UI
make prometheus-ui

# Open Grafana UI
make grafana-ui
```

## Troubleshooting

### Prometheus shows targets as "DOWN"

Check if your applications are running:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
```

If these endpoints return metrics, restart Prometheus:

```bash
docker restart acme-prometheus
```

### Grafana shows "No data"

1. Verify Prometheus is receiving metrics:
   - Go to <http://localhost:9090/targets>
   - All targets should show "UP" status
2. Check that you selected the correct data source in Grafana
3. Adjust the time range in Grafana (top-right corner)

### Cache metrics not showing

Ensure `recordStats` is enabled in the Caffeine cache configuration (see above).

## Performance Testing

To generate meaningful metrics, run some load tests against your APIs:

```bash
# Simple load test with curl
for i in {1..100}; do
  curl -H "x-dn: CN=test" http://localhost:8080/api/books &
done

# Or use Apache Bench
ab -n 1000 -c 10 -H "x-dn: CN=test" http://localhost:8080/api/books

# Or use wrk
wrk -t4 -c100 -d30s -H "x-dn: CN=test" http://localhost:8080/api/books
```

Then compare MVC vs WebFlux performance in Grafana dashboards.

## Data Retention

Prometheus data is stored in a Docker volume and persists across container restarts.

To reset monitoring data:

```bash
docker compose down
docker volume rm spring-mvc-vs-webflux_prometheus-data
docker volume rm spring-mvc-vs-webflux_grafana-data
make monitoring-up
```
