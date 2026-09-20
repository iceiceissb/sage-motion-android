param([string]$BaseUrl = "")
$ErrorActionPreference = "Stop"

# Read only the two client settings; never print credentials or make a model request.
$clientSettings = @{}
$propertiesPath = Join-Path $PSScriptRoot "local.properties"
if (Test-Path -LiteralPath $propertiesPath) {
    foreach ($line in Get-Content -LiteralPath $propertiesPath) {
        if ($line -match '^\s*(SAGE_AGENT_BACKEND_URL|SAGE_AGENT_CLIENT_TOKEN)\s*=\s*(.*)$') {
            $clientSettings[$Matches[1]] = $Matches[2].Trim()
        }
    }
}
if (-not $BaseUrl) { $BaseUrl = $clientSettings["SAGE_AGENT_BACKEND_URL"] }
if (-not $BaseUrl) { throw "Set SAGE_AGENT_BACKEND_URL in local.properties or pass -BaseUrl." }
$endpoint = [Uri]$BaseUrl
if ($endpoint.Scheme -ne "https" -and -not ($endpoint.Scheme -eq "http" -and $endpoint.IsLoopback)) {
    throw "Use HTTPS for deployed backends; HTTP is only accepted for a local check."
}
if ($endpoint.UserInfo -or $endpoint.Query -or $endpoint.Fragment) { throw "Use a base URL without credentials, query or fragment." }
$headers = @{}
$clientToken = if ($env:SAGE_AGENT_CLIENT_TOKEN) { $env:SAGE_AGENT_CLIENT_TOKEN } else { $clientSettings["SAGE_AGENT_CLIENT_TOKEN"] }
if ($clientToken) { $headers.Authorization = "Bearer $clientToken" }
foreach ($path in @("healthz", "readyz", "v1/capabilities")) {
    try {
        $result = Invoke-RestMethod -Uri ($BaseUrl.TrimEnd('/') + "/" + $path) -Headers $headers -TimeoutSec 12
        [pscustomobject]@{ Endpoint = $path; Reachable = $true; Result = ($result | ConvertTo-Json -Compress) }
    } catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "network_error" }
        [pscustomobject]@{ Endpoint = $path; Reachable = $false; Result = $code }
    }
}
