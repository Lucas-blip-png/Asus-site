# ASUS RPG Platform — Frontend (VTT)

SPA em **React + Vite** que consome a API do backend (`../asus-platform`). Tema
escuro com dourado, inspirado no design de referência (`frontend.pdf`).

## Funcionalidades

- **Login/registro** com JWT (Fase 7) — guarda os tokens e renova via refresh.
- **Personagens**: lista e criação; cada ficha é uma **VTT** com:
  - **heptágono** dos 7 atributos (SVG radar),
  - barras de **Vida/Mana/Energia** com ajuste (`PATCH` de status),
  - **perícias** com treino (steppers, teto = 2 × atributo) salvas via `PUT`,
  - abas **Combate** (ataques — CRUD), **Habilidades** (de classe),
    **Magias** (feitiços — CRUD), **Inventário** (itens do sistema) e **Descrição**.
- **Campanhas**: membros, personagens, convites por código e **rolagens em tempo
  real** (WebSocket/STOMP, Fase 6).
- **Escudo do Mestre** (Fase 8): editar status e revelar rolagens ocultas.
- **Bestiário**: listar/criar/remover criaturas do sistema ASUS.
- **Livros**: catálogo do sistema (raças, classes, perícias, itens, progressão e
  regras de construção de feitiços).
- **Overlay OBS** (Fase 9): rota transparente `/overlay/:campanhaId` que mostra a
  última rolagem ao vivo — adicione como *Browser Source* no OBS.

## Rodar

Pré-requisito: **Node 18+**. Suba o backend em `http://localhost:8080` antes.

```bash
npm install
npm run dev      # http://localhost:5173 (faz proxy de /api e /ws para :8080)
npm run build    # gera dist/ (produção)
```

Variáveis opcionais (`.env`):

- `VITE_API_BASE` — base da API (padrão: vazio, usa o proxy do Vite).
- `VITE_WS_URL` — URL do WebSocket (padrão: `ws://<host>/ws`).

## Estrutura

```
src/
├── main.jsx, App.jsx          # bootstrap + rotas
├── api.js                     # fetch + Bearer token + obterOrgId()
├── auth.jsx                   # contexto de autenticação (login/refresh)
├── ws.js                      # cliente STOMP (tempo real)
├── styles.css                 # tema dourado/escuro
├── components/
│   ├── Layout.jsx             # nav (Personagens, Campanhas, Bestiário, Livros)
│   └── Heptagono.jsx          # radar SVG dos 7 atributos
└── pages/                     # Home, Personagens, Ficha (VTT), Campanhas,
                               # Bestiario, Livros, Escudo, Overlay
```
