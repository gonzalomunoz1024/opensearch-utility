# OpenSearch Utility - API Reference

## Table of Contents
1. [Index Management](#1-index-management)
2. [Document Operations](#2-document-operations)
3. [Populate Index (Batch Processing)](#3-populate-index-batch-processing)
4. [Cluster Administration](#4-cluster-administration)
5. [Node Management](#5-node-management)
6. [Snapshot & Restore](#6-snapshot--restore)

---

## 1. Index Management

### 1.1 Create Index

**POST** `/api/v1/indices`

Creates a new index with optional settings and mappings.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices \
  -H "Content-Type: application/json" \
  -d '{
    "indexName": "products",
    "settings": {
      "numberOfShards": 3,
      "numberOfReplicas": 1,
      "refreshInterval": "1s",
      "maxResultWindow": 10000
    },
    "mappings": {
      "properties": {
        "id": {
          "type": "keyword"
        },
        "name": {
          "type": "text",
          "analyzer": "standard"
        },
        "description": {
          "type": "text"
        },
        "price": {
          "type": "float"
        },
        "category": {
          "type": "keyword"
        },
        "createdAt": {
          "type": "date",
          "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
        },
        "tags": {
          "type": "keyword"
        },
        "inStock": {
          "type": "boolean"
        }
      }
    }
  }'
```

**Response (201 Created):**
```json
{
  "name": "products",
  "acknowledged": true,
  "shardsAcknowledged": true,
  "settings": {
    "numberOfShards": 3,
    "numberOfReplicas": 1,
    "refreshInterval": "1s"
  },
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Response (409 Conflict):**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Index [products] already exists",
  "path": "/api/v1/indices"
}
```

---

### 1.2 List All Indices

**GET** `/api/v1/indices`

Returns a list of all indices with their metadata.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/indices
```

**Response (200 OK):**
```json
[
  {
    "name": "products",
    "uuid": "abc123xyz",
    "health": "green",
    "status": "open",
    "primaryShards": 3,
    "replicaShards": 1,
    "documentCount": 15420,
    "storeSizeBytes": 52428800,
    "storeSize": "50mb",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "name": "orders",
    "uuid": "def456uvw",
    "health": "yellow",
    "status": "open",
    "primaryShards": 5,
    "replicaShards": 1,
    "documentCount": 89234,
    "storeSizeBytes": 209715200,
    "storeSize": "200mb",
    "createdAt": "2024-01-10T08:15:00Z"
  },
  {
    "name": "customers",
    "uuid": "ghi789rst",
    "health": "green",
    "status": "open",
    "primaryShards": 2,
    "replicaShards": 2,
    "documentCount": 5670,
    "storeSizeBytes": 10485760,
    "storeSize": "10mb",
    "createdAt": "2024-01-05T14:20:00Z"
  }
]
```

---

### 1.3 Get Index Details

**GET** `/api/v1/indices/{indexName}`

Returns detailed information about a specific index.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/indices/products
```

**Response (200 OK):**
```json
{
  "name": "products",
  "uuid": "abc123xyz",
  "health": "green",
  "status": "open",
  "primaryShards": 3,
  "replicaShards": 1,
  "documentCount": 15420,
  "deletedDocuments": 120,
  "storeSizeBytes": 52428800,
  "storeSize": "50mb",
  "settings": {
    "numberOfShards": 3,
    "numberOfReplicas": 1,
    "refreshInterval": "1s",
    "maxResultWindow": 10000,
    "analysis": {
      "analyzer": {
        "default": {
          "type": "standard"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "name": { "type": "text" },
      "price": { "type": "float" },
      "category": { "type": "keyword" },
      "createdAt": { "type": "date" }
    }
  },
  "aliases": ["products-alias", "active-products"],
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Index [products] not found",
  "path": "/api/v1/indices/products"
}
```

---

### 1.4 Delete Index

**DELETE** `/api/v1/indices/{indexName}`

Permanently deletes an index and all its data.

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/v1/indices/products
```

**Response (204 No Content):**
```
(empty body)
```

**Error Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Index [products] not found",
  "path": "/api/v1/indices/products"
}
```

---

### 1.5 Open Index

**POST** `/api/v1/indices/{indexName}/_open`

Opens a previously closed index, making it available for read/write operations.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_open
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "shardsAcknowledged": true,
  "index": "products",
  "status": "open"
}
```

---

### 1.6 Close Index

**POST** `/api/v1/indices/{indexName}/_close`

Closes an index, blocking read/write operations while reducing resource usage.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_close
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "shardsAcknowledged": true,
  "index": "products",
  "status": "closed"
}
```

---

### 1.7 Refresh Index

**POST** `/api/v1/indices/{indexName}/_refresh`

Forces a refresh, making all recently indexed documents available for search.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_refresh
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "index": "products",
  "shards": {
    "total": 6,
    "successful": 6,
    "failed": 0
  }
}
```

---

### 1.8 Reindex

**POST** `/api/v1/indices/_reindex`

Copies documents from a source index to a destination index.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/_reindex \
  -H "Content-Type: application/json" \
  -d '{
    "source": {
      "index": "products-v1",
      "query": {
        "match": {
          "status": "active"
        }
      },
      "size": 1000
    },
    "destination": {
      "index": "products-v2",
      "pipeline": "product-enrichment"
    },
    "script": {
      "source": "ctx._source.migrated = true; ctx._source.migratedAt = params.now",
      "params": {
        "now": "2024-01-15T10:30:00Z"
      }
    },
    "conflicts": "proceed",
    "refresh": true
  }'
```

**Response (200 OK):**
```json
{
  "took": 15234,
  "timedOut": false,
  "total": 15420,
  "updated": 0,
  "created": 15420,
  "deleted": 0,
  "batches": 16,
  "versionConflicts": 0,
  "noops": 0,
  "retries": {
    "bulk": 0,
    "search": 0
  },
  "failures": []
}
```

---

### 1.9 Clone Index

**POST** `/api/v1/indices/{indexName}/_clone/{targetIndex}`

Creates a copy of an existing index. Source index must be read-only.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_clone/products-backup \
  -H "Content-Type: application/json" \
  -d '{
    "settings": {
      "index.number_of_replicas": 2
    }
  }'
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "shardsAcknowledged": true,
  "sourceIndex": "products",
  "targetIndex": "products-backup"
}
```

---

### 1.10 Update Index Settings

**PUT** `/api/v1/indices/{indexName}/_settings`

Updates dynamic settings for an index.

**Request:**
```bash
curl -X PUT http://localhost:8080/api/v1/indices/products/_settings \
  -H "Content-Type: application/json" \
  -d '{
    "numberOfReplicas": 2,
    "refreshInterval": "5s",
    "maxResultWindow": 50000
  }'
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "index": "products",
  "updatedSettings": {
    "numberOfReplicas": 2,
    "refreshInterval": "5s",
    "maxResultWindow": 50000
  }
}
```

---

### 1.11 Update Index Mappings

**PUT** `/api/v1/indices/{indexName}/_mapping`

Adds new fields to an existing index mapping. Existing field mappings cannot be changed.

**Request:**
```bash
curl -X PUT http://localhost:8080/api/v1/indices/products/_mapping \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {
      "brand": {
        "type": "keyword"
      },
      "rating": {
        "type": "float"
      },
      "reviewCount": {
        "type": "integer"
      },
      "specifications": {
        "type": "nested",
        "properties": {
          "name": { "type": "keyword" },
          "value": { "type": "text" }
        }
      }
    }
  }'
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "index": "products"
}
```

---

## 2. Document Operations

### 2.1 Index Single Document

**POST** `/api/v1/indices/{indexName}/_doc`

Indexes a single document. Auto-generates document ID if not provided.

**Request (with auto-generated ID):**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_doc \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Bluetooth Headphones",
    "description": "Premium noise-cancelling headphones with 30-hour battery life",
    "price": 299.99,
    "category": "electronics",
    "brand": "AudioMax",
    "tags": ["wireless", "bluetooth", "noise-cancelling", "premium"],
    "inStock": true,
    "specifications": {
      "batteryLife": "30 hours",
      "connectivity": "Bluetooth 5.0",
      "weight": "250g"
    },
    "createdAt": "2024-01-15T10:30:00Z"
  }'
```

**Response (201 Created):**
```json
{
  "index": "products",
  "id": "xK7bQ40BvTkp9QRZ1234",
  "version": 1,
  "result": "created",
  "shards": {
    "total": 2,
    "successful": 2,
    "failed": 0
  },
  "seqNo": 156,
  "primaryTerm": 1
}
```

---

### 2.2 Index Document with ID

**PUT** `/api/v1/indices/{indexName}/_doc/{docId}`

Indexes or updates a document with a specified ID.

**Request:**
```bash
curl -X PUT http://localhost:8080/api/v1/indices/products/_doc/prod-12345 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Bluetooth Headphones",
    "description": "Premium noise-cancelling headphones with 30-hour battery life",
    "price": 279.99,
    "category": "electronics",
    "brand": "AudioMax",
    "tags": ["wireless", "bluetooth", "noise-cancelling"],
    "inStock": true,
    "updatedAt": "2024-01-15T14:45:00Z"
  }'
```

**Response (200 OK - Updated):**
```json
{
  "index": "products",
  "id": "prod-12345",
  "version": 2,
  "result": "updated",
  "shards": {
    "total": 2,
    "successful": 2,
    "failed": 0
  },
  "seqNo": 157,
  "primaryTerm": 1
}
```

---

### 2.3 Get Document

**GET** `/api/v1/indices/{indexName}/_doc/{docId}`

Retrieves a document by its ID.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/indices/products/_doc/prod-12345
```

**Response (200 OK):**
```json
{
  "index": "products",
  "id": "prod-12345",
  "version": 2,
  "seqNo": 157,
  "primaryTerm": 1,
  "found": true,
  "source": {
    "name": "Wireless Bluetooth Headphones",
    "description": "Premium noise-cancelling headphones with 30-hour battery life",
    "price": 279.99,
    "category": "electronics",
    "brand": "AudioMax",
    "tags": ["wireless", "bluetooth", "noise-cancelling"],
    "inStock": true,
    "updatedAt": "2024-01-15T14:45:00Z"
  }
}
```

**Error Response (404 Not Found):**
```json
{
  "index": "products",
  "id": "prod-99999",
  "found": false
}
```

---

### 2.4 Update Document (Partial)

**POST** `/api/v1/indices/{indexName}/_doc/{docId}/_update`

Partially updates a document by merging the provided fields.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_doc/prod-12345/_update \
  -H "Content-Type: application/json" \
  -d '{
    "doc": {
      "price": 249.99,
      "onSale": true,
      "saleEnds": "2024-02-01T00:00:00Z"
    },
    "docAsUpsert": false
  }'
```

**Response (200 OK):**
```json
{
  "index": "products",
  "id": "prod-12345",
  "version": 3,
  "result": "updated",
  "shards": {
    "total": 2,
    "successful": 2,
    "failed": 0
  },
  "seqNo": 158,
  "primaryTerm": 1
}
```

---

### 2.5 Delete Document

**DELETE** `/api/v1/indices/{indexName}/_doc/{docId}`

Deletes a document by its ID.

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/v1/indices/products/_doc/prod-12345
```

**Response (200 OK):**
```json
{
  "index": "products",
  "id": "prod-12345",
  "version": 4,
  "result": "deleted",
  "shards": {
    "total": 2,
    "successful": 2,
    "failed": 0
  },
  "seqNo": 159,
  "primaryTerm": 1
}
```

---

### 2.6 Bulk Operations

**POST** `/api/v1/indices/{indexName}/_bulk`

Performs multiple index, update, or delete operations in a single request.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_bulk \
  -H "Content-Type: application/json" \
  -d '{
    "documents": [
      {
        "id": "prod-001",
        "name": "Laptop Stand",
        "price": 49.99,
        "category": "accessories"
      },
      {
        "id": "prod-002",
        "name": "USB-C Hub",
        "price": 79.99,
        "category": "accessories"
      },
      {
        "id": "prod-003",
        "name": "Mechanical Keyboard",
        "price": 149.99,
        "category": "peripherals"
      },
      {
        "id": "prod-004",
        "name": "Wireless Mouse",
        "price": 59.99,
        "category": "peripherals"
      },
      {
        "id": "prod-005",
        "name": "Monitor Light Bar",
        "price": 89.99,
        "category": "accessories"
      }
    ]
  }'
