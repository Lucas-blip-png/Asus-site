package com.asus.platform.config;

import com.asus.platform.domain.*;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Semeia o conteudo inicial (plano, secao 6.1 — fase inicial via seed no banco).
 *
 * <p>Cria: sistema ASUS_V1, racas, classes e pericias oficiais, um usuario dev
 * e a organizacao padrao (criterio de aceite 1). Idempotente: so roda se o
 * sistema ASUS ainda nao existir.</p>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String SLUG_ORG_PADRAO = "asus-oficial";

    private final GameSystemRepository gameSystemRepository;
    private final RacaRepository racaRepository;
    private final ClasseRepository classeRepository;
    private final PericiaRepository periciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final OrganizacaoMembroRepository membroRepository;
    private final CampanhaRepository campanhaRepository;
    private final CampanhaMembroRepository campanhaMembroRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DataSeeder(GameSystemRepository gameSystemRepository,
                      RacaRepository racaRepository,
                      ClasseRepository classeRepository,
                      PericiaRepository periciaRepository,
                      UsuarioRepository usuarioRepository,
                      OrganizacaoRepository organizacaoRepository,
                      OrganizacaoMembroRepository membroRepository,
                      CampanhaRepository campanhaRepository,
                      CampanhaMembroRepository campanhaMembroRepository,
                      AssinaturaRepository assinaturaRepository,
                      org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.gameSystemRepository = gameSystemRepository;
        this.racaRepository = racaRepository;
        this.classeRepository = classeRepository;
        this.periciaRepository = periciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.membroRepository = membroRepository;
        this.campanhaRepository = campanhaRepository;
        this.campanhaMembroRepository = campanhaMembroRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (gameSystemRepository.existsByCodigo(AsusV1Engine.SYSTEM_ID)) {
            log.info("Seed ja aplicado; pulando.");
            return;
        }
        log.info("Aplicando seed inicial do ASUS...");

        GameSystem asus = gameSystemRepository.save(GameSystem.builder()
                .codigo(AsusV1Engine.SYSTEM_ID)
                .nome("ASUS RPG")
                .versao(AsusV1Engine.VERSION)
                .oficial(true)
                .ativo(true)
                .build());

        Long sid = asus.getId();

        // Racas
        racaRepository.save(Raca.builder().gameSystemId(sid).codigo("HUMANO").nome("Humano")
                .descricao("Versatil e adaptavel.").pvBase(12).pmBase(6).peBase(4)
                .jsonHabilidades("{\"bonusAtributos\":{\"presenca\":1,\"intelecto\":1}}")
                .oficial(true).build());

        racaRepository.save(Raca.builder().gameSystemId(sid).codigo("ANAO").nome("Anao")
                .descricao("Resistente e teimoso.").pvBase(16).pmBase(4).peBase(3)
                .jsonHabilidades("{\"bonusAtributos\":{\"vigor\":2}}")
                .oficial(true).build());

        racaRepository.save(Raca.builder().gameSystemId(sid).codigo("ELFO").nome("Elfo")
                .descricao("Agil e perceptivo.").pvBase(10).pmBase(8).peBase(5)
                .jsonHabilidades("{\"bonusAtributos\":{\"agilidade\":2,\"intelecto\":1}}")
                .oficial(true).build());

        // Classes
        classeRepository.save(Classe.builder().gameSystemId(sid).codigo("GUERREIRO").nome("Guerreiro")
                .descricao("Combatente de linha de frente.")
                .multiplicadorPv(6).multiplicadorPm(1).multiplicadorPe(3)
                .oficial(true).build());

        classeRepository.save(Classe.builder().gameSystemId(sid).codigo("MAGO").nome("Mago")
                .descricao("Conjurador arcano.")
                .multiplicadorPv(3).multiplicadorPm(6).multiplicadorPe(2)
                .oficial(true).build());

        classeRepository.save(Classe.builder().gameSystemId(sid).codigo("LADINO").nome("Ladino")
                .descricao("Especialista em furtividade e pericias.")
                .multiplicadorPv(4).multiplicadorPm(2).multiplicadorPe(5)
                .oficial(true).build());

        // Pericias
        periciaRepository.save(pericia(sid, "ATLETISMO", "Atletismo", Atributo.FORCA));
        periciaRepository.save(pericia(sid, "ACROBACIA", "Acrobacia", Atributo.AGILIDADE));
        periciaRepository.save(pericia(sid, "FORTITUDE", "Fortitude", Atributo.VIGOR));
        periciaRepository.save(pericia(sid, "CONHECIMENTO", "Conhecimento", Atributo.INTELECTO));
        periciaRepository.save(pericia(sid, "DIPLOMACIA", "Diplomacia", Atributo.PRESENCA));
        periciaRepository.save(pericia(sid, "PERCEPCAO", "Percepcao", Atributo.INTELECTO));

        // Usuario dev (login: dev@asus.local / senha: dev12345)
        Usuario dev = usuarioRepository.save(Usuario.builder()
                .nome("Dev")
                .email("dev@asus.local")
                .senhaHash(passwordEncoder.encode("dev12345"))
                .build());

        // Organizacao padrao (criterio de aceite 1). Plano GUILD para o demo nao ter limites.
        Organizacao org = organizacaoRepository.save(Organizacao.builder()
                .nome("ASUS Oficial")
                .slug(SLUG_ORG_PADRAO)
                .donoId(dev.getId())
                .plano(Plano.GUILD)
                .build());

        assinaturaRepository.save(Assinatura.builder()
                .organizacaoId(org.getId())
                .plano(Plano.GUILD)
                .status("ATIVA")
                .build());

        membroRepository.save(OrganizacaoMembro.builder()
                .organizacaoId(org.getId())
                .usuarioId(dev.getId())
                .papel(PapelOrganizacao.DONO)
                .build());

        // Campanha inicial (Fase 4) com o dev como mestre.
        Campanha campanha = campanhaRepository.save(Campanha.builder()
                .organizacaoId(org.getId())
                .mestreId(dev.getId())
                .gameSystemId(asus.getId())
                .nome("Campanha Inicial")
                .descricao("Campanha de exemplo criada no seed.")
                .config(CampanhaConfig.builder().rolagemOcultaPermitida(true).build())
                .arquivada(false)
                .build());

        campanhaMembroRepository.save(CampanhaMembro.builder()
                .campanhaId(campanha.getId())
                .usuarioId(dev.getId())
                .papel(PapelCampanha.MESTRE)
                .build());

        log.info("Seed concluido. Organizacao padrao id={} (slug={}), campanha inicial id={}.",
                org.getId(), SLUG_ORG_PADRAO, campanha.getId());
    }

    private Pericia pericia(Long sid, String codigo, String nome, Atributo atributo) {
        return Pericia.builder()
                .gameSystemId(sid)
                .codigo(codigo)
                .nome(nome)
                .atributoBase(atributo.name())
                .oficial(true)
                .build();
    }
}
