# Local infrastructure

The root `compose.yaml` is the source of truth. It runs a single-node KRaft Kafka broker (development only), PostgreSQL, Redis, MinIO and Mailpit alongside the application. Named volumes persist database, Redis, broker and encrypted object data. Published ports bind to 127.0.0.1.

Kafka topics and dead-letter counterparts are declared by `KafkaConfig`; the private MinIO bucket is lazily created by the storage adapter. No manual bucket/topic initialization is needed. Inspect status with `docker compose ps`; inspect failures with `docker compose logs backend` or the relevant service. Never paste logs containing production data into public issues.

Do not expose this Compose topology directly to the internet. Production services need authentication, network policy, TLS, secure secrets, durable backup/restore and operational monitoring.
