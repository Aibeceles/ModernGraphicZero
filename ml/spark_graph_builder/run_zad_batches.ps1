# run_zad_batches.ps1
# Drains the :Configure node queue by spawning batches of Java workers.
#
# Pre-condition: m :Configure nodes exist in the Neo4j database.
# Each Java process atomically claims and deletes one :Configure node on
# startup (via configureQuery1 / ConfigLock).  This script:
#   1. Counts remaining :Configure nodes via the Neo4j HTTP API.
#   2. Spawns min($ConcurrentCount, remaining) Java processes.
#   3. Waits for the batch to finish.
#   4. Repeats until the count reaches 0.
#
# Usage:
#   .\spark_graph_builder\run_zad_batches.ps1
#   .\spark_graph_builder\run_zad_batches.ps1 -ConcurrentCount 2
#   .\spark_graph_builder\run_zad_batches.ps1 -ConcurrentCount 4 -Neo4jDatabase neo4j

param (                                                                                
    [int]    $ConcurrentCount = 4,
    [string] $JarPath         = "C:\Users\tomas\JavaProjects\Aibeceles\ZerosAndDifferences033021\dist\ZerosAndDifferences.jar",
    [string] $LogDir          = "C:\Users\tomas\logs\zad",
    [string] $Neo4jHttpUrl    = "http://localhost:7474",
    [string] $Neo4jUser       = "neo4j",
    [string] $Neo4jPassword   = "ChiefQuippy",
    [string] $Neo4jDatabase   = "tagtest"
)

# --------------------------------------------------------------------------
# Helper: query Neo4j HTTP API and return count of :Configure nodes.
# --------------------------------------------------------------------------
function Get-ConfigureCount {
    $authBytes  = [System.Text.Encoding]::UTF8.GetBytes("${Neo4jUser}:${Neo4jPassword}")
    $authHeader = "Basic " + [Convert]::ToBase64String($authBytes)

    $body = '{"statements":[{"statement":"MATCH (c:Configure) RETURN count(c) AS remaining"}]}'

    try {
        $response = Invoke-RestMethod `
            -Uri     "$Neo4jHttpUrl/db/$Neo4jDatabase/tx/commit" `
            -Method  Post `
            -Headers @{ Authorization = $authHeader; "Content-Type" = "application/json" } `
            -Body    $body `
            -ErrorAction Stop

        return [int]$response.results[0].data[0].row[0]
    }
    catch {
        Write-Host "ERROR: could not reach Neo4j at $Neo4jHttpUrl - $_"
        exit 1
    }
}

# --------------------------------------------------------------------------
# Main loop
# --------------------------------------------------------------------------
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$round        = 0
$totalSpawned = 0

Write-Host "ZAD batch runner started. ConcurrentCount=$ConcurrentCount"
Write-Host "Jar   : $JarPath"
Write-Host "Logs  : $LogDir"
Write-Host "Neo4j : $Neo4jHttpUrl / db=$Neo4jDatabase"
Write-Host ""

while ($true) {

    $remaining = Get-ConfigureCount

    if ($remaining -eq 0) {
        Write-Host "No :Configure nodes remaining. Queue drained."
        break
    }

    $round++
    $toSpawn = [Math]::Min($ConcurrentCount, $remaining)

    Write-Host "Round $round : $remaining :Configure node(s) remaining - spawning $toSpawn process(es)..."

    $processes = @()

    for ($i = 1; $i -le $toSpawn; $i++) {
        $logIndex = $totalSpawned + $i
        $p = Start-Process -FilePath "java" `
                           -ArgumentList "-jar `"$JarPath`"" `
                           -RedirectStandardOutput "$LogDir\process_$logIndex.log" `
                           -RedirectStandardError  "$LogDir\process_err_$logIndex.log" `
                           -NoNewWindow -PassThru
        $processes += $p
        Write-Host "  Started worker $i/$toSpawn (PID $($p.Id))  log=process_$logIndex.log"
    }

    $processes | ForEach-Object { $_.WaitForExit() }
    $totalSpawned += $toSpawn

    $failed = 0
    for ($i = 0; $i -lt $processes.Count; $i++) {
        $proc     = $processes[$i]
        $exitCode = if ($proc.HasExited) { $proc.ExitCode } else { $null }
        if ($exitCode -ne 0) {
            $failed++
            Write-Host "  WARNING: PID $($proc.Id) exited with code $exitCode"
        }
    }

    if ($failed -eq 0) {
        Write-Host "  Round $round complete - all workers exited cleanly."
    } else {
        Write-Host "  Round $round complete - $failed worker(s) reported non-zero exit."
    }
    Write-Host ""
}

Write-Host ""
Write-Host "All done. Rounds=$round  TotalWorkersSpawned=$totalSpawned"
Write-Host "Logs in: $LogDir"