```

**Response (200 OK):**
```json
{
  "took": 45,
  "errors": false,
  "totalDocuments": 5,
  "successCount": 5,
  "failureCount": 0,
  "items": [
    {
      "index": {
        "index": "products",
        "id": "prod-001",
        "version": 1,
        "result": "created",
        "status": 201,
        "seqNo": 160,
        "primaryTerm": 1
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-002",
        "version": 1,
        "result": "created",
        "status": 201,
        "seqNo": 161,
        "primaryTerm": 1
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-003",
        "version": 1,
        "result": "created",
        "status": 201,
        "seqNo": 162,
        "primaryTerm": 1
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-004",
        "version": 1,
        "result": "created",
        "status": 201,
        "seqNo": 163,
        "primaryTerm": 1
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-005",
        "version": 1,
        "result": "created",
        "status": 201,
        "seqNo": 164,
        "primaryTerm": 1
      }
    }
  ]
}
```

**Response with Partial Failures (200 OK):**
```json
{
  "took": 52,
  "errors": true,
  "totalDocuments": 5,
  "successCount": 3,
  "failureCount": 2,
  "failedDocumentIds": ["prod-002", "prod-004"],
  "items": [
    {
      "index": {
        "index": "products",
        "id": "prod-001",
        "version": 1,
        "result": "created",
        "status": 201
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-002",
        "status": 400,
        "error": {
          "type": "mapper_parsing_exception",
          "reason": "failed to parse field [price] of type [float]"
        }
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-003",
        "version": 1,
        "result": "created",
        "status": 201
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-004",
        "status": 400,
        "error": {
          "type": "mapper_parsing_exception",
          "reason": "failed to parse field [price] of type [float]"
        }
      }
    },
    {
      "index": {
        "index": "products",
        "id": "prod-005",
        "version": 1,
        "result": "created",
        "status": 201
      }
    }
  ]
}
```

---

### 2.7 Multi-Get Documents

**POST** `/api/v1/indices/{indexName}/_mget`

Retrieves multiple documents by their IDs in a single request.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_mget \
  -H "Content-Type: application/json" \
  -d '{
    "ids": ["prod-001", "prod-002", "prod-003", "prod-999"]
  }'
```

