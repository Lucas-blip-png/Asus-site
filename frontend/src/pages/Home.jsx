import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function Home() {
  const { user } = useAuth()
  return (
    <>
      <h1>Olá, {user?.nome} 👋</h1>
      <p className="muted">Plataforma de RPG de mesa — sistema ASUS.</p>
      <div className="carousel">
        <div className="slide" />
        <div className="slide mid" />
        <div className="slide" />
      </div>
      <div className="row" style={{ justifyContent: 'center', marginTop: 28 }}>
        <Link to="/personagens"><button>Meus Personagens</button></Link>
        <Link to="/campanhas"><button className="ghost">Minhas Campanhas</button></Link>
        <Link to="/livros"><button className="ghost">Livro de Regras</button></Link>
      </div>
    </>
  )
}
