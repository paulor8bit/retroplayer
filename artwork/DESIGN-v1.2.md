# RetroPlayer 1.2 — referência Cyberdeck

Referência fornecida pelo usuário: `/sdcard/Download/modelo.jpg`, inspecionada antes da implementação. A fotografia de referência não é distribuída no APK ou no repositório.

## Identidade compartilhada

- Fundo escuro avermelhado: `#201617`.
- Superfícies: `#3D2729` e `#492E30`; divisórias: `#6B494B`.
- Texto principal e molduras em ciano: `#98E3E6`.
- Legendas em rosa acinzentado: `#C6A19C`.
- Ações e progresso em coral: `#E75C50`.
- Fontes locais: Share Tech Mono e Rajdhani SemiBold, com licenças SIL OFL incluídas no projeto e no APK.
- Molduras finas, cantos cortados, códigos de barras decorativos, títulos em caixa alta e separadores `//`.

## Aplicativo nativo

O cabeçalho, os botões, a navegação, a biblioteca, a sala e o navegador compartilham a nova paleta e tipografia. O player usa capa compacta à esquerda, identificação do arquivo à direita, indicação de progresso com traçado vetorial, controles em coral e uma lista dos últimos arquivos adicionados. Vídeos continuam com uma área própria de reprodução abaixo do arquivo ativo.

Os relógios, percentuais da reprodução e nível de bateria usam dados reais. Não foram adicionados números fictícios de CPU, memória ou conexões. O traçado visual é um indicador decorativo de progresso, não uma análise espectral do áudio.

## Sala do convidado

A página servida pelo aplicativo usa cabeçalho Cyberdeck, molduras em cantoneira, a mesma capa, fontes locais, cores, formulário de apelido e chat. No computador, os controles auxiliares ficam em uma coluna lateral; no celular, os painéis são empilhados. Identificadores e comportamento dos controles de sincronização e chat foram preservados.

As fontes e a capa são servidas apenas por uma lista fixa de caminhos sob o token da sala. Não existe dependência de CDN para as fontes. Encerrar a sala também bloqueia novas requisições desses recursos. A página continua seguindo a reprodução do anfitrião, com ajuste local de volume e ativação inicial por toque.

Para receber o novo desenho, instale o APK 1.2 e abra uma sala usando essa versão.

## Fontes

- Share Tech Mono: `https://github.com/google/fonts/tree/main/ofl/sharetechmono`
- Rajdhani: `https://github.com/google/fonts/tree/main/ofl/rajdhani`

As licenças estão em `artwork/FONT-LICENSE-MONO.txt`, `artwork/FONT-LICENSE-DISPLAY.txt` e nos arquivos `*-license.txt` dentro de `app/src/main/assets/theme/`.
