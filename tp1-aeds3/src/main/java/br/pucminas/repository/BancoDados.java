package br.pucminas.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import aed3.Arquivo;
import aed3.ArvoreBMais;
import aed3.ParIdId;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Resposta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.IndicePalavrasChave;
import br.pucminas.persistence.ParDataId;
import br.pucminas.persistence.ParEmailId;

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
    private final Arquivo<Resposta> respostas;
    private final ArvoreBMais<ParEmailId> indiceEmail;
    private final ArvoreBMais<ParIdId> indicePerguntasPorUsuario;
    private final ArvoreBMais<ParIdId> indiceRespostasPorPergunta;
    private final ArvoreBMais<ParIdId> indiceRespostasPorUsuario;
    private final ArvoreBMais<ParDataId> indicePerguntasPorData;
    private final IndicePalavrasChave indicePalavrasChave;

    public BancoDados(Path diretorio) throws Exception {
        Files.createDirectories(diretorio);
        String base = diretorio.toString();

        usuarios = new Arquivo<>(base, "usuarios", Usuario.class.getConstructor());
        perguntas = new Arquivo<>(base, "perguntas", Pergunta.class.getConstructor());
        respostas = new Arquivo<>(base, "respostas", Resposta.class.getConstructor());

        indiceEmail = new ArvoreBMais<>(ParEmailId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceEmail.bplus").toString());
        indicePerguntasPorUsuario = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indicePerguntas.bplus").toString());
        indiceRespostasPorPergunta = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceRespostasPergunta.bplus").toString());
        indiceRespostasPorUsuario = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceRespostasUsuario.bplus").toString());
        indicePerguntasPorData = new ArvoreBMais<>(ParDataId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indicePerguntasData.bplus").toString());


        reconstruirIndicesSeVazios();
        indicePalavrasChave = new IndicePalavrasChave(diretorio);
        if (indicePalavrasChave.precisaReconstruir()) {
            // Nós também indexamos os registros que já existiam antes do TP3.
            for (Pergunta pergunta : perguntas.readAll()) {
                indicePalavrasChave.incluir(pergunta);
            }
        }
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
            if (anterior == null) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
            if (!perguntas.update(pergunta)) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
            // Nós retiramos os termos antigos antes de calcular o TF das palavras novas.
            indicePalavrasChave.remover(anterior);
            indicePalavrasChave.incluir(pergunta);
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

    public List<Integer> buscarPerguntasPorPalavrasChave(String consulta) {
        try {
            return indicePalavrasChave.buscar(consulta);
        } catch (Exception e) {
            throw falha("buscar perguntas por palavras-chave", e);
        }
    }

    // -------------------- respostas (TP2) --------------------

    /**
     * Cria uma resposta para uma pergunta ativa e indexa o relacionamento
     * pergunta -> respostas e usuário -> respostas.
     */
    public Resposta criarResposta(Resposta resposta) {
        if (resposta == null) {
            throw new IllegalArgumentException("Resposta inválida.");
        }

        try {
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

            String texto = resposta.getResposta();
            if (texto == null || texto.isBlank()) {
                throw new IllegalArgumentException("A resposta não pode ser vazia.");
            }
            if (texto.length() > 20000) {
                throw new IllegalArgumentException("A resposta é longa demais.");
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

    /** Lê uma resposta por ID, incluindo respostas arquivadas. */
    public Resposta buscarResposta(int idResposta) {
        try {
            return respostas.read(idResposta);
        } catch (Exception e) {
            throw falha("ler resposta", e);
        }
    }

    /**
     * Atualiza os campos mutáveis da resposta (nota, avaliações e estado).
     * Os IDs de pergunta e usuário são imutáveis no modelo.
     */
    public void atualizarResposta(Resposta resposta) {
        if (resposta == null) {
            throw new IllegalArgumentException("Resposta inválida.");
        }

        try {
            if (respostas.read(resposta.getIdResposta()) == null
                    || !respostas.update(resposta)) {
                throw new IllegalArgumentException("Resposta não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw falha("atualizar resposta", e);
        }
    }

    /**
     * Arquiva uma resposta sem apagar o registro. A ação é permitida ao autor
     * da resposta ou ao autor da pergunta associada.
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
                throw new IllegalArgumentException(
                        "Sem permissão para arquivar esta resposta.");
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

    /** Lista as respostas ativas de uma pergunta, por nota decrescente. */
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

    /** Lista as respostas ativas criadas por um usuário. */
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
        try {
            usuarios.close();
        } finally {
            try {
                perguntas.close();
            } finally {
                try {
                    respostas.close();
                } finally {
                    indicePalavrasChave.close();
                }
            }
        }
    }
}