**Response (200 OK):**
```json
{
  "docs": [
    {
      "index": "products",
      "id": "prod-001",
      "version": 1,
      "found": true,
      "source": {
        "name": "Laptop Stand",
        "price": 49.99,
        "category": "accessories"
      }
    },
    {
      "index": "products",
      "id": "prod-002",
      "version": 1,
      "found": true,
      "source": {
        "name": "USB-C Hub",
        "price": 79.99,
        "category": "accessories"
      }
    },
    {
      "index": "products",
      "id": "prod-003",
      "version": 1,
      "found": true,
      "source": {
        "name": "Mechanical Keyboard",
        "price": 149.99,
        "category": "peripherals"
      }
    },
    {
      "index": "products",
      "id": "prod-999",
      "found": false
    }
  ]
}
```

---

### 2.8 Delete By Query

**POST** `/api/v1/indices/{indexName}/_delete_by_query`

Deletes all documents matching a query.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_delete_by_query \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "bool": {
        "must": [
          { "term": { "category": "discontinued" } },
          { "range": { "lastUpdated": { "lt": "2023-01-01" } } }
        ]
      }
    },
    "conflicts": "proceed",
    "refresh": true
  }'
```

**Response (200 OK):**
```json
{
  "took": 1523,
  "timedOut": false,
  "total": 342,
  "deleted": 342,
  "batches": 1,
  "versionConflicts": 0,
  "noops": 0,
  "retries": {
    "bulk": 0,
    "search": 0
  },
  "failures": []
}
```

---

### 2.9 Update By Query

**POST** `/api/v1/indices/{indexName}/_update_by_query`

Updates all documents matching a query.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/products/_update_by_query \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "term": { "category": "electronics" }
    },
    "script": {
      "source": "ctx._source.taxRate = params.rate; ctx._source.priceWithTax = ctx._source.price * (1 + params.rate)",
      "params": {
        "rate": 0.08
      }
    },
    "conflicts": "proceed",
    "refresh": true
  }'
```

**Response (200 OK):**
```json
{
  "took": 2341,
  "timedOut": false,
  "total": 1523,
  "updated": 1523,
  "deleted": 0,
  "batches": 2,
  "versionConflicts": 0,
  "noops": 0,
  "retries": {
    "bulk": 0,
    "search": 0
  },
  "failures": []
}
```

---

## 3. Populate Index (Batch Processing)

### 3.1 Start Populate Job

**POST** `/api/v1/indices/{indexName}/populate`

Starts an asynchronous job to populate an index from an external data source. Documents are batched (20 items OR 5 seconds threshold) and processed with automatic retry (3 attempts) and DLQ handling.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/indices/customers/populate \
  -H "Content-Type: application/json" \
  -d '{
    "sourceEndpoint": "https://api.example.com/customers/export",
    "headers": {
      "Authorization": "Bearer eyJhbGciOiJIUzI1NiIs...",
      "X-API-Version": "2.0"
    },
    "batchSize": 20,
    "timeoutSeconds": 5,
    "retryCount": 3,
    "transformScript": "ctx._source.fullName = ctx._source.firstName + \" \" + ctx._source.lastName",
    "idField": "customerId"
  }'
```

**Response (202 Accepted):**
```json
{
  "jobId": "pop-job-abc123",
  "targetIndex": "customers",
  "sourceEndpoint": "https://api.example.com/customers/export",
  "status": "RUNNING",
  "batchConfiguration": {
    "batchSize": 20,
    "timeoutSeconds": 5,
    "retryCount": 3
  },
  "startedAt": "2024-01-15T10:30:00Z",
  "message": "Populate job started successfully"
}
```

---

### 3.2 Get Populate Job Status

**GET** `/api/v1/indices/{indexName}/populate/status`

Returns the current status of the populate job for an index.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/indices/customers/populate/status
```

