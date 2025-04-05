# Manorrock Issue Workflow - PowerShell Script Helpers
# This script provides functions for implementing issues and verifying implementations
# 
# Usage:
# 1. Import this script in your PowerShell session:
#    . C:\path\to\manorrock-issue-workflow.ps1
# 2. Call the functions:
#    Implement-FromIssueFile -IssueFile "C:\path\to\issue.md" -OutputDir "C:\path\to\output"
#    Implement-IssueFromClipboard -OutputDir "C:\path\to\output"
#    Verify-Implementation -IssueFile "C:\path\to\issue.md" -ImplementationDir "C:\path\to\implementation"

# Find CLI JAR
function Find-CliJar {
    $possibleLocations = @(
        "$env:USERPROFILE\.manorrock\assistant\cli.jar",
        "$env:USERPROFILE\.m2\repository\com\manorrock\assistant\cli\*\cli-*-jar-with-dependencies.jar",
        "$pwd\cli\target\cli-*-jar-with-dependencies.jar",
        "$pwd\assistant\cli\target\cli-*-jar-with-dependencies.jar",
        "C:\Program Files\Manorrock Assistant\cli-*-jar-with-dependencies.jar",
        "C:\Program Files (x86)\Manorrock Assistant\cli-*-jar-with-dependencies.jar"
    )

    foreach ($pattern in $possibleLocations) {
        $foundJars = Resolve-Path -Path $pattern -ErrorAction SilentlyContinue
        if ($foundJars -and $foundJars.Count -gt 0) {
            return $foundJars[0].Path
        }
    }

    # If we couldn't find it automatically, ask the user
    Write-Host "Could not locate the Manorrock Assistant CLI JAR file." -ForegroundColor Yellow
    Write-Host "Please provide the path to cli-*-jar-with-dependencies.jar:" -ForegroundColor Yellow
    $jarPath = Read-Host

    if (Test-Path $jarPath) {
        # Save this path for future use
        $configDir = "$env:USERPROFILE\.manorrock\assistant\config"
        if (-not (Test-Path $configDir)) {
            New-Item -Path $configDir -ItemType Directory -Force | Out-Null
        }
        Set-Content -Path "$configDir\cli_jar_path" -Value $jarPath
        return $jarPath
    }
    else {
        Write-Error "Invalid path: $jarPath does not exist."
        return $null
    }
}

# Get CLI JAR path (from config or by searching)
function Get-CliJar {
    $configPath = "$env:USERPROFILE\.manorrock\assistant\config\cli_jar_path"
    if (Test-Path $configPath) {
        $jarPath = Get-Content $configPath
        if (Test-Path $jarPath) {
            return $jarPath
        }
    }
    
    return Find-CliJar
}

# Implement issue from a file
function Implement-FromIssueFile {
    param(
        [Parameter(Mandatory=$true)]
        [string]$IssueFile,
        
        [Parameter(Mandatory=$false)]
        [string]$OutputDir
    )
    
    if (-not (Test-Path $IssueFile)) {
        Write-Error "Error: Issue file not found - $IssueFile"
        return
    }
    
    $cliJar = Get-CliJar
    if (-not $cliJar) {
        return
    }
    
    Write-Host "Implementing issue from file: $IssueFile" -ForegroundColor Cyan
    
    # Prepare command with options
    if ($OutputDir) {
        # Make sure the output directory exists
        if (-not (Test-Path $OutputDir)) {
            New-Item -Path $OutputDir -ItemType Directory -Force | Out-Null
        }
        
        java -jar $cliJar --no-banner "/implement-issue options output_dir=$OutputDir"
        java -jar $cliJar --no-banner "/implement-issue file $IssueFile"
    }
    else {
        # Use default target/test-output directory
        $defaultOutput = Join-Path $pwd "target\test-output"
        if (-not (Test-Path $defaultOutput)) {
            New-Item -Path $defaultOutput -ItemType Directory -Force | Out-Null
        }
        
        java -jar $cliJar --no-banner "/implement-issue options output_dir=$defaultOutput"
        java -jar $cliJar --no-banner "/implement-issue file $IssueFile"
    }
}

