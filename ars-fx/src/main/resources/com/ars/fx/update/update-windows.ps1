# ARS Suite - Windows self-update helper.
#
# Started by the app (UpdateApplier) right before it exits. Windows will not let a running jar be replaced,
# so this waits for the app to go away, swaps in the already-downloaded and checksum-verified jar, relaunches,
# and rolls back to the old jar if the new one does not stay up.
#
#   -AppPid        process id of the app that is exiting
#   -Jar           the installed jar to replace (e.g. ...\ars-fx\target\ars-fx-windows.jar)
#   -NewJar        the verified download
#   -Launcher      the installed ars-fx.bat (its folder is the working directory)
#   -StartupSecs   how long the new jar must stay up to count as a good start (default 20)
#   -Log           append progress here
param(
    [Parameter(Mandatory)][int]$AppPid,
    [Parameter(Mandatory)][string]$Jar,
    [Parameter(Mandatory)][string]$NewJar,
    [Parameter(Mandatory)][string]$Launcher,
    [int]$StartupSecs = 20,
    [string]$Log = ''
)
$ErrorActionPreference = 'Stop'

function Write-Log([string]$m) {
    $line = '{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $m
    if ($Log) { try { Add-Content -LiteralPath $Log -Value $line } catch { } }
}

# True if something still has the jar open (a running app, or the background hub it started).
function Test-Locked([string]$path) {
    try {
        $fs = [System.IO.File]::Open($path, 'Open', 'ReadWrite', 'None')
        $fs.Close(); return $false
    } catch [System.IO.FileNotFoundException] { return $false
    } catch { return $true }
}

function Wait-Unlocked([string]$path, [int]$secs) {
    $end = (Get-Date).AddSeconds($secs)
    while ((Get-Date) -lt $end) {
        if (-not (Test-Locked $path)) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return -not (Test-Locked $path)
}

function Start-App {
    $dir = Split-Path -Parent $Launcher
    return Start-Process -FilePath 'javaw' -ArgumentList @('-Dfile.encoding=UTF-8', '-jar', ('"{0}"' -f $Jar)) `
        -WorkingDirectory $dir -PassThru
}

try {
    Write-Log "update start: app pid $AppPid, jar $Jar, new $NewJar"
    if (-not (Test-Path -LiteralPath $NewJar)) { throw "downloaded jar is missing: $NewJar" }
    # a corrupt jar makes javaw show a dialog and stay running, which would look like a good start - so check it opens
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($NewJar)
        $hasManifest = $null -ne ($zip.Entries | Where-Object { $_.FullName -eq 'META-INF/MANIFEST.MF' })
        $zip.Dispose()
    } catch { throw "downloaded file is not a valid jar: $($_.Exception.Message)" }
    if (-not $hasManifest) { throw 'downloaded jar has no manifest' }

    # 1. the app that asked for the update is exiting - wait for it
    $end = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $end -and (Get-Process -Id $AppPid -ErrorAction SilentlyContinue)) { Start-Sleep -Milliseconds 300 }

    # 2. wait for the jar to be released; the background hub may still hold it
    if (-not (Wait-Unlocked $Jar 20)) {
        # stop only suite processes running THIS jar (the hub started from it), then wait again
        Write-Log 'jar still in use - stopping suite processes that run it'
        Get-CimInstance Win32_Process |
            Where-Object { $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -and $_.CommandLine.Contains($Jar) } |
            ForEach-Object { try { Stop-Process -Id $_.ProcessId -Force } catch { } }
        if (-not (Wait-Unlocked $Jar 15)) { throw 'the installed jar is still locked; leaving the current version in place' }
    }

    # 3. swap, keeping the old jar as .bak
    $bak = "$Jar.bak"
    if (Test-Path -LiteralPath $bak) { Remove-Item -LiteralPath $bak -Force }
    Move-Item -LiteralPath $Jar -Destination $bak -Force
    try { Move-Item -LiteralPath $NewJar -Destination $Jar -Force }
    catch { Move-Item -LiteralPath $bak -Destination $Jar -Force; throw }
    Write-Log 'swapped in the new jar'

    # 4. relaunch and make sure it stays up
    $p = Start-App
    Start-Sleep -Seconds $StartupSecs
    if ($p.HasExited) {
        Write-Log "new version exited within $StartupSecs s (code $($p.ExitCode)) - rolling back"
        Move-Item -LiteralPath $Jar -Destination "$Jar.failed" -Force
        Move-Item -LiteralPath $bak -Destination $Jar -Force
        [void](Start-App)
        Write-Log 'rolled back to the previous version'
        exit 2
    }
    Write-Log 'new version is running; previous jar kept as .bak'
    exit 0
}
catch {
    Write-Log "update failed: $($_.Exception.Message)"
    # leave the user with a working app: relaunch whatever jar is installed
    try { if (Test-Path -LiteralPath $Jar) { [void](Start-App) } } catch { }
    exit 1
}
