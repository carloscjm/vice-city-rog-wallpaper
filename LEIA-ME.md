# Vice City ROG • Kit visual v1

Criado a partir da imagem e do vídeo fornecidos pelo usuário, com ícones originais inspirados nas cores tropicais de Vice City.

## Conteúdo

- `01_Wallpaper/home_GTA6_1080x2448.jpg`: imagem estática centralizada e cortada para 1080×2448 (a proporção oficial da tela do ROG Phone 5)
- `01_Wallpaper/home_GTA6_escurecido_1080x2448.jpg`: alternativa com leve escurecimento no rodapé, caso os ícones fiquem pouco legíveis
- `01_Wallpaper/imagem_original.jpg`: imagem intacta como foi enviada
- `02_Bloqueio_video/video_bloqueio_720x1632_sem_audio.mp4`: vídeo otimizado (24 FPS, sem áudio), recomendado para teste
- `02_Bloqueio_video/video_bloqueio_1080x2448_sem_audio.mp4`: versão em resolução da tela (ampliada do vídeo original)
- `02_Bloqueio_video/video_original.mp4`: vídeo original, intacto
- `03_Icones_256px/`: 16 ícones individuais PNG e a prancha original
- `04_Previews/comparacao_home_bloqueio.jpg`: prévia ilustrativa, não captura real do aparelho
- `05_Instalacao/enviar_para_celular.ps1`: script PowerShell opcional para copiar dois arquivos para o telefone via ADB

## Usar a imagem de fundo

Envie `home_GTA6_1080x2448.jpg` ao celular e abra pela Galeria. Escolha definir como papel de parede **da tela inicial** (não a tela de bloqueio). Os nomes das opções podem variar conforme a interface do aparelho.

## Usar o vídeo de bloqueio

Este ZIP **não contém aplicativo instalado** nem consegue tornar o vídeo animado automaticamente. Primeiro verifique se a Galeria da ASUS permite definir um vídeo como papel de parede da tela de bloqueio. Se não permitir, será necessário criar e instalar um Live Wallpaper em APK próprio; o comportamento exclusivo no bloqueio terá de ser testado no Android 13/ROG Phone 5.

Evite aplicativos aleatórios de vídeo-wallpaper com permissões invasivas. O arquivo MP4 aqui já foi preparado para uso por um serviço nativo de Live Wallpaper que poderá ser desenvolvido na próxima etapa.

## Aplicar os ícones

Os arquivos PNG **não substituem ícones automaticamente**. O ASUS Launcher pode não reconhecer pacotes não oficiais diretamente. Para aplicar em massa sem mudar de launcher, precisaremos empacotar os ícones num APK de icon pack compatível e testar o aplicativo de código aberto `ASUS Icon Pack Applyer` (https://github.com/Peketr/asusiconpackapply). Compatibilidade ainda não confirmada nesse aparelho; não há APK de ícones pronto neste kit. Também podemos consultar as activities reais dos seus aplicativos via ADB antes de gerar o mapa appfilter.xml.

## Copiar arquivos pelo ADB (alternativa)

Com este pacote extraído em `C:\platform-tools\vice-city-rog` e o telefone conectado:

```powershell
.\adb push ".\vice-city-rog\01_Wallpaper\home_GTA6_1080x2448.jpg" /sdcard/Download/home_GTA6.jpg
.\adb push ".\vice-city-rog\02_Bloqueio_video\video_bloqueio_720x1632_sem_audio.mp4" /sdcard/Download/bloqueio_GTA6.mp4
```

Depois localize os arquivos em Downloads no telefone. **Copiar não aplica nem instala tema.**

## Economia de bateria

O vídeo tem cerca de 10 segundos em 24 FPS, sem áudio. Para consumo baixo, qualquer eventual serviço Live Wallpaper deve parar reprodução quando a tela apagar ou quando não estiver visível. O gasto final precisa ser testado no telefone.
