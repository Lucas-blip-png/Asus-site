// Leitura e escrita do crítico das armas. O valor mora numa string só
// (`critico`, em Ataque / ItemJogo / ItemPersonagem) que guarda as duas
// metades da regra: a margem de ameaça e o multiplicador de dano.

// Interpreta o crítico: "18/x2" -> {alvo:18,mult:2}; "18" -> {alvo:18,mult:2}; "x3" -> {alvo:20,mult:3}.
// O multiplicador é escrito das duas formas na prática, "x3" e "3x", e as duas
// contam. O que sobra depois de tirar o multiplicador é a margem de ameaça.
// Formatos antigos (só a margem ou só o multiplicador) continuam valendo — a
// metade que falta cai no padrão da regra (margem 20, multiplicador x2).
export function parseCritico(critStr) {
  const s = String(critStr || '').toLowerCase()
  let mult = 2
  let alvo = 20
  let resto = s
  const posfixo = s.match(/x\s*(\d+)/)   // "x3"
  const prefixo = s.match(/(\d+)\s*x/)   // "3x"
  if (posfixo) {
    mult = Math.max(1, Number(posfixo[1]))
    resto = s.replace(/x\s*\d+/g, '')
  } else if (prefixo) {
    mult = Math.max(1, Number(prefixo[1]))
    resto = s.replace(/\d+\s*x/g, '')
  }
  const mAlvo = resto.match(/(\d+)/)
  if (mAlvo) alvo = Math.min(20, Math.max(2, Number(mAlvo[1])))
  return { alvo, mult }
}

// Forma canônica gravada no campo `critico`: margem de ameaça + multiplicador ("19/x2").
export const fmtCritico = (alvo, mult) => `${Math.min(20, Math.max(2, alvo))}/x${Math.max(1, mult)}`

// Rótulo de exibição: "19" -> "19+ x2"; "x3" -> "20+ x3"; vazio -> "".
export function rotuloCritico(critStr) {
  if (!String(critStr || '').trim()) return ''
  const { alvo, mult } = parseCritico(critStr)
  return `${alvo}+ x${mult}`
}
