# OpenSearch Migration Utility

A REST API utility to migrate indices, documents, visualizations, dashboards, and saved objects between two OpenSearch clusters.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/migration` | Execute migration |
| `POST` | `/api/v1/migration/dry-run` | Preview what will be migrated |
| `GET` | `/api/v1/migration/{id}` | Check migration status |
| `GET` | `/api/v1/migration/latest` | Get latest migration status |

---

## Request Format

### Minimal Request (Required Fields Only)

```json
{
  "source": {
    "url": "http://localhost:9200"
  },
  "target": {
    "url": "http://localhost:9201"
  }
}
```

### Full Request (All Options)

```json
{
  "source": {
    "url": "http://localhost:9200",
    "username": "admin",
    "password": "admin",
    "connectionTimeoutMs": 5000,
    "socketTimeoutMs": 120000
  },
  "target": {
    "url": "http://localhost:9201",
    "username": "admin",
    "password": "admin",
    "connectionTimeoutMs": 5000,
    "socketTimeoutMs": 120000
  },
  "dryRun": false,
  "migrateSavedObjects": true,
  "includeIndices": null,
  "excludeIndices": null,
  "options": {
    "batchSize": 1000,
    "scrollTimeout": "5m",
    "scrollSize": 1000,
    "maxConcurrentIndices": 3,
    "continueOnFailure": true,
    "savedObjectsIndex": ".opensearch_dashboards",
    "savedObjectsTypes": ["visualization", "dashboard", "search", "index-pattern"]
  }
}
```

### Request Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `source` | object | Yes | - | Source cluster configuration |
| `source.url` | string | Yes | - | Source OpenSearch URL |
| `source.username` | string | No | `admin` | Source cluster username |
| `source.password` | string | No | `admin` | Source cluster password |
| `source.connectionTimeoutMs` | int | No | `5000` | Connection timeout in milliseconds |
| `source.socketTimeoutMs` | int | No | `120000` | Socket timeout in milliseconds |
| `target` | object | Yes | - | Target cluster configuration |
| `target.url` | string | Yes | - | Target OpenSearch URL |
| `target.username` | string | No | `admin` | Target cluster username |
| `target.password` | string | No | `admin` | Target cluster password |
| `target.connectionTimeoutMs` | int | No | `5000` | Connection timeout in milliseconds |
| `target.socketTimeoutMs` | int | No | `120000` | Socket timeout in milliseconds |
| `dryRun` | boolean | No | `false` | Preview mode - no actual migration |
| `migrateSavedObjects` | boolean | No | `true` | Migrate dashboards, visualizations, etc. |
| `includeIndices` | array | No | `null` | Only migrate these indices (null = all) |
| `excludeIndices` | array | No | `null` | Exclude these indices from migration |
| `options` | object | No | - | Advanced migration options |
| `options.batchSize` | int | No | `1000` | Documents per bulk request |
| `options.scrollTimeout` | string | No | `5m` | Scroll context timeout |
| `options.scrollSize` | int | No | `1000` | Documents per scroll page |
| `options.maxConcurrentIndices` | int | No | `3` | Parallel index migrations |
| `options.continueOnFailure` | boolean | No | `true` | Continue if one index fails |
| `options.savedObjectsIndex` | string | No | `.opensearch_dashboards` | Dashboards index name |
| `options.savedObjectsTypes` | array | No | `["visualization", "dashboard", "search", "index-pattern"]` | Saved object types to migrate |

---

## cURL Examples

### Dry Run (Preview)

```bash
curl -X POST http://localhost:8080/api/v1/migration/dry-run \
  -H "Content-Type: application/json" \
  -d '{
    "source": { "url": "http://localhost:9200" },
    "target": { "url": "http://localhost:9201" }
  }'
```

### Execute Migration

```bash
curl -X POST http://localhost:8080/api/v1/migration \
  -H "Content-Type: application/json" \
  -d '{
    "source": { "url": "http://localhost:9200", "username": "admin", "password": "admin" },
    "target": { "url": "http://localhost:9201", "username": "admin", "password": "admin" }
  }'
```

### Migrate Specific Indices Only

```bash
curl -X POST http://localhost:8080/api/v1/migration \
  -H "Content-Type: application/json" \
  -d '{
    "source": { "url": "http://localhost:9200" },
    "target": { "url": "http://localhost:9201" },
    "includeIndices": ["products", "customers", "orders"]
  }'
```

### Exclude Certain Indices

```bash
curl -X POST http://localhost:8080/api/v1/migration \
  -H "Content-Type: application/json" \
  -d '{
    "source": { "url": "http://localhost:9200" },
    "target": { "url": "http://localhost:9201" },
    "excludeIndices": ["logs-2024", "temp-index"]
  }'
```

### Skip Saved Objects Migration

```bash
curl -X POST http://localhost:8080/api/v1/migration \
  -H "Content-Type: application/json" \
  -d '{
    "source": { "url": "http://localhost:9200" },
    "target": { "url": "http://localhost:9201" },
    "migrateSavedObjects": false
  }'
```

### Check Migration Status

```bash
curl http://localhost:8080/api/v1/migration/latest
```

### Check Specific Migration

```bash
curl http://localhost:8080/api/v1/migration/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

## Response Format

### Migration Response

```json
{
  "migrationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "COMPLETED",
  "startedAt": "2026-05-06T10:30:00",
  "completedAt": "2026-05-06T10:35:42",
  "totalIndices": 5,
  "completedIndices": 5,
  "failedIndices": 0,
  "totalDocuments": 150000,
  "migratedDocuments": 150000,
  "indexResults": [
    {
      "indexName": "products",
      "success": true,
      "documentCount": 50000,
      "migratedCount": 50000,
      "failedCount": 0,
      "durationMs": 12340,
      "errorMessage": null
    },
    {
      "indexName": "customers",
      "success": true,
      "documentCount": 100000,
      "migratedCount": 100000,
      "failedCount": 0,
      "durationMs": 24560,
      "errorMessage": null
    }
  ],
  "errorMessage": null,
  "dryRun": false
}
```

### Migration Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Migration created but not started |
| `RUNNING` | Migration in progress |
| `COMPLETED` | All indices migrated successfully |
| `COMPLETED_WITH_ERRORS` | Migration finished but some indices failed |
| `FAILED` | Migration failed critically |

---

## What Gets Migrated

### User Indices
- All user-created indices (excluding system indices starting with `.`)
- Index settings (shards, replicas, refresh interval, analyzers)
- Index mappings
- All documents

### Saved Objects (from `.opensearch_dashboards` index)
- Visualizations
- Dashboards
- Saved searches (Discover queries)
- Index patterns

---

## Notes

- System indices (starting with `.`) are automatically excluded except for the dashboards index
- If an index already exists in the target cluster, document migration will proceed but index creation is skipped
- Use `dryRun: true` to preview what will be migrated without making changes
- The migration uses the Scroll API for efficient handling of large indices
- Failed documents are logged but migration continues if `continueOnFailure` is enabled
