$nodeModulePath = Split-Path -Path $app.entrypoint
    while ($nodeModulePath -and (-not(Test-Path -Path "$nodeModulePath\node_modules"))) {
        $nodeModulePath = "$((Get-Item $nodeModulePath).parent.FullName)"
    }