**Response (200 OK - In Progress):**
```json
{
  "jobId": "pop-job-abc123",
  "targetIndex": "customers",
  "status": "RUNNING",
  "progress": {
    "totalBatches": 150,
    "completedBatches": 87,
    "percentComplete": 58,
    "documentsProcessed": 1740,
    "documentsSucceeded": 1725,
    "documentsFailed": 15,
    "documentsInDLQ": 5,
    "currentRetryAttempts": {
      "batch-88": 2,
      "batch-89": 1
    }
  },
  "timing": {
    "startedAt": "2024-01-15T10:30:00Z",
    "elapsedSeconds": 145,
    "estimatedRemainingSeconds": 105,
    "averageBatchTimeMs": 967
  },
  "lastBatchStatus": {
    "batchNumber": 87,
    "documentsInBatch": 20,
    "succeeded": 18,
    "failed": 2,
    "retriesRemaining": 1,
    "completedAt": "2024-01-15T10:32:25Z"
  }
}
```

**Response (200 OK - Completed):**
```json
{
  "jobId": "pop-job-abc123",
  "targetIndex": "customers",
  "status": "COMPLETED",
  "progress": {
    "totalBatches": 150,
    "completedBatches": 150,
    "percentComplete": 100,
    "documentsProcessed": 3000,
    "documentsSucceeded": 2985,
    "documentsFailed": 15,
    "documentsInDLQ": 8
  },
  "timing": {
    "startedAt": "2024-01-15T10:30:00Z",
    "completedAt": "2024-01-15T10:34:12Z",
    "totalDurationSeconds": 252,
    "averageBatchTimeMs": 1680
  },
  "summary": {
    "successRate": 99.5,
    "dlqEntries": 8,
    "totalRetries": 23
  }
}
```

**Response (200 OK - Failed):**
```json
{
  "jobId": "pop-job-abc123",
  "targetIndex": "customers",
  "status": "FAILED",
  "error": {
    "type": "SOURCE_UNAVAILABLE",
    "message": "Failed to connect to source endpoint after 3 attempts",
    "details": "Connection timed out: api.example.com:443",
    "occurredAt": "2024-01-15T10:30:45Z"
  },
  "progress": {
    "completedBatches": 12,
    "documentsProcessed": 240
  }
}
```

---

### 3.3 Cancel Populate Job

**DELETE** `/api/v1/indices/{indexName}/populate`

Cancels a running populate job.

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/v1/indices/customers/populate
```

**Response (200 OK):**
```json
{
  "jobId": "pop-job-abc123",
  "targetIndex": "customers",
  "status": "CANCELLED",
  "cancelledAt": "2024-01-15T10:35:00Z",
  "progress": {
    "completedBatches": 87,
    "documentsProcessed": 1740
  },
  "message": "Populate job cancelled. 1740 documents were successfully indexed before cancellation."
}
```

---

### 3.4 Get DLQ Entries

**GET** `/api/v1/populate/dlq`

Returns all entries in the Dead Letter Queue with filtering options.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/populate/dlq?targetIndex=customers&limit=10"
```

**Response (200 OK):**
```json
{
  "totalEntries": 8,
  "entries": [
    {
      "dlqId": "dlq-entry-001",
      "eventId": "evt-batch-88-doc-3",
      "targetIndex": "customers",
      "documentId": "cust-5678",
      "document": {
        "customerId": "cust-5678",
        "firstName": "John",
        "lastName": "Doe",
        "email": "invalid-email-format"
      },
      "failureReason": "mapper_parsing_exception: failed to parse field [email] of type [email]",
      "originalBatchId": "batch-88",
      "retryAttempts": 3,
      "firstFailedAt": "2024-01-15T10:32:10Z",
      "lastFailedAt": "2024-01-15T10:32:25Z"
    },
    {
      "dlqId": "dlq-entry-002",
      "eventId": "evt-batch-88-doc-7",
      "targetIndex": "customers",
      "documentId": "cust-9012",
      "document": {
        "customerId": "cust-9012",
        "firstName": "Jane",
        "lastName": "Smith",
        "age": "not-a-number"
      },
      "failureReason": "mapper_parsing_exception: failed to parse field [age] of type [integer]",
      "originalBatchId": "batch-88",
      "retryAttempts": 3,
      "firstFailedAt": "2024-01-15T10:32:10Z",
      "lastFailedAt": "2024-01-15T10:32:25Z"
    }
  ],
  "pagination": {
    "limit": 10,
    "offset": 0,
    "hasMore": false
  }
}
```

---

### 3.5 Retry DLQ Entries

**POST** `/api/v1/populate/dlq/retry`

Retries failed documents from the DLQ.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/populate/dlq/retry \
  -H "Content-Type: application/json" \
  -d '{
    "dlqIds": ["dlq-entry-001", "dlq-entry-002"],
    "targetIndex": "customers",
    "retryCount": 3,
    "transformScript": "ctx._source.email = ctx._source.email.toLowerCase().trim()"
  }'
```

**Response (202 Accepted):**
```json
{
  "retryJobId": "dlq-retry-xyz789",
  "entriesQueued": 2,
  "targetIndex": "customers",
  "status": "PROCESSING",
  "startedAt": "2024-01-15T11:00:00Z"
}
```

---

## 4. Cluster Administration

### 4.1 Get Cluster Health

**GET** `/api/v1/cluster/health`

Returns the health status of the cluster.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/cluster/health
```

**Response (200 OK):**
```json
{
  "clusterName": "opensearch-production",
  "status": "green",
  "timedOut": false,
  "numberOfNodes": 5,
  "numberOfDataNodes": 3,
  "activePrimaryShards": 45,
  "activeShards": 90,
  "relocatingShards": 0,
  "initializingShards": 0,
  "unassignedShards": 0,
  "delayedUnassignedShards": 0,
  "numberOfPendingTasks": 0,
  "numberOfInFlightFetch": 0,
  "taskMaxWaitingInQueueMillis": 0,
  "activeShardsPercentAsNumber": 100.0,
  "clusterHealthy": true
}
```

**Response (200 OK - Degraded):**
```json
{
  "clusterName": "opensearch-production",
  "status": "yellow",
  "timedOut": false,
  "numberOfNodes": 5,
  "numberOfDataNodes": 3,
  "activePrimaryShards": 45,
  "activeShards": 75,
  "relocatingShards": 2,
  "initializingShards": 3,
  "unassignedShards": 15,
  "delayedUnassignedShards": 5,
  "numberOfPendingTasks": 3,
  "numberOfInFlightFetch": 2,
  "taskMaxWaitingInQueueMillis": 1523,
  "activeShardsPercentAsNumber": 83.3,
  "clusterHealthy": false,
  "issues": [
    "15 unassigned replica shards",
    "Node opensearch-data-2 recently restarted"
  ]
}
```

