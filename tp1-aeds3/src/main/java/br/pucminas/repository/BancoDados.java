package br.pucminas.repository;

import aed3.Arquivo;
import aed3.ArvoreBMais;
import aed3.ParIdId;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Resposta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.ParDataId;
import br.pucminas.persistence.ParEmailId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/*
 Camada de acesso a dados.

 Estruturas usadas (todas vindas do pacote aed3, do professor):
  - Arquivo<T>          -> CRUD com lapide, indicador de tamanho (short) e
                           indice direto por ID em hash extensivel
  - ArvoreBMais<ParIdId>   -> indice indireto usuario -> perguntas
  - ArvoreBMais<ParEmailId>-> indice indireto e-mail -> usuario
  - ArvoreBMais<ParDataId> -> (TP2) perguntas em ordem decrescente de data
*/
public class BancoDados implements AutoCloseable {

    private static final int ORDEM_ARVORE = 5;

    private final Arquivo<Usuario> usuarios;
    private final Arquivo<Pergunta> perguntas;
    private final ArvoreBMais<ParEmailId> indiceEmail;
    private final ArvoreBMais<ParIdId> indicePerguntasPorUsuario;
    private final ArvoreBMais<ParDataId> indicePerguntasPorData;

    // TP2 - Pessoa 2: respostas e os dois indices do relacionamento N:N
    private static final int MAX_CARACTERES_RESPOSTA = 20000;
    private final Arquivo<Resposta> respostas;
    private final ArvoreBMais<ParIdId> indiceRespostasPorPergunta;
    private final ArvoreBMais<ParIdId> indiceRespostasPorUsuario;

