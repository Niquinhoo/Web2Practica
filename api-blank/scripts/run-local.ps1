param(
    [string]$PostgresDirectory = (Join-Path $env:PUBLIC 'codex-tp2-runtime\pgsql'),
    [string]$DataDirectory = (Join-Path $env:PUBLIC 'codex-tp2-runtime\data'),
    [int]$Port = 55432,
    [string]$JavaDirectory = 'C:\Program Files\Java\jdk-25.0.4',
    [switch]$Verify
)

$ErrorActionPreference = 'Stop'
$taskProject = Split-Path $PSScriptRoot -Parent
$taskPgBin = Join-Path $PostgresDirectory 'bin'
if (!(Test-Path -LiteralPath (Join-Path $taskPgBin 'pg_ctl.exe'))) {
    throw 'Indicá -PostgresDirectory con la carpeta pgsql de los binarios de PostgreSQL. También podés usar Docker según el README.'
}
if (Test-Path -LiteralPath (Join-Path $JavaDirectory 'bin\java.exe')) {
    $env:JAVA_HOME = $JavaDirectory
}
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'release')) -or
        (Get-Content -LiteralPath (Join-Path $env:JAVA_HOME 'release') -Raw) -notmatch 'JAVA_VERSION="25[."]') {
    throw 'Seleccioná un JDK 25 con -JavaDirectory o JAVA_HOME.'
}
$taskTemp = Join-Path $env:PUBLIC 'codex-tp2-temp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$taskTemp".Trim()
if (!$env:POSTGRES_USER) { $env:POSTGRES_USER = 'webii_tp2' }
if (!$env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD = 'webii_tp2' }
$env:PGPASSWORD = $env:POSTGRES_PASSWORD

if (!(Test-Path -LiteralPath (Join-Path $DataDirectory 'PG_VERSION'))) {
    $taskPasswordFile = Join-Path $taskTemp 'initdb-password.txt'
    try {
        [IO.File]::WriteAllText($taskPasswordFile, $env:POSTGRES_PASSWORD)
        & "$taskPgBin\initdb.exe" -D $DataDirectory -U $env:POSTGRES_USER --pwfile=$taskPasswordFile -A scram-sha-256 -E UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo inicializar PostgreSQL.' }
    } finally { Remove-Item -LiteralPath $taskPasswordFile -ErrorAction SilentlyContinue }
}

& "$taskPgBin\pg_ctl.exe" -D $DataDirectory status | Out-Null
if ($LASTEXITCODE -ne 0) {
    $taskPgLog = Join-Path (Split-Path $DataDirectory -Parent) 'postgres.log'
    $taskPgStart = Start-Process -FilePath "$taskPgBin\pg_ctl.exe" -ArgumentList @(
        '-D', "`"$DataDirectory`"", '-l', "`"$taskPgLog`"",
        '-o', "`"-p $Port -h 127.0.0.1`"", '-w', 'start'
    ) -WindowStyle Hidden -PassThru
    if (!$taskPgStart.WaitForExit(60000) -or $taskPgStart.ExitCode -ne 0) {
        throw "No se pudo arrancar PostgreSQL. Revisá $taskPgLog y el puerto."
    }
}

$taskExists = & "$taskPgBin\psql.exe" -h 127.0.0.1 -p $Port -U $env:POSTGRES_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='webii_tp2'"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar a PostgreSQL. Revisá usuario, contraseña y puerto.' }
if ($taskExists -ne '1') {
    & "$taskPgBin\createdb.exe" -h 127.0.0.1 -p $Port -U $env:POSTGRES_USER webii_tp2
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base webii_tp2.' }
}

$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:$Port/webii_tp2"
$env:TEST_DATABASE_URL = $env:DATABASE_URL
$env:TEST_DATABASE_USER = $env:POSTGRES_USER
$env:TEST_DATABASE_PASSWORD = $env:POSTGRES_PASSWORD
Push-Location $taskProject
try {
    if ($Verify) { & .\mvnw.cmd --batch-mode verify }
    else { & .\mvnw.cmd spring-boot:run }
    $taskExitCode = $LASTEXITCODE
} finally { Pop-Location }
exit $taskExitCode
