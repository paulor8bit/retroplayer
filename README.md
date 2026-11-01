# ϟ RetroPlayer • Together

Player Android de música e vídeo com visual retrô inspirado no Winamp, navegador integrado, salas por link e chat.

## Instalar

Baixe **[apk/RetroPlayer.apk](apk/RetroPlayer.apk)** ou o APK na seção **Releases**. Compatível com **Android 8 ou superior, ARM64**. O APK desta primeira versão usa assinatura de desenvolvimento.

## Usar

1. Toque em **Abrir arquivos** para adicionar músicas ou vídeos do celular. É possível escolher vários arquivos.
2. Controle reprodução, avanço, posição, volume, repetição e modo aleatório. **EQ** ajusta o áudio local quando disponível no aparelho.
3. Toque em **Criar sala**, aguarde **Sala online** e use **Compartilhar** ou **Copiar link**.
4. O convidado abre o link no navegador e toca em **Entrar e ouvir junto**. Não precisa instalar o aplicativo.
5. O anfitrião controla a faixa, o play, a pausa e a posição. Todos podem conversar no chat e escolher seu nome.
6. **Encerrar sala** invalida o link e limpa o histórico. O histórico existe apenas na memória durante a sessão.

### Internet

**Navegar na internet** abre um navegador integrado com pesquisa, histórico de navegação da sessão e reprodução de sites. **Tocar link** adiciona um endereço HTTPS direto de áudio ou vídeo ao player e permite acompanhar essa faixa na sala.

Uma página do YouTube ou de outro site não equivale a um link direto de mídia. O conteúdo reproduzido dentro do navegador é local; páginas, logins, DRM e anúncios não são retransmitidos. A compatibilidade dos sites depende do Android System WebView instalado. Arquivos MP3 e MP4 com codecs comuns têm maior compatibilidade entre aparelhos e navegadores. Links remotos também dependem das permissões de acesso do servidor de origem e podem expirar.

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

Os testes de integração executam o servidor HTTP real com adaptadores de arquivo no JVM: token, sala encerrada, mídia completa, ranges, faixa antiga, redirecionamento remoto, chat e bloqueio de POST entre origens. O teste JavaScript verifica ativação pelo usuário, pausa, seek, correção de deriva, troca de faixa e encerramento.

O executável do túnel foi executado neste ambiente Android e conectou ao TryCloudflare. Os testes de JVM e JavaScript não substituem testes de instalação, interface, codecs, serviço em segundo plano e navegação em aparelhos reais. Veja [VALIDATION.md](VALIDATION.md) para os resultados desta entrega.

## Terceiros

Cloudflared é distribuído sob Apache 2.0; sua licença está em [tools/CLOUDFLARED-LICENSE](tools/CLOUDFLARED-LICENSE). O arquivo `ca.pem` contém os certificados públicos usados para validar HTTPS. RetroPlayer é um projeto independente e não é afiliado ao Winamp ou à Cloudflare.
