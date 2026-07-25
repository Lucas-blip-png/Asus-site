import { useCallback, useEffect, useState } from 'react'

// Imagens em public/carousel (servidas em /carousel/slideN.jpg).
const SLIDES = Array.from({ length: 11 }, (_, i) => `/carousel/slide${i + 1}.jpg`)
const INTERVALO_MS = 4000

/** Carrossel de imagens estilo "coverflow": card central em destaque, laterais menores. */
export default function Carousel() {
  const [idx, setIdx] = useState(0)
  const [pausado, setPausado] = useState(false)
  const n = SLIDES.length

  const ir = useCallback((delta) => setIdx((i) => (i + delta + n) % n), [n])

  useEffect(() => {
    if (pausado) return undefined
    const t = setInterval(() => setIdx((i) => (i + 1) % n), INTERVALO_MS)
    return () => clearInterval(t)
  }, [pausado, n])

  const prev = (idx - 1 + n) % n
  const next = (idx + 1) % n

  return (
    <div
      className="carousel-wrap"
      onMouseEnter={() => setPausado(true)}
      onMouseLeave={() => setPausado(false)}
    >
      <div className="carousel">
        <button className="carousel-nav prev" onClick={() => ir(-1)} aria-label="Imagem anterior">
          ‹
        </button>

        <div className="slide side" onClick={() => ir(-1)}>
          <img src={SLIDES[prev]} alt="" loading="lazy" />
        </div>

        <div className="slide mid" key={idx}>
          <img src={SLIDES[idx]} alt={`Ilustração ${idx + 1}`} />
        </div>

        <div className="slide side" onClick={() => ir(1)}>
          <img src={SLIDES[next]} alt="" loading="lazy" />
        </div>

        <button className="carousel-nav next" onClick={() => ir(1)} aria-label="Próxima imagem">
          ›
        </button>
      </div>

      <div className="carousel-dots">
        {SLIDES.map((src, i) => (
          <button
            key={src}
            className={'dot' + (i === idx ? ' on' : '')}
            onClick={() => setIdx(i)}
            aria-label={`Ir para a imagem ${i + 1}`}
          />
        ))}
      </div>
    </div>
  )
}
