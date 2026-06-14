import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

const ATALHOS = [
  ['/personagens', '🎭', 'Personagens', 'Crie e gerencie suas fichas'],
  ['/campanhas', '🗺️', 'Campanhas', 'Mesas, sessões e jogadores'],
  ['/livros', '📖', 'Livro de Regras', 'Atributos, classes e magias'],
  ['/bestiario', '🐉', 'Bestiário', 'Criaturas e adversários'],
]

export default function Home() {
  const { user } = useAuth()
  return (
    <>
      <div className="page-head">
        <h1>Olá, {user?.nome} 👋</h1>
      </div>
      <p className="muted">Plataforma de RPG de mesa — sistema ASUS.</p>

      <div className="carousel">
        <div className="slide" />
        <div className="slide mid" />
        <div className="slide" />
      </div>

      <div className="home-actions">
        {ATALHOS.map(([to, ic, titulo, sub]) => (
          <Link key={to} to={to} className="home-card">
            <span className="ic">{ic}</span>
            <b>{titulo}</b>
            <span className="muted">{sub}</span>
          </Link>
        ))}
      </div>
    </>
  )
}
