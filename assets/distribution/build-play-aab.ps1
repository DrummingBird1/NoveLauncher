<#
.SYNOPSIS
  Builds the signed release AAB for Google Play.

.DESCRIPTION
  release.yml builds an APK only, so the bundle Play wants is built here, on
  your machine. Run it from a checkout of the release tag so the bundle matches
  the APK that was released:

      git worktree add ..\play-10.1.0 v10.1.0
      cd ..\play-10.1.0
      .\assets\distribution\build-play-aab.ps1 -Keystore E:\AI\projects\nove-launcher\android\release.keystore

  The keystore and key passwords are typed at the prompts below. They are put
  into environment variables of this PowerShell process only, for the length of
  the build, and are never written to disk or to the repository. Do not paste
  them anywhere else, including into a chat with Claude.

  Needs JDK 21 (JAVA_HOME) and the Android SDK (android\local.properties or
  ANDROID_HOME). Never generate a second release keystore: a different key
  cannot update anything already installed under the existing one.
#>
param(
    # The release keystore is gitignored, so a fresh checkout or worktree does not have it.
    [string]$Keystore = (Join-Path $PSScriptRoot '..\..\android\release.keystore'),
    [string]$Alias = 'Idan'
)

$ErrorActionPreference = 'Stop'

function Read-Secret([string]$Prompt) {
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    [System.Net.NetworkCredential]::new('', $secure).Password
}

function Find-JdkTool([string]$Name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$Name.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $Name
}

if (-not (Test-Path $Keystore)) {
    throw "Keystore not found at '$Keystore'. Pass -Keystore with the path to android\release.keystore."
}

$androidDir = (Resolve-Path (Join-Path $PSScriptRoot '..\..\android')).Path
$aab = Join-Path $androidDir 'app\build\outputs\bundle\release\app-release.aab'

$storePassword = Read-Secret 'Keystore password'
$keyPassword = Read-Secret 'Key password (leave empty if it is the same)'
if (-not $keyPassword) { $keyPassword = $storePassword }

$env:RELEASE_KEYSTORE_PATH = (Resolve-Path $Keystore).Path
$env:RELEASE_KEYSTORE_PASSWORD = $storePassword
$env:RELEASE_KEY_ALIAS = $Alias
$env:RELEASE_KEY_PASSWORD = $keyPassword

Push-Location $androidDir
try {
    & .\gradlew.bat :app:bundleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "bundleRelease failed (exit $LASTEXITCODE)." }
}
finally {
    Pop-Location
    Remove-Item Env:RELEASE_KEYSTORE_PATH, Env:RELEASE_KEYSTORE_PASSWORD, Env:RELEASE_KEY_ALIAS, Env:RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
    $storePassword = $null
    $keyPassword = $null
}

if (-not (Test-Path $aab)) { throw "Build finished but '$aab' was not produced." }

Write-Host "`n--- Signature check ---"
& (Find-JdkTool 'jarsigner') -verify $aab
if ($LASTEXITCODE -ne 0) { throw 'jarsigner could not verify the bundle: it is not signed with the release key.' }
& (Find-JdkTool 'keytool') -printcert -jarfile $aab | Select-String -Pattern 'Owner:|SHA256:|Valid from'

Write-Host "`n--- Bundle ---"
Get-Item $aab | Select-Object FullName, @{ n = 'MB'; e = { [math]::Round($_.Length / 1MB, 2) } }
Get-FileHash $aab -Algorithm SHA256 | Select-Object Hash

Write-Host "`nThe certificate fingerprint above must match the one on the released APK (the project notes record it as starting 9fd148bd and ending 948df8a1)."
Write-Host 'Upload this file to Play Console -> Test and release -> Testing -> Internal testing. Do not attach it to a public GitHub release.'
