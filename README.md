# ϟ RetroPlayer • Together

Player Android de música e vídeo com visual retrô inspirado no Winamp, navegador integrado, salas por link e chat.

## Instalar

Baixe **[apk/RetroPlayer.apk](apk/RetroPlayer.apk)** ou o APK na seção **Releases**. Compatível com **Android 8 ou superior, ARM64**. A versão 1.2 usa assinatura de desenvolvimento e o mesmo identificador/assinante das versões anteriores, permitindo atualizar sem desinstalar.

## Tema Cyberdeck 1.2

O app e a página do convidado seguem a referência `modelo.jpg`: fundo escuro avermelhado, ciano claro, detalhes em coral, fontes de terminal e painéis com cantos recortados. A composição do player mostra a capa junto à identificação do arquivo, progresso e últimos arquivos adicionados. A sala do convidado usa o mesmo desenho, com painéis reorganizados para celular ou computador.

As fontes e a capa são fornecidas pelo próprio app, sem CDN. Instale a versão 1.2 e crie uma nova sala para ver o tema no link. Veja [artwork/DESIGN-v1.2.md](artwork/DESIGN-v1.2.md) para a paleta e os detalhes da adaptação.

## Usar

A interface do aplicativo é nativa Android, com abas **Player**, **Biblioteca**, **Sala** e **Web**, barra de navegação inferior e miniplayer. A capa e o mascote animado são imagens originais; os controles e ícones são vetores nativos.

1. Na aba **Biblioteca**, use **Um arquivo** para selecionar uma mídia, ou **Escolher pasta** para importar todos os áudios e vídeos da pasta e de suas subpastas. O botão **+** também permite escolher vários arquivos.
2. A importação acontece fora da interface, informa o progresso e pode ser cancelada. Arquivos que já estão na fila não são adicionados novamente. Imagens e documentos ficam de fora. A pasta permanece autorizada pelo seletor do Android; arquivos movidos ou excluídos podem exigir nova seleção.
3. Busque arquivos na lista, toque para reproduzir e use o menu da faixa para removê-la. **Limpar lista** não apaga arquivos do aparelho.
4. No **Player**, controle reprodução, avanço, posição, volume, repetição e modo aleatório. O equalizador fica no ícone de ajustes e no menu superior.
5. Na aba **Sala**, use **Criar / encerrar**, aguarde **Sala online** e toque em **Convidar** ou **Copiar link**. O convidado abre o link no navegador, toca em **Entrar e ouvir junto** e participa do chat.
6. O anfitrião controla a reprodução. Encerrar a sala invalida o link e limpa o chat temporário.

