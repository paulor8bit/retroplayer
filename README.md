# RetroPlayer Together 2.0

Player Android de música e vídeo com biblioteca local, letras, YouTube Music, salas por convite, fila colaborativa e transmissão do áudio permitido de outros aplicativos.

## Instalar

Baixe o **[APK da versão mais recente](https://github.com/paulor8bit/retroplayer/releases/latest/download/RetroPlayer.apk)** ou use a [cópia no repositório](apk/RetroPlayer.apk). Android 8 ou superior, ARM64. A captura de outros aplicativos exige **Android 10 ou superior**. A versão 2.0 mantém o identificador `br.com.retroplayer` e a assinatura de desenvolvimento das versões anteriores, permitindo atualizar sem desinstalar.

## Uma nova identidade

O app e a sala usam tipografia de sistema, fundo azul-escuro, detalhes em azul-claro, superfícies discretas e controles com áreas de toque amplas. Foram retirados os mostradores decorativos, códigos de barras e mascotes da interface. A nova arte de capa, criada com imagegen, e o ícone circular seguem a mesma identidade. A sala adapta o player, a fila e a conversa para celular e computador.

[Direção visual e prompt da arte](artwork/DESIGN-v2.md).

## Biblioteca e reprodução

Na **Biblioteca**, escolha um arquivo, vários arquivos ou uma pasta com subpastas. Imagens e documentos ficam de fora. Importações rodam fora da interface e podem ser canceladas; arquivos duplicados não são adicionados novamente. A autorização de pastas fica no seletor do Android.

Use **Player** para play/pausa, posição, repetição e modo aleatório. Na Biblioteca, o menu de cada faixa permite reproduzir ou remover; o menu superior mantém o equalizador. Remover uma faixa local não apaga o arquivo original do celular. As cópias de músicas recebidas de convidados são apagadas ao remover a faixa ou limpar a lista.

Links do YouTube Music entram como itens da fila; arquivos e links podem ser misturados. A reprodução dos links YouTube acontece no site dentro do navegador integrado. Se o site exigir interação, use **Abrir YouTube Music** no Player e toque em reproduzir. A busca no catálogo fica em **Explorar**. Favoritos e o navegador continuam disponíveis.

A reprodução de arquivos locais roda no serviço em primeiro plano, com MediaSession e controles na notificação. O segundo plano de sites depende do WebView e das regras de cada site.

## Criar e compartilhar uma sala

1. Abra **Sala** e toque em **Criar / encerrar**.
2. Aguarde **Sala online** e use **Convidar** ou **Copiar link**.
3. O convidado toca em **Entrar e ouvir junto**. Quem tem o RetroPlayer atualizado pode usar **Abrir no app**, ou **Sala → Entrar por link**, para participar dentro do aplicativo.
4. Cada convidado escolhe seu nome, adiciona músicas à fila e conversa no chat. O anfitrião continua controlando play, posição, próxima faixa e remoção de músicas.

O link HTTPS abre em navegadores. O sistema pode oferecer o RetroPlayer como opção de abertura; o botão **Abrir no app** usa o esquema `retroplayer://room?url=…` quando o aplicativo está instalado. Não há domínio próprio com App Links verificados. Entrar em outra sala enquanto hospeda uma sala pede confirmação no aplicativo e encerra a sala anterior. O anfitrião não pode entrar no próprio convite como convidado no mesmo aparelho.

### Fila colaborativa

Na página da sala, em **Qual é a sua escolha?**, o convidado pode:

- Colar um link do YouTube/YouTube Music ou um link HTTPS direto de áudio/vídeo. O título é opcional.
- Usar **Enviar música do celular** para selecionar e enviar um arquivo local, inclusive dentro do RetroPlayer pelo seletor nativo do Android.

As músicas entram no fim da fila, sem interromper a reprodução atual ou uma captura em andamento. A fila exibe fonte e nome de quem adicionou. Os nomes são apelidos sem verificação. O anfitrião pode remover faixas pela Biblioteca.

Arquivos são transferidos para o armazenamento privado do anfitrião. Há limite de **100 MiB por arquivo**, **300 MiB de cópias recebidas**, dois uploads simultâneos e até 200 faixas para novas contribuições. O envio informa progresso. Envios incompletos e interrompidos são descartados; arquivos recebidos permanecem na fila após fechar a sala até serem removidos ou a lista ser limpa. A disponibilidade de espaço e os limites do túnel também se aplicam.

Uma música do YouTube é reproduzida independentemente na conexão de cada participante pelo player incorporado oficial. Não compartilhamos a sessão ou os cookies do anfitrião. Restrições de idade, região, login e incorporação podem impedir a reprodução; anúncios e buffering podem afetar a sincronia. A aplicação não incorpora o mecanismo InnerTube nem o código Kotlin/Compose do [OpenTune](https://github.com/justshivv/Opentune), que foi uma referência para os recursos de música.

### Som de outros aplicativos

1. Crie uma sala no Android 10+.
2. Em **Sala → Som de outros apps**, toque em **Transmitir áudio de outro app**.
3. Conceda a permissão de áudio exigida pelo Android e confirme a autorização do sistema para compartilhar a reprodução.
4. Abra o app de origem e dê play. Os convidados usam o mesmo convite; quem ainda não ativou o áudio precisa tocar para ouvir.
5. Encerre pelo botão **Parar transmissão**, pela notificação ou fechando a sala. A revogação pelo sistema também interrompe o acesso ao áudio.

Esta função usa AudioPlaybackCapture e MediaProjection. **Não captura o microfone e não grava a tela.** O áudio do próprio RetroPlayer é excluído para evitar retorno. O Android só entrega o áudio de outros aplicativos que permitem captura. Apps protegidos podem enviar silêncio; a interface informa quando não recebe sinal. Não há tentativa de contornar bloqueios.

O áudio é transmitido em PCM 16 bits, estéreo, 44,1 kHz, para até **quatro ouvintes simultâneos**. O convidado toca os dados recebidos com Web Audio. Isso usa aproximadamente **11 MB por minuto por ouvinte** da conexão de upload do anfitrião. O buffer é limitado e ouvintes com conexão muito lenta são desconectados para evitar atraso crescente; o botão de reconexão permite tentar novamente. O áudio ao vivo tem atraso e não oferece alinhamento exato entre aparelhos. [Captura no Android](https://developer.android.com/media/platform/av-capture).

## Letras

No Player, abra **Ver letra e ajustar**. Busque no LRCLIB com título e artista, ou cole uma letra LRC (`[00:12.50] Texto`). A duração da gravação participa da busca; gravações diferentes podem não coincidir. Letras sem timestamps aparecem como texto.

A letra e o ajuste de sincronia ficam salvos por faixa. A sala recebe a linha atual e a próxima. Na letra completa do aplicativo, toque numa linha para mudar a posição; adiante ou atrase a letra em passos de 0,5 segundo, até 30 segundos. Uma captura de outros apps não fornece metadados nem letras sincronizadas automaticamente.

## Funcionamento da sala

O aplicativo mantém um servidor HTTP em `127.0.0.1`, numa porta aleatória, e abre um túnel HTTPS temporário TryCloudflare. O endereço contém um token aleatório. Estado, mídia, chat, fila, uploads e áudio ao vivo exigem esse token. Fechar a sala invalida o convite, encerra a captura e limpa o chat temporário.

Quem tem o convite pode ouvir, conversar, adicionar links e enviar arquivos. Os apelidos não identificam pessoas verificadas. Não há criptografia ponta a ponta adicional ao HTTPS, contas ou banco de dados do chat. Evite publicar o convite se quiser manter a sala restrita.

Os controles remotos de reprodução permanecem com o anfitrião. Contribuições usam validação de formato, limites de tamanho/fila, controle de frequência e verificação de origem. O servidor grava uploads em blocos, sem carregar arquivos inteiros na memória. O áudio ao vivo usa filas pequenas por ouvinte. A política de referência envia somente a origem ao player YouTube, sem expor o token no cabeçalho Referer.

O anfitrião precisa manter o aparelho ligado, conectado e com o serviço ativo. O serviço gratuito do túnel e restrições de bateria do fabricante podem interromper a sala. Compartilhar arquivos e capturas utiliza a conexão do anfitrião. A sincronia de arquivos e YouTube é aproximada.

## Compilar e testar

Requisitos: JDK 17 ou 21, Python 3, Android SDK 34, Gradle e acesso à internet para dependências.

```sh
python3 tools/prepare_cloudflared.py
./gradlew assembleDebug
./gradlew lintDebug
sh tests/run.sh
```

No Termux com SDK e AAPT2 ARM64 já instalados:

```sh
sh tools/build-termux.sh
```

Configure `sdk.dir` em `local.properties` ou `ANDROID_HOME`. O APK sai em `app/build/outputs/apk/debug/app-debug.apk`. Os testes usam `tools/test-json.jar` ou `TEST_JSON_JAR` apontando para `org.json:json:20240303`.

Os testes executam o servidor HTTP real com adaptadores JVM e os scripts reais do convidado com adaptadores de mídia/YouTube/Web Audio. Cobrem token, estado, mídia, ranges, chat, origem, fila, uploads, áudio ao vivo, limites de ouvintes, LRC, importação e favoritos. Não substituem instalação e reprodução em aparelhos reais. Consulte [VALIDATION.md](VALIDATION.md).

`prepare_cloudflared.py` baixa Cloudflared 2026.10.7 ARM64 Linux, verifica SHA256 e ajusta o resolvedor para o diretório privado do aplicativo. O executável preparado fica fora do controle de versão.

## Arte e terceiros

A nova capa está em `app/src/main/res/drawable-nodpi/album_art.png`; o prompt e a ferramenta usados estão em [artwork/DESIGN-v2.md](artwork/DESIGN-v2.md). As artes e as fontes antigas foram mantidas no repositório, mas não definem a interface da versão 2.0.

Cloudflared é Apache 2.0: [licença](tools/CLOUDFLARED-LICENSE). As fontes antigas têm seus avisos SIL OFL nos assets. RetroPlayer é independente e não é afiliado ao YouTube, Google ou Cloudflare.