---

### 4.2 Get Index Health

**GET** `/api/v1/cluster/health/{indexName}`

Returns health status for a specific index.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/cluster/health/products
```

**Response (200 OK):**
```json
{
  "clusterName": "opensearch-production",
  "status": "green",
  "index": "products",
  "numberOfShards": 3,
  "numberOfReplicas": 1,
  "activePrimaryShards": 3,
  "activeShards": 6,
  "relocatingShards": 0,
  "initializingShards": 0,
  "unassignedShards": 0,
  "shardDetails": [
    {
      "shard": 0,
      "primary": true,
      "state": "STARTED",
      "node": "opensearch-data-1",
      "documentsCount": 5140
    },
    {
      "shard": 0,
      "primary": false,
      "state": "STARTED",
      "node": "opensearch-data-2",
      "documentsCount": 5140
    },
    {
      "shard": 1,
      "primary": true,
      "state": "STARTED",
      "node": "opensearch-data-2",
      "documentsCount": 5130
    },
    {
      "shard": 1,
      "primary": false,
      "state": "STARTED",
      "node": "opensearch-data-3",
      "documentsCount": 5130
    },
    {
      "shard": 2,
      "primary": true,
      "state": "STARTED",
      "node": "opensearch-data-3",
      "documentsCount": 5150
    },
    {
      "shard": 2,
      "primary": false,
      "state": "STARTED",
      "node": "opensearch-data-1",
      "documentsCount": 5150
    }
  ]
}
```

---

### 4.3 Get Cluster Stats

**GET** `/api/v1/cluster/stats`

Returns comprehensive statistics about the cluster.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/cluster/stats
```

**Response (200 OK):**
```json
{
  "clusterName": "opensearch-production",
  "clusterUuid": "abc123-def456-ghi789",
  "timestamp": "2024-01-15T10:30:00Z",
  "status": "green",
  "indices": {
    "count": 25,
    "shards": {
      "total": 90,
      "primaries": 45,
      "replication": 1.0,
      "index": {
        "shards": {
          "min": 1,
          "max": 5,
          "avg": 1.8
        },
        "primaries": {
          "min": 1,
          "max": 5,
          "avg": 1.8
        },
        "replication": {
          "min": 0,
          "max": 2,
          "avg": 1.0
        }
      }
    },
    "docs": {
      "count": 15234567,
      "deleted": 12345
    },
    "store": {
      "sizeInBytes": 53687091200,
      "size": "50gb"
    },
    "fieldData": {
      "memorySizeInBytes": 104857600,
      "memorySize": "100mb",
      "evictions": 12
    },
    "queryCache": {
      "memorySizeInBytes": 52428800,
      "memorySize": "50mb",
      "totalCount": 1523456,
      "hitCount": 1234567,
      "missCount": 288889,
      "cacheSize": 15234,
      "cacheCount": 20345,
      "evictions": 5111
    }
  },
  "nodes": {
    "count": {
      "total": 5,
      "clusterManager": 3,
      "coordinatingOnly": 0,
      "data": 3,
      "ingest": 3,
      "remoteClusterClient": 2
    },
    "versions": ["2.11.0"],
    "os": {
      "availableProcessors": 32,
      "allocatedProcessors": 24,
      "mem": {
        "totalInBytes": 137438953472,
        "total": "128gb",
        "freeInBytes": 34359738368,
        "free": "32gb",
        "usedInBytes": 103079215104,
        "used": "96gb",
        "freePercent": 25,
        "usedPercent": 75
      }
    },
    "jvm": {
      "maxUptimeInMillis": 8640000000,
      "maxUptime": "100d",
      "mem": {
        "heapUsedInBytes": 21474836480,
        "heapUsed": "20gb",
        "heapMaxInBytes": 34359738368,
        "heapMax": "32gb"
      },
      "threads": 523
    },
    "fs": {
      "totalInBytes": 1099511627776,
      "total": "1tb",
      "freeInBytes": 549755813888,
      "free": "512gb",
      "availableInBytes": 494780232499,
      "available": "460gb"
    }
  }
}
```

---

### 4.4 Get Cluster Settings

**GET** `/api/v1/cluster/settings`

Returns current cluster settings.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/cluster/settings
```

**Response (200 OK):**
```json
{
  "persistent": {
    "cluster": {
      "routing": {
        "allocation": {
          "enable": "all",
          "nodeInitialPrimariesRecoveries": "4",
          "nodeConcurrentRecoveries": "2"
        }
      }
    },
    "indices": {
      "recovery": {
        "maxBytesPerSec": "100mb"
      }
    }
  },
  "transient": {
    "cluster": {
      "routing": {
        "allocation": {
          "exclude": {
            "_name": "opensearch-data-2"
          }
        }
      }
    }
  },
  "defaults": {
    "cluster": {
      "maxShardsPerNode": "1000",
      "routing": {
        "allocation": {
          "awareness": {
            "attributes": ""
          }
        }
      }
    }
  }
}
```

---

### 4.5 Update Cluster Settings

**PUT** `/api/v1/cluster/settings`

Updates cluster-wide settings.

**Request:**
```bash
curl -X PUT http://localhost:8080/api/v1/cluster/settings \
  -H "Content-Type: application/json" \
  -d '{
    "persistent": {
      "cluster.routing.allocation.enable": "all",
      "indices.recovery.max_bytes_per_sec": "150mb"
    },
    "transient": {
      "cluster.routing.allocation.exclude._name": ""
    }
  }'
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "persistent": {
    "cluster": {
      "routing": {
        "allocation": {
          "enable": "all"
        }
      }
    },
    "indices": {
      "recovery": {
        "maxBytesPerSec": "150mb"
      }
    }
  },
  "transient": {
    "cluster": {
      "routing": {
        "allocation": {
          "exclude": {
            "_name": ""
          }
        }
      }
    }
  }
}
```

---

### 4.6 Explain Shard Allocation

**GET** `/api/v1/cluster/allocation/explain`

Explains why a shard is unassigned or on a particular node.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/cluster/allocation/explain \
  -H "Content-Type: application/json" \
  -d '{
    "index": "products",
    "shard": 0,
    "primary": false
  }'
```

