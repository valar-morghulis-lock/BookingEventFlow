$ErrorActionPreference = "Stop"

$ScriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDirectory

$EnvFile = Join-Path $ProjectRoot ".env"
$TemplateFile = Join-Path $ScriptDirectory "outbox-connector.template.json"

# Load .env
Get-Content $EnvFile |
        Where-Object {
            $_ -and
                    -not $_.Trim().StartsWith("#") -and
                    $_ -match "^\s*[^=]+\s*="
        } |
        ForEach-Object {
            $Name, $Value = $_ -split "=", 2
            [System.Environment]::SetEnvironmentVariable(
                    $Name.Trim(),
                    $Value.Trim()
            )
        }

# Read connector template
$ConnectorJson = Get-Content $TemplateFile -Raw

# Substitute ${VARIABLE} placeholders
$ConnectorJson = [regex]::Replace(
        $ConnectorJson,
        '\$\{([^}]+)\}',
        {
            param($Match)

            $VariableName = $Match.Groups[1].Value
            $VariableValue = [System.Environment]::GetEnvironmentVariable($VariableName)

            if ($null -eq $VariableValue) {
                throw "Environment variable '$VariableName' is not defined."
            }

            return $VariableValue
        }
)

# Validate generated JSON
$ConnectorJson | ConvertFrom-Json | Out-Null

# Register connector
$Response = Invoke-RestMethod `
    -Uri "http://localhost:8083/connectors" `
    -Method Post `
    -ContentType "application/json" `
    -Body $ConnectorJson

$Response | ConvertTo-Json -Depth 20