# ASUS RPG Platform — Frontend (Fases 2 e 9)

SPA em **React + Vite** que consome a API do backend (`../asus-platform`).

## Funcionalidades

- **Login/registro** com JWT (Fase 7) — guarda os tokens e renova via refresh.
- **Organizações**: listar/criar, ver plano e limites.
- **Personagens**: ficha completa (atributos, status com barras, perícias) e
  ajuste de PV/PM/PE.
- **Campanhas**: membros, personagens, convites por código e **rolagens em tempo
  real** (WebSocket/STOMP, Fase 6).
- **Escudo do Mestre** (Fase 8): editar status e revelar rolagens ocultas.
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
├── main.jsx, App.jsx        # bootstrap + rotas
├── api.js                   # fetch + Bearer token
├── auth.jsx                 # contexto de autenticação (login/refresh)
├── ws.js                    # cliente STOMP (tempo real)
├── components/Layout.jsx
└── pages/                   # Login, Dashboard, Organizacao, Personagem,
                             # Campanha, Escudo, Overlay
```
