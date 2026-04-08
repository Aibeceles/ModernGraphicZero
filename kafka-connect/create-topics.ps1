<#
.SYNOPSIS
    Creates the Kafka topics required by the TwoPolynomialGenerator and ZADScripts pipeline.

.DESCRIPTION
    Creates four topics: twoPoly, s12, s3t, s3a.
    Kafka broker must be running on localhost:9092 before executing this script.

.PARAMETER KafkaHome
    Root directory of the Kafka installation.
    Default: C:\kafka

.PARAMETER BootstrapServer
    Kafka bootstrap server address.
    Default: localhost:9092
#>

param(
    [string]$KafkaHome = "C:\kafka",
    [string]$BootstrapServer = "localhost:9092"
)

$ErrorActionPreference = "Continue"

$kafkaTopicsBat = Join-Path $KafkaHome "bin\windows\kafka-topics.bat"

if (-not (Test-Path $kafkaTopicsBat)) {
    Write-Error "kafka-topics.bat not found at $kafkaTopicsBat - verify KafkaHome parameter."
    exit 1
}

$topics = @("twoPoly", "s12", "s3t", "s3a")

Write-Host "Kafka home   : $KafkaHome"
Write-Host "Bootstrap    : $BootstrapServer"
Write-Host ""

foreach ($topic in $topics) {
    Write-Host "Creating topic: $topic ..." -NoNewline

    & $kafkaTopicsBat `
        --create `
        --bootstrap-server $BootstrapServer `
        --replication-factor 1 `
        --partitions 1 `
        --topic $topic 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host " OK" -ForegroundColor Green
    } else {
        Write-Host " FAILED (exit code $LASTEXITCODE)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Listing topics on ${BootstrapServer}:"
& $kafkaTopicsBat --list --bootstrap-server $BootstrapServer