    public BancoDados(Path diretorio) throws Exception {
        Files.createDirectories(diretorio);
        String base = diretorio.toString();

        usuarios = new Arquivo<>(base, "usuarios", Usuario.class.getConstructor());
        perguntas = new Arquivo<>(base, "perguntas", Pergunta.class.getConstructor());

        indiceEmail = new ArvoreBMais<>(ParEmailId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceEmail.bplus").toString());
        indicePerguntasPorUsuario = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indicePerguntas.bplus").toString());
        indicePerguntasPorData = new ArvoreBMais<>(ParDataId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indicePerguntasData.bplus").toString());

        // TP2 - Pessoa 2: respostas (arquivo + indices [idPergunta, idResposta] e [idUsuario, idResposta])
        respostas = new Arquivo<>(base, "respostas", Resposta.class.getConstructor());
        indiceRespostasPorPergunta = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceRespostasPorPergunta.bplus").toString());
        indiceRespostasPorUsuario = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceRespostasPorUsuario.bplus").toString());


        reconstruirIndicesSeVazios();
    }

    // -------------------- usuarios --------------------

    /**
     * Utiliza email como chave para encontrar os pares email-id existentes, 
     * por meio da ArvoreBMais e dos métodos do repositório de aeds3
     * 
     * @param email do registro a ser encontrado
     * @return A entidade encontrada, ou null se não existir 
     * @throws Exception Se houver erro ao deserializar ou ler do arquivo
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
            // se hover usuário com o mesmo email não permite a modificação
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

    // -------------------- perguntas --------------------

    public Pergunta criarPergunta(Pergunta pergunta) {
        try {
            // O enunciado pede que toda pergunta pertenca a um usuario existente
            if (buscarUsuario(pergunta.getIdUsuario()) == null) {
                throw new IllegalArgumentException("Usuário informado não existe.");
            }
            perguntas.create(pergunta);
            indicePerguntasPorUsuario.create(
                    new ParIdId(pergunta.getIdUsuario(), pergunta.getIdPergunta()));
            // TP2: índice por data (a data de criação nunca muda, então não há update)
            indicePerguntasPorData.create(
                    new ParDataId(pergunta.getCriacao(), pergunta.getIdPergunta()));
            return pergunta;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("criar pergunta", e);
        }
    }

    public void atualizarPergunta(Pergunta pergunta) {
        try {
            if (!perguntas.update(pergunta)) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("atualizar pergunta", e);
        }
    }

    /** Somente as perguntas ativas do usuário. */
    public List<Pergunta> perguntasDoUsuario(int idUsuario) {
        return perguntasDoUsuario(idUsuario, false);
    }

    /**
     * Lista as perguntas do usuário usando a árvore B+.
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

    /** Lê uma pergunta pelo ID (null se não existir). */
    public Pergunta buscarPergunta(int id) {
        try {
            return perguntas.read(id);
        } catch (Exception e) {
            throw falha("ler pergunta", e);
        }
    }

    /**
     * TP2 - Pessoa 1: todas as perguntas ATIVAS em ordem decrescente de data.
     * A ordem vem direto da Árvore B+ (ParDataId tem compareTo decrescente);
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

    // -------------------- respostas (TP2 - Pessoa 2) --------------------

    /**
     * Cria uma resposta para uma pergunta, escrita por um usuario.
     * Valida o texto, o usuario e a pergunta (que precisa existir e estar ativa)
     * e registra a resposta nos dois indices do relacionamento N:N.
     */
    public Resposta criarResposta(Resposta resposta) {
        try {
            String texto = resposta.getResposta();
            if (texto == null || texto.isBlank()) {
                throw new IllegalArgumentException("A resposta não pode ser vazia.");
            }
            if (texto.length() > MAX_CARACTERES_RESPOSTA) {
                throw new IllegalArgumentException("Resposta longa demais (máximo de "
                        + MAX_CARACTERES_RESPOSTA + " caracteres).");
            }
            if (buscarUsuario(resposta.getIdUsuario()) == null) {
                throw new IllegalArgumentException("Usuário informado não existe.");
            }
            Pergunta pergunta = buscarPergunta(resposta.getIdPergunta());
            if (pergunta == null || !pergunta.isAtiva()) {
                throw new IllegalArgumentException("Pergunta informada não existe ou está arquivada.");
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

    /** Le uma resposta pelo ID, mesmo arquivada (null se nao existir). */
    public Resposta buscarResposta(int id) {
        try {
            return respostas.read(id);
        } catch (Exception e) {
            throw falha("ler resposta", e);
        }
    }

    /** Regrava uma resposta existente (usado para nota, avaliacoes e arquivamento). */
    public void atualizarResposta(Resposta resposta) {
        try {
            if (!respostas.update(resposta)) {
                throw new IllegalArgumentException("Resposta não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("atualizar resposta", e);
        }
    }

    /**
     * Respostas ATIVAS de uma pergunta, da maior nota para a menor
     * (em caso de empate, a mais antiga primeiro). Usa a arvore B+ [idPergunta, idResposta].
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
                    .thenComparingLong(Resposta::getCriacao));
            return lista;
        } catch (Exception e) {
            throw falha("listar respostas da pergunta", e);
        }
    }

    /**
     * Respostas ATIVAS escritas por um usuario, da mais recente para a mais antiga.
     * Usa a arvore B+ [idUsuario, idResposta].
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
            lista.sort(Comparator.comparingLong(Resposta::getCriacao).reversed());
            return lista;
        } catch (Exception e) {
            throw falha("listar respostas do usuário", e);
        }
    }

    /**
     * Arquiva uma resposta (ativa = false; nada e apagado).
     * Quem pode: o autor da resposta ou o autor da pergunta respondida.
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
                throw new IllegalArgumentException("Você não tem permissão para arquivar esta resposta.");
            }
            resposta.setAtiva(false);
            respostas.update(resposta);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("arquivar resposta", e);
        }
    }

    // -------------------- apoio --------------------

    // Se os indices estiverem vazios (primeira execucao ou arquivo apagado),
    // eles sao montados novamente a partir dos arquivos de dados.
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
        // TP2 - Pessoa 2: indices das respostas
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
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalStateException falha(String operacao, Exception e) {
        return new IllegalStateException("Falha ao " + operacao + ".", e);
    }

    @Override
    public void close() throws Exception {
        usuarios.close();
        perguntas.close();
        respostas.close();
    }
}
