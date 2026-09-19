package br.pucminas;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.TabelaHashExtensivel;
import br.pucminas.repository.BancoDados;
import br.pucminas.security.Seguranca;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
            assertTrue(Seguranca.hashResposta("sao paulo")
                    .equals(usuario.getHashRespostaSecreta()));

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

            pergunta.setAtiva(false);
            banco.atualizarPergunta(pergunta);
            assertTrue(banco.perguntasDoUsuario(usuario.getIdUsuario()).isEmpty());
        }

        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario recuperado = banco.buscarUsuarioPorEmail("ANA@EXEMPLO.COM");
            assertNotNull(recuperado);
            assertEquals("Ana", recuperado.getNome());
            assertNull(banco.perguntasDoUsuario(recuperado.getIdUsuario()).stream()
                    .filter(pergunta -> pergunta.getIdPergunta() == idPergunta)
                    .findFirst()
                    .orElse(null));
        }
    }

    @Test
    public void tabelaHashDeveDividirBucketsESobreviverAoReabrir() throws Exception {
        Path arquivo = pastaTemporaria.newFile("emails.hash").toPath();
        try (TabelaHashExtensivel indice = new TabelaHashExtensivel(arquivo)) {
            for (int i = 0; i < 20; i++) {
                indice.put("usuario" + i + "@exemplo.com", i + 1);
            }
        }

        try (TabelaHashExtensivel indice = new TabelaHashExtensivel(arquivo)) {
            assertEquals(Integer.valueOf(1), indice.get("usuario0@exemplo.com"));
            assertEquals(Integer.valueOf(20), indice.get("usuario19@exemplo.com"));
            assertNull(indice.get("nao-existe@exemplo.com"));
        }
    }
}