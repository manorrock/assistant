# Manorrock Coding Assistant - Auto-completion script for PowerShell
# This script provides tab completion for the Manorrock Coding Assistant CLI on Windows
#
# Installation instructions:
# 1. Create or edit your PowerShell profile:
#    - First check if you have a profile: Test-Path $PROFILE
#    - If it returns False, create it: New-Item -Path $PROFILE -Type File -Force
# 2. Add the following line to your PowerShell profile:
#    . C:\path\to\manorrock-coding-assistant-completion.ps1
# 3. Restart PowerShell or run: . $PROFILE

# Define the completion function
Register-ArgumentCompleter -CommandName 'manorrock-coding-assistant.sh', 'manorrock-coding-assistant' -ScriptBlock {
    param($wordToComplete, $commandAst, $cursorPosition)
    
    # Get all command elements
    $commandElements = $commandAst.CommandElements
    
    # If there's only one element (the script name itself), suggest commands
    if ($commandElements.Count -eq 1) {
        $commands = @('analyze', 'debug', 'fix', 'generate', 'document', 'review', 'refactor', 'context', 'help')
        return $commands | Where-Object { $_ -like "$wordToComplete*" } | ForEach-Object {
            [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterValue', $_)
        }
    }
    
    # Get the command (first argument after script name)
    $command = $commandElements[1].Value
    
    # If completing the second argument
    if ($commandElements.Count -eq 2 -and $wordToComplete -ne '') {
        # For file-based commands, suggest files and directories
        switch ($command) {
            { $_ -in @('analyze', 'debug', 'fix', 'document', 'review', 'refactor') } {
                $fileExtensions = @('.java', '.py', '.js', '.ts', '.jsx', '.tsx', '.c', '.cpp', '.h', '.hpp', 
                                    '.rb', '.php', '.cs', '.go', '.rs', '.swift', '.kt', '.scala', '.html', 
                                    '.css', '.json', '.yaml', '.yml', '.xml', '.sql', '.md', '.sh')
                
                # Get files and directories
                $items = Get-ChildItem -Path (if ($wordToComplete) { $wordToComplete + '*' } else { '.' }) -ErrorAction SilentlyContinue
                
                # Format directories
                $directories = $items | Where-Object { $_.PSIsContainer } | ForEach-Object {
                    $completionText = $_.FullName -replace '\\', '/'
                    if ($completionText -match ' ') { $completionText = "'$completionText'" }
                    [System.Management.Automation.CompletionResult]::new($completionText, $_.Name, 'ParameterValue', $_.Name + '/')
                }
                
                # Format files with appropriate extensions
                $files = $items | Where-Object { -not $_.PSIsContainer -and $_.Extension -in $fileExtensions } | ForEach-Object {
                    $completionText = $_.FullName -replace '\\', '/'
                    if ($completionText -match ' ') { $completionText = "'$completionText'" }
                    [System.Management.Automation.CompletionResult]::new($completionText, $_.Name, 'ParameterValue', $_.Name)
                }
                
                return @($directories) + @($files)
            }
            'context' {
                $subcommands = @('add', 'list', 'clear', 'use')
                return $subcommands | Where-Object { $_ -like "$wordToComplete*" } | ForEach-Object {
                    [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterValue', $_)
                }
            }
            'implement-issue' {
                $subcommands = @('file', 'clipboard')
                return $subcommands | Where-Object { $_ -like "$wordToComplete*" } | ForEach-Object {
                    [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterValue', $_)
                }
            }
            'verify-implementation' {
                # Return files and directories for first argument of verify-implementation
                $items = Get-ChildItem -Path (if ($wordToComplete) { $wordToComplete + '*' } else { '.' }) -ErrorAction SilentlyContinue
                
                # Format directories
                $directories = $items | Where-Object { $_.PSIsContainer } | ForEach-Object {
                    $completionText = $_.FullName -replace '\\', '/'
                    if ($completionText -match ' ') { $completionText = "'$completionText'" }
                    [System.Management.Automation.CompletionResult]::new($completionText, $_.Name, 'ParameterValue', $_.Name + '/')
                }
                
                # Format files, preferring markdown and text files
                $mdFiles = $items | Where-Object { -not $_.PSIsContainer -and ($_.Extension -eq '.md' -or $_.Extension -eq '.txt') } | ForEach-Object {
                    $completionText = $_.FullName -replace '\\', '/'
                    if ($completionText -match ' ') { $completionText = "'$completionText'" }
                    [System.Management.Automation.CompletionResult]::new($completionText, $_.Name, 'ParameterValue', $_.Name)
                }
                
                return @($directories) + @($mdFiles)
            }
            default {
                # No completion for other commands
                return $null
            }
        }
    }
    
    # For the context add subcommand, suggest files
    if ($command -eq 'context' -and $commandElements.Count -eq 3 -and $commandElements[2].Value -eq 'add') {
        $items = Get-ChildItem -Path (if ($wordToComplete) { $wordToComplete + '*' } else { '.' }) -ErrorAction SilentlyContinue
        
        # Format directories
        $directories = $items | Where-Object { $_.PSIsContainer } | ForEach-Object {
            $completionText = $_.FullName -replace '\\', '/'
            if ($completionText -match ' ') { $completionText = "'$completionText'" }
            [System.Management.Automation.CompletionResult]::new($completionText, $_.Name, 'ParameterValue', $_.Name + '/')
        }
        
        # Format files
        $files = $items | Where-Object { -not $_.PSIsContainer } | ForEach-Object {
            $completionText = $_.FullName -replace '\\', '/'
            if ($completionText -match ' ') { $completionText = "'$completionText'" }
            [System.Management.Automation.CompletionResult]::new($completionText, $_.Name, 'ParameterValue', $_.Name)
        }
        
        return @($directories) + @($files)
    }
    
    # For all other cases, no completion
    return $null
}

Write-Host "Manorrock Coding Assistant tab completion loaded for PowerShell." -ForegroundColor Green