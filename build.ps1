param(
    [string[]]$Tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug'),
    [switch]$Offline,
    [switch]$Online,
    [string]$JavaHome,
    [string]$GradleUserHome
)
$ErrorActionPreference = 'Stop'
if ($Offline -and $Online) { throw 'Choose either -Offline or -Online.' }
$priorJava = $env:JAVA_HOME
$priorCache = $env:GRADLE_USER_HOME
try {
    if ($JavaHome) { $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path }
    if ($GradleUserHome) {
        $env:GRADLE_USER_HOME = [IO.Path]::GetFullPath($GradleUserHome)
    }
    if ($env:JAVA_HOME) {
        $javaName = if ($env:OS -eq 'Windows_NT') { 'java.exe' } else { 'java' }
        if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin/$javaName"))) {
            throw 'JAVA_HOME must point to a JDK installation.'
        }
    } elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw 'Set JAVA_HOME or pass -JavaHome for JDK 17 or 21.'
    }
    $wrapper = if ($env:OS -eq 'Windows_NT') { 'gradlew.bat' } else { 'gradlew' }
    $gradleArgs = @('-p', $PSScriptRoot, '--no-daemon')
    if ($Offline) { $gradleArgs += '--offline' }
    & (Join-Path $PSScriptRoot $wrapper) @gradleArgs @Tasks
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $priorJava
    $env:GRADLE_USER_HOME = $priorCache
}
