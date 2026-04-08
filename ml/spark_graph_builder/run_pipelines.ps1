# run_pipelines.ps1
# Drains the Parquet staging directory by spawning batches of pipeline.py workers.
#
# Pre-condition: one or more *.parquet files exist in $ParquetBatchDir.
# Each Python process creates one Spark session, then repeatedly claims and
# archives Parquet files via the lock-file mechanism in file_claim.py until the
# staging directory is drained.  This script:
#   1. Counts unclaimed Parquet files in the staging directory.
#   2. Spawns up to $ConcurrentCount long-lived pipeline workers.
#   3. Waits for the worker batch to finish.
#   4. Repeats only if new files appeared mid-run.
#
# Each process gets its own TEMP/TMP so that the PySpark Windows launcher
# script (spark-class2.cmd) does not collide on its temp file when multiple
# Spark JVMs initialize at the same time.  The trick that works on PS 5.1:
# temporarily set $env:TEMP/$env:TMP in the parent session immediately before
# each Start-Process call.  Windows snapshots the environment at CreateProcess
# time, so process N keeps its own TEMP even after the parent restores the
# original value for the next launch.
#
# Usage:
#   .\spark_graph_builder\run_pipelines.ps1
#   .\spark_graph_builder\run_pipelines.ps1 -ConcurrentCount 4
#   .\spark_graph_builder\run_pipelines.ps1 -NoNeo4j
#   .\spark_graph_builder\run_pipelines.ps1 -ParquetBatchDir "D:\data\parquet_batches"

param (
    [int]    $ConcurrentCount  = 1,
    [string] $PythonExe        = "C:\Users\tomas\JavaProjects\Aibeceles\.venv\Scripts\python.exe",
    [string] $WorkDir          = "C:\Users\tomas\JavaProjects\Aibeceles\ml",
    [string] $LogDir           = "C:\Users\tomas\logs\pipeline",
    [string] $ParquetBatchDir  = "C:\Users\tomas\JavaProjects\Aibeceles\ml\data\parquet_batches",
    [switch] $NoNeo4j
)

# --------------------------------------------------------------------------
# Helper: count unclaimed *.parquet files in the staging directory.
# A file is "unclaimed" when it has no <name>.lock sidecar.
# --------------------------------------------------------------------------
function Get-AvailableParquetCount {
    $doneDir = Join-Path $ParquetBatchDir "done"
    return @(
        Get-ChildItem -Path $ParquetBatchDir -Filter "*.parquet" -File |
        Where-Object {
            $_.DirectoryName -ne $doneDir -and
            -not (Test-Path ($_.FullName + ".lock"))
        }
    ).Count
}

function Get-LockedParquetCount {
    return @(
        Get-ChildItem -Path $ParquetBatchDir -Filter "*.parquet.lock" -File
    ).Count
}

# --------------------------------------------------------------------------
# Build base argument list for pipeline.py
# --------------------------------------------------------------------------
$pipelineArgs = "-m spark_graph_builder.pipeline --batch-dir `"$ParquetBatchDir`""
if ($NoNeo4j) { $pipelineArgs += " --no-neo4j" }

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

# Save original TEMP so we can restore after each round's launches.
$originalTemp = $env:TEMP
$originalTmp  = $env:TMP

$round        = 0
$totalSpawned = 0

Write-Host "Pipeline batch runner started. ConcurrentCount=$ConcurrentCount"
Write-Host "Python    : $PythonExe"
Write-Host "BatchDir  : $ParquetBatchDir"
Write-Host "Logs      : $LogDir"
Write-Host ""

# --------------------------------------------------------------------------
# Main loop
# --------------------------------------------------------------------------
while ($true) {

    $remaining = Get-AvailableParquetCount

    if ($remaining -eq 0) {
        Write-Host "No unclaimed Parquet files remaining. Queue drained."
        break
    }

    $round++
    $toSpawn = [Math]::Min($ConcurrentCount, $remaining)

    Write-Host "Round $round : $remaining unclaimed file(s) - spawning $toSpawn process(es)..."

    $processes = @()

    for ($i = 1; $i -le $toSpawn; $i++) {
        $logIndex = $totalSpawned + $i

        # Isolate TEMP for this process.  Must be set BEFORE Start-Process so
        # Windows includes it in the environment snapshot passed to CreateProcess.
        $tempDir = Join-Path $LogDir "tmp_$logIndex"
        New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
        $env:TEMP = $tempDir
        $env:TMP  = $tempDir

        $p = Start-Process -FilePath $PythonExe `
                           -ArgumentList $pipelineArgs `
                           -WorkingDirectory $WorkDir `
                           -RedirectStandardOutput "$LogDir\pipeline_$logIndex.log" `
                           -RedirectStandardError  "$LogDir\pipeline_err_$logIndex.log" `
                           -NoNewWindow -PassThru

        $processes += $p
        Write-Host "  Started pipeline $i/$toSpawn (PID $($p.Id))  TEMP=$tempDir  log=pipeline_$logIndex.log"
    }

    # Restore parent session TEMP before doing anything else.
    $env:TEMP = $originalTemp
    $env:TMP  = $originalTmp

    # Primary wait: block until every launcher process has exited.
    $processes | ForEach-Object { $_.WaitForExit() }

    # Safety net: if a child process outlives the launcher stub,
    # wait for any remaining .lock files to clear.
    while ((Get-LockedParquetCount) -gt 0) {
        Start-Sleep -Seconds 2
    }

    $totalSpawned += $toSpawn

    Write-Host "  Round $round complete - claimed files finished processing."
    Write-Host ""
}

Write-Host ""
Write-Host "All done. Rounds=$round  TotalWorkersSpawned=$totalSpawned"
Write-Host "Logs in: $LogDir"