**Response (200 OK):**
```json
{
  "index": "products",
  "shard": 0,
  "primary": false,
  "currentState": "unassigned",
  "unassignedInfo": {
    "reason": "REPLICA_ADDED",
    "at": "2024-01-15T10:30:00Z",
    "lastAllocationStatus": "no_attempt"
  },
  "canAllocate": "no",
  "allocateExplanation": "cannot allocate because allocation is not permitted to any of the nodes",
  "nodeAllocationDecisions": [
    {
      "nodeId": "node-1",
      "nodeName": "opensearch-data-1",
      "nodeDecision": "no",
      "deciders": [
        {
          "decider": "same_shard",
          "decision": "NO",
          "explanation": "a copy of this shard is already allocated to this node"
        }
      ]
    },
    {
      "nodeId": "node-2",
      "nodeName": "opensearch-data-2",
      "nodeDecision": "no",
      "deciders": [
        {
          "decider": "filter",
          "decision": "NO",
          "explanation": "node is excluded by cluster allocation settings"
        }
      ]
    },
    {
      "nodeId": "node-3",
      "nodeName": "opensearch-data-3",
      "nodeDecision": "yes",
      "weight": 2.5
    }
  ]
}
```

---

## 5. Node Management

### 5.1 List All Nodes

**GET** `/api/v1/nodes`

Returns information about all nodes in the cluster.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/nodes
```

**Response (200 OK):**
```json
{
  "clusterName": "opensearch-production",
  "nodes": [
    {
      "id": "node-abc123",
      "name": "opensearch-master-1",
      "transportAddress": "10.0.1.10:9300",
      "host": "10.0.1.10",
      "ip": "10.0.1.10",
      "version": "2.11.0",
      "roles": ["cluster_manager", "ingest"],
      "attributes": {
        "zone": "us-east-1a"
      },
      "os": {
        "name": "Linux",
        "arch": "amd64",
        "version": "5.15.0",
        "availableProcessors": 8,
        "allocatedProcessors": 8
      },
      "jvm": {
        "version": "17.0.9",
        "vmName": "OpenJDK 64-Bit Server VM",
        "vmVersion": "17.0.9+9",
        "memoryPools": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"],
        "gcCollectors": ["G1 Young Generation", "G1 Old Generation"]
      }
    },
    {
      "id": "node-def456",
      "name": "opensearch-data-1",
      "transportAddress": "10.0.1.20:9300",
      "host": "10.0.1.20",
      "ip": "10.0.1.20",
      "version": "2.11.0",
      "roles": ["data", "ingest"],
      "attributes": {
        "zone": "us-east-1a",
        "storage": "hot"
      },
      "os": {
        "name": "Linux",
        "arch": "amd64",
        "version": "5.15.0",
        "availableProcessors": 16,
        "allocatedProcessors": 16
      }
    },
    {
      "id": "node-ghi789",
      "name": "opensearch-data-2",
      "transportAddress": "10.0.1.21:9300",
      "host": "10.0.1.21",
      "ip": "10.0.1.21",
      "version": "2.11.0",
      "roles": ["data", "ingest"],
      "attributes": {
        "zone": "us-east-1b",
        "storage": "hot"
      }
    }
  ]
}
```

---

### 5.2 Get Node Stats

**GET** `/api/v1/nodes/{nodeId}/stats`

Returns detailed statistics for a specific node.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/nodes/node-def456/stats
```

**Response (200 OK):**
```json
{
  "clusterName": "opensearch-production",
  "nodeId": "node-def456",
  "nodeName": "opensearch-data-1",
  "timestamp": "2024-01-15T10:30:00Z",
  "indices": {
    "docs": {
      "count": 5234567,
      "deleted": 4321
    },
    "store": {
      "sizeInBytes": 17895697408,
      "size": "16.7gb"
    },
    "indexing": {
      "indexTotal": 1523456,
      "indexTimeInMillis": 523456,
      "indexCurrent": 5,
      "indexFailed": 123,
      "deleteTotal": 45678,
      "deleteTimeInMillis": 12345,
      "deleteCurrent": 0
    },
    "get": {
      "total": 2345678,
      "timeInMillis": 345678,
      "existsTotal": 2300000,
      "existsTimeInMillis": 340000,
      "missingTotal": 45678,
      "missingTimeInMillis": 5678,
      "current": 2
    },
    "search": {
      "openContexts": 5,
      "queryTotal": 8765432,
      "queryTimeInMillis": 1234567,
      "queryCurrent": 10,
      "fetchTotal": 4567890,
      "fetchTimeInMillis": 456789,
      "fetchCurrent": 3,
      "scrollTotal": 12345,
      "scrollTimeInMillis": 23456,
      "scrollCurrent": 1
    },
    "merges": {
      "current": 2,
      "currentDocs": 50000,
      "currentSizeInBytes": 104857600,
      "total": 456,
      "totalTimeInMillis": 567890,
      "totalDocs": 12345678,
      "totalSizeInBytes": 26843545600
    },
    "refresh": {
      "total": 12345,
      "totalTimeInMillis": 234567,
      "listeners": 0
    },
    "flush": {
      "total": 234,
      "totalTimeInMillis": 45678
    },
    "queryCache": {
      "memorySizeInBytes": 17825792,
      "memorySize": "17mb",
      "totalCount": 567890,
      "hitCount": 456789,
      "missCount": 111101,
      "cacheSize": 5678,
      "cacheCount": 6789,
      "evictions": 1111
    },
    "fieldData": {
      "memorySizeInBytes": 35651584,
      "memorySize": "34mb",
      "evictions": 5
    }
  },
  "os": {
    "timestamp": 1705315800000,
    "cpu": {
      "percent": 45,
      "loadAverage": {
        "1m": 2.5,
        "5m": 2.3,
        "15m": 2.1
      }
    },
    "mem": {
      "totalInBytes": 68719476736,
      "total": "64gb",
      "freeInBytes": 17179869184,
      "free": "16gb",
      "usedInBytes": 51539607552,
      "used": "48gb",
      "freePercent": 25,
      "usedPercent": 75
    },
    "swap": {
      "totalInBytes": 0,
      "freeInBytes": 0,
      "usedInBytes": 0
    }
  },
  "jvm": {
    "timestamp": 1705315800000,
    "uptime": "10d 5h 30m",
    "uptimeInMillis": 897000000,
    "mem": {
      "heapUsedInBytes": 8589934592,
      "heapUsed": "8gb",
      "heapUsedPercent": 50,
      "heapCommittedInBytes": 17179869184,
      "heapCommitted": "16gb",
      "heapMaxInBytes": 17179869184,
      "heapMax": "16gb",
      "nonHeapUsedInBytes": 209715200,
      "nonHeapUsed": "200mb",
      "nonHeapCommittedInBytes": 220200960,
      "nonHeapCommitted": "210mb"
    },
    "threads": {
      "count": 156,
      "peakCount": 178
    },
    "gc": {
      "collectors": {
        "young": {
          "collectionCount": 1234,
          "collectionTimeInMillis": 23456
        },
        "old": {
          "collectionCount": 12,
          "collectionTimeInMillis": 3456
        }
      }
    }
  },
  "fs": {
    "timestamp": 1705315800000,
    "total": {
      "totalInBytes": 214748364800,
      "total": "200gb",
      "freeInBytes": 107374182400,
      "free": "100gb",
      "availableInBytes": 96636764160,
      "available": "90gb"
    },
    "data": [
      {
        "path": "/var/lib/opensearch",
        "mount": "/dev/sda1",
        "type": "ext4",
        "totalInBytes": 214748364800,
        "freeInBytes": 107374182400,
        "availableInBytes": 96636764160
      }
    ]
  },
  "transport": {
    "serverOpen": 15,
    "rxCount": 12345678,
    "rxSizeInBytes": 1073741824,
    "rxSize": "1gb",
    "txCount": 12345678,
    "txSizeInBytes": 2147483648,
    "txSize": "2gb"
  },
  "http": {
    "currentOpen": 25,
    "totalOpened": 56789
  }
}
```

