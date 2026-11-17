# Local-only version history helper (no remotes, no internet, no auto-update).
# Usage:  tools\snap.bat <command> [arg]
#   save [message]   Save a snapshot of every change right now
#   list [count]     Show recent snapshots (1 = newest)
#   show <number>    Inspect what a snapshot changed
#   diff             Show what is changed but not saved yet
#   back <number>    Return the project to that snapshot (saves current work first)
#   status           Short git status
#   help             This text

param(
    [Parameter(Position = 0)]
    [string]$Command = 'help',

    [Parameter(Position = 1)]
    [string]$Argument = ''
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot

function Write-Utf8File([string]$path, [string]$text) {
    [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))
}

function Invoke-GitCommit([string]$message) {
    $file = Join-Path $env:TEMP ("snapmsg_" + [guid]::NewGuid().ToString("N") + ".txt")
    Write-Utf8File $file $message
    try {
        git commit -q -F $file
    }
    finally {
        Remove-Item $file -Force -ErrorAction SilentlyContinue
    }
}

function Get-Versions([int]$count = 15) {
    $format = '%h%x09%ad%x09%s'
    $lines = & git log "--pretty=format:$format" '--date=format:%Y-%m-%d %H:%M' "-n$count"
    $list = @()
    $index = 0
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $parts = $line -split "`t", 3
        $index++
        $list += [pscustomobject]@{
            Index   = $index
            Hash    = $parts[0]
            Date    = $parts[1]
            Subject = $parts[2]
        }
    }
    return $list
}

function Resolve-Version([string]$value) {
    $number = 0
    if ([int]::TryParse($value, [ref]$number)) {
        $items = @(Get-Versions 100)
        if ($number -lt 1 -or $number -gt $items.Count) {
            throw "No snapshot number $number. Run: snap.bat list"
        }
        return $items[$number - 1]
    }

    # Also accept a commit hash (e.g. the one printed as an undo hint).
    $resolved = (& git rev-parse --verify --short=7 "$value^{commit}" 2>$null) -join ''
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolved)) {
        throw "Unknown snapshot '$value'. Give a number from: snap.bat list"
    }
    $subject = (& git log -1 --pretty=format:'%s' $resolved) -join ''
    $date = (& git log -1 '--date=format:%Y-%m-%d %H:%M' --pretty=format:'%ad' $resolved) -join ''
    return [pscustomobject]@{
        Index   = $null
        Hash    = $resolved
        Date    = $date
        Subject = $subject
    }
}

function Get-VersionLabel($version) {
    if ($null -ne $version.Index) {
        return ('snapshot #{0} ({1})' -f $version.Index, $version.Hash)
    }
    return ('snapshot {0}' -f $version.Hash)
}

function Test-CleanTree {
    $changes = & git status --porcelain
    return [string]::IsNullOrWhiteSpace(($changes -join ''))
}

