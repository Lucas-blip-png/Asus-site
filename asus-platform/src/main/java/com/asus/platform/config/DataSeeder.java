package com.asus.platform.config;

import com.asus.platform.domain.*;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Semeia o sistema ASUS real (fontes: "Asus - Projeto de Sistema", "Classes",
 * "Sistema de Atributos", "PERICIAS"): 13 racas, 26 pericias, 16 classes com
 * suas trilhas (bonus de atributos/pericias + passiva). Idempotente.
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
    private final HabilidadeRepository habilidadeRepository;
    private final ItemJogoRepository itemJogoRepository;
    private final ProgressaoNivelRepository progressaoNivelRepository;
    private final CriaturaRepository criaturaRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final TemplateRepository templateRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /** Conta de dono/admin, configuravel por ambiente (NUNCA hardcode de senha no repo). */
    @Value("${asus.admin.email:dev@asus.local}")
    private String adminEmail;
    @Value("${asus.admin.senha:dev12345}")
    private String adminSenha;
    @Value("${asus.admin.nome:Dono}")
    private String adminNome;

    private Long sid;

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
                      HabilidadeRepository habilidadeRepository,
                      ItemJogoRepository itemJogoRepository,
                      ProgressaoNivelRepository progressaoNivelRepository,
                      CriaturaRepository criaturaRepository,
                      MarketplaceItemRepository marketplaceItemRepository,
                      TemplateRepository templateRepository,
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
        this.habilidadeRepository = habilidadeRepository;
        this.itemJogoRepository = itemJogoRepository;
        this.progressaoNivelRepository = progressaoNivelRepository;
        this.criaturaRepository = criaturaRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.templateRepository = templateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (gameSystemRepository.existsByCodigo(AsusV1Engine.SYSTEM_ID)) {
            log.info("Seed ja aplicado; pulando.");
            // Cada refresh isolado: uma falha (ex.: migracao de coluna) nao derruba o app.
            safeRefresh("admin", this::ensureAdmin);
            safeRefresh("bestiario", this::refreshBestiario);
            safeRefresh("classes", this::refreshClasses);
            safeRefresh("progressao", this::refreshProgressao);
            safeRefresh("pericias", this::refreshPericias);
            safeRefresh("itens", this::refreshItens);
            safeRefresh("habilidades", this::refreshHabilidades);
            safeRefresh("vitrine", this::refreshVitrine);
            return;
        }
        log.info("Aplicando seed do sistema ASUS...");

        GameSystem asus = gameSystemRepository.save(GameSystem.builder()
                .codigo(AsusV1Engine.SYSTEM_ID).nome("ASUS RPG")
                .versao(AsusV1Engine.VERSION).oficial(true).ativo(true).build());
        sid = asus.getId();

        seedRacas();
        seedPericias();
        seedClasses();
        seedProgressao();
        seedItens();
        seedHabilidades();
        seedBestiario();
        seedComunidade(asus);
        seedVitrine();

        log.info("Seed ASUS concluido: {} racas, {} pericias, {} classes, {} niveis, {} itens, {} habilidades, {} criaturas.",
                racaRepository.count(), periciaRepository.count(), classeRepository.count(),
                progressaoNivelRepository.count(), itemJogoRepository.count(), habilidadeRepository.count(),
                criaturaRepository.count());

        ensureAdmin();
    }

    /** Roda um passo de seed/refresh isolado: loga e segue se falhar (nao derruba o boot). */
    private void safeRefresh(String nome, Runnable passo) {
        try {
            passo.run();
        } catch (Exception e) {
            log.warn("Falha ao reaplicar '{}' (seguindo mesmo assim): {}", nome, e.toString());
        }
    }

    /**
     * Garante a conta de dono/admin (configurada por {@code asus.admin.*}). Roda em todo
     * boot: cria a conta se ainda nao existir e a torna DONO da organizacao padrao.
     * A senha vem de variavel de ambiente — nunca fica hardcoded no repositorio.
     */
    private void ensureAdmin() {
        Usuario admin = usuarioRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null) {
            admin = usuarioRepository.save(Usuario.builder()
                    .nome(adminNome == null || adminNome.isBlank() ? adminEmail : adminNome)
                    .email(adminEmail)
                    .senhaHash(passwordEncoder.encode(adminSenha))
                    .build());
            log.info("Conta de dono '{}' criada.", adminEmail);
        }
        final Long adminId = admin.getId();
        organizacaoRepository.findBySlug(SLUG_ORG_PADRAO).ifPresent(org -> {
            boolean jaMembro = membroRepository.findByOrganizacaoId(org.getId()).stream()
                    .anyMatch(m -> adminId.equals(m.getUsuarioId()));
            if (!jaMembro) {
                membroRepository.save(OrganizacaoMembro.builder()
                        .organizacaoId(org.getId()).usuarioId(adminId)
                        .papel(PapelOrganizacao.DONO).build());
            }
            if (!adminId.equals(org.getDonoId())) {
                org.setDonoId(adminId);
                organizacaoRepository.save(org);
            }
        });
    }

    // ---------------- Progressao de 50 niveis (planilha) ----------------

    private void seedProgressao() {
        int[] xp = {0, 100, 210, 330, 460, 600, 750, 910, 1080, 1260, 1450, 1650, 1860, 2080, 2310,
                2550, 2800, 3060, 3330, 3610, 3900, 4200, 4510, 4830, 5160, 5500, 5850, 6210, 6580, 6960,
                7350, 7750, 8160, 8580, 9010, 9450, 9900, 10360, 10830, 11310, 11800, 12300, 12810, 13330,
                13860, 14400, 14950, 15510, 16080, 16660};
        int[] lim = {4, 5, 5, 6, 7, 8, 8, 9, 9, 11, 11, 12, 12, 13, 14, 15, 15, 16, 16, 18, 18, 19, 19, 20,
                21, 22, 22, 23, 23, 25, 25, 26, 26, 27, 28, 29, 29, 30, 30, 32, 32, 33, 33, 34, 35, 36, 36,
                37, 37, 40};
        for (int i = 0; i < 50; i++) {
            int nivel = i + 1;
            progressaoNivelRepository.save(ProgressaoNivel.builder()
                    .gameSystemId(sid).nivel(nivel).xpNecessario(xp[i])
                    .foco(focoDoNivel(nivel)).recompensa(recompensaDoNivel(nivel))
                    .limiteAtributo(lim[i]).build());
        }
    }

    private String focoDoNivel(int n) {
        if (n == 10) return "Classe Primária (MAX)";
        if (n <= 10) return "Classe Primária";
        if (n <= 20) return "Trilha Primária";
        if (n == 30) return "Classe Secundária (MAX)";
        if (n <= 30) return "Classe Secundária";
        if (n <= 40) return "Trilha Secundária";
        return "Nível de Maestria";
    }

    /**
     * Recompensa principal de cada marco.
     * Nivel 1: classe/bonus/atributos. Marco de 10 niveis (10,20,30,40,50): pericia + atributo + PV/PM/PE
     * da classe e raca. Marco de 5 niveis (5,15,25,35,45): so o bonus de PV/PM/PE da classe e raca.
     * Demais niveis: 2 pontos de atributo.
     */
    private String recompensaDoNivel(int n) {
        if (n == 1) return "Classe, Bônus e Atributos";
        if (n % 10 == 0) return "20 pontos de perícia, 10 pontos de Atributo, PV, PM e PE da classe e raça";
        if (n % 5 == 0) return "PV, PM e PE da classe e raça";
        // Niveis de desbloqueio (11, 21, 31, 41): so a habilidade do respectivo desbloqueio,
        // sem pontos de atributo nem de pericia. A passiva entra sozinha ao escolher.
        if (n % 10 == 1 && n > 1) {
            switch (n) {
                case 11: return "Trilha: escolhe a trilha e ganha 1 habilidade dela";
                case 21: return "Classe secundária: escolhe e ganha 1 habilidade dela";
                case 31: return "Trilha secundária: escolhe e ganha 1 habilidade dela";
                case 41: return "Maestria: ganha 1 habilidade de maestria";
                default: return "Ganha 1 habilidade do desbloqueio do nível";
            }
        }
        return "Atributos (2 pontos)";
    }

    // ---------------- Itens (catalogo representativo, moeda T$) ----------------

    private void seedItens() {
        // ===== ARMAS SIMPLES ===== (codigo, nome, grupo, preco, dano, critico, alcance, tipoDano, espacos)
        String simples = "ARMA_SIMPLES", marcial = "ARMA_MARCIAL";
        arma("ADAGA", "Adaga", simples, "Corpo a Corpo - Leves", "2", "1d4", "19", "Curto", "Perfuração", 1);
        arma("ESPADA_CURTA", "Espada curta", simples, "Corpo a Corpo - Leves", "10", "1d6", "19", "", "Perfuração", 1);
        arma("FOICE", "Foice", simples, "Corpo a Corpo - Leves", "4", "1d6", "x3", "", "Corte", 1);
        arma("CLAVA", "Clava", simples, "Corpo a Corpo - Uma Mão", "0", "1d6", "x2", "", "Impacto", 1);
        arma("LANCA", "Lança", simples, "Corpo a Corpo - Uma Mão", "2", "1d6", "x2", "Curto", "Perfuração", 1);
        arma("MACA", "Maça", simples, "Corpo a Corpo - Uma Mão", "12", "1d8", "x2", "", "Impacto", 1);
        arma("BORDAO", "Bordão", simples, "Corpo a Corpo - Duas Mãos", "0", "1d6/1d6", "x2", "", "Impacto", 2);
        arma("PIQUE", "Pique", simples, "Corpo a Corpo - Duas Mãos", "2", "1d8", "x2", "", "Perfuração", 2);
        arma("TACAPE", "Tacape", simples, "Corpo a Corpo - Duas Mãos", "0", "1d10", "x2", "", "Impacto", 2);
        arma("AZAGAIA", "Azagaia", simples, "Ataque à Distância", "1", "1d6", "x2", "Médio", "Perfuração", 1);
        arma("BESTA_LEVE", "Besta leve", simples, "Ataque à Distância", "35", "1d8", "19", "Médio", "Perfuração", 1);
        arma("FUNDA", "Funda", simples, "Ataque à Distância", "0", "1d4", "x2", "Médio", "Impacto", 1);
        arma("ARCO_CURTO", "Arco curto", simples, "Ataque à Distância", "30", "1d6", "x3", "Médio", "Perfuração", 2);

        // ===== ARMAS MARCIAIS =====
        arma("CIMITARRA", "Cimitarra", marcial, "Corpo a Corpo - Uma Mão", "15", "1d6", "18", "", "Corte", 1);
        arma("ESPADA_LONGA", "Espada longa", marcial, "Corpo a Corpo - Uma Mão", "15", "1d8", "19", "", "Corte", 1);
        arma("FLORETE", "Florete", marcial, "Corpo a Corpo - Uma Mão", "20", "1d6", "18", "", "Perfuração", 1);
        arma("MACHADO_BATALHA", "Machado de batalha", marcial, "Corpo a Corpo - Uma Mão", "10", "1d8", "x3", "", "Corte", 1);
        arma("MANGUAL", "Mangual", marcial, "Corpo a Corpo - Uma Mão", "8", "1d8", "x2", "", "Impacto", 1);
        arma("MARTELO_GUERRA", "Martelo de guerra", marcial, "Corpo a Corpo - Uma Mão", "12", "1d8", "x3", "", "Impacto", 1);
        arma("PICARETA", "Picareta", marcial, "Corpo a Corpo - Uma Mão", "8", "1d6", "x4", "", "Perfuração", 1);
        arma("TRIDENTE", "Tridente", marcial, "Corpo a Corpo - Uma Mão", "15", "1d8", "x2", "", "Perfuração", 1);
        arma("ALABARDA", "Alabarda", marcial, "Corpo a Corpo - Duas Mãos", "10", "1d10", "x3", "", "Corte/Perfuração", 2);
        arma("ALFANGE", "Alfange", marcial, "Corpo a Corpo - Duas Mãos", "75", "2d4", "18", "", "Corte", 2);
        arma("GADANHO", "Gadanho", marcial, "Corpo a Corpo - Duas Mãos", "18", "2d4", "x4", "", "Corte", 2);
        arma("LANCA_MONTADA", "Lança montada", marcial, "Corpo a Corpo - Duas Mãos", "10", "1d8", "x3", "", "Perfuração", 2);
        arma("MACHADO_GUERRA", "Machado de guerra", marcial, "Corpo a Corpo - Duas Mãos", "20", "1d12", "x3", "", "Corte", 2);
        arma("MARRETA", "Marreta", marcial, "Corpo a Corpo - Duas Mãos", "20", "3d4", "x2", "", "Impacto", 2);
        arma("MONTANTE", "Montante", marcial, "Corpo a Corpo - Duas Mãos", "50", "2d6", "19", "", "Corte", 2);
        arma("ARCO_LONGO", "Arco longo", marcial, "Distância - Duas Mãos", "100", "1d8", "x3", "Médio", "Perfuração", 2);
        arma("BESTA_PESADA", "Besta pesada", marcial, "Distância - Duas Mãos", "50", "1d12", "19", "Médio", "Perfuração", 2);

        // ===== ARMADURAS E ESCUDOS ===== (codigo, nome, cat, grupo, preco, defesa, penalidade, espacos)
        protecao("ARM_ACOLCHOADA", "Acolchoada", "ARMADURA", "Leves", "5", 1, 0, 2);
        protecao("ARM_COURO", "Couro", "ARMADURA", "Leves", "20", 2, 0, 2);
        protecao("ARM_COURO_BATIDO", "Couro batido", "ARMADURA", "Leves", "35", 3, -2, 2);
        protecao("ARM_GIBAO_PELES", "Gibão de peles", "ARMADURA", "Leves", "25", 4, -3, 2);
        protecao("ARM_COURACA", "Couraça", "ARMADURA", "Leves", "500", 5, -4, 2);
        protecao("ARM_BRUNEA", "Brunea", "ARMADURA", "Pesadas", "50", 5, -2, 5);
        protecao("ARM_COTA_MALHA", "Cota de malha", "ARMADURA", "Pesadas", "150", 6, -2, 5);
        protecao("ARM_LORIGA", "Loriga segmentada", "ARMADURA", "Pesadas", "250", 7, -3, 5);
        protecao("ARM_MEIA", "Meia armadura", "ARMADURA", "Pesadas", "600", 8, -4, 5);
        protecao("ARM_COMPLETA", "Armadura completa", "ARMADURA", "Pesadas", "3000", 10, -5, 5);
        protecao("ESCUDO_LEVE", "Escudo leve", "ESCUDO", "Escudos", "5", 1, null, 1);
        protecao("ESCUDO_PESADO", "Escudo pesado", "ESCUDO", "Escudos", "15", 2, null, 2);

        // ===== ITENS GERAIS =====
        item("MOCHILA", "Mochila", "ITEM_GERAL", "Equipamentos", "10", -2); // alivia 2 de carga (nao ocupa)
        item("CORDA", "Corda", "ITEM_GERAL", "Equipamentos", "10", 1);
        item("LAMPIAO", "Lampião", "ITEM_GERAL", "Equipamentos", "10", 1);
        item("ESPELHO", "Espelho", "ITEM_GERAL", "Equipamentos", "10", 1);
        item("PE_CABRA", "Pé de cabra", "ITEM_GERAL", "Equipamentos", "5", 1);
        item("SACO_DORMIR", "Saco de dormir", "ITEM_GERAL", "Equipamentos", "10", 1);
        item("GAZUA", "Gazua", "FERRAMENTA", "Ferramentas", "5", 1);
        item("INSTR_OFICIO", "Instrumentos de ofício", "FERRAMENTA", "Ferramentas", "30", 1);
        item("MALETA_MEDICA", "Maleta médica", "FERRAMENTA", "Ferramentas", "50", 1);

        // ===== ALQUÍMICOS ===== (ocupam 0,5 espaço)
        item("ACIDO", "Ácido", "ALQUIMICO", "Preparados", "10", 0.5);
        item("BOMBA", "Bomba", "ALQUIMICO", "Preparados", "50", 0.5);
        item("FOGO_ALQUIMICO", "Fogo alquímico", "ALQUIMICO", "Preparados", "10", 0.5);
        item("ESSENCIA_MANA", "Essência de mana", "ALQUIMICO", "Preparados", "50", 0.5);
        item("ELIXIR_AMOR", "Elixir do amor", "ALQUIMICO", "Preparados", "100", 0.5);
        itemPreco("BELADONA", "Beladona", "VENENO", "Venenos", "1500");
        itemPreco("CICUTA", "Cicuta", "VENENO", "Venenos", "60");
        itemPreco("PECONHA_COMUM", "Peçonha comum", "VENENO", "Venenos", "15");
        itemPreco("PECONHA_CONCENTRADA", "Peçonha concentrada", "VENENO", "Venenos", "90");
        itemPreco("PECONHA_POTENTE", "Peçonha potente", "VENENO", "Venenos", "600");

        // ===== ALIMENTAÇÃO & MAIS =====
        itemPreco("RACAO_VIAGEM", "Ração de viagem (dia)", "ALIMENTACAO", "Alimentação", "0.5");
        itemPreco("REFEICAO_COMUM", "Refeição comum", "ALIMENTACAO", "Alimentação", "0.3");
        itemPreco("PRATO_AVENTUREIRO", "Prato do aventureiro", "ALIMENTACAO", "Alimentação", "1");
        itemPreco("SOPA_PEIXE", "Sopa de peixe", "ALIMENTACAO", "Alimentação", "1");
        itemPreco("CAVALO", "Cavalo", "ANIMAL", "Animais", "75");
        itemPreco("CAVALO_GUERRA", "Cavalo de guerra", "ANIMAL", "Animais", "400");
        itemPreco("PONEI", "Pônei", "ANIMAL", "Animais", "55");
        itemPreco("CAO_CACA", "Cão de caça", "ANIMAL", "Animais", "150");
        itemPreco("CARROCA", "Carroça", "VEICULO", "Veículos", "150");
        itemPreco("CARRUAGEM", "Carruagem", "VEICULO", "Veículos", "500");
        itemPreco("CANOA", "Canoa", "VEICULO", "Veículos", "70");
        itemPreco("VELEIRO", "Veleiro", "VEICULO", "Veículos", "10000");
        itemPreco("ESTADIA_COMUM", "Estadia comum", "SERVICO", "Serviços", "0.5");
        itemPreco("ESTADIA_LUXO", "Estadia luxo", "SERVICO", "Serviços", "20");
        itemPreco("CURANDEIRO", "Curandeiro", "SERVICO", "Serviços", "5");
        itemPreco("MAGIA_1", "Magia 1º círculo", "SERVICO", "Serviços", "10");
        itemPreco("MAGIA_2", "Magia 2º círculo", "SERVICO", "Serviços", "90");
        itemPreco("MAGIA_3", "Magia 3º círculo", "SERVICO", "Serviços", "360");
    }

    /** Reaplica o catalogo de itens do ASUS em banco ja existente (substitui os oficiais). */
    void refreshItens() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            itemJogoRepository.deleteAll(itemJogoRepository.findByGameSystemIdAndOficialTrue(sid));
            seedItens();
        });
    }

    private void arma(String codigo, String nome, String cat, String grupo, String preco, String dano,
                      String critico, String alcance, String tipoDano, double espacos) {
        itemJogoRepository.save(ItemJogo.builder()
                .gameSystemId(sid).codigo(codigo).nome(nome).categoria(cat).grupo(grupo)
                .preco(new java.math.BigDecimal(preco)).moeda("T$")
                .dano(dano).critico(critico).alcance(alcance == null || alcance.isBlank() ? null : alcance)
                .tipoDano(tipoDano).espacos(espacos).oficial(true).build());
    }

    private void protecao(String codigo, String nome, String cat, String grupo, String preco,
                          int bonusDefesa, Integer penalidade, double espacos) {
        itemJogoRepository.save(ItemJogo.builder()
                .gameSystemId(sid).codigo(codigo).nome(nome).categoria(cat).grupo(grupo)
                .preco(new java.math.BigDecimal(preco)).moeda("T$")
                .bonusDefesa(bonusDefesa).penalidade(penalidade).espacos(espacos)
                .oficial(true).build());
    }

    private void item(String codigo, String nome, String cat, String grupo, String preco, double espacos) {
        itemJogoRepository.save(ItemJogo.builder()
                .gameSystemId(sid).codigo(codigo).nome(nome).categoria(cat).grupo(grupo)
                .preco(new java.math.BigDecimal(preco)).moeda("T$")
                .espacos(espacos).oficial(true).build());
    }

    private void itemPreco(String codigo, String nome, String cat, String grupo, String preco) {
        itemJogoRepository.save(ItemJogo.builder()
                .gameSystemId(sid).codigo(codigo).nome(nome).categoria(cat).grupo(grupo)
                .preco(new java.math.BigDecimal(preco)).moeda("T$")
                .oficial(true).build());
    }

    // ---------------- Bestiario (conjunto inicial de criaturas oficiais) ----------------

    private void seedBestiario() {
        seedCriaturasAsus();
    }

    /** Reaplica o bestiario autoral em bancos ja existentes (roda em todo boot). */
    void refreshBestiario() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            criaturaRepository.deleteAll(criaturaRepository.findByGameSystemIdAndOficialTrue(sid));
            seedCriaturasAsus();
        });
    }

    /** Bestiario autoral do ASUS (categorias + ranks; stats detalhados entram depois). */
    private void seedCriaturasAsus() {
        // ---- Criaturas humanoides ----
        criaturaAsus("Grande Lobo Nevado", "Humanoide", "S+",
                "Lobo das Neves, Lobisomem sem Cor, Fenrir ou Fera Branca. Uma criatura de origem "
                + "desconhecida que assombra a noite ao norte; principal motivo dos anoes se abrigarem "
                + "nas grandes montanhas. Em sua ultima aparicao dizimou um grupo de aventureiros.");
        criaturaAsus("Demogorgon", "Humanoide", "SSS?",
                "Uma das criaturas mais grotescas que ja existiu; devastou o antigo inferno. Sua "
                + "aparencia e como a uniao bestial de varios animais. Dizem que poderia dizimar um "
                + "reino inteiro sozinho.");
        criaturaAsus("Os 4 Shoguns do Apocalypse", "Humanoide", "A, A+, S",
                "Grupo formado apos a grande Guerra Tormenta. Dizem que ja foram Onis que cederam ao "
                + "poder e entregaram suas almas por forca. Agem como assassinos de figuras poderosas, "
                + "com trabalho em equipe impecavel e imbativel.");
        criaturaAsus("Centorium", "Humanoide", "A",
                "O unico sobrevivente da raca dos centauros, extinta durante a grande Guerra Tormenta. "
                + "Abracou seu lado frio, tornando-se um monstro sem piedade contra qualquer raca que "
                + "passe em sua frente.");
        criaturaAsus("Anubis", "Humanoide", "S+",
                "Uma das criaturas mais antigas destas terras. Extremamente inteligente, possui o poder "
                + "unico de 5 elementos diferentes sem se prender aos Deuses; por isso, em algumas "
                + "culturas antigas, foi considerado um Deus.");
        criaturaAsus("Wendigo", "Humanoide", "B+",
                "Misteriosa criatura das florestas do norte. Usa o frio como principal aliado na caca, "
                + "esperando as presas ficarem debilitadas o suficiente para mata-las.");
        criaturaAsus("Jack, a Lanterna", "Humanoide", "A",
                "Em vida, um assassino maniaco que dilacerava a quem quisesse. Executado publicamente, "
                + "queimou em uma fogueira coberto de palha como um espantalho; logo apos, usando os "
                + "poderes de Tenebris, lancou sua alma vingativa no mundo.");
        criaturaAsus("Medusa", "Humanoide", "B",
                "Humana seguidora do Deus Drakko que ajudou a besta Hydra debilitada em troca de "
                + "protecao. Aprendeu a lingua unica da Hydra (variacao da linguagem Draconica) e, no "
                + "leito de morte, abencoou seu embriao em nome de Hydra - assim nasceu Medusa.");

        // ---- Criaturas bestiais ----
        criaturaAsus("Dragao Ancestral Carmesim", "Bestial", "SS",
                "Faz parte dos dragoes ancestrais, os primeiros criados por Drakko. Seu poder se "
                + "sobressai entre todos os dragoes, so se igualando aos irmaos que mantem o poder "
                + "original. Vive onde as montanhas tocam o ceu.");
        criaturaAsus("Dragao Ancestral Corrupcao", "Bestial", "SS",
                "Um dos tres ancestrais e o mais cruel de todos. Capaz de invadir a mente das pessoas e "
                + "prometer poderes inimaginaveis, corrompendo os de coracao mais puro. Odeia todos os "
                + "outros dragoes, julgando-se superior.");
        criaturaAsus("Dragao Primordial Consagracao", "Bestial", "SS",
                "Poderoso e brilhante, o mais forte entre os tres, responsavel por cuidar dos dragoes "
                + "inferiores que regem o mundo. Gracas a ele a raca dos Dragoes possui inumeros vivos.");
        criaturaAsus("Hydra", "Bestial", "S+",
                "Dragao que sofreu mutacoes impossiveis e sobreviveu, ganhando a habilidade de restaurar "
                + "e dobrar as cabecas quando decepadas. Ninguem sabe quantas cabecas ela ja possui.");
        criaturaAsus("Fenix", "Bestial", "A",
                "O lendario passaro imortal. Inumeros herois tentaram mata-la, mas cederam a sua "
                + "habilidade de voltar dos mortos - a unica criatura que retorna sem virar um zumbi.");
        criaturaAsus("Planterra", "Bestial", "?",
                "As lendas contam que, na floresta mais profunda, existe um bulbo de flor rosa sagrado "
                + "para a existencia da floresta. Se alguem retira-lo, a floresta reage como um conjunto, "
                + "criando sua morte certa.");
        criaturaAsus("Cerberus", "Bestial", "S+",
                "O infernal cao demoniaco e principal protetor de Hades, formado pela ira dos vulcoes do "
                + "Inferno. Serve os reis demonios desde os primordios de Asus, leal apenas ao Rei do "
                + "Inferno que possui o sangue da familia real.");
        criaturaAsus("Leviathan", "Bestial", "SSS+?",
                "Forca da natureza que existe antes dos proprios Deuses do Baixo Escalao. Coromuel, deus "
                + "dos mares, a fez adormecer por seculos; durante a Guerra Tormenta despertou e inundou "
                + "grande parte do continente, voltando a repousar em seguida.");
    }

    private void criaturaAsus(String nome, String categoria, String rank, String descricao) {
        criaturaRepository.save(Criatura.builder()
                .gameSystemId(sid).nome(nome).categoria(categoria).rank(rank)
                .especie(categoria).tipo(categoria)
                .nivel(0).pv(0).pm(0).pe(0).defesa(0)
                .descricao(descricao).oficial(true).build());
    }

    // ---------------- Habilidades (representativas, gerais + por classe) ----------------

    private void seedHabilidades() {
        // ---- Habilidades do ASUS (importadas do docx; classeCodigo aceita lista separada por virgula) ----
        hab("ATAQUE_FORTE", "Ataque Forte", "BARBARO,CAVALEIRO,LUTADOR", "ATIVA", 2, "PE",
                "Cavaleiro; Bárbaro; Lutador.", "O combatente concentra toda a força em um único golpe brutal e direto, abrindo mão da precisão para causar o máximo de impacto possível.Ao utilizar toda a sua força poderá desferir um ataque poderoso, porém previsível, a cada 2 de PE que o usuário gastar, será aumentado +1 dado de dano, mas também terá -5 na sua perícia de ataque.");
        hab("CHANCE_UNICA", "Chance Única", "ASSASSINO", "ATIVA", 5, "PE",
                "Assassino.", "“O assassino aposta tudo em um único momento decisivo, desferindo um golpe pensado para encerrar o confronto antes que ele realmente comece.”A fim de não extender o confronto, este ataque faz com que o primeiro golpe desferido em 1 alvo cause o dobro de dano, porém, se o alvo não morrer instantaneamente você causará -2 dados de dano em seus próximos 2 ataques contra o mesmo alvo.");
        hab("PASSOS_LEVES", "Passos Leves", "LADRAO", "ATIVA", 10, "PE",
                "Ladrão.", "Movendo-se com leveza e agilidade extrema, o ladrão cruza o campo de batalha como uma sombra, aproveitando cada brecha para se reposicionar rapidamente.Ao utilizar a Ação Secundária para se deslocar, poderá gastar PE, limitado a metade do seu atributo de Agilidade, adicionando a metade desse valor em deslocamento, uma vez por cena. Ao utilizar a Ação Secundária para deslocar-se também poderá utilizar 10 PÉ para ganhar uma Ação Secundária a mais em seu turno.");
        hab("ESTADO_DE_FURIA", "Estado de Fúria", "BARBARO", "ATIVA", 3, "PE",
                "Bárbaro.", "O bárbaro se entrega à própria raiva, transformando dor e ódio em força bruta, mantendo-se em combate enquanto a fúria arde em seu corpo.Força a si mesmo o sentimento de raiva e fúria, o que aumentará seu desempenho em combate lhe concedendo +2 em testes físicos e +5 de dano por acúmulo de fúria gasto, o efeito também durará o tanto de fúria gasto em Turnos. Ganha 1 acúmulo de fúria ao final da habilidade.");
        hab("ATAQUE_DEBILITANTE", "Ataque Debilitante", "ASSASSINO", "ATIVA", 2, "PE",
                "Assassino", "O assassino ataca pontos vitais e membros expostos, enfraquecendo o inimigo de forma progressiva a cada golpe bem executado.Ao cortar uma parte específica do inimigo e causar dano com base na constituição do inimigo, o alvo logo ficará com o estado de “Debilitado”. Assim continuamente aumentando os efeitos toda vez que isso ocorrer.");
        hab("FEITICARIA_DE_1_GRAU", "Feitiçaria de 1° Grau", "GERAL", "PASSIVA", 0, null,
                "Nível 1", "O personagem domina os fundamentos da magia, sendo capaz de conjurar feitiços simples e compreender, ainda que de forma instável, encantamentos mais avançados. A partir deste conhecimento, o personagem é capaz de conjurar feitiços de 1º Grau normalmente. Além disso, demonstra uma compreensão precoce que permite o uso de feitiços de 2º Grau. No entanto, os feitiços de 2º Grau possuem o custo triplicado em relação ao valor original, e esse custo não pode ser reduzido sem a aquisição da habilidade \"Feitiçaria de 2º Grau”.");
        hab("FEITICARIA_DE_2_GRAU", "Feitiçaria de 2° Grau", "GERAL", "PASSIVA", 0, null,
                "Feitiçaria de 1° Grau & Nível 10", "Com maior controle arcano, o conjurador lança magias mais poderosas e começa a manipular forças que exigem concentração e preparo superiores. A partir deste conhecimento, o personagem é capaz de conjurar feitiços de 2º Grau normalmente. Além disso, demonstra uma compreensão precoce que permite o uso de feitiços de 3º Grau. No entanto, os feitiços de 3º Grau possuem o custo triplicado em relação ao valor original, e esse custo não pode ser reduzido sem a aquisição da habilidade \"Feitiçaria de 3º Grau”.");
        hab("FEITICARIA_DE_3_GRAU", "Feitiçaria de 3° Grau", "GERAL", "PASSIVA", 0, null,
                "Feitiçaria de 2° Grau & Nível 20", "O mago alcança um patamar elevado de entendimento mágico, moldando energias arcanas complexas e flertando com feitiços de grande impacto. A partir deste conhecimento, o personagem é capaz de conjurar feitiços de 3º Grau normalmente. Além disso, demonstra uma compreensão precoce que permite o uso de feitiços de 4º Grau. No entanto, os feitiços de 4º Grau possuem o custo triplicado em relação ao valor original, e esse custo não pode ser reduzido sem a aquisição da habilidade \"Feitiçaria de 4º Grau”.");
        hab("FEITICARIA_DE_4_GRAUEFEITO", "Feitiçaria de 4° Grau", "BRUXO,CARTOMANTE,DRUIDA,MAGO,QUIMICO_ARCANO", "PASSIVA", 0, null,
                "Mago; Bruxo; Druida; Químico Arcano; Cartomante & (Feitiçaria de 3° Grau & Nível 40)", "A partir deste conhecimento, o personagem é capaz de conjurar feitiços de 4º Grau normalmente.");
        hab("ESTOURAR_SANGRAMENTO", "Estourar Sangramento", "ESPADACHIM,SICARIO", "ATIVA", 2, "PE",
                "Sicário, ou Espadachim", "Ao atacar alguém que está sangrando, você pode optar por estourar o “Sangramento” o que "
                        + "duplicará o dano de sangramento que o alvo receberá naquele turno, removendo todos os acúmulos "
                        + "no processo.");
        hab("INVESTIDA", "Investida", "GERAL", "ATIVA", 2, "PE",
                "Arma Perfurante", "Segure sua arma com ambas as mãos e avance contra um inimigo distante para empalá-lo, ao avançar unicamente em uma linha reta, receberá metade do valor de “Deslocamento” atual no seu total, e causará seu “Deslocamento” total a mais de dano.");
        hab("ARMAZENAR_ALMAS", "Armazenar Almas", "MISTICO", "PASSIVA", 0, null,
                "Místico", "Uma habilidade desenvolvida por aqueles que possuem um bom uso para almas, é capaz de guardá-las em um espaço que somente o usuário tem acesso. O limite de almas é de 4 a cada 10 Níveis do Personagem.");
        hab("AUMENTO_DE_ATRIBUTO", "Aumento de Atributo", "GERAL", "PASSIVA", 0, null,
                "Todas as Classes", "Você recebe +2 pontos de atributo para distribuir como desejar. Você pode escolher várias vezes essa habilidade e o valor ganho aumenta de acordo com o Nível de Personagem atual. Sendo +2 a cada 10 níveis de Personagem.");
        // Habilidades novas de Lutador (revisao enviada pelo autor).
        hab("RITMO_DO_RINGUE", "Ritmo do Ringue", "ARTISTA_MARCIAL", "ATIVA", 4, "PE",
                "Artista Marcial", "“Esquerda pra abrir a guarda. Direita pra fechar a conta.” O Lutador entra na cadência de quem "
                        + "cresceu trocando golpes no ringue, onde cada soco já prepara o seguinte e a guarda nunca para "
                        + "de se mover. Ativação: Ação Livre. O Lutador entra em Ritmo até o fim do combate ou até ficar 1 "
                        + "turno sem desferir um ataque desarmado. Durante o Ritmo, seus ataques desarmados alternam "
                        + "entre: Jab (golpes ímpares), golpe rápido que concede +1 em Esquiva até o início do seu próximo "
                        + "turno; e Direto (golpes pares), golpe pesado que causa dano extra de +2 a cada 5 de Força e "
                        + "empurra o alvo em 1 metro. Funciona apenas desarmado e com armadura leve.");
        hab("INVESTIDA_ESMAGADORA", "Investida Esmagadora", "LUTADOR", "ATIVA", 6, "PE",
                "Agilidade 10", "“Corre. Não vai adiantar, mas corre.” Aproveitando o impulso da corrida, o Lutador transforma o "
                        + "próprio peso em dois golpes que desabam sobre o inimigo antes que ele perceba a distância "
                        + "encurtando. Ativação: Ação Principal. O Lutador se move em linha reta em direção a um inimigo, "
                        + "recebendo metade do valor de Deslocamento nesse movimento. Ao alcançá-lo, seus dois próximos "
                        + "ataques desarmados causam dano extra igual ao seu deslocamento total. Se percorreu pelo menos o "
                        + "seu deslocamento total antes de acertar, o primeiro golpe é crítico.");
        hab("ESMAGA_CRANIOS", "Esmaga-Crânios", "LUTADOR", "ATIVA", 5, "PE",
                null, "“De um lado e do outro. Agora vão se conhecer de bem perto.” Técnica brutal de quem aprendeu a "
                        + "lutar cercado: o Lutador agarra dois oponentes pelos lados e os arremessa um contra o outro num "
                        + "único impacto. Ativação: Ação Principal. O Lutador puxa até 2 inimigos adjacentes (um de cada "
                        + "lado) para junto de si, exigindo um teste de Atletismo contra a resistência de cada alvo. Ambos "
                        + "sofrem dano igual ao seu ataque desarmado, com dano extra de +2 a cada 5 de Força. Se os dois "
                        + "colidirem (estavam em lados opostos), ambos ficam Confusos por 1 turno. Contra um único alvo, "
                        + "em vez disso ele fica com Lentidão até o fim do seu próximo turno.");
        hab("CRATERA", "Cratera", "CAMPEAO", "ATIVA", 6, "PE",
                "Campeão", "“Eu não venço a luta. Eu encerro o assunto.” A finalização do Lutador: ele ergue o inimigo "
                        + "acima da cabeça e o esmaga contra o chão com todo o peso do corpo, abrindo um buraco no ponto "
                        + "de impacto. Ativação: Ação Completa. O Lutador agarra um inimigo (teste de Atletismo contra a "
                        + "resistência do alvo). Se bem-sucedido, ergue e arremessa o alvo no chão, causando dano igual ao "
                        + "seu ataque desarmado somado à superfície: mole, +2 a cada 5 pontos de Força mais o dano da "
                        + "altura da queda; comum, +4 a cada 5 pontos de Força mais o dano da queda; rígida, +8 a cada 5 "
                        + "pontos de Força mais o dano da queda. Todos os inimigos adjacentes ao ponto de impacto sofrem "
                        + "metade desse dano. Quanto maior o alvo agarrado (maior que o Lutador), maior a área de impacto, "
                        + "a critério do Mestre.");
        // Novas habilidades do Patch v0.1 (secao "Novidades").
        hab("MINUCIOSO", "Minucioso", "GERAL", "PASSIVA", 0, null,
                "Destreza 10", "Você domina armas pequenas com extrema precisão, transformando velocidade e técnica em golpes "
                        + "letais. Para cada 10 pontos no atributo Destreza, adiciona +2 de dano para armas pequenas.");
        hab("VERSATIL", "Versátil", "GERAL", "PASSIVA", 0, null,
                "Agilidade 10", "Você domina armas médias com equilíbrio e técnica, aproveitando cada abertura para desferir "
                        + "golpes eficientes. Para cada 10 pontos no atributo Agilidade, adiciona +2 de dano para armas "
                        + "médias.");
        hab("BRUTAMONTES", "Brutamontes", "GERAL", "PASSIVA", 0, null,
                "Força 10", "Você utiliza sua força para desferir golpes devastadores, extraindo o máximo poder de armas "
                        + "pesadas. Para cada 10 pontos no atributo Força, adiciona +2 de dano para armas grandes.");
        hab("ERUDITO", "Erudito", "GERAL", "PASSIVA", 0, null,
                "Inteligência 10", "Você domina o uso de catalizadores arcanos, canalizando seu conhecimento para potencializar "
                        + "feitiços. Para cada 10 pontos no atributo Inteligência, adiciona +2 de dano para Feitiços ao "
                        + "utilizar um catalizador.");
        hab("DEVOTO", "Devoto", "GERAL", "PASSIVA", 0, null,
                "Sabedoria 10", "Você domina o uso de catalizadores sagrados, canalizando sua fé para fortalecer bênçãos "
                        + "ofensivas. Para cada 10 pontos no atributo Sabedoria, adiciona +2 de dano para Bênçãos ao "
                        + "utilizar um catalizador.");
        hab("CRIACAO_DE_POCOES", "Criação de Poções", "GERAL", "PASSIVA", 0, null,
                "Alquimia 10", "O usuário é capaz de utilizar seus conhecimentos em Alquimia para produzir diferentes tipos de "
                        + "poções. Para criar uma poção, deve possuir os ingredientes necessários e realizar um teste de "
                        + "Alquimia. A qualidade da poção aumenta em um passo para cada 10 pontos de Inteligência do "
                        + "usuário.");
        hab("RECARGA_ARCANA", "Recarga Arcana", "GERAL", "ATIVA", 0, "PM",
                "Nível 10", "Durante um Descanso Longo, o usuário pode canalizar sua própria Mana para um Cajado Mágico, "
                        + "restaurando PM no valor de metade da Mana utilizada. Caso possua a habilidade Absorção Arcana, "
                        + "pode transferir os PM diretamente para o cajado, sem gastar sua própria Mana no processo. "
                        + "(Custo variável de PM.)");
        hab("ENCERRAR_DUELO", "Encerrar Duelo", "DUELISTA", "ATIVA", 5, "PE",
                "Duelista e Destreza 15", "Enquanto a habilidade Duelar estiver ativa, acumule turnos de duelo. Ao ativar esta habilidade, "
                        + "encerre Duelar e no mesmo turno, contra o mesmo alvo, receba 1 dado de dano com lâminas e +2 de "
                        + "Acerto por cada turno acumulado. Se o alvo for derrotado, recupere metade dos PE gastos com "
                        + "Duelar; se ele sobreviver, perca novamente os PE gastos com Duelar.");
        hab("KIT_DE_REPAROS", "Kit de Reparos", "INVENTOR", "ATIVA", 3, "PE",
                "Inventor", "O usuário realiza reparos rápidos em um mecanismo ou construto, restaurando 2d6 PV. Caso o alvo "
                        + "seja uma Torreta Portátil ou outro dispositivo criado pelo usuário, também remove um efeito "
                        + "negativo relacionado a danos.");
        hab("SUCATA_IMPROVISADA", "Sucata Improvisada", "INVENTOR", "ATIVA", 4, "PE",
                "Inventor", "O usuário improvisa um pequeno mecanismo utilizando materiais encontrados no ambiente. O "
                        + "dispositivo funciona normalmente por 1 turno antes de se desfazer, podendo realizar apenas uma "
                        + "única função simples, a critério do mestre.");
        hab("FURIOSO", "Furioso", "BARBARO", "PASSIVA", 0, null,
                "Bárbaro", "Sempre que sofrer dano, o usuário recebe 1 Acúmulo de Fúria, respeitando o limite de 1 acúmulo "
                        + "por rodada a cada 5 Níveis do Personagem. Ao passar 1 turno sem sofrer dano, perde 1 Acúmulo de "
                        + "Fúria.");
        // Habilidades Gerais (estavam grudadas na descricao de "Relato de Viagem" no documento).
        hab("VONTADE_DE_SOBREVIVENCIA", "Vontade de Sobrevivência", "GERAL", "PASSIVA", 0, null,
                "Constituição 15 e nível 20", "Quando o PV chegar a zero pela primeira vez em combate, o personagem permanece com 1 PV. (Pode ser usada 1 vez por dia).");
        hab("TREINAMENTO_DE_CAMPO", "Treinamento de Campo", "GERAL", "PASSIVA", 0, null,
                null, "Concede um bônus permanente de +2 em uma perícia à escolha do jogador (aumentando em +2 a cada 5 níveis).");
        hab("FOCO_DE_ADRENALINA", "Foco de Adrenalina", "GERAL", "ATIVA", 0, null,
                "Constituição 10", "Como uma Ação Livre, o usuário pode utilizar PV ao invés de PE para realizar uma habilidade de classe (uma vez por dia).");
        hab("MAOS_FIRMES", "Mãos Firmes", "GERAL", "PASSIVA", 0, null,
                "Destreza 8", "O personagem ganha Vantagem em testes para evitar ser desarmado ou derrubar objetos.");
        hab("AMBIDESTRIA", "Ambidestria", "GERAL", "PASSIVA", 0, null,
                "Destreza 10", "O usuário poderá utilizar de 2 armas ao mesmo tempo, podendo atacar com ambas em apenas 1 ação. Para isso é necessário ter a capacidade de usar ambas as armas individualmente, somando seus requerimentos.");
        hab("MASTERIZAR_ARMA", "Masterizar Arma", "GERAL", "PASSIVA", 0, null,
                "Todas as Classes", "Escolha uma arma. Com essa arma, seu dano aumenta em um passo, porém caso utilize uma arma que não seja a sua, seu dano diminui em um passo. Você pode pegar até 2 vezes essa habilidade");
        hab("REDUCAO_DE_DANO", "Redução de Dano", "BARBARO,ESCUDEIRO,PALADINO", "ATIVA", 3, "PE",
                "Bárbaro; Escudeiro; Paladino", "Ao sofrer um ataque, você pode escolher receber o ataque e gastar sua reação para adicionar seu valor de “Constituição” em sua “Armadura”.");
        hab("DUELAR", "Duelar", "CAVALEIRO,ESPADACHIM,LUTADOR", "ATIVA", 4, "PE",
                "Espadachim, Cavaleiro, Lutador", "Ao entrar em uma luta um contra um no alvo selecionado, irá receber “Vantagem” para atacar o "
                        + "alvo e “Desvantagem” para atacar qualquer outro que não seja o alvo selecionado, também recebe "
                        + "+5 em Armadura a cada 5 Níveis do Personagem, contra qualquer ataque vindo de fora do Duelo. Manter o Duelo custa 2 PE por Turno.");
        hab("ESCUDO_ALIADO", "Escudo Aliado", "CAVALEIRO,LUTADOR,MONGE,PALADINO", "ATIVA", 3, "PE",
                "Cavaleiro, Monge, Lutador, Paladino", "Caso um aliado esteja sendo atacado, você pode gastar sua Reação para tomar o ataque para si e ficar a frente de seu aliado, se o fizer receberá um aumento equivalente ao Nível atual do personagem em Armadura contra esse ataque.");
        hab("MURALHA_IMPENETRAVEL", "Muralha Impenetrável", "CAVALEIRO,PALADINO", "ATIVA", 5, "PE",
                "Cavaleiro, Paladino", "Ao defender um ataque e reduzir o dano sofrido para 0 com o seu valor de armadura, esse irá instantaneamente causar “Intimidado” em seu atacante nesta rodada / Além disso poderá realizar um ataque contra seu alvo de volta.");
        hab("BRAVURA_FINAL", "Bravura Final", "CAVALEIRO,IMORTAL", "ATIVA", 4, "PE",
                "Cavaleiro, Imortal", "Quando o PV chega em 0, pode-se ativar esse poder e assim se tornar imune a condição de “inconsciente”, fazendo um teste de “Vigor” a cada turno para permanecer de pé, sendo a DT inicial 20 e aumentando mais 20 a cada turno que se passar.");
        hab("ULTIMO_ESFORCO", "Último Esforço", "CAVALEIRO", "ATIVA", 8, "PE",
                "Cavaleiro & Nível 20", "Com a habilidade “Bravura Final” ativa, é possível motivar-se para aumentar seu desempenho até seus últimos minutos em pé, se assim feito o personagem ganha +1 “Ação Principal” a cada turno que permanecer de pé.");
        hab("EMPUNHADURA_SANGRENTA", "Empunhadura Sangrenta", "ASSASSINO", "ATIVA", 3, "PE",
                "Assassino", "Faz com que nos próximos 3 turnos seguintes, a sua arma cause +1 acúmulo de “Sangramento” ao atingir um alvo.");
        hab("ATAQUE_PREDOMINANTE", "Ataque Predominante", "ASSASSINO,ESPADACHIM,LANCEIRO,LUTADOR,MONGE", "ATIVA", 6, "PE",
                "Assassino, Monge, Espadachim, Lanceiro, Lutador", "Pode usar sua “Reação” para lançar um ataque contra o ataque do inimigo, se feito ambos rodam seu dano, os valores serão subtraídos e causará o valor em dano a aquele que causou mais dano. Para funcionar é necessário que ambos os ataques possuam o mesmo tipo de dano.");
        hab("VULTO_AMEACADOR", "Vulto Ameaçador", "ARQUEIRO,ASSASSINO,LADRAO", "ATIVA", 4, "PE",
                "Assassino, Arqueiro, Ladrão", "Ao entrar em furtividade durante um combate que já havia sido percebido, pode-se usar essa habilidade para aumentar sua margem de ameaça em +2, até seu próximo ataque / Além disso, caso consiga entrar em furtividade novamente depois de usar essa habilidade, ganhará +2 dados de dano crítico.");
        hab("ARREMESSO_CERTEIRO", "Arremesso Certeiro", "ASSASSINO,CARTOMANTE,LADRAO,LANCEIRO", "ATIVA", 3, "PE",
                "Assassino, Ladrão, Lanceiro, Cartomante", "Ao utilizar armas corpo a corpo para arremessar em seu alvo, ganha-se +2 Pontos em “Arremessar” a cada 5 Níveis do Personagem, além disso o dano de sua arma aumenta em um passo para esse ataque.");
        hab("INSTINTO_ASSASSINO", "Instinto Assassino", "ASSASSINO", "ATIVA", 4, "PE",
                "Assassino & Nível 20", "Ao esquivar-se com sucesso de um ataque, poderá realizar um teste de “Acrobacia” para se movimentar rapidamente para trás de seu inimigo, com isso poderá utilizar a habilidade “Chance Única” irrestritamente. A DT para o teste de acrobacia é a defesa natural do alvo.");
        hab("SELVAGEM", "Selvagem", "BARBARO,CACADOR,DRUIDA", "ATIVA", 2, "PE",
                "Bárbaro, Druida, Caçador", "Ao perseguir um alvo que tenha lhe causado dano no turno anterior, poderá movimentar-se sem gastar sua “Ação Secundária”.");
        hab("FURIA_INCANSAVEL", "Fúria Incansável", "BARBARO", "PASSIVA", 0, null,
                "Bárbaro", "Após o término da habilidade “Estado de Fúria”, o usuário ainda permanecerá com metade dos acúmulos de fúria gastos. (Passivo)");
        hab("CORPO_SAUDAVEL", "Corpo Saudável", "BARBARO", "PASSIVA", 0, null,
                "Bárbaro", "O usuário reduz metade do dano para o primeiro ataque que receber no dia.");
        hab("FORCA_IMPARAVEL", "Força Imparável", "BARBARO", "ATIVA", 8, "PE",
                "Bárbaro", "O usuário torna-se irracional atacando qualquer coisa em sua frente, assim dobrando os efeitos de “Estado de Fúria” e dobrando sua armadura natural, porém é necessário atacar um alvo todos os turnos, até acabar.");
        hab("ESCOLHA_DIVINA", "Escolha Divina", "CLERIGO", "PASSIVA", 0, null,
                "Clérigo", "Escolha um Deus. Sua armadura natural dobra e você tem “Vantagem” em testes de resistência a efeitos negativos contra “Bênçãos” do seu Deus escolhido. Você pode pegar essa habilidade várias vezes, porém somente 2 vezes para o mesmo Deus.");
        hab("PRECE_DIVINA", "Prece Divina", "CLERIGO", "PASSIVA", 0, null,
                "Clérigo", "Escolha entre receber uma nova bênção de sua Divindade ou aumentar a categoria de uma bênção que já possuí. Você pode escolher essa habilidade várias vezes, porém a cada 5 níveis do Personagem.");
        hab("CONVERSAO", "Conversão", "CLERIGO", "ATIVA", 3, "PE",
                "Clérigo", "Durante um combate ao escolher uma Divindade qualquer, todas as bênçãos utilizadas daquela Divindade terão seu dano reduzido em um passo e todos presentes terão +10 de Armadura contra bênçãos de tal Divindade.");
        hab("MIMETIZACAO_DIVINA", "Mimetização Divina", "CLERIGO,LADRAO", "ATIVA", 6, "PE",
                "Clérigo, Ladrão", "Uma vez por cena o usuário é capaz de tomar para sí a bênção de alguém com a mesma Divindade que sí, a bênção é aleatória e a cada uso seu gasto aumenta em 2 PE.");
        hab("MORTE_AOS_HEREGES", "Morte aos Hereges", "CLERIGO", "ATIVA", 5, "PE",
                "Clérigo", "Ao atacar um alvo que não possua sua Divindade, o dano de suas bênçãos aumentará em um passo, além disso caso a Divindade do alvo seja de outro Panteão este terá “Desvantagem” para reagir.");
        hab("VINCULO_DIVINO", "Vínculo Divino", "CLERIGO", "PASSIVA", 0, null,
                "Clérigo", "O usuário poderá realizar um ritual especial, para se conectar com sua Divindade, dessa forma aumentando a categoria de +1 benção a cada 5 Níveis do Personagem. Você pode pegar essa habilidade várias vezes, porém a cada 5 Níveis do Personagem. (Passivo)");
        hab("BENCAO_MAIOR", "Bênção Maior", "CLERIGO,CURANDEIRO", "ATIVA", 2, "PE",
                "Clérigo, Curandeiro", "O usuário torna-se capaz de invocar até 2 bênçãos por turno em sua “Ação Principal”.");
        hab("FE_LEAL", "Fé Leal", "CLERIGO", "ATIVA", 5, "PE",
                "Clérigo", "Você pode gastar sua Reação para realizar um teste de “Fé” contra uma bênção do inimigo, ao ganhar o embate você converte a mana de tal bênção para si.");
        hab("BRECHA_CELESTE", "Brecha Celeste", "CLERIGO", "ATIVA", 8, "PE",
                "Clérigo", "O usuário converte toda a mana ao seu redor para a de sua Divindade por um breve momento, fazendo com que some sua “Fé” em sua armadura contra bênçãos de outra Divindade, além disso reduzindo o gasto de suas bênçãos em -2 a cada 5 Níveis do Personagem. A habilidade permanece ativa por 1d4 Turnos.");
        hab("VINCULO_NATURAL", "Vínculo Natural", "DRUIDA", "ATIVA", 0, null,
                "Druida", "Pode-se escolher 1 companheiro bestial como seu aliado, começando em uma categoria inferior como C ou D. Caso escolha novamente essa habilidade, poderá ganhar um novo companheiro ou subir a categoria de um que já possua. Essa habilidade pode ser escolhida várias vezes, porém a cada 10 Níveis do Personagem.");
        hab("MAGIA_DRUIDA", "Magia Druida", "DRUIDA", "ATIVA", 5, "PE",
                "Druida", "O usuário irá instigar a natureza ao seu redor, dessa forma fazendo com que ela ataque seu alvo, a forma do ataque irá variar de acordo com o local desde que seja uma zona natural. O dano é de 1d10 a cada 10 Níveis do Personagem, além disso pode causar os mais variados efeitos negativos.");
        hab("RASTRO_ARBOREO", "Rastro Arbóreo", "CACADOR,DRUIDA", "ATIVA", 2, "PE",
                "Druida, Caçador", "Caso esteja em uma floresta, ganha +1 de Deslocamento a cada 5 Níveis do Personagem, além disso não deixa rastros para trás / Pode também movimentar-se pelas árvores, dessa forma ganhando +1 Ação Secundária apenas para Deslocamento.");
        hab("FORMA_BESTIAL", "Forma Bestial", "DRUIDA", "ATIVA", 3, "PE",
                "Druida", "Assim como os Meio Fera, pode se tornar um animal bestial qualquer após beber o sangue do mesmo, é necessário estar sem armadura pesada.");
        hab("SENHOR_DAS_FERAS", "Senhor das Feras", "DOMADOR_FERAS", "PASSIVA", 0, null,
                "Domador de Feras", "O usuário pode escolher 2 de seus companheiros para subir em 1 categoria, ou 1 companheiro para "
                        + "subir em 2 categorias. (Passiva) O efeito dessa habilidade não pode ser aplicado duas vezes na "
                        + "mesma criatura.");
        hab("FORMA_BESTIAL_INSTAVEL", "Forma Bestial Instável", "DRUIDA", "ATIVA", 4, "PE",
                "Druida & Nível 20", "Enquanto estiver com a “Forma Bestial” e beber o sangue de uma criatura compatível com a em que está, irá criar um resultado híbrido o que fará sua categoria subir em 1.");
        hab("COMUNHAO_COM_A_NATUREZA", "Comunhão com a Natureza", "DRUIDA", "PASSIVA", 0, null,
                "Druida", "Você se torna mais próximo da mata sagrada, dessa forma toda vez que sofrer dano em combate "
                        + "você pode ressoar sua dor para a vegetação ao redor, ao rolar 1d4 e o resultado for 4, irá "
                        + "surgir um espírito da floresta para lhe auxiliar. A categoria do espírito vai depender da "
                        + "localidade e do Nível de Personagem. ()");
        hab("MAO_VERDE", "Mão Verde", "DRUIDA", "ATIVA", 5, "PE",
                "Druida & Nível 10", "Crie um Microbioma a partir de um toque no chão, dessa forma possibilitando o uso de todas as Habilidades e Magias que requerem estar em uma zona de natureza. Além disso, caso usado em um local já natural, irá densificar a vegetação o que fará a habilidade “Magia Druida” ter seu gasto reduzido em -2 PE e sua invocação se torna Ação Secundária.");
        hab("DANO_DESARMADO", "Dano Desarmado", "LUTADOR,MONGE", "PASSIVA", 0, null,
                "Monge, Lutador", "O dano desarmado do usuário aumenta em um passo. Você pode pegar até 5 vezes essa habilidade, porém a cada 10 Níveis do Personagem (Passivo)");
        hab("SEQUENCIA_DE_SOCOS", "Sequência de Socos", "LUTADOR,MONGE", "ATIVA", 3, "PE",
                "Monge, Lutador", "O usuário ataca o seu alvo com 1d4 golpes desarmados. Você pode pegar essa habilidade várias "
                        + "vezes, porém a cada 10 Níveis do Personagem. O custo da habilidade é aplicado novamente para "
                        + "cada dado de golpe adicional.");
        hab("TECNICA_ANTIGA", "Técnica Antiga", "MONGE", "ATIVA", 3, "PE",
                "Monge", "O usuário pode utilizar a perícia de “Combate” como teste de reação, caso falhe ainda poderá lançar um ataque desarmado, como redutor de dano.");
        hab("CHI", "Chi", "MONGE", "ATIVA", 3, "PE",
                "Monge", "O usuário impõe magia em suas mãos, tornando esse golpe físico e mágico que caso seja bloqueado ainda causa o dano mínimo, além disso ignora 5 da armadura do alvo a cada 5 Níveis do Personagem.");
        hab("INTEGRIDADE_SABIA", "Integridade Sábia", "MONGE", "PASSIVA", 0, null,
                "Monge", "Uma vez por dia o usuário pode gastar uma ação completa meditando, onde irá recuperar sua “Sabedoria” em Pontos de Vida. Caso feito em um interlúdio de descanso longo, não irá gastar seu uso diário. (Passivo)");
        hab("VISLUMBRE_MARCIAL", "Vislumbre Marcial", "ARTISTA_MARCIAL,MONGE", "ATIVA", 3, "PE",
                "Monge, Artista Marcial", "Se torna capaz de prever os próximos movimentos de seu inimigo, assim ganhando +5 na perícia “Esquiva” a cada 10 Níveis do Personagem / Ao desviar de um ataque com sucesso, pode realizar um contra ataque.");
        hab("TROCA_EVANESCENTE", "Troca Evanescente", "MONGE", "ATIVA", 2, "PE",
                "Monge", "O usuário transforma qualquer efeito negativo que esteja sofrendo, em efeito positivo, os efeitos duram por 1d4 turnos até acabarem.");
        hab("MEDITACAO_INALCANCAVEL", "Meditação Inalcançável", "MONGE", "ATIVA", 5, "PE",
                "Monge & Nível 20", "O usuário deverá passar 1 turno meditando, depois disso deve rodar 1d4, se o resultado for 4, este ganhará um aumento de +5 na perícia “Fé” a cada 5 Níveis do Personagem, além disso reduz o gasto de bênçãos em -2 PM a cada 5 Níveis do Personagem. Ao passar mais turnos meditando a margem de acerto também diminui, no máximo, até 3 turnos.");
        hab("PRESA_SELECIONADA", "Presa Selecionada", "ARQUEIRO", "PASSIVA", 0, null,
                "Arqueiro", "No primeiro turno de um combate, você deverá selecionar um de seus inimigos, sua margem de ameaça com armas aumenta em 2 contra esse inimigo. Você pode escolher várias vezes essa habilidade, porém a cada 15 Níveis do Personagem. (Passivo)");
        hab("ANALISADOR_SAGAZ", "Analisador Sagaz", "CACADOR", "ATIVA", 2, "PE",
                "Caçador", "Pode perceber o ponto fraco de um alvo após analisá-lo, o sucesso dessa análise depende da ação gasta, sendo Ação Completa garantida, Ação Principal rode um d4 e deve cair 4, Ação Secundária rode um d6 e deve cair 6 e Ação Livre rode um d12 e deve cair 12.");
        hab("EXTERMINADOR_IMPACIENTE", "Exterminador Impaciente", "ARQUEIRO", "ATIVA", 2, "PE",
                "Arqueiro", "Caso seja o primeiro a atacar, seu primeiro ataque terá seu dado de dano aumentado em um passo, e caso esteja utilizando uma arma de longo alcance, sua margem de ameaça aumenta em 2.");
        hab("RECOMPENSA", "Recompensa", "ARQUEIRO", "PASSIVA", 0, null,
                "Arqueiro", "Ao finalizar seu alvo de “Presa Selecionada”, sua arma utilizada ganha +1 dado de mesmo dano até o final da cena. (Passivo)");
        hab("SENTIDO_DA_CACA", "Sentido da Caça", "ARQUEIRO", "ATIVA", 3, "PE",
                "Arqueiro", "O usuário é imune a ataques furtivos e armadilhas / Pode rodar um teste de “Intuição”, se passar na DT do atributo “Carisma” dos inimigos ao seu redor, saberá suas intenções.");
        hab("ADAPTACAO_DIVERSA", "Adaptação Diversa", "CACADOR,LADRAO", "ATIVA", 8, "PE",
                "Caçador, Ladrão", "O usuário pode usar sua perícia mais alta para qualquer teste.");
        hab("POS_RECOMPENSA", "Pós Recompensa", "ARQUEIRO", "ATIVA", 2, "PE",
                "Arqueiro", "Ao ativar a habilidade de “Recompensa”, no primeiro turno pós, o usuário pode reduzir a Ação necessária para qualquer realização que tentar em um passo.");
        hab("PREDADOR_OFUSCANTE", "Predador Ofuscante", "ARQUEIRO", "ATIVA", 10, "PE",
                "Arqueiro & Nível 20", "Ao ingerir o sangue de seu alvo, cria-se uma forte conexão forçada com seu alvo, assim sempre sabendo uma estimativa aproximada de sua localização, também recebendo “Vantagem” contra o alvo. Porém, só é possível ter 1 alvo marcado por vez.");
        hab("FLUXO_DE_MANA", "Fluxo de Mana", "BRUXO,MAGO", "ATIVA", 8, "PE",
                "Mago, Bruxo", "Ganha +2 no atributo “Inteligência” a cada 5 Níveis do Personagem, além disso recebe “Vantagem” para reagir a golpes mágicos de todos os tipos.");
        hab("SIGILO_VINCULATIVO", "Sigilo Vinculativo", "BRUXO,MAGO,QUIMICO_ARCANO", "PASSIVA", 0, null,
                "Mago, Bruxo, Químico Arcano", "Transforma um item não mágico até tamanho médio a sua escolha em uma manifestação mágica, que pode ser invocada a qualquer momento. Você pode escolher várias vezes essa habilidade, porém a cada 10 Níveis do Personagem. (Passivo)");
        hab("ABSORCAO_ARCANA", "Absorção Arcana", "MAGO", "ATIVA", 6, "PE",
                "Mago", "O usuário pode realizar uma ação completa para absorver a mana do ambiente e restaurar a sua própria, dessa forma recuperando o equivalente ao seu atributo de “Inteligência” em Pontos de Mana / Pode absorver a mana de um ataque mágico contra si como reação, dessa forma reduzindo em -4 o dano do ataque a cada 5 Níveis do Personagem, o dano reduzido volta como Pontos de Mana para o usuário.");
        hab("MANTO_SABIO", "Manto Sábio", "MAGO", "ATIVA", 5, "PE",
                "Mago", "O usuário transforma a mana ao seu redor em uma cobertura mágica para suas vestes, dessa forma ganhando +5 de Armadura Mágica a cada 5 Níveis do Personagem, além disso enquanto o manto estiver ativo todos tem “Desvantagem” identificar sua magia. A habilidade dura cena.");
        hab("OLHAR_ARCANO", "Olhar Arcano", "MAGO", "ATIVA", 3, "PE",
                "Mago", "O usuário pode analisar um feitiço utilizado, caso possua um feitiço de mesmo elemento, pode realizar uma cópia improvisada de tal feitiço, deve rodar 1d4 se o resultado for 4 consegue utilizar tal feitiço por 3 Turnos.");
        hab("COMBINACAO_ESOTERICA", "Combinação Esotérica", "MAGO", "ATIVA", 2, "PE",
                "Mago & Nível 10", "Junta todos os feitiços do usuário em uma esfera de mana condensada, causando 1d10 de Dano Mágico(Arcano) por feitiço que possuir e além disso, ignora 3 de armadura do alvo por feitiço que possuir.");
        hab("SINTETIZAR_O_ARCANO", "Sintetizar o Arcano", "MAGO", "PASSIVA", 0, null,
                "Mago & Nível 15", "Você compreende uma parte da mana, e a partir disso desenvolve uma feitiçaria com o elemento que desejar, ou com um novo elemento gerado a partir da junção correta de outros elementos. Você pode escolher várias vezes essa habilidade, porém a cada 15 Níveis do Personagem. (Passivo)");
        hab("CONTEMPLAR_DO_MAGO", "Contemplar do Mago", "MAGO", "ATIVA", 10, "PE",
                "Mago & Nível 20", "O usuário intensifica seus conhecimentos e os aplica sobre si, moldando a mana do ambiente para utilizar a benefício próprio, com isso pode invocar até 3 Feitiços em sua Ação Principal e todos terão seu gasto reduzido em -3 a cada 10 de Mana gasta.");
        hab("GLORIOSA_EVOLUCAO", "Gloriosa Evolução", "MAGO", "PASSIVA", 0, null,
                "Mago", "Aprimore um de seus feitiços para sua versão arcana permanentemente, esse novo feitiço tem os mesmos danos, efeitos e vantagens, porém passa a ignorar Armadura e Resistências contra o seu elemento original. Você pode escolher várias vezes essa habilidade. (Passivo)");
        hab("SALTO_INICIAL", "Salto Inicial", "ASSASSINO,ESPADACHIM,LANCEIRO", "ATIVA", 2, "PE",
                "Espadachim, Assassino, Lanceiro", "Após rolarem iniciativa, no primeiro turno pode usar essa habilidade para somar +2 a cada 5 Níveis do Personagem em sua iniciativa, caso após a soma dos valores ficar antes de seu adversário na ordem de turnos, receberá +1 dado de dano do mesmo tipo de sua arma no Primeiro Turno.");
        hab("BRECHA_LIVRE", "Brecha Livre", "ESPADACHIM", "ATIVA", 2, "PE",
                "Espadachim", "O usuário foca em pontos abertos na defesa do seu alvo, assim ignorando 4 de armadura a cada 5 Níveis do Personagem.");
        hab("CORTE_PROLONGADO", "Corte Prolongado", "ESPADACHIM", "ATIVA", 1, "PE",
                "Espadachim + Arma Cortante", "Estende o alcance de sua lâmina a partir de um deslocamento de ar, dessa forma aumentando o alcance de seu ataque em um passo, caso o inimigo use a reação de “Esquiva” e tenha sucesso, ele ainda receberá o dano mínimo do usuário.");
        hab("CORTE_CIRURGICO", "Corte Cirúrgico", "ESPADACHIM", "ATIVA", 10, "PE",
                "Espadachim & Nível 20 + Dano Cortante", "Ao rolar um ataque crítico, você pode escolher ao invés de atacar, fragilizar um membro do alvo, dessa forma toda vez que o alvo receber um crítico de dano cortante, terá 1d6 de chance de perder um membro, onde se o resultado for 6 ele perde.");
        hab("CORTE_GEOMETRICO", "Corte Geométrico", "ESPADACHIM", "ATIVA", 3, "PE",
                "Espadachim & Nível 15", "Escolha entre 1d4, 1d6, 1d8 ou 1d12. O resultado determina a quantidade de golpes consecutivos. "
                        + "O último golpe recebe 1 dado de dano do mesmo passo de dado escolhido. O custo da habilidade é "
                        + "aplicado novamente para cada passo de dado acima de 1d4.");
        hab("ESPADA_DA_VINGANCA", "Espada da Vingança", "CAVALEIRO,ESPADACHIM", "ATIVA", 4, "PE",
                "Espadachim, Cavaleiro", "Caso tenha sido atacado por vários inimigos no turno anterior, pode além de sua ação principal, realizar um ataque simples contra todos aqueles que lhe atacaram no turno passado.");
        hab("FINALIZACAO", "Finalização", "ESPADACHIM", "ATIVA", 0, "PE",
                "Espadachim", "Caso o confronto já tenha passado de 3 rodadas, o usuário poderá lançar 2 habilidades como ação padrão no mesmo turno. Só é possível utilizar essa habilidade uma vez por cena.");
        hab("CORTE_ABSOLUTO", "Corte Absoluto", "ESPADACHIM", "ATIVA", 5, "PE",
                "Espadachim & Nível 10", "Após passar 1 turno imóvel e “Vulnerável”, o usuário receberá em seu próximo turno +2 a cada 5 Níveis do Personagem em testes de ataque, além disso, caso ataque com uma espada seu dano aumenta em um passo.");
        hab("ROTA_DE_FUGA", "Rota de Fuga", "LADRAO", "ATIVA", 1, "PE",
                "Ladrão", "Ao passar 1 turno analisando o terreno, o usuário recebe “Vantagens” para qualquer teste que lhe ajude a fugir de um inimigo/ameaça, além disso ignora “Terreno Difícil”.");
        hab("EVASAO_DO_PALHACO", "Evasão do Palhaço", "LADRAO", "ATIVA", 3, "PE",
                "Ladrão / Nível 10", "O usuário brinca com seu inimigo, desviando do golpe de maneiras improváveis, dessa forma ganhando +2 a cada 5 Níveis do Personagem em testes de Esquiva / Uma vez por cena, pode esquivar de um ataque automaticamente colocando um aliado para receber o ataque em seu lugar.");
        hab("PASSOS_LEVES_X", "Passos Leves", "LADRAO,NINJA", "PASSIVA", 0, null,
                "Ladrão, Ninja", "Ao estar em estado de furtividade o usuário não sofre penalidade em seu deslocamento. (Passivo)");
        hab("MALANDRAGEM", "Malandragem", "LADRAO", "ATIVA", 3, "PE",
                "Ladrão", "O usuário ganha +2 a cada 5 Níveis do Personagem em testes de “enganação”, além disso pode realizar uma ação simples de forma furtiva enquanto engana alguém.");
        hab("MAO_LEVE", "Mão Leve", "LADRAO", "ATIVA", 4, "PE",
                "Ladrão", "Caso esteja em furtividade o usuário tem “Vantagem” para roubar objetos até o tamanho médio do seu alvo, caso falhe ainda pode rodar um teste de “Aparência” para não levantar suspeitas contra si.");
        hab("DRAMA_FINAL", "Drama Final", "LADRAO", "ATIVA", 3, "PE",
                "Ladrão", "Após receber um ataque, pode realizar um teste de “Enganação” ou “Aparência” contra seu atacante, caso passe entra em estado de furtividade automaticamente.");
        hab("MACACO_URBANO", "Macaco Urbano", "LADRAO", "ATIVA", 2, "PE",
                "Ladrão", "Em um cenário de cidade o usuário se move livremente, ganhando +5 a cada 10 Níveis do Personagem em “Acrobacia” / Caso tenha realizado “Mão Leve” o usuário ganha +1 Ação de Movimento apenas para deslocamento.");
        hab("VULTO_SOBREPOSTO", "Vulto Sobreposto", "LADRAO", "ATIVA", 5, "PE",
                "Ladrão & Nível 10", "Caso o usuário seja percebido em seu estado de furtividade, ele pode realizar um teste de “Enganação” ou “Aparência” para manter-se furtivo, se feito, ainda recebe “Vantagem” em seu próximo teste.");
        hab("CURAR_FERIMENTOS", "Curar Ferimentos", "ALQUIMISTA,BRUXO,CLERIGO,CURANDEIRO,DRUIDA,MONGE", "ATIVA", 2, "PE",
                "Curandeiro, Clérigo, Druida, Monge, Bruxo, Alquimista", "O usuário restaura 1d10 Pontos de Vida do seu alvo. Você pode pegar várias vezes essa "
                        + "habilidade, porém a cada 5 Níveis do Personagem. Cada vez que adquirir esta habilidade, aumenta "
                        + "a cura em 1d10. Caso não seja utilizada por um Curandeiro, não levantará alvos que estejam em "
                        + "estado de morrendo.");
        hab("LIMPEZA_ESPIRITUAL", "Limpeza Espiritual", "BRUXO,CURANDEIRO", "ATIVA", 1, "PE",
                "Curandeiro, Bruxo", "Ao tocar um alvo remove todos os seus efeitos negativos, além disso o deixa imune a tais efeitos durante 2 turnos seguintes.");
        hab("AREA_MEDICA", "Área Médica", "CURANDEIRO", "ATIVA", 5, "PE",
                "Curandeiro", "Ao gastar uma ação completa, o usuário cria um ambiente improvisado para tratamentos, todos nesse ambiente adicionam 1d10 a cada 5 Níveis do Personagem em curas de todas as fontes, porém, um inimigo pode realizar um ataque contra esse local destruindo-o.");
        hab("ACUPUNTURA_NEURAL", "Acupuntura Neural", "CURANDEIRO", "ATIVA", 4, "PE",
                "Curandeiro", "O usuário dita palavras mágicas cujo agirão no sistema nervoso do alvo deixando-o paralisado, é necessário rodar um teste de “Medicina” contra “Vigor” ou “Virtude” do alvo.");
        hab("SANGUE_ABENCOADO", "Sangue Abençoado", "CURANDEIRO", "ATIVA", 3, "PE",
                "Curandeiro", "O usuário ativa sobre si uma regeneração constante de +4 PV a cada 10 Níveis do Personagem / Ao utilizar sobre outra pessoa o efeito reduz pela metade.");
        hab("CURA_DISTANTE", "Cura Distante", "CURANDEIRO", "PASSIVA", 0, null,
                "Curandeiro", "O usuário pode usar a habilidade de “Curar Ferimentos” a distância. (Gasto: 2 PE a cada Passo de Distância + Curar Ferimentos]");
        hab("TRANSFERENCIA_VITAL", "Transferência Vital", "CURANDEIRO", "PASSIVA", 0, null,
                "Curandeiro", "O usuário pode transferir PV, PM e PE para os seus aliados, a cada 10 pontos gastos ele dará 1d12 de recuperação para o seu aliado. (Passivo)");
        hab("RECONSTRUCAO_CELULAR", "Reconstrução Celular", "CURANDEIRO", "ATIVA", 8, "PE",
                "Curandeiro & Nível 20", "Caso seja feito na mesma rodada que um aliado recebeu o ataque, é possível utilizar o sangue perdido para curá-lo de volta, assim restaurando o seu “Curar Ferimentos” mais metade do dano sofrido no turno dele em PV / O usuário pode também restaurar membros perdidos de seus aliados, através de 10 Descansos Longos.");
        hab("MEU_BARALHO", "Meu Baralho", "CARTOMANTE", "ATIVA", 1, "PE",
                "Cartomante", "O usuário pode escolher uma de suas cartas mágicas a mão, dessa forma garantindo seu efeito.");
        hab("AS_NA_MANGA", "Ás na Manga", "CARTOMANTE", "PASSIVA", 0, null,
                "Cartomante", "No início da Cena, o usuário deverá puxar uma carta onde ficará escondida em suas vestes, após isso a qualquer momento que desejar como uma Ação Livre pode lançar essa carta. Você pode pegar várias vezes essa habilidade, porém a cada 5 Níveis do Personagem. (Passiva)");
        hab("EMBARALHAR", "Embaralhar", "CARTOMANTE", "ATIVA", 2, "PE",
                "Cartomante", "Embaralhe suas cartas, agora veja as suas 6 próximas cartas, escolha 2 para descartar, agora você saberá o efeito das suas próximas 4 cartas.");
        hab("TRUQUE_DE_MESTRE", "Truque de Mestre", "CARTOMANTE,LADRAO", "ATIVA", 3, "PE",
                "Cartomante, Ladrão", "O usuário invoca uma rápida explosão de fumaça sobre si, pode se fazer um teste de “Furtividade”, caso passe entra em furtividade e seu inimigo fica “Confuso”.");
        hab("CARA_OU_COROA", "Cara ou Coroa", "CARTOMANTE", "ATIVA", 3, "PE",
                "Cartomante", "O usuário irá desafiar um alvo e jogará uma moeda ao alto, se cair o lado que este venho a escolher ele terá “Vantagem” contra o alvo até o final da cena, caso caia o lado contrário terá “Desvantagem” contra o alvo até o final da cena.");
        hab("TORRE_DE_CARTAS", "Torre de Cartas", "CARTOMANTE", "ATIVA", 4, "PE",
                "Cartomante", "Monta uma torre de cartas ao chão que funcionará como uma armadilha, caso um inimigo se aproxime ele irá a derrubar, o alvo recebe um ataque de 1d6+1 cartas mágicas.");
        hab("BALANCA_MALDITA", "Balança Maldita", "CARTOMANTE", "ATIVA", 2, "PE",
                "Cartomante", "Ao tocar em um alvo rouba toda a sorte dele, assim em seu próximo embate de testes, o usuário pegará o dado de valor mais alto para si.");
        hab("TRUCO", "Truco?", "CARTOMANTE", "ATIVA", 5, "PE",
                "Cartomante & Nível 10", "O usuário puxa 3 cartas, deverá rodar 1d4 para cada uma delas, cada carta funciona como um bônus para seus próximos ataques, o usuário pode escolher quantas e quando usá-las: Espadas(1): Ignora 4 da Armadura do Alvo; Paus(2): Adiciona +5 de dano ao seu ataque; Copas(3): Adiciona +5 de Roubo de Vida ao seu ataque; Ouros(4): Causa -2 na Reação do seu Alvo. Cada um dos efeitos aumenta a cada 20 Níveis do Personagem.");
        hab("CONSUMIR_FORCAS", "Consumir Forças", "BRUXO", "ATIVA", 6, "PE",
                "Bruxo", "Ao tocar em um alvo pode drenar suas forças, do alvo “Força” do alvo, além disso recebe metade "
                        + "dos pontos reduzidos em Pontos de Energia.");
        hab("ODOR_DO_DECREPITO", "Odor do Decrépito", "BRUXO", "ATIVA", 4, "PE",
                "Bruxo", "Após abater um alvo em combate, o usuário pode transferir a essência de morte para o seu alvo, dessa forma o mesmo deverá rodar um teste de “Vigor” para resistir, caso falhe o alvo fica sem reação de “Bloqueio” e “Esquiva” até o final da cena.");
        hab("FERIMENTOS_HORRENDOS", "Ferimentos Horrendos", "BRUXO", "ATIVA", 2, "PE",
                "Bruxo", "Faz com que seu próximo ataque corpo a corpo cause um ferimento feio, inibindo qualquer tipo de cura do alvo, menos divina, pelas 2 rodadas seguintes.");
        hab("VISAO_TURVA", "Visão Turva", "BRUXO,SOMBRA", "PASSIVA", 0, null,
                "Bruxo, Sombra", "Caso seu alvo esteja sofrendo algum efeito negativo, você se torna uma visão embaçada contra o mesmo, dessa forma ganhando +2 a cada 5 Níveis do Personagem em sua Reação. (Passivo)");
        hab("INFERIOR_A_MIM", "Inferior a Mim", "BRUXO", "ATIVA", 4, "PE",
                "Bruxo", "Ao encarar um oponente o oprime com sua aura, pode usar “Feitiçaria” como teste de intimidação, caso passe o alvo fica “Intimidado” e a habilidade “Visão Turva” ganha +1 de efeito a cada 5 Níveis do Personagem.");
        hab("ALVOROCA_MALDICAO", "Alvoroça Maldição", "BRUXO", "ATIVA", 8, "PE",
                "Bruxo", "Ao gastar uma Ação Completa, o usuário molda a mana do ambiente e lança uma maldição sobre um "
                        + "alvo. O usuário define uma condição para ativá-la; quando a condição for cumprida, a maldição "
                        + "reduz 4 pontos de atributos, aumentando em +2 a cada 10 níveis. Os pontos podem ser distribuídos "
                        + "entre diferentes atributos, mas nenhum atributo pode ser reduzido em mais de 4 pontos.");
        hab("RESGATAR_FAMILIAR", "Resgatar Familiar", "BRUXO", "ATIVA", 5, "PE",
                "Bruxo", "O usuário absorve as capacidades de seu familiar para si, oque o faz entrar em seu intervalo de retorno, mas com isso, ganha +2 do maior atributo do seu familiar a cada 5 Níveis do Personagem e consegue utilizar as bênçãos do mesmo como se fossem suas, até o final da cena.");
        hab("TRANSMUTAR", "Transmutar", "ALQUIMISTA", "ATIVA", 3, "PE",
                "Alquimista", "O usuário pode moldar materiais inorgânicos simples para criar o que desejar, é necessário rolar um teste de “Alquimia” para executar, a dificuldade do teste aumenta de acordo com o material, materiais compostos têm a dificuldade dobrada.");
        hab("CIRCULO_DE_TRANSFERENCIA", "Círculo de Transferência", "ALQUIMISTA", "ATIVA", 4, "PE",
                "Alquimista", "Ao gastar uma ação completa, demarca um grande círculo em uma área, a partir disso as bênçãos e feitiços invocados pelo usuário terão seus gastos reduzidos em -5 PM a cada 5 Níveis do Personagem, o círculo permanece em Turnos de acordo com a mana do ambiente em que foi colocado.");
        hab("DESINTEGRAR", "Desintegrar", "ALQUIMISTA", "ATIVA", 5, "PE",
                "Alquimista", "O usuário pode destruir itens simples com facilidade, é necessário rolar um teste de “Alquimia” para executar, se o teste passar por uma diferença de 10 ou maior, ele funciona normalmente, caso contrário é necessário rodar 1d12 de dano verdadeiro no objeto, a dificuldade do teste aumenta de acordo com o material.");
        hab("RECONSTRUCAO", "Reconstrução", "ALQUIMISTA", "ATIVA", 5, "PE",
                "Alquimista", "O usuário pode reconstruir um objeto destruído, é necessário rolar um teste de “Alquimia” para executar, a dificuldade do teste aumenta de acordo com o material e complexidade do item.");
        hab("SANGUE_DE_FERRO", "Sangue de Ferro", "ALQUIMISTA", "ATIVA", 3, "PE",
                "Alquimista", "O usuário modifica a própria estrutura física para ficar mais rígido, dessa forma ganhando 4 de armadura a cada 5 Níveis do Personagem, além de ficar imune a sangramento e envenenamento.");
        hab("FRAGILIZAR", "Fragilizar", "ALQUIMISTA", "ATIVA", 3, "PE",
                "Alquimista", "Ao tocar em um alvo remove 10 a cada 5 Níveis de Personagem de armadura, também faz com que caso um inimigo bloqueie ele ainda receberá metade do dano.");
        hab("CIRCULO_ARCANO", "Círculo Arcano", "ALQUIMISTA", "ATIVA", 8, "PE",
                "Alquimista", "Ao gastar uma ação completa para desenhar um grande símbolo ao chão, o usuário pode modificar as propriedades de uma bênção usada dentro do círculo. Podendo aumentar ou diminuir o dano em 5, aumentar ou diminuir o acerto em 2, estender ou encurtar efeitos em 1. Cada um dos efeitos aumenta a cada 5 Níveis de Personagem.");
        hab("CIRCULO_DA_VERDADE", "Círculo da Verdade", "ALQUIMISTA", "ATIVA", 1, "PE",
                "Alquimista & Nível 10", "O usuário ganha a capacidade de usar “Círculo de Transferência” e “Círculo Arcano”, sem precisar desenhar o círculo.");
        hab("BALANCEAR_PESO", "Balancear Peso", "LANCEIRO", "PASSIVA", 0, null,
                "Lanceiro", "Reduz o peso de qualquer arma da categoria de “Lança” em -1, além disso, sacar lanças é uma ação livre. (Passiva)");
        hab("BARRAGEM_DE_GOLPES", "Barragem de Golpes", "LANCEIRO", "ATIVA", 8, "PE",
                "Lanceiro", "O usuário desfere uma sequência de estocadas rápidas. Role 1d10 para determinar o número de "
                        + "ataques realizados. Cada ataque causa tem 2 de acerto e de dano em relação ao anterior.");
        hab("ATAQUE_EXTENSOR", "Ataque Extensor", "GERAL", "ATIVA", 2, "PE",
                "Arma Corpo a Corpo Média", "O usuário pega na ponta do cabo de sua arma e assim desfere um ataque, o alcance da sua arma aumenta em um passo, além disso, no primeiro turno caso use essa habilidade sua arma receberá +1 dado de dano do mesmo tipo.");
        hab("PANCADA_CRITICA", "Pancada Crítica", "BARBARO,LANCEIRO", "ATIVA", 10, "PE",
                "Lanceiro, Bárbaro, & Nível 10", "O usuário pode lançar um ataque imprudente com sua arma, trocando sua habilidade por brutalidade, assim que atacar o dado de dano da sua arma aumenta em um passo, recebe +1 dado de dano do mesmo tipo e +2 na sua margem de ameaça. Porém, sua arma recebe 1d12 de dano verdadeiro de volta.");
        hab("ATAQUE_GIRATORIO", "Ataque Giratório", "BARBARO,LANCEIRO,MONGE", "ATIVA", 3, "PE",
                "Lanceiro, Monge, Bárbaro", "O usuário gira sua arma acima de sua cabeça, a partir disso todos aqueles que entrarem em seu alcance curto, irão receber um ataque automaticamente, além disso recebe 4 de Armadura a cada 10 Pontos de Destreza.");
        hab("PERFURAR_OS_CEUS", "Perfurar os Céus", "LANCEIRO", "ATIVA", 5, "PE",
                "Lanceiro", "Caso possua um alvo voando, o usuário pode utilizar toda sua força para arremessar sua arma neste, ganhando +2 a cada 5 Níveis do Personagem em “Arremesso”, além disso caso acerte o alvo precisa fazer um teste de “Vigor” para não cair ao chão. É necessário uma arma da categoria “Lança”.");
        hab("BARREIRA_GIRATORIA", "Barreira Giratória", "LANCEIRO", "ATIVA", 3, "PE",
                "Lanceiro", "O usuário pode usar como reação girar sua arma rapidamente a frente de seu corpo, assim criando uma barreira que irá barrar projéteis de todos os tipos, o projétil tem seu dano reduzido pelo valor que o usuário tirar em 1d4 golpes.");
        hab("BATE_ESTACA", "Bate Estaca", "LANCEIRO", "ATIVA", 10, "PE",
                "Lanceiro & Nível 10", "O usuário salta sobre seu oponente e o empala com sua arma, cravando-a ao chão fazendo com que seu alvo fique agarrado pela arma, sendo necessário passar no valor de ataque do usuário para se soltar. Além disso, o dano do usuário aumenta de acordo com a altura da queda, sendo que a cada 10 de altura para a arma +1 dado do de dano do mesmo tipo e dá ao usuário 5 de armadura.");
        hab("TESTE_DE_FORCA", "Teste de Força", "BARBARO,LUTADOR,MONGE", "ATIVA", 4, "PE",
                "Lutador, Bárbaro, Monge", "O usuário se posiciona para demonstrar sua força, com um golpe demolidor este causa +2 dados de dano desarmado a cada 10 Níveis do Personagem, contra objetos e construtos.");
        hab("FINTA", "Finta", "LUTADOR", "PASSIVA", 0, null,
                "Lutador", "O usuário entende muito bem qual o momento certo para atacar seu oponente, assim sempre que desviar ou defender por um valor maior que 10 do ataque inimigo, irá poder realizar um ataque contra o atacante. (Passiva)");
        hab("SEQUENCIA_FLUIDA", "Sequência Fluída", "LUTADOR", "ATIVA", 4, "PE",
                "Lutador", "O usuário utiliza o impacto de um golpe para emendar outro imediatamente. Sempre que acertar um "
                        + "ataque corpo a corpo, role 1d4. Caso o resultado seja 4, poderá realizar um ataque adicional "
                        + "contra o mesmo alvo. Antes da rolagem, o usuário pode gastar PE adicionais para aumentar suas "
                        + "chances. Para cada 1 PE gasto, reduz em 1 o resultado necessário para ativar a habilidade.");
        hab("REFORCAR", "Reforçar", "LUTADOR", "ATIVA", 2, "PE",
                "Lutador", "Ao contrair todos os seus músculos, o usuário faz um esforço que lhe garante +2 a cada 5 pontos de Força em Armadura, Dano e Penetração de Armadura.");
        hab("MEMORIA_MUSCULAR", "Memória Muscular", "LUTADOR", "ATIVA", 4, "PE",
                "Lutador", "Ao escolher gastar uma ação completa para passar um turno com sua guarda totalmente aberta e receber ataques, o usuário irá memorizar com seu corpo os golpes recebidos, dessa forma ganhando “Vantagem” contra o alvo e +2 a cada 10 de dano sofrido em testes de reação contra o alvo.");
        hab("PANCADARIA", "Pancadaria", "LUTADOR", "ATIVA", 1, "PE",
                "Lutador", "Ao receber um ataque desarmado, o usuário se inspira para dobrar e passar para o próximo, nesse momento pode lançar um ataque como Ação Livre em alguém no seu alcance que não seja seu atacante / Caso só possua o seu atacante em seu alcance, recebe +1 dado de dano desarmado para o seu próximo ataque.");
        hab("ABERTURA", "Abertura", "LUTADOR", "ATIVA", 3, "PE",
                "Lutador", "Ao realizar uma habilidade de “Finta” com sucesso, o usuário pode realizar um teste de “Atletismo” para abrir a defesa do oponente, dessa forma no próximo ataque que ele receber o mesmo terá “Desvantagem” em sua reação.");
        hab("AUDACIA", "Audácia", "LUTADOR", "ATIVA", 8, "PE",
                "Lutador", "O usuário acumula rancor de todo o dano recebido durante um combate e canaliza para o seu próximo, com isso seu próximo golpe causa +2 de dano a cada 10 de dano recebido e +2 de dano para cada golpe recebido nos turnos anteriores. Ao ser utilizado, o usuário perde os acúmulos dos turnos anteriores.");
        hab("CANTO_DIVINO", "Canto Divino", "CIGANO", "PASSIVA", 0, null,
                "Cigano", "O usuário pode realizar ataques musicais com sua perícia de “Arte”, o alcance do ataque é médio e causa 1d6 a cada 5 pontos de “Carisma” de dano, além de ignorar armadura física. Você pode pegar essa habilidade várias vezes para aumentar a quantidade de alvos em seu ataque. (Passiva)");
        hab("KARMA", "Karma", "CIGANO", "ATIVA", 2, "PE",
                "Cigano", "O usuário marca seu alvo com um toque sútil, criando uma ligação que transfere parte do dano sofrido para o seu alvo também, dessa forma, toda vez que o usuário sofrer 10 pontos de dano o seu alvo receberá 1d6 de dano verdadeiro também.");
        hab("DANCA_RITUALISTICA", "Dança Ritualística", "CIGANO", "ATIVA", 10, "PE",
                "Cigano", "O usuário flui pelo campo de batalha como se fosse alheio aos perigos, durante sua dança este ganha +1 Ação Secundária e +5 a cada 5 Níveis do Personagem em testes de “Esquiva”. Realizar testes ofensivos ou complexos, cancelam a dança.");
        hab("PALMA_SUAVE", "Palma Suave", "CIGANO", "ATIVA", 4, "PE",
                "Cigano", "Caso o usuário seja alvo de um projétil ou ataque a distância, ao conseguir esquivar do ataque pode-se realizar um teste de “Arremessar” com “Vantagem” para devolver o ataque ao atacante. Caso acerte, ainda causa o dano original do ataque + 2 a cada 5 pontos em Carisma de dano.");
        hab("APRESENTACAO_CHAMATIVA", "Apresentação Chamativa", "CIGANO", "ATIVA", 10, "PE",
                "Cigano", "Ao realizar uma ação completa o usuário começa a dançar e cantar nessa rodada, dessa forma adquirindo “Vantagem” em quaisquer testes de reação, além disso, todos os inimigos no local devem fazer um teste de “Virtude” contra as “Artes” do usuário, caso falhem, ficam “Distraídos”.");
        hab("LUXO_ACUMULADO", "Luxo Acumulado", "CIGANO", "ATIVA", 4, "PE",
                "Cigano", "O usuário adiciona ao seu ataque um bônus baseado na sua ostentação, recebe +1 a cada 100 moedas de ouro em dados de dano do mesmo tipo para qualquer ataque.");
        hab("TANDAVA_KARMA", "Tandava Karma", "CIGANO", "ATIVA", 15, "PE",
                "Cigano & Nível 20", "O usuário se torna o ápice de suas técnicas, convertendo todas elas para uma versão agressiva, sua dança se torna tão poderosa ao ponto de consumir seu próprio corpo causando 3d10+5 de dano verdadeiro a cada turno, mas com isso, o usuário ganha +1 Ação Principal, +1 Ação Secundária e a cada turno com a habilidade ativa o usuário ganha +1 dado de dano do mesmo tipo para seus ataques.");
        hab("PAREDE_DE_FERRO", "Parede de Ferro", "ESCUDEIRO", "ATIVA", 5, "PE",
                "Escudeiro & Escudo Grande", "Ao utilizar um escudo grande, o usuário pode fincá-lo ao chão dessa forma ganhando uma cobertura completa para si e uma cobertura parcial para todos seus aliados que permanecerem atrás do escudo. O usuário perde a capacidade de movimentar-se, mas também se torna imune a efeitos de empurrão ou derrubada.");
        hab("BALUARTE_ALIADO", "Baluarte Aliado", "ESCUDEIRO", "ATIVA", 3, "PE",
                "Escudeiro", "Sempre que um aliado sofrer um ataque corpo a corpo no campo de batalha, o usuário pode gastar sua ação de movimento do turno seguinte, para saltar à frente do seu aliado e abrir espaço entre ele e o atacante, dessa forma o alvo fica impossibilitado de atacar seu aliado corpo a corpo, só podendo atacar o usuário.");
        hab("PANCADA_ATORDOANTE", "Pancada Atordoante", "ESCUDEIRO", "ATIVA", 5, "PE",
                "Escudeiro", "O usuário ataca com seu escudo uma parte aberta do alvo, se acertar o alvo deve fazer um teste de “Vigor” contra o “Bloqueio” do usuário, caso falhe, fica atordoado por 1 rodada e perde sua ação.");
        hab("VEM_PRA_CIMA", "Vem pra Cima", "ESCUDEIRO", "ATIVA", 5, "PE",
                "Escudeiro & Escudo", "O usuário bate ou arranha seu escudo para provocar todos os inimigos em alcance médio, que devem realizar um teste de “Virtude” contra quaisquer perícia de Carisma que o usuário queira utilizar, caso falhem, recebem “Desvantagem” para atacar qualquer um que não seja o usuário.");
        hab("ESCUDO_BRILHANTE", "Escudo Brilhante", "COMANDANTE,ESCUDEIRO", "ATIVA", 3, "PE",
                "Escudeiro, Comandante", "Ao utilizar um escudo leve, o usuário pode gastar sua reação contra um projétil para rebate-lo de volta ao seu atacante, rodando um teste de “Bloqueio”, o atacante reage ao valor do teste de “Bloqueio” / Pode mudar o alvo do projétil rebatido para qualquer um em seu campo de visão.");

        // ---- Habilidades por classe/trilha (secoes do docx, importadas automaticamente) ----
        hab("ESCUDO_DE_BALUARTE", "Escudo de Baluarte", "CAVALEIRO", "ATIVA", 4, "PE",
                null, "O usuário se fixa no chão, aumentando sua Armadura em +2 para cada 5 pontos de Vigor. Enquanto ativa, o deslocamento é reduzido a zero.");
        hab("GRITO_DE_ORDEM", "Grito de Ordem", "CAVALEIRO,COMANDANTE", "ATIVA", 5, "PE",
                "Trilha Comandante", "O Cavaleiro inspira aliados em alcance médio, concedendo um bônus de +2 (a cada 5 niveis) em testes de Iniciativa e Ataque por 2 rodadas.");
        hab("GOLPE_DE_CONVICCAO", "Golpe de Convicção", "CAMPEAO,CAVALEIRO,COMANDANTE", "ATIVA", 3, "PE",
                "Trilha Comandante, Campeão, Carisma 10", "Adicione metade do valor de carisma ao dano de um ataque físico, representando a força de seus ideais.");
        hab("RESISTENCIA_BLINDADA", "Resistência Blindada", "CAVALEIRO,MONGE", "PASSIVA", 0, null,
                "cavaleiro, monge", "Passivamente, o Usuário reduz o dano de ataques críticos recebidos em um passo de dado.");
        hab("LAMINA_FANTASMAGORICA", "Lâmina Fantasmagórica", "ASSASSINO", "ATIVA", 4, "PE",
                null, "O ataque ignora qualquer Cobertura Parcial do alvo e adiciona +2 de dano por cada 5 pontos de Furtividade.");
        hab("PASSO_DE_SOMBRA", "Passo de Sombra", "ASSASSINO,SOMBRA", "ATIVA", 3, "PE",
                "Sombra", "Permite que o usuário se mova até seu Deslocamento total sem provocar ataques de oportunidade enquanto estiver em sombras.");
        hab("ANALISE_DE_PONTO_CEGO", "Análise de Ponto Cego", "ARQUEIRO,ASSASSINO", "PASSIVA", 0, null,
                "ARQUEIRO, Assassino", "Se o usuário estiver em um nível superior (terreno alto), sua margem de crítico aumenta em +2.");
        hab("EXECUCAO_SILENCIOSA", "Execução Silenciosa", "ASSASSINO", "ATIVA", 15, "PE",
                null, "Se o alvo estiver com o dobro de sua constituição, o Assassino pode gastar toda sua energia para tentar um golpe fatal (DT de Vigor do alvo).");
        hab("SALTO_ESMAGADOR", "Salto Esmagador", "BARBARO", "ATIVA", 6, "PE",
                null, "O Bárbaro salta em direção a um alvo devendo ter que fazer um teste de atletismo; o dano aumenta em +1 dado para cada 3 metros de altura/distância saltados.");
        hab("ARREMESSO_BRUTAL", "Arremesso Brutal", "BARBARO", "ATIVA", 5, "PE",
                null, "Permite arremessar armas de duas mãos ou objetos pesados usando a perícia Atletismo em vez de Pontaria.");
        hab("RUGIDO_DE_PAVOR", "Rugido de Pavor", "BARBARO", "ATIVA", 4, "PE",
                null, "fazendo um teste de intimidação Força todos os inimigos em alcance curto a realizarem um teste de Virtude; se falharem, ficam intimidados.");
        hab("LUZ_DE_PURIFICACAO", "Luz de Purificação", "CLERIGO", "ATIVA", 4, "PM",
                "Curandeiro tbm", "Remove um efeito de status negativo (Sangramento, Envenenamento ou Confusão) de até dois aliados próximos.");
        hab("MARTELO_DIVINO", "Martelo Divino", "CLERIGO,PALADINO", "ATIVA", 8, "PE",
                "Trilha Paladino", "Impregna a arma própria com fé, causando dano mágico adicional baseado na perícia Fé, este dano "
                        + "impregna a arma com a mana de sua divindade. O efeito permanece na arma por 3 Turnos. Enquanto "
                        + "com efeito ativo, a arma não pode receber outras Magias.");
        hab("AURA_DE_SANTIDADE", "Aura de Santidade", "CLERIGO,SANTO", "PASSIVA", 0, null,
                "Trilha Santo", "Aliados em alcance curto recebem um bônus de +2 a cada 5 níveis em todos os testes de reação "
                        + "contra magias.");
        hab("VINCULO_ESPIRITUAL_CURANDEIRO_L", "Vínculo Espiritual (CURANDEIRO)l", "CLERIGO", "ATIVA", 5, "PM",
                null, "O Clérigo escolhe um aliado; metade da cura recebida pelo Clérigo é transferida para esse aliado.");
        hab("SENTENCA_CELESTIAL", "Sentença Celestial", "CLERIGO", "ATIVA", 4, "PM",
                null, "Marca um inimigo como \"Herege\"; ataques contra este alvo ignoram 2 (a cada 5 níveis) pontos de armadura.");
        hab("FORMA_DE_CARVALHO", "Forma de Carvalho", "DRUIDA", "ATIVA", 6, "PE",
                null, "Aumenta a Constituição em +2 (a cada 5 niveis) e concede 5 (a cada 10 niveis) de armadura verdadeira enquanto estiver em ambiente de floresta.");
        hab("ESPINHOS_APRISIONADORES", "Espinhos Aprisionadores", "DRUIDA", "ATIVA", 6, "PM",
                "feitiço; Trilha Arauto", "O usuário faz raízes espinhosas emergirem do solo, prendendo tudo em seu caminho. Escolha um "
                        + "ponto em alcance médio. As raízes cobrem uma área durante 3 turnos. Criaturas que entrarem na "
                        + "área ou iniciarem seu turno nela devem realizar um teste de Esquiva. Em caso de falha, ficam "
                        + "Imobilizadas, sofrem 1d6 de dano perfurante e consideram a área como terreno difícil. Caso uma "
                        + "criatura já Imobilizada falhe novamente, fica Paralisada até o fim do turno.");
        hab("COMANDO_DA_MATILHA", "Comando da Matilha", "DRUIDA", "ATIVA", 6, "PE",
                "Trilha Domador", "Concede uma ação padrão extra para seu companheiro animal sendo um gasto para cada animal neste turno.");
        hab("SIMBIOSE_NATURAL", "Simbiose Natural", "DRUIDA", "ATIVA", 8, "PE",
                null, "O Druida pode transferir metade do dano recebido para uma planta ou árvore próxima(nível 20).");
        hab("PALMA_DE_VACUO", "Palma de Vácuo", "MONGE", "ATIVA", 4, "PE",
                null, "Um ataque desarmado que empurra o inimigo 5 metros para trás. Se ele colidir com uma parede ou "
                        + "outro objeto grande e rígido, ficará atordoado.");
        hab("MEDITACAO_DE_COMBATE", "Meditação de Combate", "MONGE,SABIO", "PASSIVA", 0, null,
                "Trilha Sábio", "O Monge gasta sua ação para recuperar PE igual ao seu bônus de metade da sabedoria (1 vez por dia) .");
        hab("PASSOS_DE_VENTO", "Passos de Vento", "MONGE", "ATIVA", 15, "PE",
                null, "O Monge aumenta em 4 seu deslocamento por uma rodada e pode andar sobre superfícies líquidas.");
        hab("REFLEXO_BLINDADO", "Reflexo Blindado", "MONGE", "ATIVA", 2, "PE",
                null, "Permite usar a perícia Combate para bloquear projéteis físicos (flechas, facas).");
        hab("FLECHA_DE_RASTREIO", "Flecha de Rastreio", "ARQUEIRO", "ATIVA", 3, "PE",
                null, "Marca um alvo; o Arqueiro e seus aliados ganham Vantagem em testes de Percepção contra este "
                        + "alvo por toda a cena.");
        hab("TIRO_DE_INTERDICAO", "Tiro de Interdição", "ARQUEIRO", "ATIVA", 4, "PE",
                null, "Atira nas pernas do alvo, reduzindo seu deslocamento pela metade na próxima rodada.");
        hab("ARMADILHA_DE_CACADOR", "Armadilha de Caçador", "ARQUEIRO,CACADOR", "ATIVA", 5, "PE",
                "Trilha Caçador", "suas armadilhas são especiais, causando o dobro de efeito ou dano dependendo da armadilha.");
        hab("CHUVA_DE_PROJETEIS", "Chuva de Projéteis", "ARQUEIRO", "ATIVA", 8, "PE",
                null, "Dispara uma chuva de flechas em uma área curta. Cada inimigo na área sofre 1 golpe. Para cada "
                        + "inimigo adicional atingido, o dano de todas as flechas é reduzido em 1.");
        hab("OLHO_DO_PREDADOR", "Olho do Predador", "MERCENARIO", "ATIVA", 8, "PE",
                "Trilha Mercenário", "Adicione 1 a cada 10 pontos de inteligência na margem de crítico para o próximo disparo.");
        hab("BARREIRA_ARCANA", "Barreira Arcana", "MAGO", "ATIVA", 6, "PM",
                null, "Cria um escudo de mana que absorve dano mágico igual a duas vezes o bônus de Inteligência.");
        hab("SOBRECARGA_MAGICA", "Sobrecarga Mágica", "ARQUIMAGO,MAGO", "ATIVA", 6, "PM",
                "Trilha Arquimago", "O próximo feitiço causa o dobro de dano, mas o Mago fica Vulnerável por 1 rodada.");
        hab("ESCRITA_DE_RUNA", "Escrita de Runa", "MAGO,RUNICISTA", "ATIVA", 6, "PM",
                "Trilha Runicista", "O usuário inscreve uma runa mágica em um alvo, aliado ou superfície. Escolha um atributo, a "
                        + "runa concede 2 ao atributo escolhido por 3 turnos. A cada 15 níveis, o limite de runas que "
                        + "podem ser inscritas no mesmo alvo ou superfície aumenta em 1.");
        hab("TRANSMUTACAO_DE_MANA", "Transmutação de Mana", "MAGO", "PASSIVA", 0, null,
                null, "O Mago pode converter 10 pontos de PE em 10 pontos de PM 3 vezes por dia (a cada 10 niveis) (ou vice-versa).");
        hab("MENTE_LIMPIDA", "Mente Límpida", "MAGO", "ATIVA", 3, "PM",
                null, "Concede imunidade a efeitos de origem mágica por 3 rodadas.");
        hab("DESAFIO_DE_HONRA", "Desafio de Honra", "CAVALEIRO,ESPADACHIM,CAMPEAO", "ATIVA", 4, "PE",
                "Espadachim, Cavaleiro, Campeão", "Força um inimigo a focar seus ataques apenas no Espadachim por 1 rodada 1 vez por alvo.");
        hab("CORTE_DE_RETENCAO", "Corte de Retenção", "ESPADACHIM,SAMURAI", "ATIVA", 6, "PE",
                "Trilha Samurai", "Um ataque rápido que, se acertar, impede o alvo de realizar reações neste turno.");
        hab("DANCA_DAS_LAMINAS", "Dança das Lâminas", "ESPADACHIM,SAMURAI", "PASSIVA", 0, null,
                "samurai, nivel 10", "O usuário recebe 1 de Armadura Física para cada inimigo adjacente, aumentando esse bônus em 1 a "
                        + "cada 15 níveis. Além disso, ao empunhar uma espada média, seus ataques causam 2 de dano para "
                        + "cada inimigo adjacente.");
        hab("LAMINA_RESSONANTE", "Lâmina Ressonante", "ESPADACHIM", "ATIVA", 5, "PE",
                null, "Faça sua arma vibrar em alta frequência. Seus ataques corpo a corpo causam 4 de dano de "
                        + "impacto, aumentando em +2 a cada 5 níveis, e ignoram a redução de dano concedida por armaduras.");
        hab("GATUNO_AGIL", "Gatuno Ágil", "LADRAO", "ATIVA", 2, "PE",
                null, "Permite realizar a ação de \"Roubar\" como uma Ação Secundária durante o combate.");
        hab("SAQUE_DO_PIRATA", "Saque do Pirata", "LADRAO,PIRATA", "PASSIVA", 0, null,
                "Trilha Pirata", "Ao derrotar um inimigo, o Ladrão recupera 5 PE, aumentando em +5 PE a cada 10 níveis. Este "
                        + "efeito pode ser ativado até 3 vezes por dia.");
        hab("BOMBA_DE_FUMACA", "Bomba de fumaça", "LADRAO", "ATIVA", 5, "PE",
                null, "Cria uma nuvem que concede Cobertura Total em um raio curto por 1 rodada.");
        hab("SORTE_DO_CRIMINOSO", "Sorte do Criminoso", "LADRAO", "ATIVA", 5, "PE",
                null, "Uma vez por cena, o ladrão pode rolar um teste de Esquiva que tenha falhado.");
        hab("BALSAMO_REGENERADOR", "Bálsamo Regenerador", "CURANDEIRO", "ATIVA", 5, "PM",
                null, "pode escolher de 2 a 3 aliados para recuperar 3 de hp por turno durante os próximos 4 turno ( esse número aumenta em 2 a cada 5 níveis)");
        hab("PRECE_DE_ALIVIO", "Prece de Alívio", "CURANDEIRO,SACERDOTE", "ATIVA", 6, "PM",
                "Trilha Sacerdote", "tira os efeitos de todos os aliados podendo usar 3 vezes por dia (esse limite aumenta em 1 a cada 5 niveis).");
        hab("TOXINA_PARALISANTE", "Toxina Paralisante", "CURANDEIRO,MESTRE_TOXINAS", "ATIVA", 4, "PE",
                "Trilha Mestre de Toxinas", "Cobre uma arma com veneno que deixa o alvo Paralisado por 2 rodadas caso acerte o ataque . O "
                        + "alvo deve realizar um teste de Vigor contra a Inteligência do usuário, caso passe ainda fica 1 "
                        + "Turno lento.");
        hab("DIAGNOSTICO_RAPIDO", "Diagnóstico Rápido", "CURANDEIRO", "ATIVA", 8, "PM",
                null, "Identifica instantaneamente o HP atual e as fraquezas elementais de um inimigo (duas vezes por dia, podendo aumentar esse limite em 1 a cada 5 niveis).");
        hab("ESTABILIZAR_SINAIS", "Estabilizar Sinais", "CURANDEIRO", "ATIVA", 8, "PM",
                null, "Garante que um aliado que chegou a 0 HP não precise fazer testes de morte por 3 rodadas.");
        hab("DRENO_DE_VITALIDADE", "Dreno de Vitalidade", "BRUXO", "ATIVA", 6, "PE",
                null, "A próxima magia ofensiva do usuário passa a causar dano do elemento Morte em vez de seu "
                        + "elemento original. Além disso, o usuário recupera PV equivalentes à metade do dano causado.");
        hab("SERVO_PUTREFATO", "Servo Putrefato", "BRUXO,NECROMANTE", "ATIVA", 6, "PE",
                "Trilha Necromante", "Fortalece um morto-vivo invocado, concedendo-lhe +2 em qualquer atributo (aumentando em +2 essa quantidade a cada 5 níveis) por 3 rodadas uma vez por cena.");
        hab("MAO_DO_MEDO", "Mão do Medo", "BRUXO", "ATIVA", 4, "PM",
                null, "Ataque de toque. Se atingir o alvo, ele fica Amedrontado, tornando-se incapaz de se aproximar "
                        + "do Bruxo. O alvo pode evitar o efeito com um teste de Vigor ou Esquiva.");
        hab("PACTO_DE_SANGUE", "Pacto de Sangue", "BRUXO", "ATIVA", 10, "PE",
                null, "O Bruxo sacrifica 10 HP para recuperar 10PM ou 10 PE (2 vezes por dia) podendo aumentar o limite para + 10 de vida a cada 10 níveis.");
        hab("GRANADA_QUIMICA", "Granada Química", "ALQUIMISTA", "ATIVA", 4, "PE",
                null, "Lança um frasco que causa dano de fogo ou ácido em uma área curta, se causar algum efeito será dobrado.");
        hab("INFUSAO_ARCANA", "Infusão Arcana", "ALQUIMISTA,QUIMICO_ARCANO", "ATIVA", 6, "PM",
                "Trilha Químico Arcano", "Escolha um elemento natural ao ativar esta habilidade. Até o fim da cena, a arma de um aliado "
                        + "fica imbuída com esse elemento, fazendo com que seus ataques causem 5 de dano elemental "
                        + "adicional, aumentando em +5 a cada 10 Níveis do Personagem.");
        hab("ENGRENAGEM_DE_DEFESA", "Engrenagem de Defesa", "ALQUIMISTA,INVENTOR", "ATIVA", 4, "PE",
                "Trilha Inventor", "Conecta um dispositivo à armadura que concede +2 de bônus em Bloqueio (aumentando em +2 a cada 5 niveis) só pode ser utilizado em um dos aliados, não acumulativo.");
        hab("ANTIDOTO_RAPIDO", "Antídoto Rápido", "ALQUIMISTA", "ATIVA", 3, "PE",
                null, "Cria instantaneamente uma cura para qualquer veneno comum encontrado e podendo gastar + 7 PE caso seja um veneno incomum.");
        hab("NEVOA_DE_ESTASE", "Névoa de Estase", "ALQUIMISTA", "ATIVA", 5, "PE",
                null, "Lança um gás que reduz a Iniciativa pela metade de todos os inimigos afetados.");
        hab("ESTOCADA_DE_ALCANCE", "Estocada de Alcance", "LANCEIRO", "ATIVA", 3, "PE",
                null, "O usuário realiza uma estocada precisa, aumentando em um passe o seu alcance. O ataque recebe 2 "
                        + "em Combate, aumentando em +2 a cada 5 Níveis do Personagem.");
        hab("SALTO_DO_GENERAL", "Salto do General", "GENERAL_CEUS,LANCEIRO", "ATIVA", 5, "PE",
                "Trilha General dos Céus", "O usuário salta em direção a um inimigo, realizando um teste de Acrobacia contra a Esquiva do "
                        + "alvo. Em caso de sucesso, desfere um ataque com 2 em Combate. Se o alvo estiver no ar, o ataque "
                        + "causa dano dobrado. Em caso de falha, o usuário aterrissa desequilibrado e recebe o estado "
                        + "Vulnerável até o início de seu próximo turno.");
        hab("INVESTIDA_DA_VALQUIRIA", "Investida da Valquíria", "LANCEIRO,VALQUIRIA", "ATIVA", 6, "PE",
                "Trilha Valquíria", "O usuário avança em linha reta, atacando todos os inimigos em seu caminho, caso não acerte mais que 1, irá tomar ataque de oportunidade (máximo 3).");
        hab("BARREIRA_GIRATORIA_LANCEIRO", "Postura Defensiva", "LANCEIRO", "ATIVA", 3, "PE",
                null, "Até o início do seu próximo turno, criaturas que entrarem no alcance de sua lança provocam um "
                        + "ataque de oportunidade, mesmo que normalmente não o fariam.");
        hab("PONTA_ROMPEDORA", "Ponta Rompedora", "LANCEIRO", "ATIVA", 6, "PE",
                null, "O ataque ignora a resistência a dano perfurante do alvo por 3 rodadas, uma vez por cena.");
        hab("GANCHO_ASCENDENTE", "Gancho Ascendente", "LUTADOR", "ATIVA", 5, "PE",
                null, "Um soco potente que joga o alvo para cima, deixando-o Vulnerável por 1 rodada.");
        hab("POSTURA_DO_CAMPEAO", "Postura do Campeão", "CAMPEAO,LUTADOR", "PASSIVA", 0, null,
                "Trilha Campeão", "Enquanto estiver cercado inimigos, o Lutador ganha +2 de Dano Extra por cada inimigo (aumentando em +2 a cada 5 níveis).");
        hab("CONTRA_ATAQUE_FLUIDO", "Contra-Ataque Fluído", "ARTISTA_MARCIAL,LUTADOR", "ATIVA", 3, "PE",
                "Trilha Artista Marcial", "Ao desviar de um ataque, o Lutador pode realizar um ataque desarmado imediato como reação.");
        hab("AGARRAO_BRUTAL", "Agarrão Brutal", "LUTADOR", "ATIVA", 5, "PE",
                null, "O Lutador imobiliza o alvo; ambos ficam sob o estado Agarrado, mas o alvo recebe dano de sufocamento a cada turno (2d8) +1 dado a cada 5 níveis.");
        hab("FOCO_INABALAVEL", "Foco Inabalável", "LUTADOR", "PASSIVA", 0, null,
                null, "O lutador ignora qualquer penalidade de movimento causada por terrenos difíceis, gerando +2 em testes físicos em terrenos difíceis (aumentando em +2 a cada 5 níveis).");
        hab("DEMPSEY_ROLL_DANCA_DO_PUNHO_DEMOLIDOR", "DEMPSEY ROLL - Dança do Punho Demolidor", "LUTADOR", "ATIVA", 5, "PE",
                null, "Sempre que gastar uma Ação de Movimento, o usuário recebe 1 Acúmulo de Impulso (máximo 4). "
                        + "Enquanto possuir Acúmulos de Impulso, não pode realizar ataques, ficando limitado a ações de "
                        + "Deslocamento e Esquiva. Ao encerrar a habilidade, realiza seus ataques normalmente. Para cada "
                        + "Acúmulo de Impulso, recebe 1 dado de dano desarmado e +1 na margem de ameaça. Caso Esquive de um "
                        + "ataque enquanto a habilidade estiver ativa, pode gastar 1 Reação para realizar 1 ataque "
                        + "adicional ao final da habilidade. Se sofrer dano durante esse período, todos os Acúmulos de "
                        + "Impulso são perdidos e a habilidade é encerrada imediatamente. Ao final de cada turno em que "
                        + "permanecer acumulando impulso, realize um teste de Atletismo (DT 16 +4 por Acúmulo de Impulso). Em "
                        + "caso de falha, a habilidade é encerrada. Custo: 5 PE para ativar, mais 2 PE por Acúmulo de Impulso gasto.");
                hab("MELODIA_REVIGORANTE", "Melodia Revigorante", "BARDO,VIAJANTE", "ATIVA", 6, "PM",
                "Trilha Bardo", "Toca uma música que recupera 5 PE de todos os aliados próximos por 2 turnos (uma vez por cena).");
        hab("VATICINIO_DO_DESTINO", "Vaticínio do Destino", "CIGANO,VIAJANTE", "ATIVA", 3, "PE",
                "Trilha Cigano", "O Viajante prevê um golpe, concedendo bônus na próxima Esquiva de um aliado de +2 (aumentando em +2 a cada 5 NÍVEIS).");
        hab("CARTAS_DE_AZAR", "Cartas de Azar", "CARTOMANTE,VIAJANTE", "ATIVA", 8, "PE",
                "Trilha Cartomante", "Lança uma carta que reduz a sorte do inimigo, causando desvantagem no próximo alvo.");
        hab("RELATO_DE_VIAGEM", "Relato de Viagem", "SABIO,VIAJANTE", "ATIVA", 6, "PM",
                "SÁBIO", "O Viajante compartilha uma história que concede +8 em testes de Conhecimento para todo o grupo por 1 hora.");
        hab("REFLETIR_IMPACTO", "Refletir Impacto", "ESCUDEIRO", "ATIVA", 5, "PE",
                "Nível 15", "Ao realizar um Bloqueio Crítico, devolve metade do dano ao atacante. [Requer: Nível 15].");
        hab("ESCUDO_ESMAGADOR", "Escudo Esmagador", "ESCUDEIRO", "ATIVA", 4, "PE",
                null, "Pode realizar um Ataque corpo a corpo usando a perícia Bloqueio em vez de Combate. .");
        hab("VONTADE_DO_PROTETOR", "Vontade do Protetor", "ESCUDEIRO", "PASSIVA", 0, null,
                null, "Recebe 2 PE temporário toda vez que usar \"Escudo Aliado\" com sucesso (aumentando em +2 a cada 5 níveis). [Requer: Escudo Aliado].");
        hab("FOCO_DE_BATALHA", "Foco de Batalha", "COMANDANTE", "ATIVA", 4, "PE",
                null, "Marca um inimigo; aliados que atacarem esse alvo irão ganhar +2 no acerto, porém caso queira focar em outro alvo, irá ter -2 no acerto (aumentando esse bônus em +2 a cada 5 niveis). .");
        hab("MANOBRA_DE_FLANCO", "Manobra de Flanco", "COMANDANTE", "ATIVA", 6, "PE",
                null, "Permite que um aliado se mova como Ação Livre neste turno. .");
        hab("GRITO_DE_RETOMADA", "Grito de Retomada", "COMANDANTE", "ATIVA", 5, "PE",
                null, "Remove condições mentais de aliados em área média. .");
        hab("ESTRATEGIA_DEFENSIVA", "Estratégia Defensiva", "COMANDANTE", "PASSIVA", 0, null,
                null, "O grupo recebe +2 em testes de Iniciativa enquanto o Comandante estiver consciente(aumenta em +2 a cada 5 niveis). .");
        hab("GOLPE_COORDENADO", "Golpe Coordenado", "COMANDANTE", "ATIVA", 10, "PE",
                null, "Se o Comandante acertar um ataque, um aliado próximo pode realizar um ataque simples contra o mesmo alvo. .");
        hab("PROTECAO_FERREA", "Proteção Férrea", "COMANDANTE", "PASSIVA", 0, null,
                null, "Aliados adjacentes recebem +4 de Armadura Física, ao fazer um grito de guerra inspirando seus aliados, durante a cena. (5 PE).");
        hab("KAWARIMI", "Kawarimi", "NINJA", "ATIVA", 10, "PE",
                "Substituição", "Reação para trocar de lugar com um objeto próximo, anulando o dano recebido, uma vez por cena. .");
        hab("LAMINAS_MULTIPLAS", "Lâminas Múltiplas", "NINJA", "ATIVA", 2, "PE",
                null, "Arremessa até 3 armas leves (shurikens/facas) em uma única ação. . A partir do nível 20, pode "
                        + "lançar até 6 armas leves.");
        hab("FUMACA_CEGANTE", "Fumaça Cegante", "NINJA", "PASSIVA", 0, null,
                null, "Ao utilizar uma bomba de fumaça o inimigo também ficará ofuscado. .");
        hab("GOLPE_DE_LOTUS", "Golpe de Lótus", "MONGE,NINJA", "ATIVA", 5, "PE",
                "monge", "Ataque que causa dano direto nos Pontos de Mana (PM) do alvo. .");
        hab("DILACERAR", "Dilacerar", "SICARIO", "ATIVA", 5, "PE",
                null, "Seu próximo ataque adiciona 1 acúmulo de Sangramento. Se o alvo ainda não estiver Sangrando, "
                        + "aplica 2 acúmulos em vez de 1.");
        hab("VENENO_DE_PARALISIA", "Veneno de Paralisia", "MESTRE_TOXINAS,SICARIO", "ATIVA", 4, "PE",
                "Mestre de Toxinas", "Aplica veneno que reduz a Agilidade e destreza do alvo em -2 por 3 rodadas (bônus aumenta em +2 a cada 5 niveis). .");
        hab("SEDE_DE_SANGUE", "Sede de Sangue", "SICARIO", "PASSIVA", 0, null,
                null, "Recebe 4 PE temporários para cada inimigo que receber sangramento pela primeira vez no combate. .");
        hab("EXPOR_FRAQUEZA", "Expor Fraqueza", "SICARIO", "ATIVA", 3, "PE",
                null, "Após um ataque crítico Reduz a Armadura do alvo em valor igual à sua Destreza por 2 rodadas. .");
        hab("FURIA_SANGRENTA", "Fúria Sangrenta", "BERSERKER", "PASSIVA", 0, null,
                null, "Ganha +2 de Dano Extra para cada 10 PV perdidos em batalha, aumentando esse bônus em +2 a cada 10 niveis. .");
        hab("GOLPE_DESTRUTIVO", "Golpe Destrutivo", "BERSERKER", "ATIVA", 6, "PE",
                null, "Ignora a Armadura física do alvo, mas o Bárbaro fica \"Vulnerável\" por 1 rodada. .");
        hab("REDEMOINHO_DE_CARNE", "Redemoinho de Carne", "BERSERKER,VALQUIRIA", "ATIVA", 10, "PE",
                "Valquiria", "Ataca todos os inimigos adjacentes de uma vez, sendo que recebe -3 em testes de ataque a cada inimigo. .");
        hab("RESISTENCIA_A_DOR", "Resistência à Dor", "BERSERKER", "PASSIVA", 0, null,
                null, "Enquanto em \"Estado de Fúria\", recebe 10 de armadura física Enquanto estiver com o estado de fúria ativo (aumenta a redução em +10 a cada 10 niveis). .");
        hab("PELE_DE_FERRO", "Pele de Ferro", "IMORTAL", "PASSIVA", 0, null,
                null, "Recebe Armadura baseada em metade do atributo constituição (nível 20 necessário). .");
        hab("CURA_ADRENALINA", "Cura Adrenalina", "IMORTAL", "ATIVA", 6, "PE",
                null, "Converte todos os acúmulos de Fúria em cura direta (5 Pv por acúmulo). .");
        hab("DESAFIO_SUPREMO", "Desafio Supremo", "IMORTAL", "ATIVA", 5, "PE",
                null, "Faz com que, caso todos os inimigos em alcance curto ataquem outro alvo que não seja o Imortal, estes sofram -10 em seu Acerto. .");
        hab("VIGOR_INESGOTAVEL", "Vigor Inesgotável", "IMORTAL", "PASSIVA", 0, null,
                null, "Reduz o custo de PE de todas as habilidades de reação em -2, nunca a menos que 1.(nível 20 necessário). .");
        hab("GOLPE_DO_SOBREVIVENTE", "Golpe do Sobrevivente", "IMORTAL", "ATIVA", 10, "PE",
                null, "Dano do ataque aumenta baseado na Constituição atual do usuário gerando dano extra com base na sua constituição.");
        hab("AURA_DE_RETRIBUICAO", "Aura de Retribuição", "PALADINO", "ATIVA", 5, "PE",
                null, "Quando é atacado por bênçãos de uma divindade que não seja a sua, pode devolver 3d6 de dano mágico de sua divindade para o atacante. (aumentando 1 DADO de dano a cada 5 niveis). .");
        hab("ESCUDO_PURIFICADOR", "Escudo Purificador", "PALADINO", "ATIVA", 4, "PE",
                null, "Ao bloquear um ataque, remove um efeito negativo de si mesmo. .");
        hab("INVESTIDA_SAGRADA", "Investida Sagrada", "ESCUDEIRO,PALADINO", "ATIVA", 5, "PE",
                "escudeiro", "Avança em linha reta; o primeiro alvo atingido fica \"Atordoado\" ou derrubado. .");
        hab("BASTIAO_DA_LUZ", "Bastião da Luz", "PALADINO", "PASSIVA", 0, null,
                null, "Aliados em alcance curto ganham 10 de Armadura Mágica contra bênçãos do panteão oposto ao deus "
                        + "do usuário (+10 de Armadura a cada 20 Níveis do Personagem). (5 PE).");
        hab("LUZ_GUIA", "Luz Guia", "SANTO", "ATIVA", 5, "PE",
                null, "Concede Bônus no próximo teste de ataque com bênção de um aliado, aumentando +2 para o seu acerto. (aumentando em +2 a cada 5 niveis). .");
        hab("SANTUARIO", "Santuário", "SANTO", "ATIVA", 5, "PE",
                null, "Cria uma área de 5 metros onde inimigos que possuam divindades do panteão oposto ao usuário precisam passar em teste de Virtude para entrar ou passar algum ataque. .");
        hab("PALAVRAS_DE_PAZ", "Palavras de Paz", "SANTO", "ATIVA", 5, "PE",
                null, "Para testes cujo objetivo seja evitar confrontos ou desavenças, o usuário recebe +15 de bônus "
                        + "em Diplomacia. . Criaturas hostis devem passar em um teste de Mente contra a Diplomacia do "
                        + "usuário para iniciar um combate.");
        hab("MARTIR", "Mártir", "SANTO", "ATIVA", 3, "PE",
                null, "Pode escolher receber os efeitos negativos destinados a um aliado próximo. (Reação - Gasto: 5 PE).");
        hab("CRESCIMENTO_ACELERADO", "Crescimento Acelerado", "ARAUTO_NATUREZA", "ATIVA", 5, "PE",
                null, "Transforma o terreno em \"Terreno Difícil\" através de raízes. .");
        hab("RAIZES_CURATIVAS", "Raízes Curativas", "ARAUTO_NATUREZA", "ATIVA", 2, "PE",
                null, "Aliados em contato com solo natural recuperam 4 PV por turno (a cada 5 niveis aumenta em +4 esse PV ganho), necessário ação sustentada. .");
        hab("FURIA_ELEMENTAL", "Fúria Elemental", "ARAUTO_NATUREZA", "ATIVA", 6, "PE",
                null, "Adiciona qualquer elemento de origem natural à escolha do usuário, para qualquer Feitiço que este possuir. .");
        hab("VOZ_DA_MATA", "Voz da Mata", "ARAUTO_NATUREZA", "ATIVA", 6, "PE",
                null, "Pode \"ouvir\" as plantas para localizar inimigos em florestas em uma distância de 50 metros (caso esteja em floresta). .");
        hab("CORPO_VEGETAL", "Corpo Vegetal", "ARAUTO_NATUREZA", "ATIVA", 8, "PE",
                null, "Você invoca raízes naturais para servir como sua armadura, elas possuem sua Sabedoria em PV e 5 de armadura (a cada 5 Níveis do Personagem). (8 PE).");
        hab("COMANDO_DE_REACAO", "Comando de Reação", "DOMADOR_FERAS", "ATIVA", 8, "PE",
                null, "Quando o usuário for atacado, uma criatura aliada que possua pode realizar um ataque imediato como Reação. .");
        hab("VINCULO_VITAL", "Vínculo Vital", "DOMADOR_FERAS", "PASSIVA", 0, null,
                null, "Divide o dano recebido igualmente entre o Druida e sua fera. (6 PE).");
        hab("OLHAR_BESTIAL", "Olhar Bestial", "DOMADOR_FERAS", "ATIVA", 4, "PE",
                null, "O Druida pode enxergar através dos olhos de sua fera até no máximo sua Sabedoria em metros. .");
        hab("RUGIDO_SINCRONIZADO", "Rugido Sincronizado", "DOMADOR_FERAS", "ATIVA", 6, "PE",
                null, "Druida pode realizar um teste de Adestrar para intimidar o seu alvo, além disso, para cada criatura que possua recebe +2 de bônus em seu teste. .");
        hab("EVOLUCAO_TEMPORARIA", "Evolução Temporária", "DOMADOR_FERAS", "ATIVA", 8, "PE",
                null, "Aumenta a categoria da fera em um nível por 1d4 rodadas. .");
        hab("MENTE_DE_CRISTAL", "Mente de Cristal", "SABIO", "PASSIVA", 0, null,
                null, "Torna-se imune ao estado \"Confuso\" e efeitos de leitura de mente. .");
        hab("PALMA_DA_SABEDORIA", "Palma da Sabedoria", "SABIO", "ATIVA", 6, "PE",
                null, "Inicia uma sequência de golpes corpo a corpo. Cada ataque corpo a corpo acertado "
                        + "consecutivamente concede 1 de Sabedoria. O bônus é perdido ao errar um ataque corpo a corpo.");
        hab("FLUXO_DE_KI", "Fluxo de Ki", "SABIO", "PASSIVA", 0, null,
                null, "Escolha quantos Pontos de Energia gastar, um aliado recebe metade desse valor. Por cena só pode "
                        + "ser usado 1 vez por aliado.");
        hab("LEITURA_DE_COMBATE", "Leitura de Combate", "SABIO", "ATIVA", 5, "PE",
                null, "Para cada golpe que errar, você recebe +2 no próximo ataque, o bônus aumenta em 1 a cada 10 "
                        + "níveis do personagem.");
        hab("ESTADO_DE_NIRVANA", "Estado de Nirvana", "SABIO", "ATIVA", 12, "PE",
                null, "Pelas próximas 1d4 de rodadas, o Monge tem o gasto de qualquer habilidade reduzido para metade. .");
        hab("IMPACTO_SISMICO", "Impacto Sísmico", "PUNHOS_ACO", "ATIVA", 6, "PE",
                null, "Soco no solo que derruba todos os inimigos adjacentes, deixando-os derrubados, devem realizar um teste de Esquiva ou Vigor, contra o ataque do usuário. .");
        hab("BLINDAGEM_INTERNA", "Blindagem Interna", "PUNHOS_ACO", "ATIVA", 4, "PE",
                null, "O usuário adiciona em sua Armadura Física atual a sua Armadura Mágica e em sua Armadura Mágica "
                        + "atual a sua Armadura Física. Após sofrer um ataque físico, perde o bônus de Armadura Física e "
                        + "após sofrer um ataque mágico, perde o bônus de Armadura Mágica.");
        hab("FOCO_DESTRUTIVO", "Foco Destrutivo", "PUNHOS_ACO", "PASSIVA", 0, null,
                null, "Dobra o dano causado contra escudos e objetos inanimados. .");
        hab("ARMADILHA_DE_ESPINHOS", "Armadilha de Espinhos", "CACADOR", "ATIVA", 5, "PE",
                null, "Cria uma armadilha que imobiliza o alvo por 1 rodada caso ele passe nela. .");
        hab("TIRO_DE_RASTREIO", "Tiro de Rastreio", "CACADOR", "ATIVA", 3, "PE",
                null, "O inimigo atingido não pode se beneficiar de invisibilidade ou furtividade. .");
        hab("ABATE_SILENCIOSO", "Abate Silencioso", "CACADOR", "ATIVA", 10, "PE",
                null, "Adiciona +2 dados de dano contra alvos desprevenidos. (10 PE).");
        hab("CHAMADO_SELVAGEM", "Chamado Selvagem", "CACADOR,DRUIDA", "ATIVA", 6, "PE",
                "Druida", "Atrai pequenos animais da região para distrair o inimigo. .");
        hab("MIMETISMO_FLORESTAL", "Mimetismo Florestal", "CACADOR", "ATIVA", 4, "PE",
                null, "Fica invisível em ambientes naturais se permanecer imóvel por 1 turno. .");
        hab("TIRO_DE_OURO", "Tiro de Ouro", "MERCENARIO", "PASSIVA", 0, null,
                null, "O passo de dano aumenta em +1 para cada 500 moedas de ouro que o Arqueiro carrega. .");
        hab("VISAO_CALCULISTA", "Visão Calculista", "MERCENARIO", "ATIVA", 12, "PE",
                null, "Soma metade do atributo Inteligência no teste de Pontaria. .");
        hab("RETIRADA_AGIL", "Retirada Ágil", "LADRAO,MERCENARIO", "ATIVA", 5, "PE",
                "Ladrão", "Pode fugir do alcance de um inimigo sem provocar ataques de oportunidade. .");
        hab("CONTRATO_DE_SANGUE", "Contrato de Sangue", "MERCENARIO", "PASSIVA", 0, null,
                null, "Recebe 4 PE temporário toda vez que finalizar um inimigo humanoide. .");
        hab("CAMPO_DE_MANA", "Campo de Mana", "ARQUIMAGO", "ATIVA", 5, "PE",
                null, "Cria uma área de alcance curto que reduz o custo de magias de aliados em -2 PM, nunca a menos de 1 PM (aumentando em -2 a cada 10 níveis). .");
        hab("ANULAR_MAGIA", "Anular Magia", "ARQUIMAGO", "ATIVA", 10, "PE",
                null, "Usa Reação para dissipar um feitiço inimigo de até um Grau que possua usando um teste de Feitiçaria. .");
        hab("MENTE_EXPANDIDA", "Mente Expandida", "ARQUIMAGO", "PASSIVA", 0, null,
                null, "Permite manter duas magias de \"Concentração\" ativas ao mesmo tempo. (8 PE).");
        hab("TORRENTE_ARCANA", "Torrente Arcana", "ARQUIMAGO", "ATIVA", 20, "PE",
                null, "Consome todo o seu PM atual e dispara uma torrente de energia que causa 1d10 de Dano Arcano. "
                        + "Para cada 10 PM consumidos, o ataque recebe 1 de dano e 1 no acerto. Se o ataque errar, metade "
                        + "do PM consumido é restaurado.");
        hab("RUNA_DE_PROTECAO", "Runa de Proteção", "RUNICISTA", "ATIVA", 4, "PE",
                null, "Grava uma runa em uma armadura, concedendo +4 de Armadura Mágica dura a cena (+2 a cada 5 níveis) . .");
        hab("RUNA_EXPLOSIVA", "Runa Explosiva", "RUNICISTA", "ATIVA", 6, "PE",
                null, "Grava uma runa em um objeto que explode quando tocado por um inimigo, causando 3d6 de dano "
                        + "aumentando 3 dados a cada 10 níveis e o alcance em 1 passo a cada 15 níveis.");
        hab("ENCANTAMENTO_DE_LAMINA", "Encantamento de Lâmina", "RUNICISTA", "ATIVA", 3, "PE",
                null, "Converte todo o dano de uma arma em mágico por 1d4 turnos. .");
        hab("MESTRE_DAS_GRAVURAS", "Mestre das Gravuras", "RUNICISTA", "PASSIVA", 0, null,
                null, "Pode ativar runas como Ação Livre uma vez por cena. .");
        hab("PARADA_ELEGANTE", "Parada Elegante", "DUELISTA", "ATIVA", 5, "PE",
                null, "Ao Bloquear um ataque com sucesso ganha Bônus no próximo ataque contra o mesmo inimigo de +2 (+2 a cada 5 níveis). .");
        hab("FINTA_MAGISTRAL", "Finta Magistral", "DUELISTA", "ATIVA", 5, "PE",
                null, "Realiza um teste de enganação contra o alvo para reduzir a Esquiva do alvo em de acordo com a diferença de valores. .");
        hab("MESTRE_DO_DUELO", "Mestre do Duelo", "DUELISTA", "PASSIVA", 0, null,
                null, "Ganha +2 de Dano Extra para cada turno que permanecer lutando contra o mesmo alvo (+2 a cada 5 níveis). .");
        hab("IAIJUTSU", "Iaijutsu", "SAMURAI", "ATIVA", 6, "PE",
                null, "Fique um turno preparando o ataque; o dano é dobrado. Caso erre fica vulnerável por uma rodada .");
        hab("CORTE_ESPIRITUAL", "Corte Espiritual", "SAMURAI", "ATIVA", 5, "PE",
                null, "Ataque que ignora armadura física e foca no PE do alvo. .");
        hab("RESILIENCIA_BUSHIDO", "Resiliência Bushido", "SAMURAI", "PASSIVA", 0, null,
                null, "Ignora condições não mágicas enquanto estiver com menos da metade do HP. .");
        hab("GOLPE_DE_HONRA", "Golpe de Honra", "SAMURAI", "ATIVA", 8, "PE",
                null, "Após permanecer 2 turnos sem receber dano, o Samurai pode realizar este ataque, causando 2 "
                        + "dados de dano.");
        hab("PASSO_SOMBRIO", "Passo Sombrio", "SOMBRA", "ATIVA", 5, "PE",
                null, "Teleporta-se entre sombras em alcance curto.");
        hab("GRILHOES_DA_PENUMBRA", "Grilhões da Penumbra", "ASSASSINO,SOMBRA", "ATIVA", 2, "PE",
                "Passo de Sombra ou Passo Sombrio",
                "“Sua sombra te denuncia. E hoje ela trabalha para mim.” Ao terminar um Passo de Sombra (3 PE) ou Passo Sombrio (5 PE) — terminando adjacente a um inimigo ou tendo cruzado a sombra do alvo — pode pagar +2 PE como Ação Livre para aprisioná-lo: teste de Furtividade contra a Esquiva do alvo. Em sucesso, o alvo fica Preso nas Sombras até o fim do seu próximo turno: Deslocamento 0, não pode usar Esquiva e, ao se soltar, sai com Lentidão por 1 turno; no resto age normalmente (atacar, conjurar e usar habilidades que não exijam se mover). Como o gatilho é o passo, o efeito herda a condição de sombra. Escalonamento: a cada 10 níveis, +1 alvo adjacente ao ponto de chegada.");
        hab("LAMINA_DE_EBANO", "Lâmina de Ébano", "SOMBRA", "ATIVA", 3, "PE",
                null, "Ataque furtivo que silencia o alvo. .");
        hab("VULTO", "Vulto", "SOMBRA", "PASSIVA", 0, null,
                null, "Inimigos têm que rolar um 1d10 se cair 10 ele erra ataque físicos contra a Sombra. (passiva).");
        hab("GARROTE", "Garrote", "SOMBRA", "ATIVA", 8, "PE",
                null, "Caso esteja furtivo pode realizar um teste de Crime, para sabotar o seu alvo, dessa forma ele fica inibido de usar Magia por 1d4 Turnos. .");
        hab("GANCHO_DE_ABORDAGEM", "Gancho de Abordagem", "PIRATA", "ATIVA", 4, "PE",
                null, "Puxa um inimigo distante para perto de si. .");
        hab("PISTOLAGEM", "Pistolagem", "PIRATA", "ATIVA", 6, "PE",
                null, "Pode realizar um ataque a distância como Ação Secundária após um ataque corpo a corpo. .");
        hab("NAVEGADOR", "Navegador", "PIRATA", "PASSIVA", 0, null,
                null, "Vantagem em testes de Sobrevivência e Percepção em ambientes aquáticos. .");
        hab("SAQUE_DE_TESOURO", "Saque de Tesouro", "PIRATA", "PASSIVA", 0, null,
                null, "Ganha o dobro de ouro ao abrir baús do tesouro. .");
        hab("PRECE_DE_RESSURREICAO", "Prece de ressurreição", "SACERDOTE", "ATIVA", 15, "PE",
                null, "Levanta um aliado caído instantaneamente sem precisar de teste, só pode ser usado 1 vez por alvo. .");
        hab("AURA_DE_PUREZA", "Aura de Pureza", "SACERDOTE", "ATIVA", 4, "PE",
                null, "Aliados próximos tornam-se imunes a efeitos mentais negativos. ).");
        hab("LUZ_DE_ALIVIO", "Luz de alívio", "SACERDOTE", "ATIVA", 5, "PE",
                null, "O alvo recebe 1d12 de PE temporário e 1d12 de Armadura Mágica por 3 turnos. Ambos os valores "
                        + "aumentam em 1d12 a cada 15 níveis.");
        hab("VINCULO_SAGRADO", "Vínculo Sagrado", "SACERDOTE", "PASSIVA", 0, null,
                null, "O usuário pode escolher repartir qualquer cura mágica entre seus aliados. .");
        hab("NUVEM_TOXICA", "Nuvem Tóxica", "MESTRE_TOXINAS", "PASSIVA", 0, null,
                null, "Danos de \"Envenenamento\" em área aumenta em 1 passo. .");
        hab("NEUTRALIZADOR_TOXICO", "Neutralizador tóxico", "MESTRE_TOXINAS", "ATIVA", 3, "PE",
                null, "Remove qualquer tipo de veneno ou doença temporária de um aliado. .");
        hab("DARDO_ENTORPECENTE", "Dardo Entorpecente", "MESTRE_TOXINAS", "ATIVA", 4, "PE",
                null, "Ao realizar um Ataque à distância pode reduzir a Agilidade do alvo em -2 (A cada 10 Níveis do Personagem). .");
        hab("ESTUDO_ANATOMICO", "Estudo Anatômico", "MESTRE_TOXINAS", "PASSIVA", 0, null,
                null, "Críticos do Mestre de Toxinas ignoram a imunidade do alvo. .");
        hab("INJECAO_DE_ADRENALINA", "Injeção de Adrenalina", "MESTRE_TOXINAS", "ATIVA", 7, "PE",
                null, "Aliado ganha uma Ação Principal extra, mas perde 3d12 PV no fim do turno. .");
        hab("EXPLOSAO_DE_CADAVER", "Explosão de Cadáver", "NECROMANTE", "ATIVA", 5, "PE",
                null, "Explode um corpo no chão para causar 2d12 dano mágico em área curta, média ou grande, dependendo do corpo alvo( +1 dado a cada 10 níveis). .");
        hab("TOQUE_DA_MORTE", "Toque da Morte", "NECROMANTE", "ATIVA", 7, "PE",
                null, "Reduz o PV Máximo do alvo em 10, aumentando em +5 a cada 10 níveis, por 1 cena. Este efeito não "
                        + "pode ser acumulado no mesmo alvo.");
        hab("EXERCITO_PUTREFATO", "Exército Putrefato", "NECROMANTE", "PASSIVA", 0, null,
                null, "Pode controlar até 5 mortos-vivos simultaneamente(a cada 10 níveis). .");
        hab("CONSUMIR_MANANCIAL", "Consumir manancial", "NECROMANTE", "ATIVA", 4, "PE",
                null, "Ganha 10 de PM temporário para cada inimigo que morrer por perto. .");
        hab("OLHAR_DO_ALEM", "Olhar do Além", "MISTICO", "ATIVA", 5, "PE",
                null, "Vê através de paredes e detecta seres invisíveis na área média. .");
        hab("CORRENTE_DE_ALMAS", "Corrente de Almas", "MISTICO", "ATIVA", 10, "PE",
                null, "Une a alma de dois alvos, fazendo que tudo que um receba, o outro receba também em dano mágico, podendo ser usado somente em inimigos. .");
        hab("ABSORCAO_DE_ESSENCIA", "Absorção de Essência", "MISTICO", "PASSIVA", 0, null,
                null, "Ao sofrer pela primeira vez dano mágico em uma rodada, recebe 4 PE temporários.");
        hab("INVOCACAO_ANCESTRAL", "Invocação Ancestral", "MISTICO", "ATIVA", 10, "PE",
                null, "invoca um espírito local para atacar seus inimigos. .");
        hab("POCAO_EXPLOSIVA", "Poção Explosiva", "QUIMICO_ARCANO", "ATIVA", 2, "PE",
                null, "Aumenta a área de efeito em um passo de poções . .");
        hab("ELIXIR_DE_MANA", "Elixir de Mana", "QUIMICO_ARCANO", "ATIVA", 6, "PE",
                null, "Acrescenta um bônus de 1d10 (+1 dado a cada 10 níveis) PM e PE temporário, sempre que um aliado em alcance curto consumir uma poção feita pelo usuário. .");
        hab("MUTACAO_QUIMICA", "Mutação Química", "QUIMICO_ARCANO", "ATIVA", 5, "PE",
                null, "Aliados em alcance curto que consumirem poções feitas pelo usuário, recebem +2 em testes físicos (+2 a cada 10 Níveis). .");
        hab("SOLVENTE_UNIVERSAL", "Solvente Universal", "QUIMICO_ARCANO", "ATIVA", 4, "PE",
                null, "Ao atingir um objeto, arma, armadura ou estrutura, o solvente remove permanentemente 2 pontos "
                        + "de Armadura. Caso seja utilizado contra uma criatura, ela terá sua Armadura Física reduzida em "
                        + "2 pelo resto do dia.");
        hab("MESTRE_DE_MISTURAS", "Mestre de Misturas", "QUIMICO_ARCANO", "ATIVA", 2, "PE",
                null, "Pode consumir ou lançar duas poções em uma única ação. (2 PE).");
        hab("TORRETA_PORTATIL", "Torreta Portátil", "INVENTOR", "ATIVA", 8, "PE",
                null, "O usuário posiciona uma torreta mecânica construída durante um Descanso Longo, que ataca "
                        + "automaticamente a criatura hostil mais próxima durante 5 turnos. Uma torreta Pequena ocupa 2 "
                        + "espaços, possui 15 PV, 4 de Armadura e causa 1d6 de dano; uma Média ocupa 4 espaços, possui 30 "
                        + "PV, 8 de Armadura e causa 1d8; e uma Grande ocupa 6 espaços, possui 45 PV, 12 de Armadura e "
                        + "causa 1d10. O dano aumenta em 1 dado a cada 10 Níveis do Personagem. Caso a torreta seja "
                        + "destruída, ela só poderá ser reconstruída ao final de outro Descanso Longo. Os materiais "
                        + "utilizados em sua construção podem conceder capacidades adicionais.");
        hab("APRIMORAR_TORRETA", "Aprimorar Torreta", "INVENTOR", "ATIVA", 2, "PE",
                null, "Ao gastar seu turno, o usuário instala em uma de suas Torretas Portáteis um mecanismo "
                        + "construído durante um Descanso Longo, concedendo efeitos variados de acordo com sua construção. "
                        + "Caso não possua um mecanismo preparado, poderá improvisar uma modificação temporária, "
                        + "aumentando o dado de dano da torreta em um passo durante 2 turnos.");
        hab("DRONE_DE_RECONHECIMENTO", "Drone de Reconhecimento", "INVENTOR", "ATIVA", 4, "PE",
                null, "O usuário ativa um pequeno drone mecânico capaz de explorar locais distantes. Durante 5 turnos, "
                        + "pode enxergar e ouvir através dele enquanto o controla. O drone deve ser construído durante um "
                        + "Descanso Longo e, caso seja destruído, só poderá ser reconstruído ao final de outro descanso. "
                        + "Os materiais utilizados em sua construção podem conceder capacidades adicionais, a critério do "
                        + "mestre ou de outras habilidades do Inventor.");
        hab("MAO_NA_MASSA", "Mão na Massa", "INVENTOR", "PASSIVA", 0, null,
                null, "Durante um Interlúdio, o usuário recebe Vantagem em todos os testes realizados para criar, "
                        + "reparar ou aprimorar mecanismos, engenhocas e invenções. Além disso, uma vez a cada 3 dias, o "
                        + "usuário pode abrir mão de sua necessidade de dormir para continuar trabalhando em seus projetos "
                        + "durante a noite. Ao fazer isso, não recebe a Vantagem concedida por esta habilidade.");
        hab("SOBRECARGA_MECANICA", "Sobrecarga Mecânica", "INVENTOR", "ATIVA", 10, "PE",
                null, "O usuário força um de seus dispositivos mecânicos a operar além de seus limites. Durante este "
                        + "turno, ele recebe 2 Ações Principais. Ao final do turno seguinte, o dispositivo é destruído e "
                        + "só poderá ser reconstruído durante um Descanso Longo.");
        hab("COMANDO_DE_VOO", "Comando de Voo", "GENERAL_CEUS", "PASSIVA", 0, null,
                null, "Aliados próximos ganham +2 (+2 a cada 5 níveis) de bônus em testes de Acrobacia e Salto. .");
        hab("LANCA_DE_JULGAMENTO", "Lança de Julgamento", "GENERAL_CEUS", "ATIVA", 5, "PE",
                null, "Realiza um ataque, que causará dano no seu alvo e aqueles atrás dele. .");
        hab("ASAS_DO_COMANDO", "Asas do Comando", "GENERAL_CEUS", "PASSIVA", 0, null,
                null, "Ao sofrer dano de queda, o usuário pode gastar sua ação para realizar imediatamente um ataque "
                        + "contra o solo ou uma criatura dentro do alcance de sua arma. O dano causado por esse ataque "
                        + "reduz o dano de queda na mesma quantidade.");
        hab("IMPACTO_METEORICO", "Impacto Meteórico", "GENERAL_CEUS", "PASSIVA", 0, null,
                null, "O dano da lança aumenta em +1 de dano para cada metro de altura da queda. .");
        hab("BEIJO_DA_BATALHA", "Beijo da Batalha", "VALQUIRIA", "PASSIVA", 0, null,
                null, "Recebe 10 PE temporário toda vez que abater um inimigo com uma lança. .");
        hab("GRITO_DE_GUERRA_DAS_VALQUIRIAS", "Grito de Guerra das Valquírias", "VALQUIRIA", "ATIVA", 4, "PE",
                null, "Inspira aliados, concedendo +2 (+2 a cada 5 níveis) de dano físico por 1d4 rodadas. .");
        hab("LANCA_RELUZENTE", "Lança Reluzente", "VALQUIRIA", "ATIVA", 15, "PE",
                null, "Causa o estado \"Cego\" ao acertar o inimigo por 1d4 turnos. .");
        hab("ESPIRITO_DE_GUERRA", "Espírito de Guerra", "VALQUIRIA", "PASSIVA", 0, null,
                null, "Se o PV cair abaixo da metade, ganha uma Ação Principal extra no próximo turno. .");
        hab("POSTURA_INABALAVEL", "Postura Inabalável", "CAMPEAO", "PASSIVA", 0, null,
                null, "Torna-se imune a ser derrubado ou empurrado por ataques físicos. .");
        hab("DESAFIO_DO_RINGUE", "Desafio do Ringue", "CAMPEAO", "ATIVA", 5, "PE",
                null, "O usuário desafia todos os inimigos em alcance curto, realizando um teste de Carisma contra a "
                        + "Virtude de cada alvo. As criaturas que falharem ficam impedidas de se afastar voluntariamente "
                        + "do usuário durante 1d4 rodadas, podendo se mover normalmente dentro de seu alcance.");
        hab("GANCHO_DE_OURO", "Gancho de Ouro", "CAMPEAO", "ATIVA", 8, "PE",
                null, "Se o ataque for crítico, o alvo fica \"Atordoado\" por 1d4 turnos. .");
        hab("IDOLO_DAS_MASSAS", "Ídolo das Massas", "CAMPEAO", "PASSIVA", 0, null,
                null, "Enquanto houver 2 ou mais criaturas conscientes observando o combate de fora, recebe +2 em "
                        + "Carisma e 2 em Combate, aumentando ambos em 2 a cada 5 Níveis do Personagem.");
        hab("TROCAR_AS_MAOS_PELOS_PES", "Trocar as Mãos pelos Pés", "ARTISTA_MARCIAL", "ATIVA", 2, "PE",
                null, "Pode trocar bônus de esquiva por combate e combate por esquiva. .");
        hab("PONTO_DE_PRESSAO", "Ponto de Pressão", "ARTISTA_MARCIAL,MONGE", "ATIVA", 6, "PE",
                "Monge", "Paralisa o braço ou perna do inimigo por 1 rodada. Se paralisar o braço não consegue manejar objetos, e se for a perna perde deslocamento pela metade. .");
        hab("CONTRA_GOLPE_DE_PALMA", "Contra-golpe de Palma", "ARTISTA_MARCIAL", "ATIVA", 6, "PE",
                null, "Reação que devolve metade do dano físico recebido ao atacante. .");
        hab("PASSO_DE_VENTO", "Passo de Vento", "ARTISTA_MARCIAL", "PASSIVA", 0, null,
                null, "Permite esquivar de projéteis com bônus de +2 (+2 a cada 5 níveis). .");
        hab("COMBO_INFINITO", "Combo Infinito", "ARTISTA_MARCIAL", "PASSIVA", 0, null,
                null, "Cada ataque consecutivo que acertar o mesmo alvo no mesmo turno concede 2 de dano cumulativo, "
                        + "aumentando em +2 a cada 10 Níveis do Personagem. O bônus é perdido ao atacar outra criatura ou "
                        + "ao término do turno.");
    }

    /** Reaplica as habilidades (catalogo) em banco ja existente. */
    void refreshHabilidades() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            habilidadeRepository.deleteAll(habilidadeRepository.findByGameSystemIdAndOficialTrue(sid));
            seedHabilidades();
        });
    }

    /** Reaplica a tabela de progressao (XP/foco/recompensa/limite) numa base ja semeada. */
    void refreshProgressao() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            progressaoNivelRepository.deleteAll(progressaoNivelRepository.findByGameSystemIdOrderByNivel(sid));
            seedProgressao();
        });
    }

    private void hab(String codigo, String nome, String classe, String tipo, int custo,
                     String custoTipo, String requisito, String efeito) {
        // Deriva nivel/atributo exigidos do texto do requisito (ex.: "Nivel 10", "Destreza 10").
        int nivelMin = 1;
        String atrReq = null;
        int valorAtr = 0;
        if (requisito != null) {
            String low = java.text.Normalizer.normalize(
                    requisito.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{Mn}", ""); // remove acentos p/ casar "constituicao", "forca", etc.
            java.util.regex.Matcher mn = java.util.regex.Pattern.compile("n[ií]vel\\s+(\\d+)").matcher(low);
            if (mn.find()) {
                nivelMin = Integer.parseInt(mn.group(1));
            }
            String[][] attrs = {
                {"forca", "FORCA"}, {"constituicao", "CONSTITUICAO"}, {"destreza", "DESTREZA"},
                {"agilidade", "AGILIDADE"}, {"inteligencia", "INTELIGENCIA"},
                {"sabedoria", "SABEDORIA"}, {"carisma", "CARISMA"}
            };
            for (String[] a : attrs) {
                java.util.regex.Matcher ma = java.util.regex.Pattern.compile(a[0] + "\\s+(\\d+)").matcher(low);
                if (ma.find()) {
                    atrReq = a[1];
                    valorAtr = Integer.parseInt(ma.group(1));
                    break;
                }
            }
        }
        habilidadeRepository.save(Habilidade.builder()
                .gameSystemId(sid).codigo(codigo).nome(nome).classeCodigo(classe)
                .tipo(tipo).custo(custo).custoTipo(custoTipo).requisito(requisito)
                .nivelMinimo(nivelMin).atributoRequisito(atrReq).valorAtributoRequisito(valorAtr)
                .efeito(efeito).oficial(true).build());
    }

    // ---------------- Racas (PV/PM/PE base + habilidades raciais) ----------------

    private void seedRacas() {
        raca("HUMANO", "Humano", 5, 5, 5, "A raca mais antiga e populosa; versatil.",
                "[{\"nivel\":1,\"nome\":\"Linhagem\",\"efeito\":\"Sendo a raça mais numerosa, os humanos desenvolveram uma tradição de transmitir conhecimentos, objetos e bênçãos valiosas através das gerações. O personagem pode escolher 1 item ou 1 bênção para receber como herança. A escolha deve ser aprovada pelo Mestre.\"},"
                + "{\"nivel\":20,\"nome\":\"Versatilidade Humana\",\"efeito\":\"Ao alcançar este nível, o personagem pode escolher 1 habilidade de classe ou trilha que não pertença à sua classe, desde que cumpra seus requisitos. A habilidade escolhida passa a fazer parte de seu conjunto de habilidades.\"},"
                + "{\"nivel\":40,\"nome\":\"Almejar Maestria\",\"efeito\":\"A ambição humana permite que seus indivíduos continuem superando seus próprios limites. O personagem recebe um bônus de 50% na experiência adquirida, adicionando metade do valor recebido ao total. Além disso, pode aprimorar 1 habilidade de classe à sua escolha, desde que a alteração seja aprovada pelo Mestre.\"}]");
        raca("DEMONIO", "Demonio", 4, 6, 3, "Humanos mutados por mana intensa.",
                "[{\"nivel\":1,\"nome\":\"Mutação Demoníaca\",\"efeito\":\"O personagem pode escolher 1 característica fisiológica única para seu corpo, como quatro braços, pele escamada, cauda, chifres ou asas. A escolha deve ser aprovada pelo Mestre.\"},"
                + "{\"nivel\":20,\"nome\":\"Pactos Demoníacos\",\"efeito\":\"O personagem pode estabelecer contratos mágicos com criaturas conscientes, desde que ambas concordem com seus termos. O pacto é gravado na alma de todos os envolvidos, e sua quebra resulta nas consequências determinadas pelo contrato ou pelo Mestre.\"},"
                + "{\"nivel\":40,\"nome\":\"Pacto Restritivo\",\"efeito\":\"O personagem pode realizar contratos consigo mesmo para modificar aspectos de sua ficha, trocando um sacrifício proporcional por um benefício equivalente. Requer Ação Completa e, após realizado, o pacto é irreversível.\"}]");
        raca("GIGANTE", "Gigante", 9, 2, 3, "Enormes e resistentes.",
                "[{\"nivel\":1,\"nome\":\"Gigantismo\",\"efeito\":\"A constituição física dos Gigantes lhes concede uma resistência e vigor superiores aos das demais raças. Recebe 1 PV e 1 PE para cada ponto de Constituição. Além disso, seu tamanho grande dobra sua capacidade de inventário e seu deslocamento natural. Devido ao seu porte, não pode utilizar equipamentos comuns nem entrar em edifícios convencionais.\"},"
                + "{\"nivel\":20,\"nome\":\"Crescer Ainda Mais\",\"efeito\":\"O Gigante pode gastar uma Ação de Movimento e 5 PE para ampliar temporariamente sua força física e seu tamanho, recebendo 2 em todos os atributos físicos e 2 de Deslocamento para cada 15 Níveis do Personagem, por 1 turno.\"},"
                + "{\"nivel\":40,\"nome\":\"Casca Grossa\",\"efeito\":\"O corpo do Gigante é extremamente resistente. Recebe 20 de Armadura Verdadeira e torna-se imune aos estados Sangramento e Envenenamento.\"}]");
        raca("GOBLIN", "Goblin", 3, 5, 6, "Pequenos, inteligentes e astutos.",
                "[{\"nivel\":1,\"nome\":\"Desenvolvimento Rápido\",\"efeito\":\"A mente ágil e a capacidade de adaptação dos Goblins permitem que aprendam mais rapidamente. O personagem recebe o dobro da experiência adquirida.\"},"
                + "{\"nivel\":20,\"nome\":\"Instintos de Sobrevivência\",\"efeito\":\"Os instintos naturais do Goblin o tornam extremamente atento aos perigos. Recebe 2 Vantagens em testes de Sobrevivência e Percepção para detectar armadilhas, criaturas furtivas e outras fontes de perigo.\"},"
                + "{\"nivel\":40,\"nome\":\"Mimetismo\",\"efeito\":\"O Goblin pode copiar uma técnica que tenha observado diretamente, podendo utilizá-la a partir do turno seguinte. O uso da técnica copiada exige o dobro dos recursos normalmente necessários para utilizá-la.\"}]");
        raca("TIEFLING", "Tiefling", 4, 7, 4, "Demonios da mana; grandes pesquisadores.",
                "[{\"nivel\":1,\"nome\":\"Aptidão Mágica\",\"efeito\":\"A natureza mágica dos Tieflings lhes concede 5 de Armadura Mágica. Além disso, pode conjurar 1 magia por cena sem gastar Mana.\"},"
                + "{\"nivel\":20,\"nome\":\"Magia Inerente\",\"efeito\":\"Após alcançar um alto nível de experiência, os Tieflings desenvolvem a habilidade de moldar sua mana de maneira única, criando magias próprias que, muitas vezes, se manifestam de forma inesperada e divergente dos feitiços tradicionais. O personagem pode criar uma magia única sem a necessidade de um slot de magia. O jogador pode desenvolver as funções e capacidades da magia criada, com a aprovação do Mestre.\"},"
                + "{\"nivel\":40,\"nome\":\"Ampliar Feitiçaria\",\"efeito\":\"Utilizado por estudiosos da magia, geralmente anciões de sua raça, esse poder permite expandir o fluxo de mana de acordo com a vontade do usuário, possibilitando a realização de feitiçarias poderosas e, em alguns casos, até alterando seu próprio corpo. O personagem pode usar sua Energia como Mana passivamente sempre que desejar. O personagem pode exceder o limite de gasto de um feitiço(inteligência), aplicando o custo várias vezes e aumentando seus efeitos com base na metade da inteligência conforme o gasto adicional.\"}]");
        raca("MEIO_FERA", "Meio-Fera", 4, 4, 6, "Hibridos com aspectos animais.",
                "[{\"nivel\":1,\"nome\":\"Herança Bestial\",\"efeito\":\"Sem dúvidas, esta é a raça mais diversificada existente, sendo descendente dos animais primitivos que habitavam o mundo. Cada indivíduo carrega em si características únicas, originadas dessas raízes animais, que conferem habilidades e traços especiais. O personagem possui aspectos derivados de seu animal, que podem melhorar os sentidos perceptivos ou conferir capacidades de sobrevivência especiais. Os aspectos específicos e os bônus serão escolhidos pelo jogador e pelo Mestre, de acordo com a raça escolhida.\"},"
                + "{\"nivel\":20,\"nome\":\"Forma Bestial Completa\",\"efeito\":\"Ao permitir que seus instintos primitivos o guiem e ao esvaziar sua mente para compreender seu animal interior, a raça dos Meio Fera é capaz de amplificar seu lado bestial, tornando-se mais forte em seus aspectos animalescos. O personagem sofre uma transformação física de acordo com o seu animal interior. Atributos e habilidades dessa transformação serão decididos pelo Mestre, com base na raça e no animal escolhido.\"},"
                + "{\"nivel\":40,\"nome\":\"Renascimento da Fera\",\"efeito\":\"Há muito tempo, essa raça é formada por descendentes humanoides com diversos aspectos animalescos, coexistindo dois lados em um único corpo: o racional e o bestial. No entanto, poucos transcendem essa dualidade e renascem como um novo ser. O personagem evolui, adquirindo uma nova aparência à escolha do jogador, refletindo a superação de sua dualidade. Suas melhorias serão determinadas em conjunto com o Mestre no momento da criação do seu novo 'eu', levando em consideração o crescimento e a transformação de sua essência.\"}]");
        raca("ELFO", "Elfo", 5, 5, 4, "Antigos, ligados a natureza e a mana.",
                "[{\"nivel\":1,\"nome\":\"Aspectos da Selva\",\"efeito\":\"Descendentes de uma raça antiga que possuía um forte vínculo com a vida, seguiram seus ideais e se desenvolveram em torno de florestas e áreas com muita vegetação, adquirindo assim a habilidade de aproveitar melhor esses ambientes. O personagem recupera 5 de sua mana por turno, quando está em um cenário de floresta. Qualquer efeito de cura vinculada à natureza terá seu valor dobrado.\"},"
                + "{\"nivel\":20,\"nome\":\"Visão Amplificada\",\"efeito\":\"Por viverem entre a vegetação densa das grandes florestas, desenvolveram uma visão aguçada que lhes dá plena percepção de tudo ao seu redor. Essa habilidade lhes permite detectar ameaças e observar o ambiente com grande clareza. O personagem pode enxergar pessoas à distância, mesmo que estejam atrás de coberturas. A cobertura é reduzida de total para parcial e de parcial para nenhuma, permitindo que o personagem veja seus alvos com mais facilidade.\"},"
                + "{\"nivel\":40,\"nome\":\"Vincular-se com a Mana\",\"efeito\":\"Com o passar dos anos, surgiram elfos distintos, que se destacaram entre sua raça por suas aparências e habilidades únicas, desenvolvidas a partir de suas origens variadas. Os jogadores deverão escolher entre três variantes desta linhagem: Elfo-Superior, Drow ou Lunariano, cada um recebendo benefícios exclusivos. Lunariano: Adiciona metade da sabedoria e inteligência à PM sob a luz da Lua. Sob a lua os efeitos de cura serão dobrados. Elfo-Superior: Bônus de metade de sua constituição em HP e PE somando a constituição com o HP e PE atuais. Sob a luz do Sol, qualquer efeito de cura em si será dobrado. Drow: 30 de armadura. +10 em qualquer das Perícias quando isento de luz originada por Corpos Celestes.\"}]");
        raca("ONI", "Oni", 7, 3, 5, "Guerreiros fortes, abencoados por cristais.",
                "[{\"nivel\":1,\"nome\":\"Aptidão Física\",\"efeito\":\"Demônios que seguiram o caminho de beneficiar seus próprios corpos, aprimorando suas capacidades físicas para sobreviver sem depender da magia. Com um foco no fortalecimento da resistência física, esses demônios alcançam grande poder por meio de seu corpo, sem precisar da magia para atingir seus objetivos. O personagem recebe 20 de Armadura Física, aumentando sua resistência a ataques físicos. 3 vezes ao dia, o personagem pode reduzir o custo de Energia de uma habilidade de Classe para metade.\"},"
                + "{\"nivel\":20,\"nome\":\"Máscara Demoníaca\",\"efeito\":\"Com o passar das gerações, a raça dos Onis foi abençoada com cristais de forte concentração de mana e criou monumentos no centro de suas vilas para guardar esses artefatos. Viver sob a influência desses cristais fez com que essa raça desenvolvesse a habilidade de manifestar a força dos cristais em uma máscara, que se adapta conforme o cristal de origem. O personagem ganha um item único que confere os seguintes benefícios: Aumento de 2 Atributos de sua escolha. Aumento de +10 Perícia de sua escolha. Uma habilidade única, selecionada de acordo com o cristal em questão. O item único tem a capacidade de evoluir ao longo do tempo. O usuário pode usar a máscara 3 turnos por dia, gerando 1 de atributo para cada 5 níveis do seu personagem com base no seu cristal.\"},"
                + "{\"nivel\":40,\"nome\":\"Além do Limite\",\"efeito\":\"Para sobreviver a isso, encontraram uma forma de enfrentar oponentes mais fortes, sacrificando parte de sua vitalidade para exceder a capacidade de seus corpos e superar seus limites em combate. O usuário pode sacrificar HP equivalente ao valor que deseja adicionar em seu próximo teste físico. O personagem pode gastar sua vida sendo limitada pela metade de sua constituição, transformando em acerto em teste físico e dano.. Reduz pela metade o custo de PE em habilidades físicas\"}]");
        raca("VAMPIRO", "Vampiro", 5, 6, 5, "Sobreviventes que consomem sangue.",
                "[{\"nivel\":1,\"nome\":\"Consumir Sangue\",\"efeito\":\"Originários de uma raça que, para sobreviver, precisaram recorrer ao canibalismo, praticando a auto dilaceração e o reaproveitamento de nutrientes, o que lhes permitiu sobreviver por muitos anos em um ambiente cruel. Essa habilidade é uma remanescente dessa antiga capacidade. O usuário pode beber o sangue de seres vivos para recuperar HP com base no dobro da constituição. A habilidade pode ser utilizada uma vez por criatura.\"},"
                + "{\"nivel\":20,\"nome\":\"Afinidade Noturna\",\"efeito\":\"Suas origens vêm das profundezas escuras das cavernas onde ficaram presos. Para sobreviver às criaturas que lá habitavam, desenvolveram o dom de lutar na escuridão, aprimorando seus sentidos e aumentando suas aptidões para a sobrevivência. Ao cair da noite ou em locais sem luz solar difusa, o personagem recebe um bônus de +4 em Perícias de Força e Agilidade. Esse bônus aumenta em +4 a cada 10 níveis do personagem.\"},"
                + "{\"nivel\":40,\"nome\":\"Nutrição pelo Sacrifício\",\"efeito\":\"A função dos mais fortes sempre foi garantir a sobrevivência dos mais fracos, assegurando a continuidade da vida de sua raça. Dessa necessidade surgiu a capacidade de nutrir suas forças por meio do hábito canibal. O usuário pode regenerar HP durante uma batalha. Para isso, deve permanecer imóvel por 1 turno, concentrando-se. Após esse turno, o usuário pode gastar limitado pela metade da constituição em PE, e a outra definição de dado será limitado pela metade da sabedoria do personagem, gastando seu PM por essa limitação, e assim gerando um dado para ganhar vida\"}]");
        raca("ANAO", "Anao", 6, 4, 5, "Baixos, robustos e sabios artifices.",
                "[{\"nivel\":1,\"nome\":\"Conhecimento Antigo\",\"efeito\":\"Uma raça nascida em terras inóspitas, onde a sobrevivência era desafiadora. Para superar tais adversidades, desenvolveram uma capacidade única de registrar e transmitir seu aprendizado através de escritos ancestrais. Com o tempo, cada elemento descoberto foi registrado, formando um acervo de saberes de um passado perdido. O personagem pode selecionar um tema de conhecimento antigo. Tudo relacionado a esse tema, datado de antes do Período da Luz, será de seu total entendimento.\"},"
                + "{\"nivel\":20,\"nome\":\"Resistências Subterrâneas\",\"efeito\":\"Originários de uma terra onde as planícies e locais com alta passagem de vento eram evitados, essa raça começou a cavar cavernas para garantir sua sobrevivência. Com o tempo, seus corpos se adaptaram para manter o calor interno e aumentar a eficácia no trabalho subterrâneo. O usuário ganha resistência a danos climáticos, sendo imune a climas extremos de frio e calor, além de ganhar 20 de armadura a ataques deste tipo, o personagem possui uma habilidade natural para não se perder em cavernas, sempre sabendo onde estão as passagens naturais e os caminhos internos.\"},"
                + "{\"nivel\":40,\"nome\":\"Mestre Artífice\",\"efeito\":\"Vivendo em ambientes limitados onde todos os recursos eram preciosos, essa raça aprendeu a aproveitar ao máximo o que estava ao seu redor. Desenvolveram a habilidade de reconhecer e utilizar qualquer tipo de material de maneira eficiente, criando itens com qualidade superior. O usuário pode reconhecer materiais em sua forma mais pura ou objetos que já foram preparados, permitindo-lhe criar itens de qualidade superior. Além disso, pode incluir elementos improváveis ou teoricamente incompatíveis na criação de seus itens, combinando materiais de forma única e inovadora.\"}]");
        raca("ORC", "Orc", 8, 3, 7, "Guerreiros brutais da sobrevivencia do mais forte.",
                "[{\"nivel\":1,\"nome\":\"Marca de Guerra\",\"efeito\":\"Os Orcs nasceram para a guerra, e foi em meio a batalhas constantes que suas tribos prosperaram. Cada tribo carrega uma marca única que simboliza sua essência e estratégia de combate, escolhendo um alvo e um elemento que define seu estilo de luta. O personagem escolhe uma Marca de Guerra, composta por: Um tipo de alvo específico, contra o qual recebe um bônus. Um elemento da natureza, que simboliza o benefício recebido.\"},"
                + "{\"nivel\":20,\"nome\":\"Insanidade de Batalha\",\"efeito\":\"Para os Orcs, a batalha é uma constante, e suportar danos é parte de sua essência. No entanto, sua natureza os leva a canalizar o sofrimento em pura fúria, liberando uma força devastadora ao atingir seu limite. O personagem acumula um contador com o número de golpes recebidos durante o combate. A qualquer momento, pode liberar esse remorso, ganhando +2 em testes físicos para cada golpe acumulado. Após utilizar a habilidade, o contador é zerado.\"},"
                + "{\"nivel\":40,\"nome\":\"Brutalidade\",\"efeito\":\"Para os Orcs, atacar até vencer é um princípio absoluto. A integridade física é secundária diante da necessidade de eliminar seus inimigos. Essa determinação brutal os permite sacrificar sua vitalidade para garantir a vitória, golpe após golpe. O personagem pode realizar múltiplos ataques consecutivos, sacrificando parte de seu HP a cada golpe adicional Segundo ataque: Custa 20 do HP atual. Terceiro ataque: Custa 40 do HP atual. Quarto ataque: Custa 60 do HP atual. Quinto ataque: Custa 80 do HP atual. Sexto ataque: Custa 100 o HP atual. Após atingir a sequência máxima, o personagem pode reiniciar a tabela, mas os custos passam a consumir o HP máximo. Nesse estado, os ataques podem continuar indefinidamente sem custo adicional, até que o HP restante atinja 0. OvalordeHPconsumidoé aplicado ao final do turno. Não é possível reduzir o custo de HP para menos de 1. Caso o HP atinja exatamente 0,, a habilidade será perdida.\"}]");
        raca("DRACONATO", "Draconato", 9, 2, 5, "Herdeiros dos dragoes.",
                "[{\"nivel\":1,\"nome\":\"Descendência Dracônica\",\"efeito\":\"A origem dos Draconatos é envolta em mistério, mas é certo que possuem uma ligação direta com os dragões, possivelmente como descendentes dessa antiga e poderosa raça. Ao longo de suas existências, os Draconatos herdaram várias habilidades e capacidades que refletem a força, sabedoria e poder dos dragões. O personagem pode girar uma combinação de Deuses para determinar os ancestrais dragônicos que o influenciam e, com isso, obter buffs específicos, variando conforme a escolha dos dragões ancestrais. A combinação de Deuses definirá a linhagem e as habilidades que o personagem obterá.\"},"
                + "{\"nivel\":20,\"nome\":\"Bafo do Dragão\",\"efeito\":\"Com o desenvolvimento de sua genética, os Draconatos herdaram uma das características mais marcantes de seus ancestrais dragões: os sopros elementais, capazes de inspirar terror e devastar seus oponentes. O personagem adquire um ataque especial, que pode ser utilizado como um ataque normal sem custos adicionais. Os efeitos, danos e capacidades do sopro serão definidos com base na sua Descendência Dracônica.\"},"
                + "{\"nivel\":40,\"nome\":\"Asas de Dragão\",\"efeito\":\"Ao alcançar um alto nível de maturidade, os Draconatos se tornam capazes de manifestar asas semelhantes às de seus ancestrais dragões. Essas asas são tão fortes e resistentes quanto os próprios Draconatos, com a vantagem de serem retráteis, permitindo maior versatilidade no combate e na mobilidade. O personagem pode invocar asas dracônicas retráteis a qualquer momento. Quando ativadas, elas concedem os seguintes benefícios: Aumento de +4 em Deslocamento, e adiciona a capacidade de voar. 20 de armadura, devido à resistência natural das asas.\"}]");
        raca("CELESTIAL", "Celestial", 5, 6, 4, "Seres raros de origem divina.",
                "[{\"nivel\":1,\"nome\":\"Benção Divina\",\"efeito\":\"Por conta da sua natureza possuem grande conexão com os Deuses, e então a mana que escolher te abençoar vai correr naturalmente em seu corpo como se fosse sua própria, assim os Celestiais são capazes de aprender a utilizar sua mana de várias formas diferentes. Quando for criado seu personagem este receberá 3 bênçãos iniciais ao invés de apenas 1, além disso caso possuam uma benção que não tenha gostado, pode a rejeitar.\"},"
                + "{\"nivel\":20,\"nome\":\"Preces ao Divino\",\"efeito\":\"Durante o período que os Deuses estavam criando sua morada, eles usaram Celestiais como seus representantes, com o principal objetivo de neutralizar os monstros que ainda atormentavam os mortais, dessa forma surgiu um grupo chamado de Arcanjos, que desenvolviam um forte vínculo com a sua Divindade e eram capazes de aumentar essa conexão em batalha, assim potencializando seus poderes. O usuário pode gastar 1 Turno para recitar uma oração a sua Divindade, de acordo com suas palavras a influência de Deus será diferente, podendo aumentar drasticamente uma bênção que este possua ou apenas lhe dando uma bênção temporária, o que o player recebe é determinado pelo Mestre e pelas suas palavras..\"},"
                + "{\"nivel\":40,\"nome\":\"Verdadeiras Asas\",\"efeito\":\"Vistas apenas nos mais poderosos Celestiais, historicamente o líder dos Arcanjos sempre possuiu essa característica, é uma mudança que ocorre nas asas que varia de acordo com o portador em questão, duplas, gigantes ou mágicas, são muitas possibilidades. O personagem então receberá uma drástica mudança em suas asas, o que acontece visualmente será determinado pelo Player e pelo Mestre, de acordo com o personagem, suas vantagens são: Ganha +6 em seu deslocamento e +4 em agilidade. Suas asas passam a possuir mana própria, 2 de mana a cada 10 pontos de mana máxima, através dessa mana você pode usar Bênçãos com um passo a menos. CLASSES Sua classe é muito além de apenas uma profissão, ela é seu estilo de vida e define como você resolve seus conflitos e alcançar seus objetivos. A classe é a parte mais importante do seu personagem, define quais suas funções e capacidades dentro desse mundo. Asus conta com dezesseis classes, cada uma com no mínimo duas trilhas. Bônus de Classe Cada uma possui seu próprio multiplicador para os valores definidos pela sua raça, assim definindo quais Pontos você terá mais, sendo estes PV, PM e PE. Trilhas São um aperfeiçoamento de sua classe, chegando em determinado nível você deverá escolher um caminho a trilhar, se profissionalizando ainda mais naquele aspecto de sua classe. Perícias São as responsáveis por todos os testes no rpg, ao possuir uma perícia treinada você poderá sobrepor a limitação de seu atributo chave e assim utilizar o valor de sua perícia completa. CLASSES Classe PV PM PE Trilha Perícias Cavaleiro 6 3 6 Escudeiro e Comandante Vigor e Bloqueio, mais 2 Assassino 5 4 6 Ninja e Sicário Esquiva e Arremesso, mais 4 Bárbaro 7 1 7 Berserk e Imortal Combate e Atletismo, mais 1 Clérigo 3 7 5 Paladino e Santo Fé e Virtude, mais 2 Druida 6 6 3 Arauto da Natureza e Domador de Feras Sobrevivência e Adestrar, mais 3 Monge 5 5 5 Sábio e Punhos de Aço Percepção e Virtude, mais 4 Arqueiro 5 3 7 Caçador e Mercenário Pontaria e Sobrevivência, mais 2 Mago 3 9 3 Arquimago e Mago Rúnico Magia e Conhecimento, mais 3 Ladrão 3 5 7 Sombra e Pirata Furtividade e Iniciativa, mais 4 Curandeiro 4 7 4 Sacerdote e Mestre das Toxinas Medicina e Fé, mais 2 Espadachim 4 4 7 Duelista e Samurai Combate e Iniciativa, mais 3 Bruxo 5 7 3 Necromante e Místico Magia e Sobrevivência, mais 4 Alquimista 4 6 5 Químico Arcano e Inventor Alquimia e Conhecimento, mais 4 Lanceiro 7 2 6 General dos Céus e Valquíria Combate e Esquiva, mais 3 Lutador 6 2 7 Campeão e Artista Marcial Combate e Bloqueio, mais 1 Viajante 4 6 6 Bardo, Cigano, Cartomante e Pintor Diplomacia e Enganação, mais 4\"}]");
    }

    // ---------------- Pericias (26) com atributo governante ----------------

    private void seedPericias() {
        pericia("VIGOR", "Vigor", Atributo.CONSTITUICAO,
                "Representa a resistência física e capacidade de suportar esforços prolongados, doenças, "
                + "venenos e privações. Usado em corridas longas, trabalho pesado sem parar, resistir a "
                + "ambientes hostis, suportar dor extrema e não desmaiar em situações críticas.",
                "Resistir a venenos e doenças|Suportar esforço prolongado|Resistir a ambientes extremos|Não desmaiar após dano massivo");
        pericia("ATLETISMO", "Atletismo", Atributo.FORCA,
                "Habilidade física bruta aplicada em movimento — correr, saltar, escalar, nadar e empurrar "
                + "objetos ou criaturas. Fundamental para Guerreiros e Bárbaros que precisam de mobilidade em combate.",
                "Escalar superfícies difíceis|Nadar em correntes fortes|Saltar obstáculos|Empurrar ou derrubar criaturas");
        pericia("ESQUIVA", "Esquiva", Atributo.AGILIDADE,
                "Capacidade de desviar de ataques físicos, projéteis, armadilhas e áreas de efeito. Quanto maior "
                + "a Esquiva, mais difícil é acertar o personagem em combate ativo. Fundamental para classes leves.",
                "Desviar de ataques físicos|Escapar de explosões e áreas|Evitar armadilhas ativadas|Mover em combate denso");
        pericia("FURTIVIDADE", "Furtividade", Atributo.AGILIDADE,
                "Arte de se mover sem ser percebido, ocultar-se nas sombras e realizar ações sem chamar atenção. "
                + "Essencial para Assassinos, Ladrões e Sombras. Oposto direto de Percepção dos inimigos.",
                "Mover-se sem ser ouvido|Esconder-se em sombras|Seguir alguém sem ser visto|Preparar emboscada oculta");
        pericia("ARREMESSO", "Arremesso", Atributo.DESTREZA,
                "Precisão ao lançar objetos, armas e projéteis com a mão. Desde facas e estrelas ninja até pedras "
                + "e granadas alquímicas. A Destreza define tanto a precisão quanto o alcance efetivo do arremesso.",
                "Lançar armas arremessáveis|Atirar objetos improvisados|Alcançar alvos específicos|Lançar poções e granadas");
        pericia("PONTARIA", "Pontaria", Atributo.DESTREZA,
                "Precisão com armas de alcance: arcos, bestas, zarabatanas e armas de fogo. Determina a acurácia "
                + "de ataques a distância e capacidade de atingir pontos específicos em alvos móveis.",
                "Ataques com arco e besta|Acertar partes específicas|Atirar em movimento|Distância efetiva máxima");
        pericia("FE", "Fé", Atributo.SABEDORIA,
                "Conexão espiritual com divindades, forças sagradas e o plano divino. Determina a potência de "
                + "magias divinas, capacidade de expulsar mortos-vivos e resistência a influências malignas. "
                + "Clérigos e Paladinos dependem dela.",
                "Conjuração de magia divina|Expulsar mortos-vivos|Resistir a possessão|Rituais e orações sagradas");
        pericia("PERCEPCAO", "Percepção", Atributo.SABEDORIA,
                "Capacidade de notar detalhes sutis no ambiente, detectar criaturas ocultas, encontrar armadilhas "
                + "e ler situações ambíguas. O oposto direto de Furtividade de inimigos. Um personagem perceptivo "
                + "nunca é pego de surpresa.",
                "Detectar criaturas ocultas|Encontrar armadilhas|Notar detalhes sutis|Nunca ser surpreendido");
        pericia("CRIME", "Crime", Atributo.DESTREZA,
                "Arte de realizar atividades ilícitas: arrombamento de fechaduras, pickpocketing, sabotagem e "
                + "falsificação. Requer tanto conhecimento técnico quanto mão firme. Indispensável para Ladrões e Piratas.",
                "Arrombar fechaduras|Roubar sem ser notado|Falsificar documentos|Sabotar mecanismos");
        pericia("MAGIA", "Feitiços", Atributo.INTELIGENCIA,
                "Domínio técnico sobre a teoria e prática da magia arcana. Usada para conjurar e resistir a "
                + "feitiços. O valor de Inteligência define o limite de feitiços diferentes que o personagem pode "
                + "ter (½ INT). Fundamental para Magos e Bruxos.",
                "Conjuração de magia arcana|Limite de feitiços = ½ INT|Identificar e reagir a magias|Pesquisa mágica avançada");
        pericia("ALQUIMIA", "Alquimia", Atributo.INTELIGENCIA,
                "Conhecimento sobre transformação de substâncias, criação de poções, venenos, ácidos e materiais "
                + "especiais. O Alquimista usa essa perícia para criar itens que outros comprariam por fortunas em "
                + "lojas especializadas.",
                "Criar poções e elixires|Sintetizar venenos|Identificar substâncias|Experimentos arcanos");
        pericia("DIPLOMACIA", "Diplomacia", Atributo.CARISMA,
                "Arte de negociar, persuadir e chegar a acordos mutuamente satisfatórios. Diferente de Enganação "
                + "(que engana), Diplomacia é honesta. NPCs persuadidos com sucesso se tornam aliados mais "
                + "confiáveis a longo prazo.",
                "Negociar acordos e tréguas|Persuadir líderes e nobres|Acalmar situações tensas|Conseguir favores legítimos");
        pericia("ENGANACAO", "Enganação", Atributo.CARISMA,
                "Capacidade de mentir com convicção, disfarçar-se e manipular pessoas para acreditar em "
                + "falsidades. Perigosa se descoberta, mas incrivelmente poderosa quando bem executada. Oposto de "
                + "Percepção dos alvos.",
                "Mentir sob pressão|Criar identidades falsas|Disfarçar-se de outro|Manipular emoções alheias");
        pericia("SOBREVIVENCIA", "Sobrevivência", Atributo.SABEDORIA,
                "Habilidade de sobreviver na natureza selvagem: rastrear presas, encontrar água e alimento, prever "
                + "o clima, construir abrigos e navegar sem instrumentos. Rangers e Druidas são os mestres dessa perícia.",
                "Rastrear criaturas e pessoas|Encontrar alimento na natureza|Navegar sem bússola|Prever condições climáticas");
        pericia("BLOQUEIO", "Bloqueio", Atributo.CONSTITUICAO,
                "Capacidade de bloquear ataques físicos com escudo, arma ou mesmo com o próprio corpo. Um Bloqueio "
                + "bem-sucedido pode negar completamente o dano de um ataque. Fundamental para tanques e defensores.",
                "Bloquear ataques físicos|Proteger aliados adjacentes|Resistir a golpes poderosos|Bloquear projéteis");
        pericia("CONHECIMENTO", "Conhecimento", Atributo.INTELIGENCIA,
                "Conjunto de saberes acumulados sobre história, criaturas, magia, política, religião e ciências do "
                + "mundo. Um personagem com alto Conhecimento sabe exatamente que criatura enfrenta e quais são suas fraquezas.",
                "Identificar criaturas e fraquezas|Conhecer história e lendas|Entender artefatos antigos|Mapear territórios e rotas");
        pericia("ACROBACIA", "Acrobacia", Atributo.AGILIDADE,
                "Agilidade em movimentos complexos: cambalhota, equilíbrio em superfícies instáveis, quedas "
                + "controladas e malabarismo. Fundamental para Ninjas, Artistas Marciais e qualquer classe que "
                + "dependa de mobilidade extrema.",
                "Equilíbrio em superfícies difíceis|Quedas controladas sem dano|Movimentos acrobáticos em combate|Escapar de espaços apertados");
        pericia("COMBATE", "Combate", Atributo.FORCA,
                "Maestria técnica no combate corpo a corpo. Não é apenas força bruta — é conhecimento de "
                + "posicionamento, timing de ataque, identificação de aberturas e domínio de técnicas marciais. "
                + "Fundamental para todos os guerreiros.",
                "Bônus em ataques corpo a corpo|Reconhecer técnicas inimigas|Manobras táticas especiais|Desarmar e derrubar");
        pericia("OFICIO", "Ofício", Atributo.INTELIGENCIA,
                "Habilidade artesanal para criar e reparar objetos físicos: ferraria, carpintaria, costura, "
                + "joalheria. Alquimistas e Inventores dependem muito dessa perícia para criar seus dispositivos especiais.",
                "Criar armas e armaduras|Reparar equipamentos danificados|Construir dispositivos especiais|Avaliar qualidade de itens");
        pericia("INICIATIVA", "Iniciativa", Atributo.AGILIDADE,
                "Velocidade de reação e capacidade de agir primeiro em situações de conflito. Um personagem com "
                + "alta Iniciativa nunca é surpreendido e quase sempre age antes dos inimigos. Crítico para classes "
                + "de assassinato.",
                "Agir primeiro no combate|Nunca ser surpreendido|Reações instantâneas|Interromper ações inimigas");
        pericia("ADESTRAR", "Adestrar", Atributo.CARISMA,
                "Capacidade de criar vínculos com animais, treinar criaturas e ganhar a confiança de bestas "
                + "selvagens. Druidas e Rangers usam essa perícia para manter seus companheiros animais e para "
                + "passar por territórios hostis.",
                "Treinar animais domésticos|Acalmar criaturas selvagens|Montar criaturas exóticas|Manter companheiro animal");
        pericia("MEDICINA", "Medicina", Atributo.INTELIGENCIA,
                "Conhecimento médico para estabilizar feridos, tratar envenenamentos, realizar cirurgias e "
                + "diagnosticar doenças. Diferente de Cura Divina, Medicina é ciência pura — sem magia, mas "
                + "igualmente vital.",
                "Estabilizar personagens inconscientes|Tratar envenenamentos|Remover estados negativos|Diagnóstico de doenças");
        pericia("JOGATINA", "Jogatina", Atributo.DESTREZA,
                "Arte dos jogos de azar, trapaça em apostas, manipulação de dados e cartas marcadas. Ciganos e "
                + "Piratas são mestres nessa perícia. Pode ser usada para ganhar dinheiro ou para enganar "
                + "informações de NPCs.",
                "Trapacear em jogos|Ganhar dinheiro em tavernas|Manipular resultados de sorte|Ler padrões de apostas");
        pericia("APARENCIA", "Aparência", Atributo.CARISMA,
                "Capacidade de usar a aparência física, vestimenta e linguagem corporal para causar impressões "
                + "específicas. Um personagem com alta Aparência consegue acesso a locais restritos apenas por "
                + "parecer pertencer ali.",
                "Primeira impressão positiva|Convencer porteiros e guardas|Ser recebido em alta sociedade|Disfarçar status social");
        pericia("VIRTUDE", "Virtude", Atributo.SABEDORIA,
                "Firmeza moral e resistência a tentações, corrupções e influências malignas. Fundamental para "
                + "Paladinos e Santos. Um personagem com alta Virtude nunca pode ser corrompido por artefatos "
                + "malignos ou promessas demoníacas.",
                "Resistir à corrupção mágica|Manter código moral sob pressão|Interagir com artefatos sagrados|Detectar maldade oculta");
        pericia("ARTES", "Artes", Atributo.SABEDORIA,
                "Talento criativo e expressivo: música, pintura, escultura, dança e teatro. Bardos usam Artes como "
                + "arma, Pintores como magia e Ciganos para entretenimento. Fora do combate, é a perícia mais "
                + "versátil para interações sociais.",
                "Performance musical e teatral|Pintura e escultura mágica|Disfarce via atuação|Ganhar aprovação de multidões");
        pericia("SANIDADE", "Sanidade", Atributo.SABEDORIA,
                "Equilíbrio mental e resistência psicológica diante de horrores, traumas e manipulações. Um "
                + "personagem com alta Sanidade mantém a lucidez mesmo diante do caos e da corrupção. Fundamental "
                + "para evitar estados de Confusão e Insanidade.",
                "Resistir a trauma e horror|Manter lucidez sob pressão|Resistir a manipulação mental|Recuperar-se de estados de confusão");
        pericia("ENGENHARIA", "Engenharia", Atributo.INTELIGENCIA,
                "Conhecimento técnico para construir, reparar e operar dispositivos mecânicos e tecnológicos. "
                + "Inventores e Alquimistas dependem dessa perícia para criar e aprimorar seus gadgets, armadilhas "
                + "e máquinas de guerra.",
                "Construir dispositivos mecânicos|Desarmar mecanismos e armadilhas|Criar versões aprimoradas de dispositivos|Operar engenhocas tecnológicas");
    }

    /** Reaplica as perícias (descrição, exemplos) em banco já existente, sem trocar ids. */
    void refreshPericias() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            seedPericias();
        });
    }

    // ---------------- Classes (16) + trilhas, com bonus reais ----------------

    private void seedClasses() {
        classe("CAVALEIRO", "Cavaleiro", null,
                bonus("{\"forca\":2,\"constituicao\":2,\"carisma\":1}", "{\"vigor\":2,\"atletismo\":1,\"bloqueio\":2}", 1),
                "Lealdade Inabalável. A lealdade do usuário é inabalável. Sempre que realizar um teste de "
                        + "resistência contra efeitos que controlem sua mente ou o obriguem a agir contra sua lealdade, "
                        + "recebe um bônus igual ao seu atributo base.");
        classe("ESCUDEIRO", "Escudeiro", "CAVALEIRO",
                bonus("{\"constituicao\":8,\"forca\":2}", "{\"bloqueio\":5,\"vigor\":5}", 1),
                "Escudo Protetor. Enquanto empunhar um escudo, o usuário recebe Vantagem em testes para bloquear "
                        + "ataques direcionados a aliados em alcance curto. Caso o bloqueio seja bem-sucedido, recebe 1 "
                        + "Reação adicional até o fim da rodada, que pode ser utilizada apenas para realizar ações de "
                        + "Bloqueio.");
        classe("COMANDANTE", "Comandante", "CAVALEIRO",
                bonus("{\"constituicao\":4,\"forca\":3,\"carisma\":2,\"sabedoria\":1}", "{\"vigor\":3,\"atletismo\":3,\"diplomacia\":2,\"percepcao\":2}", 1),
                "Presença Inspiradora. Uma vez por cena, o usuário pode gastar uma Ação Secundária para inspirar "
                        + "um aliado. Até o final da cena, o próximo teste realizado por esse aliado recebe um bônus igual "
                        + "à metade do Carisma do usuário.");

        classe("ASSASSINO", "Assassino", null,
                bonus("{\"destreza\":2,\"agilidade\":2,\"forca\":1}", "{\"combate\":2,\"acrobacia\":1,\"furtividade\":1,\"arremesso\":1}", 2),
                "Golpe Silencioso. Sempre que atacar uma criatura que não tenha percebido sua presença ou que "
                        + "esteja Incapacitada, o primeiro ataque realizado contra ela será considerado um Acerto Crítico.");
        classe("NINJA", "Ninja", "ASSASSINO",
                bonus("{\"agilidade\":5,\"destreza\":3,\"forca\":2}", "{\"acrobacia\":4,\"furtividade\":3,\"arremesso\":3}", 3),
                "Sombra Ágil. Enquanto estiver em áreas de escuridão ou sombras e fora de combate, o usuário "
                        + "recebe um bônus em Acrobacia igual ao seu valor de Agilidade e ignora as penalidades de terreno "
                        + "difícil. Este efeito não funciona enquanto estiver utilizando Armadura Pesada.");
        classe("SICARIO", "Sicario", "ASSASSINO",
                bonus("{\"forca\":4,\"destreza\":4,\"agilidade\":2}", "{\"combate\":3,\"acrobacia\":3,\"iniciativa\":2,\"esquiva\":2}", 2),
                "Hemorragia. Sempre que acertar um ataque com uma arma leve de perfuração, o usuário aplica "
                        + "automaticamente um acúmulo de Sangramento ao alvo.");

        classe("BARBARO", "Barbaro", null,
                bonus("{\"forca\":3,\"constituicao\":2}", "{\"atletismo\":3,\"vigor\":2}", 2),
                "Bruto. Quando seus PV forem reduzidos para metade ou menos do máximo, o usuário recebe um bônus "
                        + "em Armadura Física e Dano Extra igual à metade de sua Força. O efeito permanece enquanto "
                        + "estiver nessa condição.");
        classe("BERSERKER", "Berserker", "BARBARO",
                bonus("{\"forca\":8,\"constituicao\":2}", "{\"atletismo\":8,\"vigor\":2}", 2),
                "Fúria Incontrolável. Enquanto estiver sob os efeitos da habilidade Estado de Fúria, o usuário "
                        + "recebe +2 em testes físicos a cada 10 pontos de Atletismo.");
        classe("IMORTAL", "Imortal", "BARBARO",
                bonus("{\"constituicao\":8,\"forca\":2}", "{\"vigor\":8,\"atletismo\":2}", 2),
                "Sobrevivência Imortal. Quando seus Pontos de Vida forem reduzidos a 0, o usuário não fica "
                        + "Inconsciente nem morre imediatamente, permanecendo de pé até sofrer dano novamente. Além disso, "
                        + "recebe Vantagem em testes de Constituição enquanto estiver no estado Morrendo e atrasa em 1 "
                        + "passo a progressão desse estado.");

        classe("CLERIGO", "Clerigo", null,
                bonus("{\"sabedoria\":4,\"constituicao\":1}", "{\"fe\":4,\"conhecimento\":1}", 3),
                "Julgamento Divino. Ao enfrentar um inimigo que possua uma divindade do mesmo panteão que a sua, "
                        + "o usuário recebe +2 em ataques e defesas relacionadas a bênçãos dessa divindade a cada 10 pontos "
                        + "em sua perícia Fé.");
        classe("PALADINO", "Paladino", "CLERIGO",
                bonus("{\"constituicao\":6,\"sabedoria\":3,\"forca\":1}", "{\"vigor\":5,\"fe\":5}", 1),
                "Escudo da Fé. Enquanto estiver empunhando um escudo, o usuário pode utilizar sua perícia Fé no "
                        + "lugar de Bloqueio para defender ataques direcionados a aliados.");
        classe("SANTO", "Santo", "CLERIGO",
                bonus("{\"sabedoria\":7,\"carisma\":2,\"constituicao\":1}", "{\"fe\":5,\"conhecimento\":3,\"diplomacia\":2}", 4),
                "Aura Sagrada. Enquanto estiver consciente, todos os aliados em alcance curto recebem Vantagem "
                        + "em testes de resistência contra efeitos negativos causados por Divindades.");

        classe("DRUIDA", "Druida", null,
                bonus("{\"constituicao\":2,\"sabedoria\":2,\"carisma\":1}", "{\"adestrar\":2,\"vigor\":1,\"percepcao\":1,\"sobrevivencia\":1}", 3),
                "Comunhão com a Natureza. O Druida possui uma profunda ligação com a natureza e é capaz de "
                        + "sentir as mudanças no ambiente natural. Enquanto este estiver em uma floresta ou ambiente "
                        + "natural, terá +2 pontos a cada 10 Níveis do Personagem adicionado a quaisquer teste de "
                        + "\"Furtividade, Percepção, Sobrevivência, Iniciativa, Adestrar, Virtude e Sanidade\".");
        classe("ARAUTO_NATUREZA", "Arauto da Natureza", "DRUIDA",
                bonus("{\"sabedoria\":4,\"inteligencia\":4,\"carisma\":2}", "{\"magia\":4,\"conhecimento\":3,\"adestrar\":2,\"sobrevivencia\":1}", 4),
                "Filho da Magia Natural. O Arauto da Natureza é um verdadeiro intermediário entre o mundo "
                        + "natural e os seres vivos. Ele é um exímio usuário de magia e sempre que está na natureza seu "
                        + "vínculo com a mana é refinado, ao ponto de gastar metade da mana com \"Magia Natural\".");
        classe("DOMADOR_FERAS", "Domador de Feras", "DRUIDA",
                bonus("{\"constituicao\":3,\"sabedoria\":3,\"carisma\":3,\"forca\":1}", "{\"adestrar\":5,\"sobrevivencia\":3,\"vigor\":2}", 3),
                "Sintonia Animal. O Domador de Feras desenvolve uma conexão profunda com as criaturas que ele "
                        + "comanda, tornando-as mais fortes e leais. Todas as feras sob o seu comando ganham a sua perícia "
                        + "\"Adestrar\" em HP permanentemente. Além de que as suas criaturas tem \"Vantagem\" em testes de "
                        + "resistência para trair o seu mestre.");

        classe("MONGE", "Monge", null,
                bonus("{\"constituicao\":2,\"sabedoria\":1,\"forca\":1,\"destreza\":1}", "{\"combate\":2,\"virtude\":2,\"conhecimento\":1}", 2),
                "Corpo Iluminado. O usuário adiciona metade do seu atributo Constituição à sua Armadura "
                        + "Universal. Além disso, torna-se Imune aos estados Envenenado, Sangramento, Confusão e "
                        + "Sonolência. Estes efeitos são anulados enquanto estiver utilizando Armadura Pesada.");
        classe("SABIO", "Sabio", "MONGE",
                bonus("{\"sabedoria\":5,\"constituicao\":3,\"inteligencia\":2}", "{\"conhecimento\":4,\"virtude\":3,\"percepcao\":2,\"medicina\":1}", 2),
                "Sabedoria Ancestral. Aliados em alcance curto recebem Vantagem em testes de Percepção e "
                        + "Conhecimento. Além disso, uma vez por dia, o usuário pode conceder Inspiração Sábia a um "
                        + "aliado, permitindo que ele adicione o atributo de Sabedoria do usuário ao próximo teste baseado "
                        + "em Sabedoria.");
        classe("PUNHOS_ACO", "Punhos de Aco", "MONGE",
                bonus("{\"constituicao\":3,\"forca\":3,\"destreza\":3,\"sabedoria\":1}", "{\"combate\":4,\"virtude\":3,\"bloqueio\":2,\"sobrevivencia\":1}", 1),
                "Golpes Rígidos. Ataques desarmados ignoram a Armadura de equipamentos do alvo para fins de "
                        + "redução de dano. Além disso, causam o dobro de dano em testes para quebrar objetos.");

        classe("ARQUEIRO", "Arqueiro", null,
                bonus("{\"destreza\":3,\"agilidade\":2}", "{\"pontaria\":3,\"furtividade\":2}", 2),
                "Olhos de Águia. Ao atacar um alvo em alcance além de longo, o usuário considera o ataque um "
                        + "Acerto Crítico. Contra alvos em alcance menor que longo, o usuário ignora Cobertura Parcial e "
                        + "considera Cobertura Completa como Cobertura Parcial.");
        classe("CACADOR", "Cacador", "ARQUEIRO",
                bonus("{\"destreza\":5,\"agilidade\":4,\"sabedoria\":1}", "{\"pontaria\":4,\"iniciativa\":3,\"sobrevivencia\":3}", 3),
                "Instinto de Caça. O usuário escolhe uma categoria de criatura entre Bestas, Dragões, Monstros "
                        + "ou Humanoides como seu Alvo Principal. Contra criaturas dessa categoria, recebe +2 na Margem de "
                        + "Ameaça e Vantagem em testes de Sobrevivência, Rastreamento e Identificação.");
        classe("MERCENARIO", "Mercenario", "ARQUEIRO",
                bonus("{\"destreza\":3,\"agilidade\":3,\"forca\":3,\"sabedoria\":1}", "{\"combate\":3,\"pontaria\":3,\"iniciativa\":2,\"sobrevivencia\":2}", 4),
                "Instinto de Sobrevivência. O usuário recebe Vantagem em testes para detectar ou resistir a "
                        + "Emboscadas e Armadilhas. Enquanto enfrentar múltiplos inimigos, recebe 1 Reação adicional para "
                        + "cada inimigo em combate.");

        classe("MAGO", "Mago", null,
                bonus("{\"inteligencia\":4,\"sabedoria\":1}", "{\"magia\":3,\"conhecimento\":2}", 5),
                "Mente Arcana. O usuário recebe Vantagem em testes de Feitiçaria para identificar ou reagir a "
                        + "feitiços. Uma vez por dia, pode utilizar seu Foco Arcano para lançar 1 feitiço em uma Ação "
                        + "Principal, aumentando em +1 feitiço adicional a cada 10 Níveis do Personagem. Os custos dos "
                        + "feitiços devem ser considerados individualmente.");
        classe("ARQUIMAGO", "Arquimago", "MAGO",
                bonus("{\"inteligencia\":8,\"sabedoria\":2}", "{\"magia\":8,\"conhecimento\":2}", 4),
                "Maestria Arcana. O usuário pode conjurar até 2 feitiços diferentes simultaneamente, porém o "
                        + "feitiço secundário custa 4 PE adicionais. Ao realizar essa conjuração, recebe +2 em Feitiçaria, "
                        + "aumentando em +2 a cada 10 Níveis do Personagem, para o seu próximo feitiço secundário. Além "
                        + "disso, ao ser alvo de um feitiço que conheça, pode realizar um embate de Magia contra o "
                        + "conjurador. Em caso de sucesso, o feitiço é anulado.");
        classe("RUNICISTA", "Runicista", "MAGO",
                bonus("{\"inteligencia\":7,\"sabedoria\":3}", "{\"magia\":7,\"conhecimento\":3}", 4),
                "Maestria Rúnica. O usuário pode fazer um teste de Feitiçaria para desfazer uma runa presente em "
                        + "um objeto ou criar uma nova runa em um objeto. Enquanto empunhar um objeto rúnico, recebe +5 em "
                        + "testes de Ataque e Reação Físicos a cada 10 pontos de Inteligência.");

        classe("ESPADACHIM", "Espadachim", null,
                bonus("{\"destreza\":3,\"agilidade\":2}", "{\"combate\":2,\"iniciativa\":2,\"esquiva\":1}", 2),
                "Mestre de Lâminas. Quando for atacado por uma criatura utilizando uma arma de corte, o usuário "
                        + "pode realizar o teste de defesa utilizando Combate no lugar de Bloqueio.");
        classe("DUELISTA", "Duelista", "ESPADACHIM",
                bonus("{\"destreza\":5,\"agilidade\":3,\"forca\":2}", "{\"combate\":4,\"iniciativa\":3,\"esquiva\":3}", 3),
                "Duelo Exímio. Enquanto estiver enfrentando apenas uma criatura e sozinho, o usuário recebe +2 em "
                        + "Combate a cada 10 Níveis do Personagem. Além disso, recebe Vantagem em testes para resistir a "
                        + "qualquer tentativa ou efeito que impeça, interrompa ou encerre seu duelo. Caso outra criatura "
                        + "interfira diretamente no combate, os benefícios do Duelo Exímio são perdidos até o fim da cena.");
        classe("SAMURAI", "Samurai", "ESPADACHIM",
                bonus("{\"destreza\":4,\"agilidade\":3,\"constituicao\":3}", "{\"combate\":4,\"iniciativa\":3,\"vigor\":3}", 3),
                "Caminho da Lâmina. Enquanto utilizar uma Katana, o usuário recebe +2 em Combate a cada 10 pontos "
                        + "de Destreza. Ao declarar um Duelo de Honra antes do combate, o confronto deve ocorrer em 1 "
                        + "contra 1 até a morte, sem interferência externa. Ao vencer o duelo, recebe o dobro da "
                        + "Experiência concedida pelo combate.");

        classe("LADRAO", "Ladrao", null,
                bonus("{\"agilidade\":4,\"destreza\":1}", "{\"furtividade\":4,\"esquiva\":1}", 5),
                "Mão Leve. O usuário recebe Vantagem em testes de Crime enquanto estiver Furtivo. Além disso, "
                        + "sempre que realizar um Saque, pode rolar 1d5; com resultado 5, encontra 1 item adicional no "
                        + "saque.");
        classe("SOMBRA", "Sombra", "LADRAO",
                bonus("{\"agilidade\":7,\"destreza\":3}", "{\"furtividade\":7,\"acrobacia\":3}", 4),
                "Ofuscado pela Escuridão. Enquanto estiver em áreas de escuridão, o usuário recebe um bônus em "
                        + "Furtividade e Esquiva igual ao seu Deslocamento, tornando-se Parcialmente Invisível.");
        classe("PIRATA", "Pirata", "LADRAO",
                bonus("{\"agilidade\":3,\"destreza\":3,\"forca\":3,\"constituicao\":1}", "{\"combate\":3,\"acrobacia\":3,\"vigor\":3,\"pontaria\":1}", 3),
                "Olho Atento ao Mar. O usuário recebe +2 em Percepção a cada 5 Níveis do Personagem em testes "
                        + "para localizar itens de valor. Enquanto estiver em alto-mar, uma vez por turno, recebe um bônus "
                        + "igual à metade de sua Destreza em testes de Ataque ou Reação.");

        classe("CURANDEIRO", "Curandeiro", null,
                bonus("{\"sabedoria\":5}", "{\"medicina\":4,\"conhecimento\":1}", 3),
                "Toque Curativo. Quando utilizar uma Magia de cura em outra criatura, o valor de PV recuperado é "
                        + "dobrado.");
        classe("SACERDOTE", "Sacerdote", "CURANDEIRO",
                bonus("{\"sabedoria\":9,\"inteligencia\":1}", "{\"medicina\":9,\"conhecimento\":1}", 3),
                "Aura de Cura. Toda magia de cura utilizada pelo usuário também afeta aliados em alcance curto, "
                        + "que recuperam metade dos PV restaurados ao alvo principal. Além disso, aliados em alcance curto "
                        + "removem 1 efeito negativo no início de cada turno.");
        classe("MESTRE_TOXINAS", "Mestre de Toxinas", "CURANDEIRO",
                bonus("{\"sabedoria\":6,\"inteligencia\":4}", "{\"medicina\":4,\"alquimia\":4,\"magia\":2}", 3),
                "Mão Venenosa. O usuário é Imune a Veneno e Doença. Ao aplicar Veneno em uma arma, todos os "
                        + "efeitos de Envenenamento causados por ela são dobrados.");

        classe("BRUXO", "Bruxo", null,
                bonus("{\"forca\":3,\"inteligencia\":2}", "{\"combate\":2,\"magia\":2,\"conhecimento\":1}", 4),
                "Pacto Sombrio. O usuário estabelece um pacto com um espírito bestial, chamado de Vínculo, que "
                        + "se torna seu aliado. O Vínculo pode ser invocado ou ocultado livremente pelo usuário. Caso seja "
                        + "morto, retorna após um período determinado por seu nível de poder.");
        classe("NECROMANTE", "Necromante", "BRUXO",
                bonus("{\"inteligencia\":7,\"sabedoria\":3}", "{\"magia\":4,\"conhecimento\":3,\"virtude\":3}", 3),
                "Liderança Putrefata. O usuário pode escolher um de seus mortos-vivos para compartilhar parte de "
                        + "sua alma, transformando-o em seu Seguidor, que pode ser invocado sem custo. Caso o Seguidor "
                        + "morra em combate, retornará após um período determinado por seu nível de força. Se o Seguidor "
                        + "for substituído ou seu corpo for destruído, não poderá mais retornar.");
        classe("MISTICO", "Mistico", "BRUXO",
                bonus("{\"forca\":4,\"inteligencia\":4,\"destreza\":2}", "{\"combate\":5,\"magia\":4,\"esquiva\":1}", 2),
                "Ligação Espiritual. O usuário pode consumir temporariamente um espírito, absorvendo sua "
                        + "essência e recebendo um bônus determinado pela natureza do espírito. O efeito dura cena e o "
                        + "usuário pode manter 1 a cada 5 Níveis do Personagem em espíritos ativos. O bônus concedido é "
                        + "definido pelo Mestre de acordo com a natureza e a força dos Espíritos.");

        classe("ALQUIMISTA", "Alquimista", null,
                bonus("{\"inteligencia\":5}", "{\"alquimia\":4,\"conhecimento\":1}", 3),
                "Maestria Alquímica. O usuário recebe +10 na Perícia Alquimia ao criar poções e elixires fora de "
                        + "combate. Além disso, pode criar poções utilizando 1 ingrediente a menos, mas nunca menos que 1, "
                        + "desde que os ingredientes sejam Comuns ou Incomuns. Ao consumir uma poção ou elixir, sua "
                        + "duração é aumentada em 1 turno.");
        classe("QUIMICO_ARCANO", "Quimico Arcano", "ALQUIMISTA",
                bonus("{\"inteligencia\":7,\"sabedoria\":3}", "{\"alquimia\":7,\"magia\":2,\"engenharia\":1}", 2),
                "Síntese Arcana. Enquanto estiver próximo de materiais orgânicos, o usuário pode transmutá-los "
                        + "para gerar 1d10 PM a cada 10 Níveis do Personagem ou criar poções comuns ou incomuns "
                        + "rapidamente, mesmo durante situações de pressão.");
        classe("INVENTOR", "Inventor", "ALQUIMISTA",
                bonus("{\"inteligencia\":6,\"sabedoria\":4}", "{\"engenharia\":8,\"alquimia\":2}", 4),
                "Engenharia Mecânica. O usuário recebe Vantagem em testes de Engenharia para desarmar ou "
                        + "desativar dispositivos tecnológicos. As peças obtidas desses dispositivos podem ser utilizadas "
                        + "na criação de versões aprimoradas de seus próprios mecanismos. Em uma Oficina, recebe +10 em "
                        + "Engenharia para criar, reparar ou aprimorar mecanismos.");

        classe("LANCEIRO", "Lanceiro", null,
                bonus("{\"forca\":3,\"destreza\":2}", "{\"combate\":4,\"atletismo\":1}", 3),
                "Precisão de Lança. Enquanto utilizar uma lança, o usuário recebe +2 em Combate a cada 10 pontos "
                        + "de Destreza. Além disso, pode realizar o arremesso de armas na categoria de lança utilizando "
                        + "Combate no lugar de Arremesso.");
        classe("GENERAL_CEUS", "General dos Ceus", "LANCEIRO",
                bonus("{\"forca\":4,\"destreza\":4,\"constituicao\":2}", "{\"combate\":5,\"atletismo\":3,\"vigor\":2}", 2),
                "Domínio Aéreo. Ao arremessar uma lança, o usuário recebe +2 em Combate a cada 10 Níveis do "
                        + "Personagem. Além disso, seus ataques à distância com lanças são considerados Acertos Críticos, "
                        + "e contra criaturas que estejam voando a uma distância longa ou superior, seus ataques causam "
                        + "dano dobrado.");
        classe("VALQUIRIA", "Valquiria", "LANCEIRO",
                bonus("{\"forca\":5,\"destreza\":5,\"constituicao\":5}", "{\"combate\":5,\"atletismo\":5,\"vigor\":5}", 3),
                "Elegância de Guerra. Enquanto utilizar uma lança, o usuário recebe +2 em testes de Ataque e "
                        + "Reação a cada 10 pontos de Força. Ao empunhar duas lanças, esse bônus é substituído por 3 a "
                        + "cada 10 pontos de Força.");

        classe("LUTADOR", "Lutador", null,
                bonus("{\"forca\":2,\"agilidade\":1,\"constituicao\":1,\"destreza\":1}", "{\"combate\":3,\"vigor\":1,\"bloqueio\":1}", 2),
                "Tenacidade de Batalha. Enquanto utilizar Armadura Leve, o usuário recebe o dobro dos benefícios "
                        + "concedidos por sua armadura. Enquanto estiver desarmado, efeitos que reduzam seus atributos ou "
                        + "características têm sua potência e duração reduzidas pela metade");
        classe("CAMPEAO", "Campeao", "LUTADOR",
                bonus("{\"forca\":4,\"constituicao\":3,\"carisma\":3}", "{\"combate\":3,\"vigor\":3,\"aparencia\":3,\"diplomacia\":1}", 2),
                "Eu sou o Campeão. Enquanto estiver em combate com aliados, sempre que derrotar sozinho um "
                        + "inimigo, recebe +2 em Dano Desarmado a cada 10 pontos de Carisma até o fim da batalha. Esse "
                        + "bônus pode ser acumulado até 3 vezes.");
        classe("ARTISTA_MARCIAL", "Artista Marcial", "LUTADOR",
                bonus("{\"forca\":3,\"agilidade\":3,\"constituicao\":3,\"destreza\":1}", "{\"combate\":5,\"bloqueio\":2,\"esquiva\":2,\"iniciativa\":1}", 3),
                "Harmonia de Combate. O usuário ignora as penalidades de Lentidão e Paralisia. Além disso, "
                        + "sempre que acertar um ataque contra uma nova criatura durante o combate, recebe +2 em Combate e "
                        + "2 em Dano Final. Esses bônus são cumulativos até que o usuário ataque uma criatura que já tenha "
                        + "atacado anteriormente, reiniciando a sequência.");

        classe("VIAJANTE", "Viajante", null,
                bonus("{\"carisma\":3,\"sabedoria\":2}", "{\"enganacao\":2,\"aparencia\":2,\"conhecimento\":1}", 1),
                "Conhecimento Itinerante. O Viajante é um observador astuto e um aprendiz constante, absorvendo "
                        + "informações de diferentes culturas e terras. Quando esse visita uma nova localidade, após "
                        + "passar uma noite em tal este receberá uma \"Habilidade\" aleatória de qualquer residente local "
                        + "que encontrou, permanecendo com tal \"Habilidade\" até sair da localização.");
        classe("BARDO", "Bardo", "VIAJANTE",
                bonus("{\"carisma\":8,\"destreza\":2}", "{\"artes\":8,\"aparencia\":2}", 2),
                "Inspiração Contínua. O Bardo é uma fonte constante de inspiração para seus aliados. O Bardo uma "
                        + "vez por dia pode inspirar um aliado com \"Tudo de Si\" que irá causar com que a próxima "
                        + "habilidade, feitiço ou passiva do seu aliado, cause o dobro de efeito. Também pode utilizar "
                        + "instrumentos musicais como catalisadores para magia, se o fizer, adicione +1 turno de efeito a "
                        + "magias de buff.");
        classe("CIGANO", "Cigano", "VIAJANTE",
                bonus("{\"carisma\":5,\"destreza\":5}", "{\"artes\":5,\"acrobacia\":5}", 3),
                "Leitura do Destino. O Cigano é mestre na arte da astúcia e da leitura de situações. Quando este "
                        + "toca em um alvo, poderá ter um leve vislumbre do que seu oponente fará, ganhando assim ganhando "
                        + "+2 a cada 5 pontos de \"Destreza\" para testes de \"Reação\" contra aquele oponente. E se for "
                        + "feito em um aliado no interlúdio, como passa tempo, será jogado um d20 para determinar o buff "
                        + "que este receberá nos seus próximos 3 testes.");
        classe("CARTOMANTE", "Cartomante", "VIAJANTE",
                bonus("{\"destreza\":7,\"inteligencia\":3}", "{\"enganacao\":5,\"arremesso\":5}", 4),
                "Cartas Arcanas. O Cartomante possui uma afinidade única com as energias arcanas associadas às "
                        + "suas cartas. Este pode imbuir cartas especiais para que estas afetam a mana de seus alvos, "
                        + "causando os mais derivados efeitos negativos, determinados por 1d6 sendo os efeitos: "
                        + "(1)Paralisia; (2)Fraqueza; (3)Ofuscado; (4)Surdo; (5)Fragilizado; (6)Ansioso. Para o efeito "
                        + "funcionar é preciso passar em um teste de \"Jogatina\" contra a resistência do alvo.");
        classe("PINTOR", "Pintor", "VIAJANTE",
                bonus("{\"destreza\":6,\"carisma\":4}", "{\"artes\":10}", 2),
                "Inspiração Artística. O pintor encontra beleza e poder em suas próprias criações visuais. "
                        + "Sempre que o pintor conclui uma obra de arte significativa, ele e os aliados podem ganhar "
                        + "metade da sua \"Arte\" em qualquer Atributo dependendo do tema da tela.");
    }

    // ---------------- Usuario / Organizacao / Campanha demo ----------------

    private void seedComunidade(GameSystem asus) {
        Usuario dev = usuarioRepository.save(Usuario.builder()
                .nome("Dev").email("dev@asus.local")
                .senhaHash(passwordEncoder.encode("dev12345")).build());

        Organizacao org = organizacaoRepository.save(Organizacao.builder()
                .nome("ASUS Oficial").slug(SLUG_ORG_PADRAO)
                .donoId(dev.getId()).plano(Plano.GUILD).build());

        assinaturaRepository.save(Assinatura.builder()
                .organizacaoId(org.getId()).plano(Plano.GUILD).status("ATIVA").build());

        membroRepository.save(OrganizacaoMembro.builder()
                .organizacaoId(org.getId()).usuarioId(dev.getId())
                .papel(PapelOrganizacao.DONO).build());
        // (sem campanha inicial — cada um cria a sua)
    }

    // ---------------- Vitrine: Marketplace + Templates oficiais (conteudo inicial) ----------------

    /** Reaplica a vitrine (marketplace + templates oficiais) em banco ja existente. */
    void refreshVitrine() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            marketplaceItemRepository.deleteAll(marketplaceItemRepository.findByOficialTrue());
            templateRepository.deleteAll(templateRepository.findByOficialTrue());
            seedVitrine();
        });
    }

    private void seedVitrine() {
        // Marketplace (itens de vitrine, gratuitos e publicados)
        mkt("Ficha pronta: Cavaleiro Iniciante", "Um Cavaleiro nível 1 pronto pra jogar, "
                + "com atributos e perícias sugeridas.", "FICHA");
        mkt("Pacote de NPCs de Taverna", "Cinco NPCs de taverna com ganchos de história.", "NPC");
        mkt("Mapa: Masmorra do Norte", "Mapa de masmorra em 3 andares para usar no Overlay/OBS.", "MAPA");
        mkt("Aventura: Relíquias do Apocalipse", "Módulo introdutório para grupos de nível 1 a 3.", "CAMPANHA");

        // Templates oficiais e públicos (pontos de partida)
        tmpl("FICHA", "Ficha em branco (ASUS)", "Estrutura base de ficha para começar rápido.",
                "{\"nivel\":1,\"observacao\":\"Preencha atributos e perícias.\"}");
        tmpl("ATAQUE", "Ataque padrão", "Modelo de ataque corpo a corpo.",
                "{\"nome\":\"Ataque\",\"dano\":\"1d6\",\"critico\":\"x2\",\"alcance\":\"Corpo a corpo\"}");
        tmpl("MAGIA", "Feitiço básico", "Modelo de feitiço de 1º círculo.",
                "{\"nome\":\"Feitiço\",\"circulo\":1,\"custoPm\":1,\"alcance\":\"Curto\"}");
        tmpl("NPC", "NPC genérico", "Ficha simplificada de NPC.",
                "{\"nome\":\"NPC\",\"pv\":10,\"defesa\":12}");
        tmpl("ITEM", "Item comum", "Modelo de item de inventário.",
                "{\"nome\":\"Item\",\"espacos\":1}");
    }

    private void mkt(String titulo, String descricao, String tipo) {
        marketplaceItemRepository.save(MarketplaceItem.builder()
                .titulo(titulo).descricao(descricao).tipo(tipo)
                .gratuito(true).publicado(true).oficial(true).build());
    }

    private void tmpl(String tipo, String nome, String descricao, String json) {
        templateRepository.save(Template.builder()
                .gameSystemId(sid).nome(nome).tipo(tipo).descricao(descricao)
                .jsonConteudo(json).oficial(true).publico(true).build());
    }

    // ---------------- helpers ----------------

    private void raca(String codigo, String nome, int pv, int pm, int pe, String desc, String habilidadesJson) {
        racaRepository.save(Raca.builder()
                .gameSystemId(sid).codigo(codigo).nome(nome).descricao(desc)
                .pvBase(pv).pmBase(pm).peBase(pe)
                .jsonHabilidades("{\"habilidades\":" + habilidadesJson + "}")
                .oficial(true).build());
    }

    // Upsert por codigo: atualiza a pericia existente ou cria uma nova (permite refresh).
    private void pericia(String codigo, String nome, Atributo atributo, String descricao, String exemplos) {
        Pericia p = periciaRepository.findByGameSystemIdAndCodigo(sid, codigo).orElseGet(Pericia::new);
        p.setGameSystemId(sid);
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setAtributoBase(atributo.name());
        p.setDescricao(descricao);
        p.setExemplos(exemplos);
        p.setOficial(true);
        periciaRepository.save(p);
    }

    /** PV/PM/PE das classes-base (tabela da pag. 20 do livro). Trilhas ficam em 0. */
    private static final java.util.Map<String, int[]> PVPMPE = java.util.Map.ofEntries(
            java.util.Map.entry("CAVALEIRO", new int[]{6, 3, 6}),
            java.util.Map.entry("ASSASSINO", new int[]{5, 4, 6}),
            java.util.Map.entry("BARBARO", new int[]{7, 1, 7}),
            java.util.Map.entry("CLERIGO", new int[]{3, 7, 5}),
            java.util.Map.entry("DRUIDA", new int[]{6, 6, 3}),
            java.util.Map.entry("MONGE", new int[]{5, 5, 5}),
            java.util.Map.entry("ARQUEIRO", new int[]{5, 3, 7}),
            java.util.Map.entry("MAGO", new int[]{3, 9, 3}),
            java.util.Map.entry("LADRAO", new int[]{3, 5, 7}),
            java.util.Map.entry("CURANDEIRO", new int[]{4, 7, 4}),
            java.util.Map.entry("ESPADACHIM", new int[]{4, 4, 7}),
            java.util.Map.entry("BRUXO", new int[]{5, 7, 3}),
            java.util.Map.entry("ALQUIMISTA", new int[]{4, 6, 5}),
            java.util.Map.entry("LANCEIRO", new int[]{7, 2, 6}),
            java.util.Map.entry("LUTADOR", new int[]{6, 2, 7}),
            java.util.Map.entry("VIAJANTE", new int[]{4, 6, 6}));

    // Upsert por codigo: atualiza a classe existente (preservando o id, e portanto os
    // personagens que a referenciam) ou cria uma nova. Permite refresh em banco ja existente.
    private void classe(String codigo, String nome, String pai, String jsonBonus, String passiva) {
        int[] pvpmpe = pai == null ? PVPMPE.getOrDefault(codigo, new int[]{5, 5, 5}) : new int[]{0, 0, 0};
        Classe c = classeRepository.findByGameSystemIdAndCodigo(sid, codigo).orElseGet(Classe::new);
        c.setGameSystemId(sid);
        c.setCodigo(codigo);
        c.setNome(nome);
        c.setClassePaiCodigo(pai);
        c.setJsonBonus(jsonBonus);
        c.setJsonPassiva(passiva);
        c.setMultiplicadorPv(pvpmpe[0]);
        c.setMultiplicadorPm(pvpmpe[1]);
        c.setMultiplicadorPe(pvpmpe[2]);
        c.setOficial(true);
        classeRepository.save(c);
    }

    /** Reaplica as classes (descricao, passiva, bonus) em banco ja existente, sem trocar ids. */
    void refreshClasses() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            seedClasses();
        });
    }

    private String bonus(String atributos, String pericias, int slots) {
        return "{\"atributos\":" + atributos + ",\"pericias\":" + pericias + ",\"slots\":" + slots + "}";
    }
}
