# Vice City ROG - SOMENTE COPIA DE ARQUIVOS, sem instalar apps ou alterar o sistema
$ErrorActionPreference = 'Stop'
$Adb = Join-Path $PSScriptRoot '..\..\adb.exe'
if (!(Test-Path $Adb)) { $Adb = 'adb' }
& $Adb devices
& $Adb shell mkdir -p /sdcard/Download/ViceCityROG
& $Adb push (Join-Path $PSScriptRoot '..\\01_Wallpaper\\home_GTA6_1080x2448.jpg') /sdcard/Download/ViceCityROG/home_GTA6.jpg
& $Adb push (Join-Path $PSScriptRoot '..\\02_Bloqueio_video\\video_bloqueio_720x1632_sem_audio.mp4') /sdcard/Download/ViceCityROG/bloqueio_GTA6.mp4
Write-Host 'Arquivos copiados para Downloads/ViceCityROG no aparelho. Nenhum papel de parede foi aplicado automaticamente.'
