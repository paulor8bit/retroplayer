# Validação da entrega — RetroPlayer 1.1

- `assembleDebug`: passou.
- Android Lint `lintDebug`: passou, **0 erros e 8 avisos**. Os avisos remanescentes abrangem internacionalização, ABI ARM64, configuração de backup e wakelock do serviço de sala.
- Integração HTTP: **14 verificações passaram**, cobrindo acesso por token, página da sala, mídia completa, intervalos de bytes, faixa antiga, chat, origem das mensagens, link remoto e encerramento da sala.
- Biblioteca/favoritos: **14 verificações passaram**, cobrindo subpastas, arquivos repetidos, ciclos de diretórios, pastas sem acesso, cancelamento, filtro de mídia, favoritos persistidos, atualização sem duplicatas, esquemas inválidos e JSON corrompido.
- JavaScript do convidado: passou para ativação pelo usuário, pausa, seek, correção de deriva, troca de faixa e encerramento.
- APK: assinatura v2 verificada com `apksigner`; mesmo certificado da versão 1.0. `applicationId` continua `br.com.retroplayer`, agora com `versionCode=2` e `versionName=1.1`.
- Pacote conferido: capa PNG, sprite PNG com transparência, classes de importação/favoritos/navegador persistente e executável Cloudflared ARM64 presentes.
- Cópia em `/sdcard/Download/RetroPlayer.apk` idêntica à versão de `apk/RetroPlayer.apk` por SHA256.

## Limites da validação

O ADB foi executado usando uma cópia isolada da biblioteca C++ atual, sem alterar a instalação do Termux. **Não existe aparelho autorizado conectado ao ADB**. Portanto não foram validados em APK instalado: aparência/layout, seleção via DocumentsProvider real, codecs, favoritos na UI, navegação em sites, reprodução com tela apagada ou sincronia medida entre dois aparelhos.

O suporte a segundo plano foi implementado com serviço de reprodução, MediaSession, wakelock do MediaPlayer, sessão WebView mantida pelo serviço e transferência de mídia HTTPS direta com posição/cabeçalhos temporários. A permanência do som em sites protegidos ou com blob/MSE/iframes depende do site e do WebView e não é garantida. O teste de conectividade do executável Cloudflared registrado na versão 1.0 não foi repetido; o binário continua idêntico e foi conferido no novo pacote.

As imagens foram criadas com a ferramenta integrada image_gen, incorporadas ao projeto e inspecionadas. A capa mede 1254 × 1254 em RGB e o sprite 1254 × 1254 em RGBA, grade 2 × 2. Os prompts estão em [artwork/PROMPTS.md](artwork/PROMPTS.md).

SHA256 do APK: `43cbf06b554bad1b535bbcd9f1b9904cb7f9b26ab69392c562cd2d734d013554`.
