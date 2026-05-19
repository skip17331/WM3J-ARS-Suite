<#
.SYNOPSIS
  ARS Suite - unified Windows installer. One script, start to finish.

.DESCRIPTION
  You do not run this directly. Double-click install.bat (or run
  .\install.bat) - that is the single command for Windows. It calls
  this script. One run on a clean Windows box installs the whole suite:

    1. Toolchain  - ensures Git + Temurin 21 JDK (winget if missing) and
                    Maven (genuine Apache zip, SHA-512 verified)
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
  Assume Git/JDK/Maven are already present; skip the toolchain step
  entirely (no winget, no Maven download).

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

# Apache publishes no official winget package for Maven (request #65391 is
# still Help-Wanted; the only winget entry is a third-party manifest whose
# source has varied between the Apache CDN and a maintainer's personal
# repo). So fetch the genuine Apache zip straight from Apache, verify its
# published SHA-512, and put it on PATH - same provenance as the apt/brew
# Maven the Linux/macOS paths get. Pinned version + hash; bump both
# together when upgrading (sha512 is from
# dlcdn.apache.org/.../apache-maven-<ver>-bin.zip.sha512, cross-checked
# against archive.apache.org).
function Ensure-Maven {
    if (Get-Command mvn -ErrorAction SilentlyContinue) { Info 'Maven - found'; return }

    $ver    = '3.9.16'
    # PowerShell string -ne is case-insensitive, so lowercase is fine here
    # (matches exactly what Apache publishes, so it's easy to re-verify).
    $sha512 = 'ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3'
    $baseDir = Join-Path $env:LOCALAPPDATA 'ARS-Suite'
    $mvnDir  = Join-Path $baseDir "apache-maven-$ver"
    $mvnBin  = Join-Path $mvnDir 'bin'
    $mvnCmd  = Join-Path $mvnBin 'mvn.cmd'

    if (-not (Test-Path -LiteralPath $mvnCmd)) {
        $zipName = "apache-maven-$ver-bin.zip"
        $urls = @(
            "https://dlcdn.apache.org/maven/maven-3/$ver/binaries/$zipName",
            "https://archive.apache.org/dist/maven/maven-3/$ver/binaries/$zipName"
        )
        $tmp = Join-Path ([IO.Path]::GetTempPath()) $zipName
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $oldProgress = $ProgressPreference
        $ProgressPreference = 'SilentlyContinue'   # PS 5.1: progress bar cripples download speed

        $got = $false
        foreach ($u in $urls) {
            try {
                Info "Maven - downloading $ver from $u ..."
                Invoke-WebRequest -Uri $u -OutFile $tmp -UseBasicParsing
                $got = $true; break
            } catch {
                Info "  ... that mirror failed ($($_.Exception.Message)); trying next"
            }
        }
        $ProgressPreference = $oldProgress
        if (-not $got) { Die "Could not download Apache Maven $ver from any Apache mirror." }

        $actual = (Get-FileHash -LiteralPath $tmp -Algorithm SHA512).Hash
        if ($actual -ne $sha512) {
            Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
            Die "Maven $ver download is corrupt or tampered (SHA-512 mismatch).`n  expected $sha512`n  got      $actual"
        }
        Info 'Maven - SHA-512 verified; extracting ...'
        if (-not (Test-Path -LiteralPath $baseDir)) {
            New-Item -ItemType Directory -Path $baseDir | Out-Null
        }
        if (Test-Path -LiteralPath $mvnDir) { Remove-Item -LiteralPath $mvnDir -Recurse -Force }
        Expand-Archive -LiteralPath $tmp -DestinationPath $baseDir -Force
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
        if (-not (Test-Path -LiteralPath $mvnCmd)) { Die "Maven extracted but $mvnCmd is missing." }
    } else {
        Info "Maven - reusing $mvnDir"
    }

    # Persist on the user PATH (future shells) + this session (rest of run).
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if (($userPath -split ';') -notcontains $mvnBin) {
        $newPath = (@($userPath, $mvnBin) | Where-Object { $_ }) -join ';'
        [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
    }
    Update-SessionPath
    if ($env:Path -notlike "*$mvnBin*") { $env:Path = "$env:Path;$mvnBin" }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Die "Maven installed to $mvnDir but 'mvn' is still not resolvable."
    }
    Info "Maven - installed ($ver -> $mvnDir)"
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
        Ensure-Maven
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
