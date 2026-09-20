package br.pucminas.repository;

<<<<<<< HEAD
import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.ArvoreBMais;
import br.pucminas.persistence.TabelaHashExtensivel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
=======
import aed3.Arquivo;
import aed3.ArvoreBMais;
import aed3.ParIdId;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.ParEmailId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

<<<<<<< HEAD
public class BancoDados implements AutoCloseable {
    private final UsuarioCRUD usuarios;
    private final PerguntaCRUD perguntas;
    private final TabelaHashExtensivel indiceEmail;
    private final ArvoreBMais indicePerguntasPorUsuario;

    public BancoDados(Path diretorio) throws IOException {
        Files.createDirectories(diretorio);
        usuarios = new UsuarioCRUD(diretorio.resolve("usuarios.db"));
        perguntas = new PerguntaCRUD(diretorio.resolve("perguntas.db"));
        indiceEmail = new TabelaHashExtensivel(diretorio.resolve("emails.hash"));
        indicePerguntasPorUsuario = new ArvoreBMais(diretorio.resolve("perguntas.bplus"));
        reconstruirIndices();
    }

    public Usuario buscarUsuarioPorEmail(String email) {
        try {
            Integer id = indiceEmail.get(normalizarEmail(email));
            return id == null ? null : usuarios.read(id);
        } catch (IOException e) {
=======
/*
 Camada de acesso a dados.

 Estruturas usadas (todas vindas do pacote aed3, do professor):
  - Arquivo<T>          -> CRUD com lapide, indicador de tamanho (short) e
                           indice direto por ID em hash extensivel
  - ArvoreBMais<ParIdId>   -> indice indireto usuario -> perguntas
  - ArvoreBMais<ParEmailId>-> indice indireto e-mail -> usuario
*/
public class BancoDados implements AutoCloseable {

    private static final int ORDEM_ARVORE = 5;

    private final Arquivo<Usuario> usuarios;
    private final Arquivo<Pergunta> perguntas;
    private final ArvoreBMais<ParEmailId> indiceEmail;
    private final ArvoreBMais<ParIdId> indicePerguntasPorUsuario;

    public BancoDados(Path diretorio) throws Exception {
        Files.createDirectories(diretorio);
        String base = diretorio.toString();

        usuarios = new Arquivo<>(base, "usuarios", Usuario.class.getConstructor());
        perguntas = new Arquivo<>(base, "perguntas", Pergunta.class.getConstructor());

        indiceEmail = new ArvoreBMais<>(ParEmailId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indiceEmail.bplus").toString());
        indicePerguntasPorUsuario = new ArvoreBMais<>(ParIdId.class.getConstructor(),
                ORDEM_ARVORE, diretorio.resolve("indicePerguntas.bplus").toString());

        reconstruirIndicesSeVazios();
    }

    // -------------------- usuarios --------------------

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
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            throw falha("buscar usuário", e);
        }
    }

    public Usuario buscarUsuario(int id) {
        try {
            return usuarios.read(id);
<<<<<<< HEAD
        } catch (IOException e) {
=======
        } catch (Exception e) {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
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
<<<<<<< HEAD
            indiceEmail.put(usuario.getEmail(), usuario.getIdUsuario());
            return usuario;
        } catch (IOException e) {
=======
            indiceEmail.create(new ParEmailId(usuario.getEmail(), usuario.getIdUsuario()));
            return usuario;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            throw falha("criar usuário", e);
        }
    }

    public void atualizarUsuario(Usuario usuario, String emailAnterior) {
        try {
            usuario.setEmail(normalizarEmail(usuario.getEmail()));
            Usuario outro = buscarUsuarioPorEmail(usuario.getEmail());
            if (outro != null && outro.getIdUsuario() != usuario.getIdUsuario()) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }
            usuarios.update(usuario);
<<<<<<< HEAD
            if (!normalizarEmail(emailAnterior).equals(usuario.getEmail())) {
                indiceEmail.remove(normalizarEmail(emailAnterior));
            }
            indiceEmail.put(usuario.getEmail(), usuario.getIdUsuario());
        } catch (IOException e) {
=======

            String anterior = normalizarEmail(emailAnterior);
            if (!anterior.equals(usuario.getEmail())) {
                indiceEmail.delete(new ParEmailId(anterior, usuario.getIdUsuario()));
                indiceEmail.create(new ParEmailId(usuario.getEmail(), usuario.getIdUsuario()));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            throw falha("atualizar usuário", e);
        }
    }

<<<<<<< HEAD
    public Pergunta criarPergunta(Pergunta pergunta) {
        try {
            perguntas.create(pergunta);
            indicePerguntasPorUsuario.inserir(pergunta.getIdUsuario(), pergunta.getIdPergunta());
            return pergunta;
        } catch (IOException e) {
=======
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
            return pergunta;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            throw falha("criar pergunta", e);
        }
    }

    public void atualizarPergunta(Pergunta pergunta) {
        try {
            if (!perguntas.update(pergunta)) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
<<<<<<< HEAD
        } catch (IOException e) {
=======
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            throw falha("atualizar pergunta", e);
        }
    }

<<<<<<< HEAD
    public List<Pergunta> perguntasDoUsuario(int idUsuario) {
        try {
            return indicePerguntasPorUsuario.buscar(idUsuario).stream()
                    .map(id -> lerPergunta(id))
                    .filter(p -> p != null && p.isAtiva())
                    .sorted(Comparator.comparingLong(Pergunta::getCriacao))
                    .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private Pergunta lerPergunta(int id) {
        try {
            return perguntas.read(id);
        } catch (IOException e) {
            throw falha("ler pergunta", e);
        }
    }

    private void reconstruirIndices() throws IOException {
        indiceEmail.clear();
        for (Usuario usuario : usuarios.readAll()) {
            indiceEmail.put(normalizarEmail(usuario.getEmail()), usuario.getIdUsuario());
        }
        indicePerguntasPorUsuario.clear();
        for (Pergunta pergunta : perguntas.readAll()) {
            indicePerguntasPorUsuario.inserir(pergunta.getIdUsuario(), pergunta.getIdPergunta());
=======
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
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        }
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

<<<<<<< HEAD
    private static IllegalStateException falha(String operacao, IOException e) {
=======
    private static IllegalStateException falha(String operacao, Exception e) {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        return new IllegalStateException("Falha ao " + operacao + ".", e);
    }

    @Override
<<<<<<< HEAD
    public void close() {
        usuarios.close();
        perguntas.close();
        indiceEmail.close();
        indicePerguntasPorUsuario.close();
    }
}
=======
    public void close() throws Exception {
        usuarios.close();
        perguntas.close();
    }
}
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
