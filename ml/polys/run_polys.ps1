param (
    [string] $SparkSubmit = "spark-submit",
    [string] $JarPath = "",
    [string] $PipelineName = "strongestPath",
    [string] $InputFormat = "parquet",
    [string] $InputPath = "",
    [string] $RangeLow = "2",
    [string] $RangeHigh = "7",
    [string] $MaxN = "8",
    [string] $Dimension = "2",
    [string] $ArtifactsRoot = "ml/polys/artifacts",
    [string] $RunId = ""
)

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    Write-Host "ERROR: -JarPath is required."
    Write-Host "Example (from ml/polys): .\run_polys.ps1 -JarPath .\target\scala-2.12\polys_2.12-0.1.0.jar -InputPath C:\data\s12 -ArtifactsRoot artifacts"
    exit 1
}

if ($InputFormat -ne "neo4j" -and [string]::IsNullOrWhiteSpace($InputPath)) {
    Write-Host "ERROR: -InputPath is required."
    exit 1
}

$argList = @(
    "--class", "polys.app.MainJob",
    $JarPath,
    "--pipelineName=$PipelineName",
    "--inputFormat=$InputFormat",
    "--inputPath=$InputPath",
    "--rangeLow=$RangeLow",
    "--rangeHigh=$RangeHigh",
    "--maxN=$MaxN",
    "--dimension=$Dimension",
    "--artifactsRoot=$ArtifactsRoot"
)

if (-not [string]::IsNullOrWhiteSpace($RunId)) {
    $argList += "--runId=$RunId"
}

Write-Host "Launching polys Spark job..."
Write-Host "SparkSubmit : $SparkSubmit"
Write-Host "JarPath     : $JarPath"
Write-Host "Pipeline    : $PipelineName"
Write-Host "InputFormat : $InputFormat"
Write-Host "InputPath   : $InputPath"
Write-Host "RangeLow    : $RangeLow"
Write-Host "RangeHigh   : $RangeHigh"
Write-Host "MaxN        : $MaxN"
Write-Host "Dimension   : $Dimension"
Write-Host "Artifacts   : $ArtifactsRoot"
if (-not [string]::IsNullOrWhiteSpace($RunId)) {
    Write-Host "RunId       : $RunId"
}
Write-Host ""

& $SparkSubmit @argList
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    Write-Host ""
    Write-Host "polys job failed with exit code $exitCode"
    exit $exitCode
}

Write-Host ""
Write-Host "polys job completed successfully."
