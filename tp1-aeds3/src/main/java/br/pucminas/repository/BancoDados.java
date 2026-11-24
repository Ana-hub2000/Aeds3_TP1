package br.pucminas.repository;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import aed3.Arquivo;
import aed3.ArvoreBMais;
import aed3.InterfaceArvoreBMais;
import aed3.ParIdId;
import br.pucminas.model.Avaliacao;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Resposta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.IndicePalavrasChave;
import br.pucminas.persistence.ParDataId;
import br.pucminas.persistence.ParEmailId;

/*
 Camada de acesso a dados.

 Arquivos de dados (Arquivo<T>, do professor): CRUD com lapide, indicador de
 tamanho e indice direto por ID em hash extensivel.
  - usuarios, perguntas, respostas, avaliacoes

 Indices (ArvoreBMais<T>, do professor):
  - e-mail -> usuario                       (ParEmailId)
  - usuario -> perguntas                    (ParIdId)
  - data de criacao -> perguntas            (ParDataId, ordem decrescente)
  - pergunta -> respostas                   (ParIdId)  relacionamento N:N
  - usuario -> respostas                    (ParIdId)  relacionamento N:N
  - usuario -> avaliacoes                   (ParIdId)
  - resposta -> avaliacoes                  (ParIdId)

 Indice invertido (ListaInvertida, do professor), por meio de IndicePalavrasChave:
  - palavra-chave -> perguntas, ranqueadas por TF x IDF

 Nada e apagado por "arquivar": perguntas e respostas arquivadas tem ativa = false
 e continuam nos arquivos e nos indices; as consultas filtram por esse campo.
*/
public class BancoDados implements AutoCloseable {

    private static final int ORDEM_ARVORE = 5;
    private static final int MAX_CARACTERES_RESPOSTA = 20000;

    private final Arquivo<Usuario> usuarios;
    private final Arquivo<Pergunta> perguntas;
    private final Arquivo<Resposta> respostas;
    private final Arquivo<Avaliacao> avaliacoes;

    private final ArvoreBMais<ParEmailId> indiceEmail;
    private final ArvoreBMais<ParIdId> indicePerguntasPorUsuario;
    private final ArvoreBMais<ParDataId> indicePerguntasPorData;
    private final ArvoreBMais<ParIdId> indiceRespostasPorPergunta;
    private final ArvoreBMais<ParIdId> indiceRespostasPorUsuario;
    private final ArvoreBMais<ParIdId> indiceAvaliacoesPorUsuario;
    private final ArvoreBMais<ParIdId> indiceAvaliacoesPorResposta;

    private final IndicePalavrasChave indicePalavrasChave;

    public BancoDados(Path diretorio) throws Exception {
        Files.createDirectories(diretorio);
        String base = diretorio.toString();

        usuarios = new Arquivo<>(base, "usuarios", Usuario.class.getConstructor());
        perguntas = new Arquivo<>(base, "perguntas", Pergunta.class.getConstructor());
        respostas = new Arquivo<>(base, "respostas", Resposta.class.getConstructor());
        avaliacoes = new Arquivo<>(base, "avaliacoes", Avaliacao.class.getConstructor());

        indiceEmail = abrirArvore(ParEmailId.class.getConstructor(), diretorio, "indiceEmail.bplus");
        indicePerguntasPorUsuario = abrirArvore(ParIdId.class.getConstructor(), diretorio, "indicePerguntas.bplus");
        indicePerguntasPorData = abrirArvore(ParDataId.class.getConstructor(), diretorio, "indicePerguntasData.bplus");
        indiceRespostasPorPergunta = abrirArvore(ParIdId.class.getConstructor(), diretorio, "indiceRespostasPergunta.bplus");
        indiceRespostasPorUsuario = abrirArvore(ParIdId.class.getConstructor(), diretorio, "indiceRespostasUsuario.bplus");
        indiceAvaliacoesPorUsuario = abrirArvore(ParIdId.class.getConstructor(), diretorio, "indiceAvaliacoesPorUsuario.bplus");
        indiceAvaliacoesPorResposta = abrirArvore(ParIdId.class.getConstructor(), diretorio, "indiceAvaliacoesPorResposta.bplus");

        reconstruirIndicesSeVazios();

        indicePalavrasChave = new IndicePalavrasChave(diretorio);
        if (indicePalavrasChave.precisaReconstruir()) {
            // Indexa tambem as perguntas que ja existiam antes do indice invertido.
            for (Pergunta pergunta : perguntas.readAll()) {
                indicePalavrasChave.incluir(pergunta);
            }
        }
    }

