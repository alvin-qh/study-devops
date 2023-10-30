function New-Path {
    param($Path)

    New-Item -ItemType "directory" -Path $Path -Force | Out-Null
}

function Test-PathExist {
    param($Path)

    return Test-Path -Path $Path
}

function Remove-Path {
    param($Path)

    Remove-Item -Path $Path -Recurse -Force -ErrorAction SilentlyContinue | Out-Null
}

function Test-FileExist {
    param($FileName)

    return Test-Path -Path $FileName -PathType Leaf
}

function Main {
    $testPath = "./_test"

    try {
        New-Path -Path "$testPath"
        Write-Host "Path ""$testPath"" exist? $(Test-PathExist -Path $testPath)"
    }
    finally {
        Remove-Path -Path "$testPath"
    }
}

Main
