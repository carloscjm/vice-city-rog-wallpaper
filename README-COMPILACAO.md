# Vice City ROG - Compilação do APK

Este projeto implementa um Live Wallpaper para Android 13: reprodução do vídeo na tela bloqueada e exibição da imagem estática após desbloquear, sem root e mantendo o launcher original ASUS.

Os arquivos de imagem/vídeo estão incorporados no módulo `app`, sem buscar mídia na internet nem solicitar permissões de arquivos. As pastas `01_Wallpaper`, `02_Bloqueio_video` etc. já enviadas anteriormente podem permanecer no repositório como referências e não participam da compilação.

## Compilar sem Android Studio

Publique na raiz do repositório (não dentro de uma subpasta) os diretórios `app/` e `.github/workflows/` junto de `build.gradle`, `settings.gradle` e `gradle.properties`. O workflow inicia automaticamente após um commit na branch `main`.

Em GitHub > Actions > Compilar Vice City ROG APK, abra a última execução. Se terminar em verde, vá à seção Artifacts e baixe `ViceCityROG-APK`. Descompacte o arquivo e instale `app-debug.apk` pelo ADB:

```powershell
& "C:\platform-tools\adb.exe" install -r ".\app-debug.apk"
```

Abra o app Vice City ROG Wallpaper no aparelho e toque em **TESTAR E APLICAR WALLPAPER**. Se a interface ASUS permitir, escolha aplicar **somente na tela de bloqueio**. Se não permitir, o wallpaper tenta exibir a imagem estática quando o aparelho estiver desbloqueado.

O APK não altera apps de sistema nem instala os ícones; essa parte é uma etapa separada. Código experimental, ainda não compilado nem testado diretamente no ROG Phone 5.
