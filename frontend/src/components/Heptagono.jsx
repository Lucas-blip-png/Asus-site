// Heptagono (radar) dos 7 atributos ASUS — ordem do mockup: For no topo, horario.
const ATTRS = [
  { key: 'forca', sigla: 'For' },
  { key: 'destreza', sigla: 'Des' },
  { key: 'carisma', sigla: 'Car' },
  { key: 'sabedoria', sigla: 'Sab' },
  { key: 'inteligencia', sigla: 'Int' },
  { key: 'agilidade', sigla: 'Agi' },
  { key: 'constituicao', sigla: 'Con' },
]

export default function Heptagono({ atributos = {}, max = 20, size = 210 }) {
  const cx = size / 2
  const cy = size / 2
  const r = size * 0.33
  const ponto = (i, radius) => {
    const ang = ((-90 + i * (360 / 7)) * Math.PI) / 180
    return [cx + radius * Math.cos(ang), cy + radius * Math.sin(ang)]
  }
  const base = ATTRS.map((_, i) => ponto(i, r).join(',')).join(' ')
  const valor = ATTRS.map((a, i) => {
    const v = Math.max(0, Math.min(max, atributos[a.key] || 0))
    return ponto(i, max > 0 ? (r * v) / max : 0).join(',')
  }).join(' ')

  return (
    <svg className="heptagono" width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <polygon className="base" points={base} />
      <polygon className="val" points={valor} />
      <text x={cx} y={cy + 3} textAnchor="middle" className="attr-val">Atributos</text>
      {ATTRS.map((a, i) => {
        const [lx, ly] = ponto(i, r + 18)
        return (
          <g key={a.key}>
            <text x={lx} y={ly} textAnchor="middle">{a.sigla}</text>
            <text x={lx} y={ly + 11} textAnchor="middle" className="attr-val">
              {atributos[a.key] || 0}
            </text>
          </g>
        )
      })}
    </svg>
  )
}