# Implement issue from clipboard content
function Implement-IssueFromClipboard {
    param(
        [Parameter(Mandatory=$false)]
        [string]$OutputDir
    )
    
    try {
        $clipboardContent = Get-Clipboard -TextFormatType Text -ErrorAction Stop
    }
    catch {
        Write-Error "Error getting clipboard content: $_"
        return
    }
    
    if ([string]::IsNullOrWhiteSpace($clipboardContent)) {
        Write-Error "Error: Clipboard is empty"
        return
    }
    
    $cliJar = Get-CliJar
    if (-not $cliJar) {
        return
    }
    
    Write-Host "Implementing issue from clipboard content..." -ForegroundColor Cyan
    
    # Create temporary file for clipboard content
    $tempFile = [System.IO.Path]::GetTempFileName()
    Set-Content -Path $tempFile -Value $clipboardContent
    
    # Implement the issue using the file command
    Implement-FromIssueFile -IssueFile $tempFile -OutputDir $OutputDir
    
    # Clean up
    Remove-Item -Path $tempFile -Force
}

# Verify implementation against issue acceptance criteria
function Verify-Implementation {
    param(
        [Parameter(Mandatory=$true)]
        [string]$IssueFile,
        
        [Parameter(Mandatory=$true)]
        [string]$ImplementationDir
    )
    
    if (-not (Test-Path $IssueFile)) {
        Write-Error "Error: Issue file not found - $IssueFile"
        return
    }
    
    if (-not (Test-Path $ImplementationDir)) {
        Write-Error "Error: Implementation directory not found - $ImplementationDir"
        return
    }
    
    $cliJar = Get-CliJar
    if (-not $cliJar) {
        return
    }
    
    Write-Host "Verifying implementation against issue: $IssueFile" -ForegroundColor Cyan
    
    # Create a temporary file to concatenate all implementation files
    $tempFile = [System.IO.Path]::GetTempFileName()
    
    Set-Content -Path $tempFile -Value "# Implementation Files`r`n`r`n"
    
    # Find and concatenate all implementation files, excluding certain directories
    Get-ChildItem -Path $ImplementationDir -Recurse -File | 
        Where-Object { 
            $_.FullName -notmatch "(\\|\/)\." -and 
            $_.FullName -notmatch "(\\|\/)node_modules(\\|\/)" -and 
            $_.FullName -notmatch "(\\|\/)target(\\|\/)" -and 
            $_.FullName -notmatch "(\\|\/)build(\\|\/)" 
        } | 
        ForEach-Object {
            Add-Content -Path $tempFile -Value "`r`n## File: $($_.FullName)`r`n`r`n```"
            Add-Content -Path $tempFile -Value (Get-Content -Path $_.FullName -Raw)
            Add-Content -Path $tempFile -Value "````r`n"
        }
    
    # Get the issue content
    $issueContent = Get-Content -Path $IssueFile -Raw
    
    # Run verification with the implementation content against the issue
    java -jar $cliJar --no-banner "/implement-issue options verify_implementation=true"
    
    # Use temporary file as input to implement-issue command
    $issueContent | java -jar $cliJar --no-banner "/implement-issue"
    
    # Clean up the temporary file
    Remove-Item -Path $tempFile -Force
}

Write-Host "Manorrock Issue Workflow helper functions loaded." -ForegroundColor Green
Write-Host "Available functions:" -ForegroundColor Green
Write-Host "  Implement-FromIssueFile -IssueFile <path> [-OutputDir <path>]" -ForegroundColor Green
Write-Host "  Implement-IssueFromClipboard [-OutputDir <path>]" -ForegroundColor Green
Write-Host "  Verify-Implementation -IssueFile <path> -ImplementationDir <path>" -ForegroundColor Green