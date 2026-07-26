param(
    [ValidateSet('baseline', 'java-output-rate', 'monitor-backpressure')]
    [string]$Scenario = 'baseline'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$serverDirectory = Join-Path $projectRoot 'server-java-SuMon'
$jarPath = Join-Path $serverDirectory 'target\server-java-SuMon-0.0.1-SNAPSHOT.jar'

if (Test-NetConnection -ComputerName '127.0.0.1' -Port 18081 -InformationLevel Quiet -WarningAction SilentlyContinue) {
    throw 'Port 18081 is already in use. Stop the existing validation service before continuing.'
}

Push-Location $serverDirectory
try {
    & mvn.cmd -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to package the current validation service source.'
    }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "Validation JAR was not created: $jarPath"
}

$databaseUser = Read-Host 'Isolated MySQL validation username'
if ([string]::IsNullOrWhiteSpace($databaseUser)) {
    throw 'MySQL validation username must not be blank.'
}
$databasePassword = & (Join-Path $PSScriptRoot 'prompt-password.ps1') -Prompt 'Isolated MySQL validation password'
if ([string]::IsNullOrWhiteSpace($databasePassword)) {
    throw 'MySQL validation password must not be blank.'
}

$randomBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($randomBytes)
$runtimeSecret = [Convert]::ToBase64String($randomBytes)

$serverEnvironment = @{
    DB_HOST = '127.0.0.1'
    DB_PORT = '3306'
    DB_NAME = 'susumonitor_metrics_validation'
    DB_USER = $databaseUser
    DB_PASSWORD = $databasePassword
    SERVER_ADDRESS = '0.0.0.0'
    SERVER_PORT = '18081'
    APP_ENV = 'test'
    SPRING_PROFILES_ACTIVE = 'test'
    JWT_SECRET = $runtimeSecret
    AES_GCM_KEY = $runtimeSecret
    TERMINAL_OUTPUT_RATE_BYTES_PER_SECOND = '1048576'
    TERMINAL_OUTPUT_BURST_BYTES = '1048576'
    TERMINAL_MONITOR_SEND_TIME_LIMIT_MILLIS = '5000'
    TERMINAL_MONITOR_BUFFER_SIZE_BYTES = '262144'
}

switch ($Scenario) {
    'java-output-rate' {
        $serverEnvironment.TERMINAL_OUTPUT_RATE_BYTES_PER_SECOND = '1'
        $serverEnvironment.TERMINAL_OUTPUT_BURST_BYTES = '16384'
    }
    'monitor-backpressure' {
        $serverEnvironment.TERMINAL_MONITOR_BUFFER_SIZE_BYTES = '1024'
    }
}

$savedEnvironment = @{}
foreach ($entry in $serverEnvironment.GetEnumerator()) {
    $savedEnvironment[$entry.Key] = [Environment]::GetEnvironmentVariable($entry.Key, 'Process')
    [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
}

try {
    "Starting isolated flow-control Java service: scenario=$Scenario database=susumonitor_metrics_validation port=18081"
    "Scope: only the isolated validation database; stop with Ctrl+C after the matching WSL scenario completes."
    & java.exe -jar $jarPath
} finally {
    foreach ($entry in $savedEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
    Remove-Item Env:AES_GCM_KEY -ErrorAction SilentlyContinue
    $databasePassword = $null
    $runtimeSecret = $null
}
