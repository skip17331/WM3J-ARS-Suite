<#
.SYNOPSIS
  ARS Suite - unified Windows installer. One script, start to finish.

.DESCRIPTION
  You do not run this directly. Double-click install.bat (or run
  .\install.bat) - that is the single command for Windows. It calls
  this script. One run on a clean Windows box installs the whole suite:

    1. Toolchain  - ensures Git, Temurin 21 JDK, and Maven (winget if missing)
    2. Build      - mvn install j-log-engine/j-learn/j-vault, then
                    mvn package the remaining modules
    3. Integrate  - builds + runs the Java installer, which writes per-module
                    .bat launchers, generates .ico icons, and creates
                    Start-Menu shortcuts under "ARS Suite"
    4. Normalize  - rewrites any stale Linux launch commands left in
                    j-hub.json so j-hub launches the Windows .bat wrappers

  Everything printed is also saved to install-log.txt next to this
  script. If anything fails, the script tells you exactly which file to
  attach to a bug report - no need to copy-paste from the window.

  Safe to re-run as an upgrade. Never touches QSO logs, the j-vault
  inventory DB, or station credentials.

.PARAMETER SkipDeps
  Assume Git/JDK/Maven are already present; do not call winget.

.PARAMETER SkipBuild
  Jars are already built; only do desktop integration + config normalize.

.EXAMPLE
  .\install.bat            (this is the single command for Windows)
#>
[CmdletBinding()]
param(
    [switch]$SkipDeps,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
Set-Location -LiteralPath $root

$LogFile    = Join-Path $root 'install-log.txt'
$RepoIssues = 'https://github.com/skip17331/WM3J-ARS-Suite/issues'

# ---------------------------------------------------------------- helpers
function Section($t) {
    Write-Host ''
    Write-Host ('=' * 64) -ForegroundColor Cyan
    Write-Host "  $t"      -ForegroundColor Cyan
    Write-Host ('=' * 64) -ForegroundColor Cyan
}
function Info($t) { Write-Host "  $t" }
# Abort the run. Throws so the outer handler logs it and prints the
# plain-language bug-report message; never just silently exits.
function Die($t)  { throw $t }

# winget-installed tools land on PATH but not in THIS process's $env:Path.
function Update-SessionPath {
    $m = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $u = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = @($m, $u | Where-Object { $_ }) -join ';'
}

function Ensure-Tool($exe, $wingetId, $label) {
    if (Get-Command $exe -ErrorAction SilentlyContinue) { Info "$label - found"; return }
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        Die "$label is missing and winget is unavailable. Install $label manually, then re-run."
    }
    Info "$label - not found; installing via winget ($wingetId) ..."
    winget install --id $wingetId -e --silent `
        --accept-package-agreements --accept-source-agreements `
        --disable-interactivity
    Update-SessionPath
    if (-not (Get-Command $exe -ErrorAction SilentlyContinue)) {
        Die "$label still not on PATH after install. Open a NEW terminal and re-run."
    }
    Info "$label - installed"
}

# Run Maven for one pom; $goals may be a single goal or several.
function Invoke-Mvn($pomRelative, [string[]]$goals) {
    $pom = Join-Path $root $pomRelative
    if (-not (Test-Path -LiteralPath $pom)) { Die "pom not found: $pomRelative" }
    Info ("mvn {0,-15} {1}" -f ($goals -join ' '), $pomRelative)
    & mvn -q -DskipTests -f $pom @goals
    if ($LASTEXITCODE -ne 0) { Die "Maven [$($goals -join ' ')] failed for $pomRelative" }
}

# ---------------------------------------------------------------- logging
# Capture the entire run (including mvn/winget/java output) to a file so
# a non-programmer can just attach it to a bug report.
$logging = $false
try {
    try { Stop-Transcript | Out-Null } catch { }
    Start-Transcript -Path $LogFile -Force | Out-Null
    $logging = $true
} catch {
    Write-Host "  (could not start logging to $LogFile : $($_.Exception.Message))" -ForegroundColor Yellow
}