---

### 5.3 Get Hot Threads

**GET** `/api/v1/nodes/{nodeId}/hot_threads`

Returns the hottest threads for a node (useful for debugging performance issues).

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/nodes/node-def456/hot_threads
```

**Response (200 OK):**
```json
{
  "nodeId": "node-def456",
  "nodeName": "opensearch-data-1",
  "timestamp": "2024-01-15T10:30:00Z",
  "hotThreads": [
    {
      "threadName": "opensearch[opensearch-data-1][search][T#5]",
      "cpuPercent": 45.2,
      "state": "RUNNABLE",
      "stackTrace": [
        "org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:740)",
        "org.opensearch.search.internal.ContextIndexSearcher.search(ContextIndexSearcher.java:250)",
        "org.opensearch.search.query.QueryPhase.execute(QueryPhase.java:115)",
        "org.opensearch.search.SearchService.executeQueryPhase(SearchService.java:590)"
      ]
    },
    {
      "threadName": "opensearch[opensearch-data-1][bulk][T#3]",
      "cpuPercent": 23.5,
      "state": "RUNNABLE",
      "stackTrace": [
        "org.apache.lucene.index.DocumentsWriter.updateDocuments(DocumentsWriter.java:425)",
        "org.apache.lucene.index.IndexWriter.updateDocuments(IndexWriter.java:1850)",
        "org.opensearch.index.engine.InternalEngine.index(InternalEngine.java:960)"
      ]
    },
    {
      "threadName": "opensearch[opensearch-data-1][merge][T#1]",
      "cpuPercent": 15.3,
      "state": "RUNNABLE",
      "stackTrace": [
        "org.apache.lucene.index.IndexWriter$IndexWriterMergeSource.merge(IndexWriter.java:5412)",
        "org.apache.lucene.index.ConcurrentMergeScheduler.doMerge(ConcurrentMergeScheduler.java:625)"
      ]
    }
  ]
}
```

---

## 6. Snapshot & Restore

### 6.1 List Snapshot Repositories

**GET** `/api/v1/snapshots/repositories`

Lists all configured snapshot repositories.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/snapshots/repositories
```

**Response (200 OK):**
```json
{
  "repositories": [
    {
      "name": "s3-backup",
      "type": "s3",
      "settings": {
        "bucket": "opensearch-backups",
        "region": "us-east-1",
        "basePath": "production/snapshots",
        "compress": true,
        "serverSideEncryption": true,
        "chunkSize": "1gb"
      }
    },
    {
      "name": "local-backup",
      "type": "fs",
      "settings": {
        "location": "/mnt/backups/opensearch",
        "compress": true,
        "chunkSize": "100mb",
        "maxRestoreBytesPerSec": "100mb",
        "maxSnapshotBytesPerSec": "50mb"
      }
    }
  ]
}
```

---

### 6.2 Create Snapshot Repository

**PUT** `/api/v1/snapshots/repositories/{repoName}`

Creates or updates a snapshot repository.

**Request (S3 Repository):**
```bash
curl -X PUT http://localhost:8080/api/v1/snapshots/repositories/s3-backup \
  -H "Content-Type: application/json" \
  -d '{
    "type": "s3",
    "settings": {
      "bucket": "opensearch-backups",
      "region": "us-east-1",
      "basePath": "production/snapshots",
      "compress": true,
      "serverSideEncryption": true,
      "storageClass": "STANDARD_IA",
      "chunkSize": "1gb",
      "maxRestoreBytesPerSec": "200mb",
      "maxSnapshotBytesPerSec": "100mb"
    }
  }'
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "repository": {
    "name": "s3-backup",
    "type": "s3"
  }
}
```

---

### 6.3 Create Snapshot

**PUT** `/api/v1/snapshots/{repoName}/{snapshotName}`

Creates a new snapshot.

**Request:**
```bash
curl -X PUT http://localhost:8080/api/v1/snapshots/s3-backup/snapshot-2024-01-15 \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["products", "orders", "customers"],
    "ignoreUnavailable": true,
    "includeGlobalState": false,
    "metadata": {
      "takenBy": "admin",
      "reason": "Daily backup",
      "environment": "production"
    },
    "partial": false
  }'
```

**Response (202 Accepted):**
```json
{
  "accepted": true,
  "snapshot": {
    "snapshot": "snapshot-2024-01-15",
    "repository": "s3-backup",
    "uuid": "snap-abc123xyz",
    "state": "IN_PROGRESS",
    "startTime": "2024-01-15T10:30:00Z",
    "indices": ["products", "orders", "customers"],
    "includeGlobalState": false,
    "metadata": {
      "takenBy": "admin",
      "reason": "Daily backup",
      "environment": "production"
    }
  }
}
```

