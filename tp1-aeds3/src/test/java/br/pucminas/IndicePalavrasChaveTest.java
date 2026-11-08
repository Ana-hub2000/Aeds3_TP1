package br.pucminas;

import aed3.ElementoLista;
import aed3.ListaInvertida;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.persistence.TratamentoTexto;
import br.pucminas.repository.BancoDados;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class IndicePalavrasChaveTest {
    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private Usuario usuario(BancoDados banco) {
        return banco.criarUsuario(new Usuario("Aluno", "aluno@exemplo.com", "hash", "Cidade?", "hash"));
    }

    private ListaInvertida abrirLista(Path pasta) throws Exception {
        return new ListaInvertida(5, pasta.resolve("palavrasChave.dicionario.db").toString(),
                pasta.resolve("palavrasChave.blocos.db").toString());
    }

    @Test
    public void normalizaMinusculasAcentosEPontuacao() {
        assertArrayEquals(new String[]{"arvore", "java", "acao", "b"},
                TratamentoTexto.palavrasValidas("ÁRVORE;JAVA, ação! B+"));
    }

    @Test
    public void removeStopWordsSemEliminarRepeticoesValidas() {
        assertArrayEquals(new String[]{"java", "java"},
                TratamentoTexto.palavrasValidas("A e de não para com JAVA java"));
        assertEquals(0, TratamentoTexto.palavrasValidas(null).length);
    }

    @Test
    public void calculaTfSemDuplicarParIdTermo() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        int id;
        try (BancoDados banco = new BancoDados(pasta)) {
            id = banco.criarPergunta(new Pergunta(usuario(banco).getId(), "TF", "de JAVA;java;árvore")).getId();
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            ElementoLista[] ocorrencias = lista.read("java");
            assertEquals(1, ocorrencias.length);
            assertEquals(id, ocorrencias[0].getId());
            assertEquals(2.0f / 3, ocorrencias[0].getFrequencia(), 0.000001f);
            assertEquals(1.0f / 3, lista.read("arvore", id).getFrequencia(), 0.000001f);
        }
    }

    @Test
    public void contaUmaVezCadaDocumentoIncluindoPalavrasVazias() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            int autor = usuario(banco).getId();
            banco.criarPergunta(new Pergunta(autor, "Primeira", "java"));
            banco.criarPergunta(new Pergunta(autor, "Segunda", "de e para"));
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            assertEquals(2, lista.numeroEntidades());
        }
    }

    @Test
    public void ordenaPorTfIdfSomandoTermosDaMesmaPergunta() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            int autor = usuario(banco).getId();
            int comum = banco.criarPergunta(new Pergunta(autor, "Comum", "java")).getId();
            int ambos = banco.criarPergunta(new Pergunta(autor, "Ambos", "java raro")).getId();
            int diluida = banco.criarPergunta(new Pergunta(autor, "Diluída", "java rede disco")).getId();
            banco.criarPergunta(new Pergunta(autor, "Outra", "memoria"));
            // Nós esperamos 0,5 log10(4/3) + 0,5 log10(4/1) > log10(4/3).
            assertEquals(List.of(ambos, comum, diluida), banco.buscarPerguntasPorPalavrasChave("JAVA e RÁRO!"));
            assertEquals(List.of(comum, ambos, diluida), banco.buscarPerguntasPorPalavrasChave("java"));
            assertEquals(List.of(ambos, comum, diluida), banco.buscarPerguntasPorPalavrasChave("java raro raro"));
        }
    }

    @Test
    public void atualizaTermosAntigosSemAcumularTfOuAlterarTotal() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        int id;
        try (BancoDados banco = new BancoDados(pasta)) {
            Pergunta pergunta = banco.criarPergunta(new Pergunta(usuario(banco).getId(), "Editar", "java java rede"));
            id = pergunta.getId();
            pergunta.setPalavrasChave("java disco");
            banco.atualizarPergunta(pergunta);
            banco.atualizarPergunta(pergunta);
            assertTrue(banco.buscarPerguntasPorPalavrasChave("rede").isEmpty());
            assertEquals(List.of(id), banco.buscarPerguntasPorPalavrasChave("disco"));
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            assertEquals(1, lista.numeroEntidades());
            assertEquals(0.5f, lista.read("java", id).getFrequencia(), 0.000001f);
        }
    }

    @Test
    public void arquivamentoRetiraOcorrenciasEDiminuiTotalUmaVez() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Pergunta pergunta = banco.criarPergunta(new Pergunta(usuario(banco).getId(), "Arquivar", "java"));
            pergunta.setAtiva(false);
            banco.atualizarPergunta(pergunta);
            banco.atualizarPergunta(pergunta);
            assertTrue(banco.buscarPerguntasPorPalavrasChave("java").isEmpty());
            assertNotNull(banco.buscarPergunta(pergunta.getId()));
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            assertEquals(0, lista.numeroEntidades());
            assertEquals(0, lista.read("java").length);
        }
    }

    @Test
    public void exclusaoFisicaRemoveIdETermos() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Pergunta pergunta = banco.criarPergunta(new Pergunta(usuario(banco).getId(), "Excluir", "java"));
            banco.excluirPergunta(pergunta.getId());
            assertNull(banco.buscarPergunta(pergunta.getId()));
            assertTrue(banco.perguntasRecentes().isEmpty());
            assertTrue(banco.perguntasDoUsuario(pergunta.getIdUsuario(), true).isEmpty());
            assertTrue(banco.buscarPerguntasPorPalavrasChave("java").isEmpty());
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            assertEquals(0, lista.numeroEntidades());
        }
    }

    @Test
    public void reabreIndiceSemDuplicarContagemEReconstruiSeFaltarArquivo() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        int id;
        try (BancoDados banco = new BancoDados(pasta)) {
            id = banco.criarPergunta(new Pergunta(usuario(banco).getId(), "Persistir", "árvore")).getId();
        }
        try (BancoDados banco = new BancoDados(pasta)) {
            assertEquals(List.of(id), banco.buscarPerguntasPorPalavrasChave("arvore"));
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            assertEquals(1, lista.numeroEntidades());
        }
        Files.delete(pasta.resolve("palavrasChave.blocos.db"));
        try (BancoDados banco = new BancoDados(pasta)) {
            assertEquals(List.of(id), banco.buscarPerguntasPorPalavrasChave("arvore"));
        }
        try (ListaInvertida lista = abrirLista(pasta)) {
            assertEquals(1, lista.numeroEntidades());
        }
    }

    @Test
    public void buscaVaziaOuSemOcorrenciasNaoDividePorZero() throws Exception {
        Path pasta = temporaria.newFolder().toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            assertTrue(banco.buscarPerguntasPorPalavrasChave("java").isEmpty());
            banco.criarPergunta(new Pergunta(usuario(banco).getId(), "Única", "java"));
            assertTrue(banco.buscarPerguntasPorPalavrasChave("de e para").isEmpty());
            assertTrue(banco.buscarPerguntasPorPalavrasChave("inexistente").isEmpty());
            assertEquals(List.of(1), banco.buscarPerguntasPorPalavrasChave("java"));
        }
    }
}