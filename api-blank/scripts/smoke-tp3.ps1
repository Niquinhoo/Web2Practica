param([Parameter(Mandatory)][string]$BaseUrl)
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
function Request([string]$Method, [string]$Path, [int]$Expected, $Body = $null, [string]$Token = '') {
    $options = @{Uri="$BaseUrl$Path"; Method=$Method; SkipHttpErrorCheck=$true; TimeoutSec=180}
    if ($null -ne $Body) { $options.ContentType='application/json'; $options.Body=$Body | ConvertTo-Json -Compress }
    if ($Token) { $options.Headers=@{Authorization="Bearer $Token"} }
    $response = Invoke-WebRequest @options
    if ([int]$response.StatusCode -ne $Expected) { throw "$Method $Path devolvió $($response.StatusCode); esperado $Expected" }
    if ($response.Content) { return $response.Content | ConvertFrom-Json }
}
$health = Request GET '/health' 200
if ($health.status -ne 'ok') { throw 'Health inválido' }
$null = Request GET '/api/listas' 401
$tag = [Guid]::NewGuid().ToString('N')
$password = [Guid]::NewGuid().ToString('N')
$accounts = @()
foreach ($suffix in @('dueno','otro')) {
    $credentials = @{email="tp3-smoke-$tag-$suffix@example.com";password=$password}
    $registered = Request POST '/api/auth/registro' 201 $credentials
    if ($registered.rol -ne 'USER') { throw 'El registro no asignó USER' }
    $login = Request POST '/api/auth/login' 200 $credentials
    $accounts += $login.token
}
$list = $null
$favorite = $null
try {
    $list = Request POST '/api/listas' 201 @{nombre="Smoke $tag"} $accounts[0]
    $null = Request GET "/api/listas/$($list.id)" 200 $null $accounts[0]
    $null = Request GET "/api/listas/$($list.id)" 403 $null $accounts[1]
    $favorite = Request POST '/api/favoritos' 201 @{productoId=1;listaId=$list.id} $accounts[0]
    $null = Request GET "/api/favoritos/$($favorite.id)" 403 $null $accounts[1]
} finally {
    if ($favorite) { $null = Request DELETE "/api/favoritos/$($favorite.id)" 204 $null $accounts[0] }
    if ($list) { $null = Request DELETE "/api/listas/$($list.id)" 204 $null $accounts[0] }
}
[ordered]@{url=$BaseUrl;fechaUtc=[DateTime]::UtcNow.ToString('o');health=200;sinToken=401;registro=201;login=200;listaPropia=200;listaAjena=403;favoritoAjeno=403;limpiezaRecursos=204;usuariosDePrueba="tp3-smoke-$tag-*"} | ConvertTo-Json
