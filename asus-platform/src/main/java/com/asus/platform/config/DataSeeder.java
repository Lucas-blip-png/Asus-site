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
            ensureAdmin(); // garante a conta de dono mesmo em banco ja existente
            refreshBestiario(); // reaplica o bestiario autoral (categorias + ranks)
            refreshClasses(); // reaplica passivas/descricoes das classes (sem trocar ids)
            refreshPericias(); // reaplica pericias (descricao + exemplos) e adiciona novas
            refreshItens(); // reaplica o catalogo de itens do ASUS (categorias, precos, tipo de dano)
            refreshHabilidades(); // reaplica habilidades (inclui as GERAIS novas)
            refreshVitrine(); // semeia/atualiza marketplace + templates oficiais
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
        if (n <= 10) return "Classe Primaria";
        if (n <= 20) return "Trilha Primaria";
        if (n <= 30) return "Classe Secundaria";
        if (n <= 40) return "Trilha Secundaria";
        return "Maestria";
    }

    private String recompensaDoNivel(int n) {
        if (n == 1) return "Classe, Bonus e Atributos";
        if (n % 5 == 0) return "Bonus de Classe/Trilha e Raca";
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
        item("MOCHILA", "Mochila", "ITEM_GERAL", "Equipamentos", "10", 1);
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
        hab("AUMENTO_ATRIBUTO", "Aumento de Atributo", "GERAL", "PASSIVA", 0, null,
                "Nivel", "+2 pontos de atributo; escalavel (+2 a cada 10 niveis).");
        hab("FEITICARIA_1", "Feiticaria de 1 Grau", "GERAL", "PASSIVA", 0, null,
                "Nivel 1", "Conjura feiticos de 1 Grau.");
        hab("FEITICARIA_2", "Feiticaria de 2 Grau", "GERAL", "PASSIVA", 0, null,
                "Feiticaria 1 e Nivel 10", "Conjura feiticos de 2 Grau.");
        hab("AMBIDESTRIA", "Ambidestria", "GERAL", "PASSIVA", 0, null,
                "Destreza 10", "Usa duas armas ao mesmo tempo.");
        hab("ATAQUE_FORTE", "Ataque Forte", "CAVALEIRO", "ATIVA", 2, "PE",
                "Arma corpo a corpo", "+1 dado de dano a cada 2 PE, -5 na pericia de ataque.");
        hab("REDUCAO_DANO", "Reducao de Dano", "CAVALEIRO", "ATIVA", 3, "PE",
                "Reacao", "Adiciona Constituicao a Armadura ao receber ataque.");
        hab("ESCUDO_ALIADO", "Escudo Aliado", "CAVALEIRO", "ATIVA", 3, "PE",
                "Reacao", "Toma o ataque por um aliado; +Armadura = Nivel.");
        hab("BRAVURA_FINAL", "Bravura Final", "CAVALEIRO", "ATIVA", 4, "PE",
                "0 PV", "Imune a inconsciencia enquanto pagar o custo por turno.");
        hab("CHANCE_UNICA", "Chance Unica", "ASSASSINO", "ATIVA", 5, "PE",
                "Ataque", "1 golpe causa o dobro de dano; penalidade se nao matar.");
        hab("PASSO_SOMBRA", "Passo de Sombra", "ASSASSINO", "ATIVA", 3, "PE",
                "Nas sombras", "Move-se sem ataques de oportunidade.");
        hab("ESTADO_FURIA", "Estado de Furia", "BARBARO", "ATIVA", 4, "PE",
                "Combate", "Aumenta forca e dano enquanto em furia.");
        hab("INVESTIDA", "Investida", "BARBARO", "ATIVA", 2, "PE",
                "Arma perfurante", "Avanca em linha reta causando dano extra pelo deslocamento.");
        hab("CURAR_FERIMENTOS", "Curar Ferimentos", "CLERIGO", "ATIVA", 2, "PM",
                "Feitico de cura", "Recupera PV de aliados.");
        hab("BARREIRA_ARCANA", "Barreira Arcana", "MAGO", "ATIVA", 4, "PM",
                "Feiticaria", "Cria protecao magica.");
        hab("DRENO_VIDA", "Dreno de Vida", "BRUXO", "ATIVA", 5, "PM",
                "Feiticaria", "Rouba PV do inimigo.");
        hab("FORMA_BESTIAL", "Forma Bestial", "DRUIDA", "ATIVA", 6, "PE",
                "Nivel 10", "Transforma-se em um animal.");
        hab("GRANADAS_POCOES", "Granadas e Pocoes", "ALQUIMISTA", "ATIVA", 3, "PE",
                "Alquimia", "Usa alquimia ofensiva e defensiva.");
        hab("CANTO_DIVINO", "Canto Divino", "VIAJANTE", "ATIVA", 3, "PE",
                "Artes", "Ataques baseados em musica.");

        // ---- Habilidades GERAIS (qualquer classe pode escolher) ----
        hab("GOLPE_PRECISO", "Golpe Preciso", "GERAL", "ATIVA", 2, "PE",
                "Nivel 1", "Gasta 2 PE para ganhar +5 na pericia de ataque de um golpe.");
        hab("INVESTIDA", "Investida", "GERAL", "ATIVA", 2, "PE",
                "Nivel 1", "Move-se e ataca no mesmo turno; +2 no dano se avancar 6m ou mais.");
        hab("DEFESA_TOTAL", "Defesa Total", "GERAL", "ATIVA", 2, "PE",
                "Nivel 1", "Reacao: dobra Bloqueio ou Esquiva contra um ataque, mas nao age no turno.");
        hab("SEGUNDA_CHANCE", "Segunda Chance", "GERAL", "ATIVA", 3, "PE",
                "Nivel 1", "1x por dia: rerrola um teste de pericia que voce falhou.");
        hab("GOLPE_GIRATORIO", "Golpe Giratorio", "GERAL", "ATIVA", 3, "PE",
                "Nivel 1", "Atinge todos os inimigos adjacentes com um ataque corpo a corpo.");
        hab("PONTO_VITAL", "Ponto Vital", "GERAL", "ATIVA", 3, "PE",
                "Nivel 1", "Aumenta em +1 a margem de critico de um ataque.");
        hab("FOCO_MENTAL", "Foco Mental", "GERAL", "ATIVA", 2, "PM",
                "Nivel 1", "Ganha +5 em um teste de pericia de Inteligencia ou Sabedoria.");
        hab("VERSATIL", "Versatil", "GERAL", "PASSIVA", 0, null,
                "Nivel 1", "Escolha 1 pericia extra para tratar como treinada.");
        hab("RESISTENCIA_FISICA", "Resistencia Fisica", "GERAL", "PASSIVA", 0, null,
                "Nivel 1", "Vantagem em testes de Vigor contra fadiga, fome e venenos.");
        hab("VIGOR_EXTRA", "Vigor Extra", "GERAL", "PASSIVA", 0, null,
                "Constituicao 2", "Ganha +metade da Constituicao em PV maximo.");
        hab("REFLEXOS_RAPIDOS", "Reflexos Rapidos", "GERAL", "PASSIVA", 0, null,
                "Agilidade 2", "+2 na Iniciativa e na Esquiva.");
        hab("LABIA", "Labia", "GERAL", "PASSIVA", 0, null,
                "Carisma 2", "+2 em Diplomacia, Enganacao e Aparencia.");
        hab("INSTINTO_SOBREVIVENCIA", "Instinto de Sobrevivencia", "GERAL", "PASSIVA", 0, null,
                "Sabedoria 2", "+2 em Percepcao e Sobrevivencia.");
        hab("MAOS_HABEIS", "Maos Habeis", "GERAL", "PASSIVA", 0, null,
                "Destreza 2", "+2 em Crime, Oficio e Jogatina.");
        hab("CONCENTRACAO_ARCANA", "Concentracao Arcana", "GERAL", "PASSIVA", 0, null,
                "Feiticaria", "Mantem 1 feitico ativo a mais ao mesmo tempo.");
        hab("APRENDIZ_RAPIDO", "Aprendiz Rapido", "GERAL", "PASSIVA", 0, null,
                "Inteligencia 2", "+2 em Conhecimento e uma pericia de Inteligencia a sua escolha.");
    }

    /** Reaplica as habilidades (catalogo) em banco ja existente. */
    void refreshHabilidades() {
        gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID).ifPresent(gs -> {
            sid = gs.getId();
            habilidadeRepository.deleteAll(habilidadeRepository.findByGameSystemIdAndOficialTrue(sid));
            seedHabilidades();
        });
    }

    private void hab(String codigo, String nome, String classe, String tipo, int custo,
                     String custoTipo, String requisito, String efeito) {
        // Deriva nivel/atributo exigidos do texto do requisito (ex.: "Nivel 10", "Destreza 10").
        int nivelMin = 1;
        String atrReq = null;
        int valorAtr = 0;
        if (requisito != null) {
            String low = requisito.toLowerCase(java.util.Locale.ROOT);
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
                "[{\"nivel\":1,\"nome\":\"Linhagem\",\"efeito\":\"Recebe 1 item ou bencao a escolha\"},"
                + "{\"nivel\":20,\"nome\":\"Versatilidade Humana\",\"efeito\":\"+1 slot de habilidade por bonus de classe/trilha\"},"
                + "{\"nivel\":40,\"nome\":\"Almejar Maestria\",\"efeito\":\"+50% de XP e melhora 1 habilidade de classe\"}]");
        raca("DEMONIO", "Demonio", 4, 6, 3, "Humanos mutados por mana intensa.",
                "[{\"nivel\":1,\"nome\":\"Mutacao Demoniaca\",\"efeito\":\"Capacidade fisiologica unica\"},"
                + "{\"nivel\":20,\"nome\":\"Pactos Demoniacos\",\"efeito\":\"Sela contratos magicos\"},"
                + "{\"nivel\":40,\"nome\":\"Pacto Restritivo\",\"efeito\":\"Modifica a propria ficha por sacrificio\"}]");
        raca("GIGANTE", "Gigante", 9, 2, 3, "Enormes e resistentes.",
                "[{\"nivel\":1,\"nome\":\"Gigantismo\",\"efeito\":\"+1 vida e +1 PE por ponto de Constituicao\"},"
                + "{\"nivel\":20,\"nome\":\"Crescer Ainda Mais\",\"efeito\":\"Gasta 5 PE: +2 atributos fisicos/turno\"},"
                + "{\"nivel\":40,\"nome\":\"Casca Grossa\",\"efeito\":\"+20 Armadura Verdadeira; imune a sangramento/veneno\"}]");
        raca("GOBLIN", "Goblin", 3, 5, 6, "Pequenos, inteligentes e astutos.",
                "[{\"nivel\":1,\"nome\":\"Desenvolvimento Rapido\",\"efeito\":\"Ganha o dobro de XP\"},"
                + "{\"nivel\":20,\"nome\":\"Instintos de Sobrevivencia\",\"efeito\":\"2 vantagens p/ detectar perigo\"},"
                + "{\"nivel\":40,\"nome\":\"Mimetismo\",\"efeito\":\"Copia tecnica observada (custo dobrado)\"}]");
        raca("TIEFLING", "Tiefling", 4, 7, 4, "Demonios da mana; grandes pesquisadores.",
                "[{\"nivel\":1,\"nome\":\"Aptidao Magica\",\"efeito\":\"+20 armadura magica; 1 magia sem custo/cena\"},"
                + "{\"nivel\":20,\"nome\":\"Magia Inerente\",\"efeito\":\"Cria 1 magia unica sem slot\"},"
                + "{\"nivel\":40,\"nome\":\"Ampliar Feiticaria\",\"efeito\":\"Usa Energia como Mana; excede limite de feitico\"}]");
        raca("MEIO_FERA", "Meio-Fera", 4, 4, 6, "Hibridos com aspectos animais.",
                "[{\"nivel\":1,\"nome\":\"Heranca Bestial\",\"efeito\":\"Sentidos/sobrevivencia do animal\"},"
                + "{\"nivel\":20,\"nome\":\"Forma Bestial Completa\",\"efeito\":\"Transformacao fisica\"},"
                + "{\"nivel\":40,\"nome\":\"Renascimento da Fera\",\"efeito\":\"Nova forma/melhorias\"}]");
        raca("ELFO", "Elfo", 5, 5, 4, "Antigos, ligados a natureza e a mana.",
                "[{\"nivel\":1,\"nome\":\"Aspectos da Selva\",\"efeito\":\"+5 mana/turno em floresta; cura natural dobrada\"},"
                + "{\"nivel\":20,\"nome\":\"Visao Amplificada\",\"efeito\":\"Reduz cobertura dos alvos\"},"
                + "{\"nivel\":40,\"nome\":\"Vincular-se com a Mana\",\"efeito\":\"Lunariano/Elfo-Superior/Drow\"}]");
        raca("ONI", "Oni", 7, 3, 5, "Guerreiros fortes, abencoados por cristais.",
                "[{\"nivel\":1,\"nome\":\"Aptidao Fisica\",\"efeito\":\"+20 Armadura Fisica; 3x/dia metade do custo de PE\"},"
                + "{\"nivel\":20,\"nome\":\"Mascara Demoniaca\",\"efeito\":\"Item unico: +2 atributos, +10 pericia\"},"
                + "{\"nivel\":40,\"nome\":\"Alem do Limite\",\"efeito\":\"Sacrifica HP por acerto/dano fisico\"}]");
        raca("VAMPIRO", "Vampiro", 5, 6, 5, "Sobreviventes que consomem sangue.",
                "[{\"nivel\":1,\"nome\":\"Consumir Sangue\",\"efeito\":\"Recupera HP = dobro da Constituicao\"},"
                + "{\"nivel\":20,\"nome\":\"Afinidade Noturna\",\"efeito\":\"+4 pericias de For/Agi no escuro\"},"
                + "{\"nivel\":40,\"nome\":\"Nutricao pelo Sacrificio\",\"efeito\":\"Regenera HP gastando PE/PM\"}]");
        raca("ANAO", "Anao", 6, 4, 5, "Baixos, robustos e sabios artifices.",
                "[{\"nivel\":1,\"nome\":\"Conhecimento Antigo\",\"efeito\":\"Dominio de um tema antigo\"},"
                + "{\"nivel\":20,\"nome\":\"Resistencias Subterraneas\",\"efeito\":\"Imune a clima extremo; +20 armadura\"},"
                + "{\"nivel\":40,\"nome\":\"Mestre Artifice\",\"efeito\":\"Cria itens de qualidade superior\"}]");
        raca("ORC", "Orc", 8, 3, 7, "Guerreiros brutais da sobrevivencia do mais forte.",
                "[{\"nivel\":1,\"nome\":\"Marca de Guerra\",\"efeito\":\"Bonus contra alvo/elemento escolhido\"},"
                + "{\"nivel\":20,\"nome\":\"Insanidade de Batalha\",\"efeito\":\"+2 testes fisicos por golpe recebido\"},"
                + "{\"nivel\":40,\"nome\":\"Brutalidade\",\"efeito\":\"Multiplos ataques sacrificando HP\"}]");
        raca("DRACONATO", "Draconato", 9, 2, 5, "Herdeiros dos dragoes.",
                "[{\"nivel\":1,\"nome\":\"Descendencia Draconica\",\"efeito\":\"Sopro/heranca de dragao\"},"
                + "{\"nivel\":20,\"nome\":\"Forma Draconica\",\"efeito\":\"Asas/escamas; voo\"},"
                + "{\"nivel\":40,\"nome\":\"Ira do Dragao\",\"efeito\":\"Poder draconico ampliado\"}]");
        raca("CELESTIAL", "Celestial", 5, 6, 4, "Seres raros de origem divina.",
                "[{\"nivel\":1,\"nome\":\"Bencao Celeste\",\"efeito\":\"Afinidade com bencaos\"},"
                + "{\"nivel\":20,\"nome\":\"Asas Sagradas\",\"efeito\":\"Voo e luz sagrada\"},"
                + "{\"nivel\":40,\"nome\":\"Ascensao\",\"efeito\":\"Forma celestial plena\"}]");
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
                "Lealdade inabalavel: atributo-base como bonus em resistencia a controle.");
        classe("ESCUDEIRO", "Escudeiro", "CAVALEIRO",
                bonus("{\"constituicao\":8,\"forca\":2}", "{\"bloqueio\":5,\"vigor\":5}", 1),
                "Escudo Protetor: vantagem ao bloquear por aliados.");
        classe("COMANDANTE", "Comandante", "CAVALEIRO",
                bonus("{\"constituicao\":4,\"forca\":3,\"carisma\":2,\"sabedoria\":1}", "{\"vigor\":3,\"atletismo\":3,\"diplomacia\":2,\"percepcao\":2}", 1),
                "Presenca Inspiradora: motiva aliado com metade da Diplomacia.");

        classe("ASSASSINO", "Assassino", null,
                bonus("{\"destreza\":2,\"agilidade\":2,\"forca\":1}", "{\"combate\":2,\"acrobacia\":1,\"furtividade\":1,\"arremesso\":1}", 2),
                "Golpe silencioso: ataque a alvo desavisado e critico.");
        classe("NINJA", "Ninja", "ASSASSINO",
                bonus("{\"agilidade\":5,\"destreza\":3,\"forca\":2}", "{\"acrobacia\":4,\"furtividade\":3,\"arremesso\":3}", 3),
                "Sombra agil: +Agilidade na Acrobacia nas sombras.");
        classe("SICARIO", "Sicario", "ASSASSINO",
                bonus("{\"forca\":4,\"destreza\":4,\"agilidade\":2}", "{\"combate\":3,\"acrobacia\":3,\"iniciativa\":2,\"esquiva\":2}", 2),
                "Hemorragia: arma de perfuracao leve causa Sangramento.");

        classe("BARBARO", "Barbaro", null,
                bonus("{\"forca\":3,\"constituicao\":2}", "{\"atletismo\":3,\"vigor\":2}", 2),
                "Bruto: com metade do HP, +metade da Forca em Armadura e dano.");
        classe("BERSERKER", "Berserker", "BARBARO",
                bonus("{\"forca\":8,\"constituicao\":2}", "{\"atletismo\":8,\"vigor\":2}", 2),
                "Furia incontrolavel: +metade do Atletismo em testes fisicos.");
        classe("IMORTAL", "Imortal", "BARBARO",
                bonus("{\"constituicao\":8,\"forca\":2}", "{\"vigor\":8,\"atletismo\":2}", 2),
                "Sobrevivencia Imortal: a 0 HP continua em pe ate sofrer dano.");

        classe("CLERIGO", "Clerigo", null,
                bonus("{\"sabedoria\":4,\"constituicao\":1}", "{\"fe\":4,\"conhecimento\":1}", 3),
                "Julgamento Divino: em confronto com inimigo de divindade semelhante a sua, ganha "
                + "+2 a cada 5 pontos de Fe, somado ao ataque e as defesas referentes a bencaos.");
        classe("PALADINO", "Paladino", "CLERIGO",
                bonus("{\"constituicao\":6,\"sabedoria\":3,\"forca\":1}", "{\"vigor\":5,\"fe\":5}", 1),
                "Escudo da Fe: defende aliados com a pericia Fe.");
        classe("SANTO", "Santo", "CLERIGO",
                bonus("{\"sabedoria\":7,\"carisma\":2,\"constituicao\":1}", "{\"fe\":5,\"conhecimento\":3,\"diplomacia\":2}", 4),
                "Aura Sagrada: aliados resistem a efeitos divinos negativos.");

        classe("DRUIDA", "Druida", null,
                bonus("{\"constituicao\":2,\"sabedoria\":2,\"carisma\":1}", "{\"adestrar\":2,\"vigor\":1,\"percepcao\":1,\"sobrevivencia\":1}", 3),
                "Comunhao com a Natureza: bonus em pericias na natureza.");
        classe("ARAUTO_NATUREZA", "Arauto da Natureza", "DRUIDA",
                bonus("{\"sabedoria\":4,\"inteligencia\":4,\"carisma\":2}", "{\"magia\":4,\"conhecimento\":3,\"adestrar\":2,\"sobrevivencia\":1}", 4),
                "Filho da Magia Natural: metade da mana com Magia Natural.");
        classe("DOMADOR_FERAS", "Domador de Feras", "DRUIDA",
                bonus("{\"constituicao\":3,\"sabedoria\":3,\"carisma\":3,\"forca\":1}", "{\"adestrar\":5,\"sobrevivencia\":3,\"vigor\":2}", 3),
                "Sintonia Animal: feras ganham Adestrar em HP.");

        classe("MONGE", "Monge", null,
                bonus("{\"constituicao\":2,\"sabedoria\":1,\"forca\":1,\"destreza\":1}", "{\"combate\":2,\"virtude\":2,\"conhecimento\":1}", 2),
                "Corpo Iluminado: +metade do Vigor em resistencia; imunidades.");
        classe("SABIO", "Sabio", "MONGE",
                bonus("{\"sabedoria\":5,\"constituicao\":3,\"inteligencia\":2}", "{\"conhecimento\":4,\"virtude\":3,\"percepcao\":2,\"medicina\":1}", 2),
                "Sabedoria Ancestral: aliados com vantagem em Percepcao/Conhecimento.");
        classe("PUNHOS_ACO", "Punhos de Aco", "MONGE",
                bonus("{\"constituicao\":3,\"forca\":3,\"destreza\":3,\"sabedoria\":1}", "{\"combate\":4,\"virtude\":3,\"bloqueio\":2,\"sobrevivencia\":1}", 1),
                "Golpes Rigidos: ataque desarmado ignora reducao de dano.");

        classe("ARQUEIRO", "Arqueiro", null,
                bonus("{\"destreza\":3,\"agilidade\":2}", "{\"pontaria\":3,\"furtividade\":2}", 2),
                "Olhos de Aguia: critico a longa distancia; anula cobertura.");
        classe("CACADOR", "Cacador", "ARQUEIRO",
                bonus("{\"destreza\":5,\"agilidade\":4,\"sabedoria\":1}", "{\"pontaria\":4,\"iniciativa\":3,\"sobrevivencia\":3}", 3),
                "Instinto de Caca: +3 margem de critico contra o alvo.");
        classe("MERCENARIO", "Mercenario", "ARQUEIRO",
                bonus("{\"destreza\":3,\"agilidade\":3,\"forca\":3,\"sabedoria\":1}", "{\"combate\":3,\"pontaria\":3,\"iniciativa\":2,\"sobrevivencia\":2}", 4),
                "Instinto de Sobrevivencia: +1 reacao por inimigo enfrentando.");

        classe("MAGO", "Mago", null,
                bonus("{\"inteligencia\":4,\"sabedoria\":1}", "{\"magia\":3,\"conhecimento\":2}", 5),
                "Mente Arcana: Vantagem em testes de Feiticaria para reagir e/ou identificar feiticos. "
                + "1x/dia usa o Foco Arcano: +1 feitico em uma acao padrao (+1 a cada 10 niveis).");
        classe("ARQUIMAGO", "Arquimago", "MAGO",
                bonus("{\"inteligencia\":8,\"sabedoria\":2}", "{\"magia\":8,\"conhecimento\":2}", 4),
                "Maestria Arcana: lanca 2 feiticos ao mesmo tempo.");
        classe("RUNICISTA", "Runicista", "MAGO",
                bonus("{\"inteligencia\":7,\"sabedoria\":3}", "{\"magia\":7,\"conhecimento\":3}", 4),
                "Maestria Runica: cria/anula runas em objetos.");

        classe("ESPADACHIM", "Espadachim", null,
                bonus("{\"destreza\":3,\"agilidade\":2}", "{\"combate\":2,\"iniciativa\":2,\"esquiva\":1}", 2),
                "Mestre de Laminas: defende com Combate contra laminas.");
        classe("DUELISTA", "Duelista", "ESPADACHIM",
                bonus("{\"destreza\":5,\"agilidade\":3,\"forca\":2}", "{\"combate\":4,\"iniciativa\":3,\"esquiva\":3}", 3),
                "Duelo Eximio: +2/10 niveis em combate 1 contra 1.");
        classe("SAMURAI", "Samurai", "ESPADACHIM",
                bonus("{\"destreza\":4,\"agilidade\":3,\"constituicao\":3}", "{\"combate\":4,\"iniciativa\":3,\"vigor\":3}", 3),
                "Caminho da Lamina: +2/10 Destreza no Combate com Katana.");

        classe("LADRAO", "Ladrao", null,
                bonus("{\"agilidade\":4,\"destreza\":1}", "{\"furtividade\":4,\"esquiva\":1}", 5),
                "Mao Leve: vantagem em Crime furtivo; loot extra.");
        classe("SOMBRA", "Sombra", "LADRAO",
                bonus("{\"agilidade\":7,\"destreza\":3}", "{\"furtividade\":7,\"acrobacia\":3}", 4),
                "Ofuscado pela Escuridao: invisibilidade parcial nas sombras.");
        classe("PIRATA", "Pirata", "LADRAO",
                bonus("{\"agilidade\":3,\"destreza\":3,\"forca\":3,\"constituicao\":1}", "{\"combate\":3,\"acrobacia\":3,\"vigor\":3,\"pontaria\":1}", 3),
                "Olho Atento ao Mar: +2/5 Percepcao p/ tesouros.");

        classe("CURANDEIRO", "Curandeiro", null,
                bonus("{\"sabedoria\":5}", "{\"medicina\":4,\"conhecimento\":1}", 3),
                "Toque Curativo: cura dobrada em outros.");
        classe("SACERDOTE", "Sacerdote", "CURANDEIRO",
                bonus("{\"sabedoria\":9,\"inteligencia\":1}", "{\"medicina\":9,\"conhecimento\":1}", 3),
                "Aura de Cura: cura em area e limpa efeitos negativos.");
        classe("MESTRE_TOXINAS", "Mestre de Toxinas", "CURANDEIRO",
                bonus("{\"sabedoria\":6,\"inteligencia\":4}", "{\"medicina\":4,\"alquimia\":4,\"magia\":2}", 3),
                "Mao Venenosa: imune a venenos; veneno dobrado.");

        classe("BRUXO", "Bruxo", null,
                bonus("{\"forca\":3,\"inteligencia\":2}", "{\"combate\":2,\"magia\":2,\"conhecimento\":1}", 4),
                "Pacto Sombrio: vinculo com espirito invocavel.");
        classe("NECROMANTE", "Necromante", "BRUXO",
                bonus("{\"inteligencia\":7,\"sabedoria\":3}", "{\"magia\":4,\"conhecimento\":3,\"virtude\":3}", 3),
                "Lideranca Putrefata: morto-seguidor sem custo.");
        classe("MISTICO", "Mistico", "BRUXO",
                bonus("{\"forca\":4,\"inteligencia\":4,\"destreza\":2}", "{\"combate\":5,\"magia\":4,\"esquiva\":1}", 2),
                "Ligacao Espiritual: consome espiritos por bonus.");

        classe("ALQUIMISTA", "Alquimista", null,
                bonus("{\"inteligencia\":5}", "{\"alquimia\":4,\"conhecimento\":1}", 3),
                "Maestria Alquimica: +10 Alquimia fora de combate.");
        classe("QUIMICO_ARCANO", "Quimico Arcano", "ALQUIMISTA",
                bonus("{\"inteligencia\":7,\"sabedoria\":3}", "{\"alquimia\":7,\"magia\":2,\"engenharia\":1}", 2),
                "Sintese Arcana: transmuta organicos em Mana.");
        classe("INVENTOR", "Inventor", "ALQUIMISTA",
                bonus("{\"inteligencia\":6,\"sabedoria\":4}", "{\"engenharia\":8,\"alquimia\":2}", 4),
                "Engenharia Mecanica: +10 Engenharia em oficina.");

        classe("LANCEIRO", "Lanceiro", null,
                bonus("{\"forca\":3,\"destreza\":2}", "{\"combate\":4,\"atletismo\":1}", 3),
                "Precisao de Lanca: +2/10 Destreza com lanca.");
        classe("GENERAL_CEUS", "General dos Ceus", "LANCEIRO",
                bonus("{\"forca\":4,\"destreza\":4,\"constituicao\":2}", "{\"combate\":5,\"atletismo\":3,\"vigor\":2}", 2),
                "Dominio Aereo: critico a distancia; dobro vs aereos.");
        classe("VALQUIRIA", "Valquiria", "LANCEIRO",
                bonus("{\"forca\":5,\"destreza\":5,\"constituicao\":5}", "{\"combate\":5,\"atletismo\":5,\"vigor\":5}", 3),
                "Elegancia de Guerra: +2/10 Forca com lanca.");

        classe("LUTADOR", "Lutador", null,
                bonus("{\"forca\":2,\"agilidade\":1,\"constituicao\":1,\"destreza\":1}", "{\"combate\":3,\"vigor\":1,\"bloqueio\":1}", 2),
                "Tenacidade de Batalha: armadura leve dobrada.");
        classe("CAMPEAO", "Campeao", "LUTADOR",
                bonus("{\"forca\":4,\"constituicao\":3,\"carisma\":3}", "{\"combate\":3,\"vigor\":3,\"aparencia\":3,\"diplomacia\":1}", 2),
                "Eu sou o Campeao: +2/10 Carisma em dano desarmado.");
        classe("ARTISTA_MARCIAL", "Artista Marcial", "LUTADOR",
                bonus("{\"forca\":3,\"agilidade\":3,\"constituicao\":3,\"destreza\":1}", "{\"combate\":5,\"bloqueio\":2,\"esquiva\":2,\"iniciativa\":1}", 3),
                "Harmonia do Combate: +5 Combate a cada novo alvo.");

        classe("VIAJANTE", "Viajante", null,
                bonus("{\"carisma\":3,\"sabedoria\":2}", "{\"enganacao\":2,\"aparencia\":2,\"conhecimento\":1}", 1),
                "Conhecimento Itinerante: aprende habilidade local.");
        classe("BARDO", "Bardo", "VIAJANTE",
                bonus("{\"carisma\":8,\"destreza\":2}", "{\"artes\":8,\"aparencia\":2}", 2),
                "Inspiracao Continua: dobra o efeito da proxima acao do aliado.");
        classe("CIGANO", "Cigano", "VIAJANTE",
                bonus("{\"carisma\":5,\"destreza\":5}", "{\"artes\":5,\"acrobacia\":5}", 3),
                "Leitura do Destino: +2/5 Destreza em reacao ao alvo.");
        classe("CARTOMANTE", "Cartomante", "VIAJANTE",
                bonus("{\"destreza\":7,\"inteligencia\":3}", "{\"enganacao\":5,\"arremesso\":5}", 4),
                "Cartas Arcanas: cartas aplicam efeitos negativos (1d6).");
        classe("PINTOR", "Pintor", "VIAJANTE",
                bonus("{\"destreza\":6,\"carisma\":4}", "{\"artes\":10}", 2),
                "Inspiracao Artistica: obra concede metade de Artes em atributo.");
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
