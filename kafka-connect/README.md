# Kafka Connect — Neo4j Sink Connector Configuration

Replacement for the deprecated neo4j-streams plugin (EOL at Neo4j 4.4).
These configs run on the **Neo4j Connector for Kafka** (Kafka Connect architecture)
against **Neo4j 5.24.0 Enterprise**.

---

## Prerequisites

| Component | Minimum Version | Notes |
|---|---|---|
| Neo4j Server | 5.24.0 Enterprise | Bolt on `localhost:7687` |
| Apache Kafka | **3.4.1+** | Neo4j Connector for Kafka requires this minimum |
| Java | 11, 17, or 21 LTS | For Kafka Connect worker |
| Neo4j Connector for Kafka | 5.3.0 | `neo4j-kafka-connect-5.3.0.jar` |
| APOC plugin | matching Neo4j version | Required by `s12`/`s3t`/`s3a` Cypher queries |

> **Kafka upgrade required.** The existing install is Kafka 2.5.0 (`kafka_2.12-2.5.0`).
> The Neo4j Connector for Kafka 5.3.0 requires Kafka Connect 3.4.1 or later.
> Download a current Kafka release from https://kafka.apache.org/downloads.

---

## Files in this folder

| File | Purpose |
|---|---|
| `sink-twoPoly.json` | Sink connector config — topic `twoPoly` (produced by `TwoPolynomialGenerator.jar`) |
| `sink-s12.json` | Sink connector config — topic `s12` (produced by `ZADScripts.jar`) |
| `sink-s3t.json` | Sink connector config — topic `s3t` (produced by `ZADScripts.jar`) |
| `sink-s3a.json` | Sink connector config — topic `s3a` (produced by `ZADScripts.jar`) |
| `create-topics.ps1` | PowerShell script to create all four Kafka topics |
| `README.md` | This file |

---

## Step 1 — Download the Neo4j Connector JAR

```powershell
$pluginsDir = "C:\kafka\plugins"
New-Item -ItemType Directory -Force -Path $pluginsDir
Invoke-WebRequest `
    -Uri "https://github.com/neo4j/neo4j-kafka-connector/releases/download/5.3.0/neo4j-kafka-connect-5.3.0.jar" `
    -OutFile "$pluginsDir\neo4j-kafka-connect-5.3.0.jar"
```

---

## Step 2 — Configure the Kafka Connect worker

Edit (or create) `connect-standalone.properties` in the Kafka `config\` directory:

```properties
bootstrap.servers=localhost:9092
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
offset.storage.file.filename=C:/kafka/connect-offsets.dat
plugin.path=C:/kafka/plugins
```

Adjust paths to match your Kafka installation.

---

## Step 3 — Create Kafka topics (first time only)

Kafka broker and Zookeeper must be running.

```powershell
.\create-topics.ps1
```

Or with a custom Kafka home:

```powershell
.\create-topics.ps1 -KafkaHome "C:\kafka\kafka_2.13-3.9.0"
```

---

## Step 4 — Start Kafka Connect

```bat
cd <KAFKA_HOME>\bin\windows
connect-standalone.bat ..\..\config\connect-standalone.properties
```

Wait until the log shows `Kafka Connect started`.
The REST API will be available at `http://localhost:8083`.

---

## Step 5 — Register the sink connectors

Submit each JSON config to the Kafka Connect REST API.
Run from this folder (`kafka-connect\`):

```powershell
$configs = @("sink-twoPoly.json", "sink-s12.json", "sink-s3t.json", "sink-s3a.json")

foreach ($cfg in $configs) {
    Write-Host "Registering $cfg ..."
    Invoke-RestMethod `
        -Method Post `
        -Uri "http://localhost:8083/connectors" `
        -ContentType "application/json" `
        -InFile $cfg
}
```

Verify all connectors are running:

```powershell
Invoke-RestMethod http://localhost:8083/connectors | ConvertTo-Json
```

Check individual connector status:

```powershell
Invoke-RestMethod http://localhost:8083/connectors/Neo4jSinkTwoPoly/status | ConvertTo-Json -Depth 5
```

---

## Step 6 — Run the pipeline

Full startup order:

```
1. Zookeeper
2. Kafka broker
3. Neo4j 5.24                    (no Kafka settings in neo4j.conf)
4. Kafka Connect standalone      (hosts the Neo4j sink connectors)
5. TwoPolynomialGenerator.jar    (produces to Kafka topic "twoPoly")
6. ZADScripts.jar                (produces to Kafka topics s12, s3t, s3a)
```

---

## Connector management

Delete a connector:

```powershell
Invoke-RestMethod -Method Delete -Uri "http://localhost:8083/connectors/Neo4jSinkTwoPoly"
```

Update a connector (PUT replaces config):

```powershell
$body = (Get-Content sink-twoPoly.json | ConvertFrom-Json).config | ConvertTo-Json
Invoke-RestMethod -Method Put `
    -Uri "http://localhost:8083/connectors/Neo4jSinkTwoPoly/config" `
    -ContentType "application/json" `
    -Body $body
```

---

## Migrated from

These configs were translated from the legacy neo4j-streams sink entries
originally stored in `ZADScriptsK/KafkaStreamsTopics.json` (Zeppelin notebook
`2FT43SUAG`, "Streams.Sink.Topic.Cypher", dated 2020-12-24).

The old `streams.sink.topic.cypher.<TOPIC>` format becomes
`neo4j.cypher.topic.<TOPIC>` in the new connector. The `event.*` variable
binding is preserved for backward compatibility.
