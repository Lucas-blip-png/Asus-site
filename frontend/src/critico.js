// Leitura e escrita do crítico das armas. O valor mora numa string só
// (`critico`, em Ataque / ItemJogo / ItemPersonagem) que guarda as duas
// metades da regra: a margem de ameaça e o multiplicador de dano.

// Interpreta o crítico: "18/x2" -> {alvo:18,mult:2}; "18" -> {alvo:18,mult:2}; "x3" -> {alvo:20,mult:3}.
// Formatos antigos (só a margem ou só o multiplicador) continuam valendo — a
// metade que falta cai no padrão da regra (margem 20, multiplicador x2).
export function parseCritico(critStr) {
  const s = String(critStr || '').toLowerCase()
  let mult = 2
  let alvo = 20
  const mMult = s.match(/x\s*(\d+)/)
  if (mMult) mult = Math.max(1, Number(mMult[1]))
  const mAlvo = s.replace(/x\s*\d+/g, '').match(/(\d+)/)
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