    // ==================== usuarios ====================

    /**
     * Utiliza o e-mail como chave para encontrar o par e-mail-id na árvore B+.
     *
     * @param email e-mail do usuário procurado
     * @return o usuário encontrado, ou null se não existir
     */
    public Usuario buscarUsuarioPorEmail(String email) {
        try {
            String chave = normalizarEmail(email);
            if (chave.isEmpty()) {
                return null;
            }
            List<ParEmailId> achados = indiceEmail.read(new ParEmailId(chave));
            if (achados.isEmpty()) {
                return null;
            }
            return usuarios.read(achados.get(0).getId());
        } catch (Exception e) {
            throw falha("buscar usuário", e);
        }
    }

    public Usuario buscarUsuario(int id) {
        try {
            return usuarios.read(id);
        } catch (Exception e) {
            throw falha("ler usuário", e);
        }
    }

    public Usuario criarUsuario(Usuario usuario) {
        try {
            usuario.setEmail(normalizarEmail(usuario.getEmail()));
            if (buscarUsuarioPorEmail(usuario.getEmail()) != null) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }
            usuarios.create(usuario);
            indiceEmail.create(new ParEmailId(usuario.getEmail(), usuario.getIdUsuario()));
            return usuario;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("criar usuário", e);
        }
    }

    public void atualizarUsuario(Usuario usuario, String emailAnterior) {
        try {
            usuario.setEmail(normalizarEmail(usuario.getEmail()));
            Usuario outro = buscarUsuarioPorEmail(usuario.getEmail());
            // Se outro usuário já usa este e-mail, a alteração não é permitida.
            if (outro != null && outro.getIdUsuario() != usuario.getIdUsuario()) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }
            usuarios.update(usuario);

            String anterior = normalizarEmail(emailAnterior);
            if (!anterior.equals(usuario.getEmail())) {
                indiceEmail.delete(new ParEmailId(anterior, usuario.getIdUsuario()));
                indiceEmail.create(new ParEmailId(usuario.getEmail(), usuario.getIdUsuario()));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("atualizar usuário", e);
        }
    }

    // ==================== perguntas ====================

    public Pergunta criarPergunta(Pergunta pergunta) {
        try {
            // Toda pergunta pertence a um usuário existente.
            if (buscarUsuario(pergunta.getIdUsuario()) == null) {
                throw new IllegalArgumentException("Usuário informado não existe.");
            }
            perguntas.create(pergunta);
            indicePerguntasPorUsuario.create(
                    new ParIdId(pergunta.getIdUsuario(), pergunta.getIdPergunta()));
            // A data de criação nunca muda, então o índice por data não precisa de update.
            indicePerguntasPorData.create(
                    new ParDataId(pergunta.getCriacao(), pergunta.getIdPergunta()));
            indicePalavrasChave.incluir(pergunta);
            return pergunta;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("criar pergunta", e);
        }
    }

    public void atualizarPergunta(Pergunta pergunta) {
        try {
            Pergunta anterior = perguntas.read(pergunta.getIdPergunta());
            if (anterior == null || !perguntas.update(pergunta)) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
            // Retira os termos antigos antes de calcular o TF das palavras novas.
            // Também cobre arquivar (ativa = false) e reativar uma pergunta.
            indicePalavrasChave.remover(anterior);
            indicePalavrasChave.incluir(pergunta);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("atualizar pergunta", e);
        }
    }

    /** Exclusão física de uma pergunta (o sistema usa arquivamento; isto é para manutenção). */
    public void excluirPergunta(int idPergunta) {
        try {
            Pergunta pergunta = perguntas.read(idPergunta);
            if (pergunta == null || !perguntas.delete(idPergunta)) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
            indicePalavrasChave.remover(pergunta);
            indicePerguntasPorUsuario.delete(new ParIdId(pergunta.getIdUsuario(), idPergunta));
            indicePerguntasPorData.delete(new ParDataId(pergunta.getCriacao(), idPergunta));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("excluir pergunta", e);
        }
    }

    /** Lê uma pergunta pelo ID (null se não existir). */
    public Pergunta buscarPergunta(int id) {
        try {
            return perguntas.read(id);
        } catch (Exception e) {
            throw falha("ler pergunta", e);
        }
    }

    /** Somente as perguntas ativas do usuário. */
    public List<Pergunta> perguntasDoUsuario(int idUsuario) {
        return perguntasDoUsuario(idUsuario, false);
    }

    /**
     * Lista as perguntas do usuário usando a árvore B+, da mais antiga para a mais nova.
     * O próprio autor pode ver também as arquivadas (lápide lógica "ativa = false").
     */
    public List<Pergunta> perguntasDoUsuario(int idUsuario, boolean incluirArquivadas) {
        try {
            List<Pergunta> lista = new ArrayList<>();
            for (ParIdId par : indicePerguntasPorUsuario.read(new ParIdId(idUsuario))) {
                Pergunta pergunta = perguntas.read(par.getId2());
                if (pergunta != null && (incluirArquivadas || pergunta.isAtiva())) {
                    lista.add(pergunta);
                }
            }
            lista.sort(Comparator.comparingLong(Pergunta::getCriacao));
            return lista;
        } catch (Exception e) {
            throw falha("listar perguntas", e);
        }
    }

    /**
     * Todas as perguntas ATIVAS em ordem decrescente de data.
     * A ordem vem direto da árvore B+ (ParDataId tem compareTo decrescente);
     * read(null) percorre todas as folhas encadeadas da esquerda para a direita.
     */
    public List<Pergunta> perguntasRecentes() {
        try {
            List<Pergunta> lista = new ArrayList<>();
            for (ParDataId par : indicePerguntasPorData.read(null)) {
                Pergunta pergunta = perguntas.read(par.getId());
                if (pergunta != null && pergunta.isAtiva()) {
                    lista.add(pergunta);
                }
            }
            return lista;
        } catch (Exception e) {
            throw falha("listar perguntas recentes", e);
        }
    }

    /**
     * Busca por palavras-chave com ranking TF x IDF (ver IndicePalavrasChave).
     *
     * @return IDs das perguntas, da mais relevante para a menos relevante
     */
    public List<Integer> buscarPerguntasPorPalavrasChave(String consulta) {
        try {
            return indicePalavrasChave.buscar(consulta);
        } catch (Exception e) {
            throw falha("buscar perguntas por palavras-chave", e);
        }
    }

    // ==================== respostas ====================

    /**
     * Cria uma resposta para uma pergunta, escrita por um usuário.
     * Valida o texto, a pergunta (precisa existir e estar ativa) e o usuário,
     * e registra a resposta nos dois índices do relacionamento N:N.
     */
    public Resposta criarResposta(Resposta resposta) {
        if (resposta == null) {
            throw new IllegalArgumentException("Resposta inválida.");
        }
        try {
            String texto = resposta.getResposta();
            if (texto == null || texto.isBlank()) {
                throw new IllegalArgumentException("A resposta não pode ser vazia.");
            }
            if (texto.length() > MAX_CARACTERES_RESPOSTA) {
                throw new IllegalArgumentException("Resposta longa demais (máximo de "
                        + MAX_CARACTERES_RESPOSTA + " caracteres).");
            }
            Pergunta pergunta = perguntas.read(resposta.getIdPergunta());
            if (pergunta == null) {
                throw new IllegalArgumentException("A pergunta informada não existe.");
            }
            if (!pergunta.isAtiva()) {
                throw new IllegalArgumentException("A pergunta está arquivada.");
            }
            if (usuarios.read(resposta.getIdUsuario()) == null) {
                throw new IllegalArgumentException("O usuário informado não existe.");
            }

            respostas.create(resposta);
            indiceRespostasPorPergunta.create(
                    new ParIdId(resposta.getIdPergunta(), resposta.getIdResposta()));
            indiceRespostasPorUsuario.create(
                    new ParIdId(resposta.getIdUsuario(), resposta.getIdResposta()));
            return resposta;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("criar resposta", e);
        }
    }

    /** Lê uma resposta pelo ID, mesmo arquivada (null se não existir). */
    public Resposta buscarResposta(int id) {
        try {
            return respostas.read(id);
        } catch (Exception e) {
            throw falha("ler resposta", e);
        }
    }

    /** Regrava uma resposta existente (usado para nota, avaliações e arquivamento). */
    public void atualizarResposta(Resposta resposta) {
        if (resposta == null) {
            throw new IllegalArgumentException("Resposta inválida.");
        }
        try {
            if (respostas.read(resposta.getIdResposta()) == null || !respostas.update(resposta)) {
                throw new IllegalArgumentException("Resposta não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("atualizar resposta", e);
        }
    }

    /**
     * Arquiva uma resposta sem apagar o registro (ativa = false).
     * Permitido ao autor da resposta e ao autor da pergunta respondida.
     */
    public void arquivarResposta(int idResposta, int idUsuarioSolicitante) {
        try {
            Resposta resposta = respostas.read(idResposta);
            if (resposta == null || !resposta.isAtiva()) {
                throw new IllegalArgumentException("Resposta não encontrada.");
            }

            Pergunta pergunta = perguntas.read(resposta.getIdPergunta());
            boolean autorDaResposta = resposta.getIdUsuario() == idUsuarioSolicitante;
            boolean autorDaPergunta = pergunta != null
                    && pergunta.getIdUsuario() == idUsuarioSolicitante;
            if (!autorDaResposta && !autorDaPergunta) {
                throw new IllegalArgumentException("Sem permissão para arquivar esta resposta.");
            }

            resposta.setAtiva(false);
            if (!respostas.update(resposta)) {
                throw new IllegalArgumentException("Resposta não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("arquivar resposta", e);
        }
    }

    /**
     * Respostas ATIVAS de uma pergunta, da maior nota para a menor
     * (empate: a mais antiga primeiro). Usa a árvore B+ [idPergunta, idResposta].
     */
    public List<Resposta> respostasDaPergunta(int idPergunta) {
        try {
            List<Resposta> lista = new ArrayList<>();
            for (ParIdId par : indiceRespostasPorPergunta.read(new ParIdId(idPergunta))) {
                Resposta resposta = respostas.read(par.getId2());
                if (resposta != null && resposta.isAtiva()) {
                    lista.add(resposta);
                }
            }
            lista.sort(Comparator.comparingInt(Resposta::getNota).reversed()
                    .thenComparingLong(Resposta::getCriacao)
                    .thenComparingInt(Resposta::getIdResposta));
            return lista;
        } catch (Exception e) {
            throw falha("listar respostas da pergunta", e);
        }
    }

    /**
     * Respostas ATIVAS escritas por um usuário, da mais antiga para a mais recente.
     * Usa a árvore B+ [idUsuario, idResposta].
     */
    public List<Resposta> respostasDoUsuario(int idUsuario) {
        try {
            List<Resposta> lista = new ArrayList<>();
            for (ParIdId par : indiceRespostasPorUsuario.read(new ParIdId(idUsuario))) {
                Resposta resposta = respostas.read(par.getId2());
                if (resposta != null && resposta.isAtiva()) {
                    lista.add(resposta);
                }
            }
            lista.sort(Comparator.comparingLong(Resposta::getCriacao)
                    .thenComparingInt(Resposta::getIdResposta));
            return lista;
        } catch (Exception e) {
            throw falha("listar respostas do usuário", e);
        }
    }

    // ==================== avaliacoes ====================

    /**
     * Registra a avaliação de um usuário sobre uma resposta e ajusta a nota
     * (+1 se positiva, -1 se negativa) e o total de avaliações da resposta.
     * Um usuário não avalia a própria resposta nem a mesma resposta duas vezes.
     */
    public Avaliacao criarAvaliacao(Avaliacao avaliacao) {
        if (avaliacao == null) {
            throw new IllegalArgumentException("Avaliação inválida.");
        }
        try {
            int idUsuario = avaliacao.get_IdUsuario();
            int idResposta = avaliacao.get_IdResposta();

            if (buscarUsuario(idUsuario) == null) {
                throw new IllegalArgumentException("Usuário informado não existe.");
            }
            Resposta resposta = buscarResposta(idResposta);
            if (resposta == null || !resposta.isAtiva()) {
                throw new IllegalArgumentException("Resposta não encontrada.");
            }
            if (resposta.getIdUsuario() == idUsuario) {
                throw new IllegalArgumentException("Você não pode avaliar a sua própria resposta.");
            }
            for (Avaliacao anterior : avaliacoesDoUsuario(idUsuario)) {
                if (anterior.get_IdResposta() == idResposta) {
                    throw new IllegalArgumentException("Você já avaliou esta resposta.");
                }
            }

            avaliacoes.create(avaliacao);
            indiceAvaliacoesPorUsuario.create(new ParIdId(idUsuario, avaliacao.getId()));
            indiceAvaliacoesPorResposta.create(new ParIdId(idResposta, avaliacao.getId()));
            ajustarNota(resposta, avaliacao.get_avaliacao() ? 1 : -1, 1);
            return avaliacao;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("criar avaliação", e);
        }
    }

    public Avaliacao buscarAvaliacao(int id) {
        try {
            return avaliacoes.read(id);
        } catch (Exception e) {
            throw falha("buscar avaliação", e);
        }
    }

    /** Avaliações feitas por um usuário (usa a árvore B+ [idUsuario, idAvaliacao]). */
    public List<Avaliacao> avaliacoesDoUsuario(int idUsuario) {
        try {
            List<Avaliacao> lista = new ArrayList<>();
            for (ParIdId par : indiceAvaliacoesPorUsuario.read(new ParIdId(idUsuario))) {
                Avaliacao avaliacao = avaliacoes.read(par.getId2());
                if (avaliacao != null) {
                    lista.add(avaliacao);
                }
            }
            return lista;
        } catch (Exception e) {
            throw falha("listar avaliações do usuário", e);
        }
    }

    /**
     * Cancela uma avaliação do próprio usuário e desfaz o efeito dela
     * na nota e no total de avaliações da resposta.
     */
    public void cancelarAvaliacao(int idAvaliacao, int idUsuario) {
        try {
            Avaliacao avaliacao = avaliacoes.read(idAvaliacao);
            if (avaliacao == null || avaliacao.get_IdUsuario() != idUsuario) {
                throw new IllegalArgumentException("Avaliação não encontrada.");
            }
            Resposta resposta = respostas.read(avaliacao.get_IdResposta());

            avaliacoes.delete(idAvaliacao);
            indiceAvaliacoesPorUsuario.delete(new ParIdId(avaliacao.get_IdUsuario(), avaliacao.getId()));
            indiceAvaliacoesPorResposta.delete(new ParIdId(avaliacao.get_IdResposta(), avaliacao.getId()));

            if (resposta != null) {
                ajustarNota(resposta, avaliacao.get_avaliacao() ? -1 : 1, -1);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("cancelar avaliação", e);
        }
    }

    /** Soma (ou subtrai) nota e total de avaliações de uma resposta e regrava o registro. */
    private void ajustarNota(Resposta resposta, int variacaoNota, int variacaoAvaliacoes) throws Exception {
        resposta.setNota((short) (resposta.getNota() + variacaoNota));
        resposta.setAvaliacoes((short) (resposta.getAvaliacoes() + variacaoAvaliacoes));
        respostas.update(resposta);
    }

    // ==================== apoio ====================

    private static <T extends InterfaceArvoreBMais<T>> ArvoreBMais<T> abrirArvore(
            Constructor<T> construtor, Path diretorio, String arquivo) throws Exception {
        return new ArvoreBMais<>(construtor, ORDEM_ARVORE, diretorio.resolve(arquivo).toString());
    }

    // Se os índices estiverem vazios (primeira execução ou arquivo apagado),
    // eles são montados novamente a partir dos arquivos de dados.
    private void reconstruirIndicesSeVazios() throws Exception {
        if (indiceEmail.empty()) {
            for (Usuario usuario : usuarios.readAll()) {
                indiceEmail.create(new ParEmailId(normalizarEmail(usuario.getEmail()),
                        usuario.getIdUsuario()));
            }
        }
        if (indicePerguntasPorUsuario.empty()) {
            for (Pergunta pergunta : perguntas.readAll()) {
                indicePerguntasPorUsuario.create(
                        new ParIdId(pergunta.getIdUsuario(), pergunta.getIdPergunta()));
            }
        }
        if (indicePerguntasPorData.empty()) {
            for (Pergunta pergunta : perguntas.readAll()) {
                indicePerguntasPorData.create(
                        new ParDataId(pergunta.getCriacao(), pergunta.getIdPergunta()));
            }
        }
        if (indiceRespostasPorPergunta.empty()) {
            for (Resposta resposta : respostas.readAll()) {
                indiceRespostasPorPergunta.create(
                        new ParIdId(resposta.getIdPergunta(), resposta.getIdResposta()));
            }
        }
        if (indiceRespostasPorUsuario.empty()) {
            for (Resposta resposta : respostas.readAll()) {
                indiceRespostasPorUsuario.create(
                        new ParIdId(resposta.getIdUsuario(), resposta.getIdResposta()));
            }
        }
        if (indiceAvaliacoesPorUsuario.empty()) {
            for (Avaliacao avaliacao : avaliacoes.readAll()) {
                indiceAvaliacoesPorUsuario.create(
                        new ParIdId(avaliacao.get_IdUsuario(), avaliacao.getId()));
            }
        }
        if (indiceAvaliacoesPorResposta.empty()) {
            for (Avaliacao avaliacao : avaliacoes.readAll()) {
                indiceAvaliacoesPorResposta.create(
                        new ParIdId(avaliacao.get_IdResposta(), avaliacao.getId()));
            }
        }
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalStateException falha(String operacao, Exception e) {
        return new IllegalStateException("Falha ao " + operacao + ".", e);
    }

    @FunctionalInterface
    private interface Fechamento {
        void fechar() throws Exception;
    }

    /** Fecha todos os recursos, mesmo que algum falhe, e relança a primeira falha. */
    @Override
    public void close() throws Exception {
        List<Fechamento> recursos = List.of(usuarios::close, perguntas::close, respostas::close,
                avaliacoes::close, indicePalavrasChave::close);
        Exception primeiraFalha = null;
        for (Fechamento recurso : recursos) {
            try {
                recurso.fechar();
            } catch (Exception e) {
                if (primeiraFalha == null) {
                    primeiraFalha = e;
                } else {
                    primeiraFalha.addSuppressed(e);
                }
            }
        }
        if (primeiraFalha != null) {
            throw primeiraFalha;
        }
    }
}
