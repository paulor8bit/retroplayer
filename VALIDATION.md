# Validação — RetroPlayer Together 2.0

- `assembleDebug` e `lintDebug`: passaram no Termux com Gradle 8.7, Android SDK 34 e AAPT2 ARM64.
- Lint: **0 erros, 15 avisos**. Os avisos incluem internacionalização, ABI ARM64, backup, wakelock e código de desenho legado.
- **82 verificações Java passaram**: 20 de HTTP/mídia/chat/acesso; 14 de importação/favoritos; 19 de letras/timestamps/links YouTube; 29 de convites, fila colaborativa, uploads e áudio ao vivo.
- **4 suítes JavaScript passaram**: sincronização de mídia local; YouTube; captura ao vivo; contribuições da fila.
- O servidor HTTP real foi executado no JVM com adaptadores de arquivo/serviço: links YouTube recebidos, fila consultada, arquivo enviado com bytes preservados, rejeição de tipos inválidos e POST entre origens, reservas liberadas, endpoint PCM com bytes preservados e revogação da sala.
- O fan-out de áudio foi testado com dados PCM sintéticos: quatro ouvintes, desconexão de cliente lento e fechamento dos ouvintes.
- O script real de áudio ao vivo foi testado com adaptador Web Audio: ativação por gesto, remontagem de frames partidos, decodificação estéreo little-endian, volume, mudança de fonte e encerramento.
- Contribuições do convidado: nomes renderizados como texto, POST de link, cabeçalhos do arquivo/nome, progresso, limite de tamanho e cancelamento ao fechar a sala.
- HTML: IDs únicos e referências dos scripts existentes. Manifesto, tema e ícones XML conferidos.
- APK: **2.0, código 5**, identificador `br.com.retroplayer`, Android 8+, ARM64. Assinatura v2 verificada, com o mesmo certificado da versão anterior.
- Página atual da sala, nova arte e classes de captura/fila/convites conferidas dentro do APK.
- APK atualizado em `apk/RetroPlayer.apk` e copiado para `/sdcard/Download/RetroPlayer.apk`, com SHA256 idêntico.
- `git diff --check`: passou.

## Limites dos testes

Não há dispositivo conectado por ADB nem navegador headless instalado. O layout Android/HTML não foi verificado por captura de tela em execução. A arte gerada foi inspecionada como imagem.

A captura real de outro aplicativo, os diálogos MediaProjection/RECORD_AUDIO, o serviço em segundo plano com tela apagada, o seletor de arquivos dentro do WebView, a abertura de convites pelo Android e a reprodução/sincronia entre dois aparelhos **ainda precisam de teste em aparelhos reais**. Os testes JavaScript usam adaptadores; não consultam YouTube nem capturam áudio de um aplicativo Android. O túnel TryCloudflare não foi reconectado para um ensaio ponta a ponta nesta entrega. A busca LRCLIB ao vivo também não foi repetida.

A implementação usa AudioPlaybackCapture no Android 10+ e respeita a política do aplicativo de origem: pode receber silêncio quando o app bloqueia captura. Não captura o microfone nem cria gravação de tela. O áudio ao vivo usa PCM estéreo 44,1 kHz com até quatro ouvintes e cerca de 11 MB/min por ouvinte.

A fila aceita uploads de até 100 MiB, com limite total de 300 MiB e dois envios simultâneos. Os arquivos completos recebidos ficam no aparelho do anfitrião até a remoção da faixa/lista. O anfitrião mantém o controle de reprodução.

O anfitrião toca YouTube pelo site dentro do WebView; os convidados usam o player incorporado oficial, cada um na sua conexão. Não foi portado InnerTube. Restrições do YouTube, anúncios e políticas de segundo plano continuam se aplicando.

SHA256 do APK: `7bae665b35e1257d40ad0e268fa1e42eb3488e5212bfc4c83b721b2e78aa91f0`.