# ---------------------------------------------------------------- run
$ok = $false
try {
    Section 'ARS Suite - unified Windows install'
    Info "Source root : $root"
    Info "User home   : $env:USERPROFILE"
    Info "Log file    : $LogFile"

    if (-not $SkipDeps) {
        Section '[1/4] Toolchain (Git, Java 21, Maven)'
        Ensure-Tool 'git'  'Git.Git'                        'Git'
        Ensure-Tool 'java' 'EclipseAdoptium.Temurin.21.JDK' 'Temurin 21 JDK'
        Ensure-Tool 'mvn'  'Apache.Maven'                    'Maven'
    } else {
        Section '[1/4] Toolchain - skipped (-SkipDeps)'
    }

    if (-not $SkipBuild) {
        Section '[2/4] Build all modules (first run downloads deps, ~5-10 min)'
        # Library/engine modules other modules depend on - install to local repo.
        Invoke-Mvn 'j-log-engine\pom.xml' 'install'
        Invoke-Mvn 'j-learn\pom.xml'      'install'
        Invoke-Mvn 'j-vault\pom.xml'      'install'
        # Remaining user-facing modules - fat-jar package.
        foreach ($mod in 'j-hub', 'j-log', 'j-map', 'j-digi', 'j-bridge', 'j-sat', 'morse-trainer') {
            Invoke-Mvn "$mod\pom.xml" 'package'
        }
    } else {
        Section '[2/4] Build - skipped (-SkipBuild)'
    }

    Section '[3/4] Desktop integration (launchers, icons, Start-Menu shortcuts)'
    Invoke-Mvn 'installer\pom.xml' @('clean', 'package')
    $installerJar = Join-Path $root 'installer\target\j-installer-1.0.8.jar'
    if (-not (Test-Path -LiteralPath $installerJar)) { Die "installer jar not built: $installerJar" }
    & java -jar $installerJar --root $root
    if ($LASTEXITCODE -ne 0) { Die 'installer run failed' }

    Section '[4/4] Normalize j-hub launch commands'
    # j-hub.json lives wherever j-hub's cwd was (j-hub dir via the shortcut, or
    # repo root via .\j-hub\start.bat). A rebuilt j-hub also self-heals stale
    # commands on first launch (JHubConfig.applyDefaults); this is belt-and-
    # braces and only rewrites command VALUES, never the rest of the file.
    $candidates = @(
        (Join-Path $root 'j-hub\j-hub.json'),
        (Join-Path $root 'j-hub.json')
    )
    $rx = '(?i)("command"\s*:\s*")([^"]*?(?:^\s*bash\s|bash\s|\.sh|/home/)[^"]*)(")'
    $fixedAny = $false
    foreach ($jf in $candidates) {
        if (-not (Test-Path -LiteralPath $jf)) { continue }
        $raw = Get-Content -Raw -LiteralPath $jf
        $new = [regex]::Replace($raw, $rx, '${1}${3}')
        if ($new -ne $raw) {
            Set-Content -LiteralPath $jf -Value $new -Encoding UTF8 -NoNewline
            Info "rewrote stale Linux commands in $jf"
            $fixedAny = $true
        }
    }
    if (-not $fixedAny) {
        Info 'no stale j-hub.json found - Windows defaults applied on first j-hub launch'
    }

    Section 'Done - ARS Suite is installed'
    Info 'Open the Start Menu, type  WM3J J-Hub , press Enter.'
    Info 'Then open  http://localhost:8081/  in your browser.'
    Info "(A copy of this run was saved to $LogFile)"
    $ok = $true
}
catch {
    Write-Host ''
    Write-Host ('!' * 64) -ForegroundColor Red
    Write-Host '  INSTALL DID NOT FINISH' -ForegroundColor Red
    Write-Host ('!' * 64) -ForegroundColor Red
    Write-Host ''
    Write-Host '  What went wrong:' -ForegroundColor Yellow
    Write-Host "    $($_.Exception.Message)"
    Write-Host ''
    Write-Host '  This is probably not something you did wrong. To get help,'
    Write-Host '  send us this one file (it has the full details):'
    Write-Host ''
    Write-Host "      $LogFile" -ForegroundColor Cyan
    Write-Host ''
    Write-Host '  Open a new issue here and drag that file into it:'
    Write-Host "      $RepoIssues" -ForegroundColor Cyan
    Write-Host ''
}
finally {
    if ($logging) { try { Stop-Transcript | Out-Null } catch { } }
}

if (-not $ok) { exit 1 }
exit 0
