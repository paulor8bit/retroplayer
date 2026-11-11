# RetroPlayer Together 2.0 — direção visual e arte

A direção elimina o painel de terminal, códigos de barras e mascotes da interface. A hierarquia passa a ser capa, título, transporte e ações de sala. Biblioteca, Sala e Explorar mantêm ações explícitas e espaçamento consistente. A interface da sala usa a mesma direção, com fila e chat próximos do player no computador e empilhados no celular.

- Fundo: `#0B111B`.
- Superfície: `#141E2C`; superfície elevada: `#1E2B3D`.
- Texto principal: `#F2F5FB`; secundário: `#9AA9BD`.
- Acento: `#8CBCFF`; divisórias: `#29384E`.
- Tipografia: sans-serif do sistema Android e system-ui no navegador.
- Botões com altura mínima de 48 dp; alvos e campos rotulados.
- Ícones circular/play em vetores Android e SVG nativo na página, alinhados à nova arte.

## Arte final

Arquivo: `app/src/main/res/drawable-nodpi/album_art.png`.

Ferramenta: habilidade **imagegen**, ferramenta integrada `image_gen.imagegen`, modo de geração, fundo opaco. Imagem nova, sem imagem de referência; o original gerado foi copiado para o projeto. Não é uma captura de tela do aplicativo.

Prompt final exato:

> Use case: stylized-concept. Asset type: final square album-cover artwork for a professionally redesigned Android music application called RetroPlayer, not a UI mockup. Create a premium editorial still-life photograph of a single sculptural brushed aluminum torus loudspeaker / vinyl-inspired sound object with a translucent smoked glass center, on a deep ink navy studio background. Restrained electric blue rim light, soft warm white key light, authentic subtle metal grain, elegant sculptural shadow. Central object fills about 65 percent of square with generous immaculate negative space. Quiet contemporary audio brand art direction, confident and refined, sophisticated realism and exceptionally good material detail. No lettering, no logo, no neon cyberpunk, no cassette, no characters, no decorative waves, no UI, no watermark. Square composition, intended as the default now-playing artwork.

A arte foi inspecionada. O layout nativo e o HTML não puderam ser conferidos em execução com captura de tela neste ambiente, que não tem aparelho ADB conectado nem navegador headless instalado.
