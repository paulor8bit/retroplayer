# Validação — RetroPlayer 1.2 / Cyberdeck

- Compilação `assembleDebug`: passou.
- Android Lint `lintDebug`: passou, **0 errors, 11 warnings**. Avisos incluem internacionalização, ABI ARM64, backup, wakelock e alocações do pequeno relógio decorativo.
- **20 verificações HTTP passaram**: token, página, mídia completa, ranges, faixa antiga, chat, origem das mensagens, link remoto, sala encerrada, fontes/capa, MIME das fontes, CSP, lista fixa de recursos e revogação do tema ao encerrar a sala.
- **14 verificações de biblioteca/favoritos passaram**: subpastas, ciclos, duplicatas, cancelamento, pastas sem acesso, filtros e persistência/validação dos favoritos.
- JavaScript do convidado: passou nos cenários de ativação por toque, pausa, seek, correção de deriva, troca de faixa e encerramento.
- HTML conferido: IDs únicos, controles existentes de mídia/chat presentes e capa com texto alternativo. O CSS inclui regras para telas pequenas e grandes.
- APK: assinatura v2 verificada; mesmo certificado da versão anterior. Identificador `br.com.retroplayer`, versão interna 3, versão 1.2.
- APK conferido: fontes locais e licenças, página Cyberdeck, classes do tema angular e navegador persistente presentes; executável Cloudflared idêntico ao binário preparado.
- Cópia em `/sdcard/Download/RetroPlayer.apk` idêntica ao arquivo `apk/RetroPlayer.apk` por SHA256.

## Limites

A referência `/sdcard/Download/modelo.jpg` foi inspecionada e usada para orientar cores, fontes, molduras, botões, composição e marcadores. O arquivo de referência não está incluído no APK nem no repositório. Os detalhes estão em [artwork/DESIGN-v1.2.md](artwork/DESIGN-v1.2.md).

Não há dispositivo Android autorizado conectado ao ADB nem navegador headless instalado neste ambiente. O layout não foi verificado por captura de tela de APK ou navegador em execução. Os testes cobrem o servidor real com adaptadores de arquivos no JVM e o comportamento JavaScript, sem substituir testes de codecs, áudio com tela apagada ou sincronização entre dois aparelhos.

O serviço de reprodução, a importação de pastas e os favoritos foram mantidos. Sites protegidos ainda podem limitar áudio em segundo plano. O binário do túnel continua o mesmo validado na versão 1.0; o teste de conectividade não foi repetido nesta alteração visual.

SHA256 do APK: `3faa9a06fedc019113a496ffb0a2c795840514a7c6aa488f0dab2132fb37fbbc`.
