param(
    [Parameter(Mandatory = $false)]
    [string]$ProjectRoot = $PSScriptRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

$script:ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
$script:CodexDir = Join-Path $script:ProjectRoot ".codex"
$script:SkillsSource = Join-Path $script:ProjectRoot ".trae\skills"
$script:SkillsTarget = Join-Path $script:CodexDir "skills"
$script:ConfigPath = Join-Path $script:CodexDir "config.toml"
$script:AgentsUrl = "http://aicode.mingyuanyun.com/AGENTS.txt"
$script:AgentsTarget = Join-Path $script:ProjectRoot "AGENTS.md"
$script:LogPath = Join-Path $script:ProjectRoot "init-codex.log"
$script:JarViewerEntry = "C:\mysoftPlugin\MY-jar-viewer\index.js"

$script:ManagedBlockBegin = "# >>> init-codex managed mcp begin >>>"
$script:ManagedBlockEnd = "# <<< init-codex managed mcp end <<<"

function Get-LocalizedText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Base64
    )

    return [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Base64))
}

function Write-Log {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Output $Message
    $line = "[{0}] {1}{2}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message, [Environment]::NewLine
    [System.IO.File]::AppendAllText($script:LogPath, $line, $utf8NoBom)
}

function Ensure-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Get-ManagedMcpBlock {
    return @"
# >>> init-codex managed mcp begin >>>
# Managed by init-codex.cmd. Put custom project settings outside this block.

[mcp_servers.my-jar-viewer]
command = "node"
args = [
  '$($script:JarViewerEntry.Replace("'", "''"))'
]
enabled = true

[mcp_servers.my-knowledge]
url = "https://aiwf.mypaas.com/mcp/qa/sse"
enabled = true

[mcp_servers.my-localmcp]
command = "python"
args = [
  "C:\\mysoftPlugin\\LocalMCP\\server.py",
  "--log-level",
  "DEBUG",
  "--base-url",
  "http://aicode.mingyuanyun.com",
  "--app-id",
  "AIchajian",
  "--app-key",
  "8d6b80e94b4d462c"
]
enabled = true
# <<< init-codex managed mcp end <<<
"@
}

function Update-ProjectConfig {
    Ensure-Directory -Path $script:CodexDir

    $managedBlock = Get-ManagedMcpBlock
    $content = ""

    if (Test-Path -LiteralPath $script:ConfigPath) {
        $content = [System.IO.File]::ReadAllText($script:ConfigPath)
    }

    $beginIndex = $content.IndexOf($script:ManagedBlockBegin, [System.StringComparison]::Ordinal)
    $endIndex = $content.IndexOf($script:ManagedBlockEnd, [System.StringComparison]::Ordinal)

    if ($beginIndex -ge 0 -and $endIndex -ge $beginIndex) {
        $afterIndex = $endIndex + $script:ManagedBlockEnd.Length
        $suffix = ""
        if ($afterIndex -lt $content.Length) {
            $suffix = $content.Substring($afterIndex)
            $suffix = $suffix.TrimStart("`r", "`n")
        }

        $prefix = $content.Substring(0, $beginIndex).TrimEnd("`r", "`n")
        if ([string]::IsNullOrWhiteSpace($prefix)) {
            $updated = $managedBlock
        } elseif ([string]::IsNullOrWhiteSpace($suffix)) {
            $updated = $prefix + [Environment]::NewLine + [Environment]::NewLine + $managedBlock
        } else {
            $updated = $prefix + [Environment]::NewLine + [Environment]::NewLine + $managedBlock + [Environment]::NewLine + [Environment]::NewLine + $suffix
        }
    } elseif ([string]::IsNullOrWhiteSpace($content)) {
        $updated = $managedBlock
    } else {
        $updated = $content.TrimEnd("`r", "`n") + [Environment]::NewLine + [Environment]::NewLine + $managedBlock
    }

    [System.IO.File]::WriteAllText($script:ConfigPath, $updated + [Environment]::NewLine, $utf8NoBom)
}

function Sync-Skills {
    if (-not (Test-Path -LiteralPath $script:SkillsSource)) {
        throw ((Get-LocalizedText "5pyq5om+5Yiw5oqA6IO95rqQ55uu5b2V77yaezB9") -f $script:SkillsSource)
    }

    Ensure-Directory -Path $script:SkillsTarget

    & robocopy.exe $script:SkillsSource $script:SkillsTarget /E /R:1 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
    $robocopyCode = $LASTEXITCODE
    if ($robocopyCode -ge 8) {
        throw ((Get-LocalizedText "5ZCM5q2l5oqA6IO955uu5b2V5aSx6LSl77yMcm9ib2NvcHkg6YCA5Ye656CB77yaezB9") -f $robocopyCode)
    }

    Write-Log ((Get-LocalizedText "5oqA6IO95bey5ZCM5q2l5Yiw77yaezB977yIcm9ib2NvcHk9ezF977yJ") -f $script:SkillsTarget, $robocopyCode)
}

function Download-Agents {
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($null -ne $curl) {
        & $curl.Source -fsSL $script:AgentsUrl -o $script:AgentsTarget | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw ((Get-LocalizedText "5LiL6L29IEFHRU5UUy5tZCDlpLHotKXvvIxjdXJsIOmAgOWHuuegge+8mnswfQ==") -f $LASTEXITCODE)
        }
    } else {
        Invoke-WebRequest -Uri $script:AgentsUrl -OutFile $script:AgentsTarget -UseBasicParsing
    }

    Write-Log ((Get-LocalizedText "QUdFTlRTLm1kIOW3suabtOaWsO+8mnswfQ==") -f $script:AgentsTarget)
}

try {
    [System.IO.File]::WriteAllText($script:LogPath, "", $utf8NoBom)
    Write-Log (Get-LocalizedText "5byA5aeL5Yid5aeL5YyWIENvZGV4IOmhueebrg==")
    Write-Log ((Get-LocalizedText "6aG555uu5qC555uu5b2V77yaezB9") -f $script:ProjectRoot)

    Ensure-Directory -Path $script:CodexDir
    Sync-Skills
    Download-Agents
    Update-ProjectConfig

    Write-Log ((Get-LocalizedText "6aG555uuIE1DUCDphY3nva7lt7LlhpnlhaXvvJp7MH0=") -f $script:ConfigPath)
    Write-Log (Get-LocalizedText "5bey5ZCv55So6aG555uuIE1DUCDmnI3liqHvvJpNWS1qYXItdmlld2Vy44CBTVktS25vd2xlZGdl44CBTVktTG9jYWxNQ1A=")
    Write-Log ((Get-LocalizedText "5Yid5aeL5YyW5a6M5oiQ77yM5pel5b+X5paH5Lu277yaezB9") -f $script:LogPath)
    exit 0
} catch {
    Write-Log ((Get-LocalizedText "5Yid5aeL5YyW5aSx6LSl77yaezB9") -f $_.Exception.Message)
    Write-Log ((Get-LocalizedText "5pel5b+X5paH5Lu277yaezB9") -f $script:LogPath)
    exit 1
}
