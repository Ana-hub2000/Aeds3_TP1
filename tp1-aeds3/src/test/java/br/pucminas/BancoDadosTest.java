package br.pucminas;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import br.pucminas.security.Seguranca;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BancoDadosTest {
    @Rule
    public TemporaryFolder pastaTemporaria = new TemporaryFolder();

    @Test
    public void devePersistirUsuarioPerguntaEIndices() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        Usuario usuario;
        int idPergunta;

        try (BancoDados banco = new BancoDados(pasta)) {
            usuario = banco.criarUsuario(new Usuario(
                    "Ana", "ANA@EXEMPLO.COM", Seguranca.hashSenha("senha"),
                    "Cidade natal?", Seguranca.hashResposta("São Paulo")));
            assertEquals("ana@exemplo.com", usuario.getEmail());
            assertEquals(1, usuario.getIdUsuario());
            assertNotNull(banco.buscarUsuarioPorEmail("ana@exemplo.com"));
            assertEquals(Seguranca.hashResposta("sao paulo"), usuario.getHashRespostaSecreta());

            Pergunta pergunta = banco.criarPergunta(
                    new Pergunta(usuario.getIdUsuario(), "Como serializar?", "java;arquivos"));
            idPergunta = pergunta.getIdPergunta();
            assertEquals(1, idPergunta);
            assertEquals(1, banco.perguntasDoUsuario(usuario.getIdUsuario()).size());

            pergunta.setPergunta("Como serializar registros?");
            pergunta.setAlteracao(System.currentTimeMillis());
            banco.atualizarPergunta(pergunta);
            assertEquals("Como serializar registros?",
                    banco.perguntasDoUsuario(usuario.getIdUsuario()).get(0).getPergunta());

            // arquivar nao apaga: some da lista publica, mas o autor continua vendo
            pergunta.setAtiva(false);
            banco.atualizarPergunta(pergunta);
            assertTrue(banco.perguntasDoUsuario(usuario.getIdUsuario()).isEmpty());
            List<Pergunta> comArquivadas =
                    banco.perguntasDoUsuario(usuario.getIdUsuario(), true);
            assertEquals(1, comArquivadas.size());
            assertFalse(comArquivadas.get(0).isAtiva());
        }

        // reabrindo o banco os dados e os indices continuam valendo
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario recuperado = banco.buscarUsuarioPorEmail("ANA@EXEMPLO.COM");
            assertNotNull(recuperado);
            assertEquals("Ana", recuperado.getNome());
            assertEquals(1, banco.perguntasDoUsuario(recuperado.getIdUsuario(), true).size());
            assertTrue(banco.perguntasDoUsuario(recuperado.getIdUsuario()).isEmpty());
            assertNull(banco.buscarUsuarioPorEmail("outro@exemplo.com"));
        }
    }

    @Test
    public void naoDeveCriarPerguntaParaUsuarioInexistente() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados2").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            try {
                banco.criarPergunta(new Pergunta(999, "Pergunta solta", "teste"));
                fail("Deveria recusar pergunta de usuário inexistente");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("não existe"));
            }
        }
    }

    @Test
    public void arvoreBMaisDeveGuardarVariasPerguntasPorUsuario() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados3").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario usuario = banco.criarUsuario(new Usuario(
                    "Bruno", "bruno@exemplo.com", Seguranca.hashSenha("123"),
                    "Time?", Seguranca.hashResposta("cruzeiro")));
            for (int i = 1; i <= 25; i++) {
                banco.criarPergunta(new Pergunta(usuario.getIdUsuario(),
                        "Pergunta " + i, "chave" + i));
            }
            assertEquals(25, banco.perguntasDoUsuario(usuario.getIdUsuario()).size());
        }

        // depois do split das paginas da arvore os dados continuam recuperaveis
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario usuario = banco.buscarUsuarioPorEmail("bruno@exemplo.com");
            assertNotNull(usuario);
            assertEquals(25, banco.perguntasDoUsuario(usuario.getIdUsuario()).size());
        }
    }
}
