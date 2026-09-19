package br.pucminas.repository;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.ArvoreBMais;
import br.pucminas.persistence.TabelaHashExtensivel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
            throw falha("buscar usuário", e);
        }
    }

    public Usuario buscarUsuario(int id) {
        try {
            return usuarios.read(id);
        } catch (IOException e) {
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
            indiceEmail.put(usuario.getEmail(), usuario.getIdUsuario());
            return usuario;
        } catch (IOException e) {
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
            if (!normalizarEmail(emailAnterior).equals(usuario.getEmail())) {
                indiceEmail.remove(normalizarEmail(emailAnterior));
            }
            indiceEmail.put(usuario.getEmail(), usuario.getIdUsuario());
        } catch (IOException e) {
            throw falha("atualizar usuário", e);
        }
    }

    public Pergunta criarPergunta(Pergunta pergunta) {
        try {
            perguntas.create(pergunta);
            indicePerguntasPorUsuario.inserir(pergunta.getIdUsuario(), pergunta.getIdPergunta());
            return pergunta;
        } catch (IOException e) {
            throw falha("criar pergunta", e);
        }
    }

    public void atualizarPergunta(Pergunta pergunta) {
        try {
            if (!perguntas.update(pergunta)) {
                throw new IllegalArgumentException("Pergunta não encontrada.");
            }
        } catch (IOException e) {
            throw falha("atualizar pergunta", e);
        }
    }

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
        }
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalStateException falha(String operacao, IOException e) {
        return new IllegalStateException("Falha ao " + operacao + ".", e);
    }

    @Override
    public void close() {
        usuarios.close();
        perguntas.close();
        indiceEmail.close();
        indicePerguntasPorUsuario.close();
    }
}