---

### 6.4 Get Snapshot Status

**GET** `/api/v1/snapshots/{repoName}/{snapshotName}/_status`

Returns the status of a snapshot.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/snapshots/s3-backup/snapshot-2024-01-15/_status
```

**Response (200 OK - In Progress):**
```json
{
  "snapshots": [
    {
      "snapshot": "snapshot-2024-01-15",
      "repository": "s3-backup",
      "uuid": "snap-abc123xyz",
      "state": "IN_PROGRESS",
      "includeGlobalState": false,
      "stats": {
        "incremental": {
          "fileCount": 1523,
          "size": "15.2gb",
          "sizeInBytes": 16321847296
        },
        "total": {
          "fileCount": 4567,
          "size": "45.6gb",
          "sizeInBytes": 48965423104
        },
        "startTimeInMillis": 1705315800000,
        "timeInMillis": 45000
      },
      "indices": {
        "products": {
          "shardsStats": {
            "initializing": 0,
            "started": 2,
            "finalizing": 1,
            "done": 0,
            "failed": 0,
            "total": 3
          },
          "stats": {
            "incremental": {
              "fileCount": 512,
              "sizeInBytes": 5456789012
            },
            "total": {
              "fileCount": 1536,
              "sizeInBytes": 16370367036
            }
          }
        },
        "orders": {
          "shardsStats": {
            "initializing": 0,
            "started": 0,
            "finalizing": 0,
            "done": 5,
            "failed": 0,
            "total": 5
          },
          "stats": {
            "incremental": {
              "fileCount": 756,
              "sizeInBytes": 8123456789
            }
          }
        }
      }
    }
  ]
}
```

**Response (200 OK - Completed):**
```json
{
  "snapshots": [
    {
      "snapshot": "snapshot-2024-01-15",
      "repository": "s3-backup",
      "uuid": "snap-abc123xyz",
      "state": "SUCCESS",
      "includeGlobalState": false,
      "startTime": "2024-01-15T10:30:00Z",
      "endTime": "2024-01-15T10:35:23Z",
      "durationInMillis": 323000,
      "indices": ["products", "orders", "customers"],
      "shards": {
        "total": 13,
        "successful": 13,
        "failed": 0
      },
      "stats": {
        "incremental": {
          "fileCount": 1523,
          "size": "15.2gb"
        },
        "total": {
          "fileCount": 4567,
          "size": "45.6gb"
        }
      }
    }
  ]
}
```

---

### 6.5 List Snapshots

**GET** `/api/v1/snapshots/{repoName}`

Lists all snapshots in a repository.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/snapshots/s3-backup
```

**Response (200 OK):**
```json
{
  "snapshots": [
    {
      "snapshot": "snapshot-2024-01-15",
      "uuid": "snap-abc123xyz",
      "state": "SUCCESS",
      "startTime": "2024-01-15T10:30:00Z",
      "endTime": "2024-01-15T10:35:23Z",
      "durationInMillis": 323000,
      "indices": ["products", "orders", "customers"],
      "shards": {
        "total": 13,
        "successful": 13,
        "failed": 0
      }
    },
    {
      "snapshot": "snapshot-2024-01-14",
      "uuid": "snap-def456uvw",
      "state": "SUCCESS",
      "startTime": "2024-01-14T10:30:00Z",
      "endTime": "2024-01-14T10:34:15Z",
      "durationInMillis": 255000,
      "indices": ["products", "orders", "customers"],
      "shards": {
        "total": 13,
        "successful": 13,
        "failed": 0
      }
    },
    {
      "snapshot": "snapshot-2024-01-13",
      "uuid": "snap-ghi789rst",
      "state": "PARTIAL",
      "startTime": "2024-01-13T10:30:00Z",
      "endTime": "2024-01-13T10:38:45Z",
      "durationInMillis": 525000,
      "indices": ["products", "orders"],
      "shards": {
        "total": 8,
        "successful": 7,
        "failed": 1
      },
      "failures": [
        {
          "index": "products",
          "shard": 2,
          "reason": "IndexShardSnapshotFailedException: shard is not started"
        }
      ]
    }
  ]
}
```

---

### 6.6 Restore Snapshot

**POST** `/api/v1/snapshots/{repoName}/{snapshotName}/_restore`

Restores indices from a snapshot.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/snapshots/s3-backup/snapshot-2024-01-15/_restore \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["products", "orders"],
    "ignoreUnavailable": true,
    "includeGlobalState": false,
    "renamePattern": "(.+)",
    "renameReplacement": "restored-$1",
    "indexSettings": {
      "index.number_of_replicas": 0
    },
    "ignoreIndexSettings": ["index.refresh_interval"],
    "includeAliases": true
  }'
```

**Response (202 Accepted):**
```json
{
  "accepted": true,
  "snapshot": {
    "snapshot": "snapshot-2024-01-15",
    "indices": ["products", "orders"],
    "renamedIndices": {
      "products": "restored-products",
      "orders": "restored-orders"
    },
    "shards": {
      "total": 8,
      "successful": 0,
      "failed": 0
    }
  }
}
```

---

### 6.7 Delete Snapshot

**DELETE** `/api/v1/snapshots/{repoName}/{snapshotName}`

Deletes a snapshot.

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/v1/snapshots/s3-backup/snapshot-2024-01-13
```

**Response (200 OK):**
```json
{
  "acknowledged": true,
  "snapshot": "snapshot-2024-01-13",
  "repository": "s3-backup"
}
```

---

## Error Responses

All endpoints return consistent error responses:

**400 Bad Request:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request body: 'indexName' is required",
  "path": "/api/v1/indices",
  "validationErrors": [
    {
      "field": "indexName",
      "message": "must not be blank"
    }
  ]
}
```

**404 Not Found:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Index [unknown-index] not found",
  "path": "/api/v1/indices/unknown-index"
}
```

**409 Conflict:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Index [products] already exists",
  "path": "/api/v1/indices"
}
```

**500 Internal Server Error:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to connect to OpenSearch cluster",
  "path": "/api/v1/cluster/health",
  "traceId": "abc123-def456-ghi789"
}
```

**503 Service Unavailable:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "OpenSearch cluster is not available",
  "path": "/api/v1/cluster/health",
  "retryAfter": 30
}
```
