[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ProjectId,

    [ValidateNotNullOrEmpty()]
    [string]$Region = "asia-east1",

    [ValidateNotNullOrEmpty()]
    [string]$ServiceName = "sage-agent-backend",

    [ValidateNotNullOrEmpty()]
    [string]$Model = "gpt-5.6-terra",

    [string]$ImageModel = "gpt-image-2.5-sunburst",

    [switch]$EnableZine,

    [switch]$SkipAndroidConfiguration
)

$ErrorActionPreference = "Stop"

function Resolve-Gcloud {
    $command = Get-Command "gcloud.cmd" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $localInstall = Join-Path $env:LOCALAPPDATA "Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
    if (Test-Path -LiteralPath $localInstall) {
        return $localInstall
    }

    throw "Google Cloud CLI was not found. Install Google.CloudSDK with winget first."
}

function Invoke-Gcloud {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    & $script:GcloudPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: $($Arguments -join ' ')"
    }
}

function Ensure-Secret {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )

    & $script:GcloudPath secrets describe $Name --project $ProjectId --format="value(name)" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Invoke-Gcloud secrets create $Name --project $ProjectId --replication-policy="automatic" --quiet
    }

    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    $tempPath = Join-Path $tempRoot ("sage-cloud-secret-" + [Guid]::NewGuid().ToString("N") + ".txt")
    $resolvedTempPath = [IO.Path]::GetFullPath($tempPath)
    if (-not $resolvedTempPath.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to create a secret file outside the temporary directory."
    }

    try {
        [IO.File]::WriteAllText($resolvedTempPath, $Value, [Text.UTF8Encoding]::new($false))
        Invoke-Gcloud secrets versions add $Name --project $ProjectId --data-file=$resolvedTempPath --quiet
    }
    finally {
        if (Test-Path -LiteralPath $resolvedTempPath) {
            Remove-Item -LiteralPath $resolvedTempPath -Force
        }
    }
}

function Read-SecretText {
    param([Parameter(Mandatory = $true)][string]$Prompt)

    $secureValue = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Set-LocalProperty {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )

    $lines = if (Test-Path -LiteralPath $Path) { [Collections.Generic.List[string]](Get-Content $Path) } else { [Collections.Generic.List[string]]::new() }
    $prefix = "$Name="
    $found = $false
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index].StartsWith($prefix, [StringComparison]::Ordinal)) {
            $lines[$index] = "$prefix$Value"
            $found = $true
        }
    }
    if (-not $found) {
        $lines.Add("$prefix$Value")
    }
    [IO.File]::WriteAllLines($Path, $lines, [Text.UTF8Encoding]::new($false))
}

$GcloudPath = Resolve-Gcloud
$activeAccount = & $GcloudPath auth list --filter="status:ACTIVE" --format="value(account)"
if ($LASTEXITCODE -ne 0 -or -not $activeAccount) {
    throw "No active Google Cloud account. Run 'gcloud auth login' first."
}

$billingEnabled = & $GcloudPath billing projects describe $ProjectId --format="value(billingEnabled)"
if ($LASTEXITCODE -ne 0 -or $billingEnabled.Trim().ToLowerInvariant() -ne "true") {
    throw "Billing is not enabled for project '$ProjectId'. Enable billing in Google Cloud Console first."
}

Invoke-Gcloud config set project $ProjectId --quiet
Invoke-Gcloud services enable `
    run.googleapis.com `
    cloudbuild.googleapis.com `
    artifactregistry.googleapis.com `
    secretmanager.googleapis.com `
    iam.googleapis.com `
    --project $ProjectId

$openAiApiKey = Read-SecretText "OpenAI API Key (input is hidden)"
if ([string]::IsNullOrWhiteSpace($openAiApiKey)) {
    throw "OpenAI API Key cannot be empty."
}

$authBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($authBytes)
$backendAuthToken = [Convert]::ToBase64String($authBytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")

Ensure-Secret -Name "sage-openai-api-key" -Value $openAiApiKey
Ensure-Secret -Name "sage-backend-auth-token" -Value $backendAuthToken
$openAiApiKey = $null

$runtimeServiceAccountName = "sage-agent-runtime"
$runtimeServiceAccount = "$runtimeServiceAccountName@$ProjectId.iam.gserviceaccount.com"
& $GcloudPath iam service-accounts describe $runtimeServiceAccount --project $ProjectId 2>$null
if ($LASTEXITCODE -ne 0) {
    Invoke-Gcloud iam service-accounts create $runtimeServiceAccountName `
        --project $ProjectId `
        --display-name="SAGE Agent Cloud Run runtime"
}
foreach ($secretName in @("sage-openai-api-key", "sage-backend-auth-token")) {
    Invoke-Gcloud secrets add-iam-policy-binding $secretName `
        --project $ProjectId `
        --member="serviceAccount:$runtimeServiceAccount" `
        --role="roles/secretmanager.secretAccessor" `
        --quiet
}

Push-Location $PSScriptRoot
try {
    $zineEnabledValue = $EnableZine.IsPresent.ToString().ToLowerInvariant()
    Invoke-Gcloud run deploy $ServiceName `
        --project $ProjectId `
        --source "." `
        --region $Region `
        --service-account=$runtimeServiceAccount `
        --allow-unauthenticated `
        --ingress="all" `
        --port 8000 `
        --cpu 1 `
        --memory="512Mi" `
        --concurrency 20 `
        --min-instances 0 `
        --max-instances 2 `
        --timeout 210 `
        --set-env-vars="SAGE_ENV=production,SAGE_OPENAI_MODEL=$Model,SAGE_OPENAI_REASONING_EFFORT=low,SAGE_RATE_LIMIT_PER_MINUTE=30,SAGE_ZINE_ENABLED=$zineEnabledValue,SAGE_IMAGE_MODEL=$ImageModel" `
        --set-secrets="OPENAI_API_KEY=sage-openai-api-key:latest,SAGE_BACKEND_AUTH_TOKEN=sage-backend-auth-token:latest" `
        --quiet
}
finally {
    Pop-Location
}

$serviceUrl = & $GcloudPath run services describe $ServiceName `
    --project $ProjectId `
    --region $Region `
    --format="value(status.url)"
if ($LASTEXITCODE -ne 0 -or -not $serviceUrl) {
    throw "Deployment finished but the Cloud Run service URL could not be resolved."
}

$health = Invoke-RestMethod "$serviceUrl/healthz"
$readiness = Invoke-RestMethod "$serviceUrl/readyz"
if ($health.status -ne "ok" -or $readiness.status -ne "ready") {
    throw "Cloud Run deployed, but the backend health checks did not pass."
}

if (-not $SkipAndroidConfiguration) {
    $androidProperties = Join-Path (Split-Path $PSScriptRoot -Parent) "local.properties"
    Set-LocalProperty -Path $androidProperties -Name "SAGE_AGENT_BACKEND_URL" -Value $serviceUrl
    Set-LocalProperty -Path $androidProperties -Name "SAGE_AGENT_CLIENT_TOKEN" -Value $backendAuthToken
}

Write-Host "SAGE Agent backend deployed successfully."
Write-Host "Service URL: $serviceUrl"
Write-Host "Health: $($health.status); readiness: $($readiness.status)"
if (-not $SkipAndroidConfiguration) {
    Write-Host "Android local.properties was updated. Rebuild the slim APK to embed the backend URL."
}
