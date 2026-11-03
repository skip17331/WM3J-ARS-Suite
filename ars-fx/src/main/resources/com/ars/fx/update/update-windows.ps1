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
#   -HubWaitSecs   how long to let the background hub exit by itself before giving up (default 40)
#   -Log           append progress here
param(
    [Parameter(Mandatory)][int]$AppPid,
    [Parameter(Mandatory)][string]$Jar,
    [Parameter(Mandatory)][string]$NewJar,
    [Parameter(Mandatory)][string]$Launcher,
    [int]$StartupSecs = 20,
    [int]$HubWaitSecs = 40,
    [string]$Log = ''
)
$ErrorActionPreference = 'Stop'

# One line the app shows on its next start ("last-result.txt" beside the log): failed / rolled back / ok.
function Write-Result([string]$m) {
    if ($Log) { try { Set-Content -LiteralPath (Join-Path (Split-Path -Parent $Log) 'last-result.txt') -Value $m } catch { } }
}

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

    # 2. the background hub may still hold the jar. An auto-started hub quits by itself a few seconds after its
    #    last client leaves, and its shutdown hook un-keys the transmitter - so wait for that. Never force-kill it:
    #    Windows has no graceful stop for it, and a hard kill would skip that hook.
    if (-not (Wait-Unlocked $Jar $HubWaitSecs)) {
        throw 'the background hub is still running the old version (a hub started with --hub stays up until quit). Quit it from the system tray, then run the update again'
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
        Write-Result 'rolled back: the new version did not start, so the previous version was restored'
        exit 2
    }
    Write-Log 'new version is running; previous jar kept as .bak'
    Write-Result 'ok'
    exit 0
}
catch {
    Write-Log "update failed: $($_.Exception.Message)"
    Write-Result "failed: $($_.Exception.Message)"
    # leave the user with a working app: relaunch whatever jar is installed
    try { if (Test-Path -LiteralPath $Jar) { [void](Start-App) } } catch { }
    exit 1
}