O Android impõe restrições ao seletor de pastas, como a raiz de Download em versões recentes. Escolha uma subpasta ou use a seleção de arquivo quando o seletor não permitir uma pasta. [Documentação do seletor Android](https://developer.android.com/training/data-storage/shared/documents-files).

### Navegador e favoritos

Na aba **Web**, abra o navegador para pesquisar ou digitar um endereço. Toque na **estrela** para salvar o site com um nome. O ícone de biblioteca no topo abre os favoritos; um toque abre o site e um toque prolongado permite removê-lo. Os favoritos e a preferência de áudio em segundo plano ficam salvos no aparelho, inclusive após encerrar o app.

O ícone de **fones** e a opção **Ouvir no player** abrem um link HTTPS direto de áudio ou vídeo no player. Uma página do YouTube ou de outro site não equivale a um endereço direto de mídia.

### Áudio com o aplicativo minimizado

O player toca no serviço de reprodução em primeiro plano, com controles na notificação, MediaSession e wakelock do MediaPlayer. Mudar de aba, abrir o navegador, minimizar o app ou apagar a tela não encerra esse player. Abrir o navegador mantém a música atual até o site começar a reproduzir.

A sessão WebView pertence ao serviço e não é destruída nem pausada pela saída da tela do navegador quando **Áudio em segundo plano** está ativado. Ao minimizar, uma mídia HTML com endereço HTTPS direto é transferida ao player nativo, mantendo a posição quando o conteúdo permite seek. A transferência usa o agente do navegador, o referer e os cookies daquele endereço apenas em memória; esses cabeçalhos não são gravados na fila.

Sites com DRM, conteúdo blob/MSE, reprodução em iframe de outra origem, Web Audio ou regras próprias de visibilidade podem interromper o áudio. A retenção da sessão WebView não garante segundo plano em todo site. Para áudio com tela apagada, prefira arquivo local, rádio ou endereço direto compatível no player. A versão e o comportamento do Android System WebView também influenciam o resultado. [API do WebView](https://developer.android.com/reference/android/webkit/WebView).

O áudio que toca dentro de uma página é local; páginas, DRM e logins não são retransmitidos na sala. Links remotos compartilhados precisam funcionar também no navegador do convidado, sem depender dos cookies do anfitrião. MP3 e MP4 com codecs comuns oferecem maior compatibilidade.

## Como a sala funciona

O aplicativo mantém um servidor HTTP vinculado a `127.0.0.1` em porta aleatória e abre um túnel HTTPS temporário com o **TryCloudflare**, sem conta ou domínio. O endereço inclui um token aleatório de sessão. Apenas a mídia atual, o estado da reprodução, a página do convidado e o chat são expostos por esse endereço. O navegador consulta o estado periodicamente e corrige desvios de posição e velocidade.

A sincronia é aproximada; atraso, buffering e codecs afetam o resultado. O túnel precisa de internet e depende da disponibilidade do serviço gratuito. O celular anfitrião deve permanecer ligado e com o serviço ativo. Alguns fabricantes podem encerrar serviços em segundo plano. O compartilhamento de arquivos locais usa o upload da conexão do anfitrião. Links HTTPS diretos de mídia são acessados pelo aparelho de cada participante.

Quem possuir o link pode abrir a mídia atual e ler/escrever no chat. O chat não usa contas: os nomes são apelidos e não identificam pessoas verificadas. As mensagens atravessam o túnel HTTPS, sem criptografia ponta a ponta adicional. Não há armazenamento do chat em banco de dados. Não compartilhe o link publicamente se quiser restringir o acesso.

## Compilar

Requisitos: JDK 17 ou 21, Python 3, Android SDK 34 e acesso à internet para baixar Gradle e as dependências.

```sh
python3 tools/prepare_cloudflared.py
./gradlew assembleDebug
```

Configure `sdk.dir` em `local.properties` ou a variável `ANDROID_HOME`. O APK será criado em `app/build/outputs/apk/debug/app-debug.apk`. No Termux, com o SDK e o AAPT2 ARM64 instalados, use `sh tools/build-termux.sh` e ajuste os caminhos se necessário.

`prepare_cloudflared.py` baixa a versão **2026.10.7 ARM64 Linux**, confere seu SHA256 e substitui o caminho fixo `/etc/resolv.conf` por `./dns/resolv.cnf`, de mesmo tamanho, para usar os resolvedores no diretório privado do app. O executável estático é empacotado como biblioteca nativa extraída pelo Android. O aplicativo fornece o arquivo de CAs e os resolvedores para o subprocesso. O binário preparado não fica no controle de versão; o script permite reproduzir a preparação.

## Verificações

```sh
# Defina TEST_JSON_JAR ou baixe org.json:json:20240303 para tools/test-json.jar.
sh tests/run.sh
./gradlew lintDebug
```

Os testes de integração executam o servidor HTTP real com adaptadores de arquivo no JVM: token, sala encerrada, mídia completa, ranges, faixa antiga, redirecionamento remoto, chat e bloqueio de POST entre origens. O teste JavaScript verifica ativação pelo usuário, pausa, seek, correção de deriva, troca de faixa e encerramento. Os testes adicionais cobrem subpastas, ciclos, cancelamento, duplicatas, pastas inacessíveis e persistência/validação dos favoritos.

O executável do túnel foi executado neste ambiente Android e conectou ao TryCloudflare. Os testes de JVM e JavaScript não substituem testes de instalação, interface, codecs, serviço em segundo plano e navegação em aparelhos reais. Veja [VALIDATION.md](VALIDATION.md) para os resultados desta entrega.

## Arte

Os arquivos `deck_cover.png` e `cassette_sprites.png` ficam em `app/src/main/res/drawable-nodpi/`. O sprite usa quatro quadros em grade 2 × 2, desenhados no Canvas nativo. Os prompts completos e o método de geração estão em [artwork/PROMPTS.md](artwork/PROMPTS.md).

As fontes Share Tech Mono e Rajdhani estão sob SIL OFL, com as licenças incluídas no projeto e no APK.

## Terceiros

Cloudflared é distribuído sob Apache 2.0; sua licença está em [tools/CLOUDFLARED-LICENSE](tools/CLOUDFLARED-LICENSE). O arquivo `ca.pem` contém os certificados públicos usados para validar HTTPS. RetroPlayer é um projeto independente e não é afiliado ao Winamp ou à Cloudflare.
