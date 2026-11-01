# Validação da entrega — RetroPlayer 1.0

- Compilação `assembleDebug`: passou.
- Android Lint `lintDebug`: passou, sem erros. O relatório inclui avisos de internacionalização, suporte exclusivo ARM64, JavaScript necessário ao navegador, configuração de backup e wakelock do serviço.
- Testes de integração HTTP: 14 verificações passaram (acesso por token, página da sala, mídia completa, intervalos de bytes, faixa antiga, chat, origem das mensagens, link remoto e sala encerrada).
- Testes JavaScript: passaram para ativação pelo usuário, pausa, seek, correção de deriva, troca de faixa e encerramento da sala.
- Assinatura APK: verificada com `apksigner`, esquema v2, um assinante de desenvolvimento.
- Conteúdo do APK: página do convidado e executável Cloudflared conferidos; executável idêntico ao binário preparado.
- Túnel: o executável Cloudflared ARM64 foi executado neste ambiente Android, gerou uma URL TryCloudflare e registrou uma conexão HTTP/2 com a Cloudflare.
- APK salvo em `/sdcard/Download/RetroPlayer.apk` e em `apk/RetroPlayer.apk`; hashes conferidos.

A interface, instalação do APK, codecs, sites no WebView e ciclo completo entre dois aparelhos não foram validados em dispositivo instalado. O ADB deste ambiente não pôde ser executado por incompatibilidade de biblioteca C++; os testes HTTP/JavaScript usam JVM e adaptadores de arquivo. Não há alegação de precisão de sincronia medida entre aparelhos.

SHA256 do APK: `21735ec0f8acf0393ecc657c8f973825a9a0367ea226879cd897bc5f949282bb`.
