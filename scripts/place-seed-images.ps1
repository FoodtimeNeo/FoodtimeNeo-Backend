# 将种子图片铺入应用 uploads/ 目录（须从本 Backend 目录执行）。
# 遍历 seed-images/*/ 下每个 <uuid>.jpg，生成 uploads/<uuid>.img + <uuid>.properties。
# owner 为 V5 种子用户（已禁用，仅满足服务端文件归属读取）；食堂封面无外键要求。
$ErrorActionPreference = 'Stop'
$seedUser = '39d46f72-3ea4-57b9-a25f-8d3a6779e84d'
$root = Split-Path -Parent $PSScriptRoot
$dst = Join-Path $root 'uploads'
New-Item -ItemType Directory -Path $dst -Force | Out-Null
$n = 0
Get-ChildItem -LiteralPath (Join-Path $root 'seed-images') -Directory | ForEach-Object {
  Get-ChildItem -LiteralPath $_.FullName -Filter '*.jpg' | ForEach-Object {
    $id = $_.BaseName
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $dst ($id + '.img')) -Force
    Set-Content -LiteralPath (Join-Path $dst ($id + '.properties')) -Value (
      'owner=' + $seedUser + "`n" + 'contentType=image/jpeg') -Encoding Ascii
    $n++
  }
}
Write-Output "placed $n seed images into $dst"