switch ($Command.ToLower()) {

    'save' {
        $message = $Argument
        & git add -A
        $staged = (& git diff --cached --name-only) -join ''
        if ([string]::IsNullOrWhiteSpace($staged)) {
            Write-Host 'Nothing new to save - the project already matches the last snapshot.'
            exit 0
        }
        if ([string]::IsNullOrWhiteSpace($message)) {
            $message = 'snapshot ' + (Get-Date -Format 'yyyy-MM-dd HH:mm')
        }
        Invoke-GitCommit $message
        if ($LASTEXITCODE -ne 0) { throw 'git commit failed.' }
        Write-Host ('Saved: ' + ((& git log -1 --pretty=format:'%h %s') -join ''))
    }

    'list' {
        $count = 15
        if (-not [string]::IsNullOrWhiteSpace($Argument)) {
            [void][int]::TryParse($Argument, [ref]$count)
        }
        if ($count -lt 1) { $count = 15 }
        $items = @(Get-Versions $count)
        if ($items.Count -eq 0) { Write-Host 'No snapshots yet.'; exit 0 }
        Write-Host ''
        Write-Host '  #   hash      date               commit'
        Write-Host '  --  --------  -----------------  ------------------------------------------'
        foreach ($item in $items) {
            Write-Host ('  {0,-2}  {1,-8}  {2,-17}  {3}' -f $item.Index, $item.Hash, $item.Date, $item.Subject)
        }
        Write-Host ''
        Write-Host 'Restore any of them with:  tools\snap.bat back <number>'
    }

    'show' {
        if ([string]::IsNullOrWhiteSpace($Argument)) { throw 'Usage: snap.bat show <number>' }
        $target = Resolve-Version $Argument
        Write-Host ''
        Write-Host ("{0}  {1}" -f (Get-VersionLabel $target), $target.Subject)
        Write-Host ''
        & git show --stat --oneline $target.Hash
    }

    'diff' {
        $changes = (& git status --short) -join "`n"
        if ([string]::IsNullOrWhiteSpace($changes)) {
            Write-Host 'Nothing changed since the last snapshot.'
            exit 0
        }
        & git status --short
        Write-Host ''
        & git diff --stat
    }

    'status' {
        & git status --short --branch
    }

    'back' {
        if ([string]::IsNullOrWhiteSpace($Argument)) { throw 'Usage: snap.bat back <number>' }
        $target = Resolve-Version $Argument
        Write-Host ''
        Write-Host ("Restoring {0}: {1}" -f (Get-VersionLabel $target), $target.Subject)
        Write-Host ("  date: {0}" -f $target.Date)
        Write-Host ''
        Write-Host 'Your current work will be saved as a separate snapshot first, so nothing is lost.'
        $answer = Read-Host 'Continue? (y/n)'
        if ($answer -notmatch '^(y|yes)$') {
            Write-Host 'Cancelled.'
            exit 0
        }

        $checkpointHash = ''
        if (-not (Test-CleanTree)) {
            & git add -A
            Invoke-GitCommit ('wip: work before restoring ' + $target.Hash)
            $checkpointHash = (& git rev-parse --short=7 HEAD) -join ''
            Write-Host ('Current work saved as snapshot {0}.' -f $checkpointHash)
        }

        # Keep this helper available even when restoring a snapshot that predates it.
        $toolsDir = Join-Path $repoRoot 'tools'
        $toolsBackupRoot = Join-Path $env:TEMP ('snap_tools_' + [guid]::NewGuid().ToString('N'))
        $hasTools = Test-Path $toolsDir
        if ($hasTools) {
            New-Item -ItemType Directory -Path $toolsBackupRoot -Force | Out-Null
            Copy-Item $toolsDir (Join-Path $toolsBackupRoot 'tools') -Recurse -Force
        }

        try {
            & git read-tree -u --reset $target.Hash
            if ($LASTEXITCODE -ne 0) { throw 'Could not restore that snapshot (existing changes block it).' }

            if ($hasTools) {
                Copy-Item (Join-Path $toolsBackupRoot 'tools') $repoRoot -Recurse -Force
                & git add -A -- tools
            }

            if (Test-CleanTree) {
                Write-Host ('Already at {0}.' -f (Get-VersionLabel $target))
                exit 0
            }

            Invoke-GitCommit ('restore: ' + $target.Hash + ' ' + $target.Subject)
        }
        finally {
            if (Test-Path $toolsBackupRoot) { Remove-Item $toolsBackupRoot -Recurse -Force -ErrorAction SilentlyContinue }
        }

        Write-Host ('Done. Project is back at {0}.' -f (Get-VersionLabel $target))
        if (-not [string]::IsNullOrWhiteSpace($checkpointHash)) {
            Write-Host ('To undo this restore:  tools\snap.bat back {0}' -f $checkpointHash)
        }
    }

    default {
        Write-Host ''
        Write-Host 'Sukoon local version history (git stays on this PC only - no remote, no internet)'
        Write-Host ''
        Write-Host '  tools\snap.bat save "what I changed"   save a snapshot now'
        Write-Host '  tools\snap.bat list                    show recent snapshots (1 = newest)'
        Write-Host '  tools\snap.bat show 3                  inspect snapshot #3'
        Write-Host '  tools\snap.bat diff                    what is unsaved right now'
        Write-Host '  tools\snap.bat back 3                  return to snapshot #3 safely'
        Write-Host '  tools\snap.bat status                  short git status'
        Write-Host ''
        Write-Host 'Rules of thumb:'
        Write-Host '  - Save before every big edit, and after every working build.'
        Write-Host '  - "back" always saves your current work first, so it is reversible.'
        Write-Host '  - After "back", snapshot numbers shift; run "list" again to see them.'
        Write-Host ''
    }
}
