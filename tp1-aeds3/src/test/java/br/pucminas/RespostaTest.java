package br.pucminas;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Resposta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// TP2 - Pessoa 2: testes das respostas e dos indices B+ do relacionamento N:N
public class RespostaTest {
    @Rule
    public TemporaryFolder pastaTemporaria = new TemporaryFolder();

    private static Usuario novoUsuario(BancoDados banco, String nome, String email) {
        return banco.criarUsuario(new Usuario(nome, email, "hash", "Pergunta secreta?", "hash"));
    }

    private static Pergunta novaPergunta(BancoDados banco, Usuario autor, String texto) {
        return banco.criarPergunta(new Pergunta(autor.getIdUsuario(), texto, "teste"));
    }

    // Executa a acao e confirma que ela foi recusada com a mensagem esperada
    private static void esperarRecusa(Runnable acao, String trechoDaMensagem) {
        try {
            acao.run();
            fail("Deveria ter recusado a operação");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(trechoDaMensagem));
        }
    }

    @Test
    public void deveCriarRespostaEIndexarPorPerguntaEPorUsuario() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Pergunta pergunta = novaPergunta(banco, ana, "Por que o céu é azul?");

            Resposta criada = banco.criarResposta(new Resposta(
                    pergunta.getIdPergunta(), bruno.getIdUsuario(), "Por causa do espalhamento da luz."));
            assertEquals(1, criada.getIdResposta());

            assertEquals(1, banco.respostasDaPergunta(pergunta.getIdPergunta()).size());
            assertEquals(1, banco.respostasDoUsuario(bruno.getIdUsuario()).size());
            assertTrue(banco.respostasDoUsuario(ana.getIdUsuario()).isEmpty());

            Resposta lida = banco.buscarResposta(criada.getIdResposta());
            assertNotNull(lida);
            assertEquals("Por causa do espalhamento da luz.", lida.getResposta());
            assertEquals(pergunta.getIdPergunta(), lida.getIdPergunta());
            assertEquals(bruno.getIdUsuario(), lida.getIdUsuario());
            assertEquals(0, lida.getNota());
            assertEquals(0, lida.getAvaliacoes());
            assertTrue(lida.isAtiva());
        }
    }

    @Test
    public void deveListarRespostasDaPerguntaPorNotaDecrescente() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Usuario carla = novoUsuario(banco, "Carla", "carla@exemplo.com");
            Usuario dani = novoUsuario(banco, "Dani", "dani@exemplo.com");
            Pergunta pergunta = novaPergunta(banco, ana, "Por que sentimos sono depois do almoço?");
            Pergunta outra = novaPergunta(banco, ana, "Outra pergunta");

            int r1 = banco.criarResposta(new Resposta(pergunta.getIdPergunta(), bruno.getIdUsuario(), "Resposta 1")).getIdResposta();
            int r2 = banco.criarResposta(new Resposta(pergunta.getIdPergunta(), carla.getIdUsuario(), "Resposta 2")).getIdResposta();
            int r3 = banco.criarResposta(new Resposta(pergunta.getIdPergunta(), dani.getIdUsuario(), "Resposta 3")).getIdResposta();
            banco.criarResposta(new Resposta(outra.getIdPergunta(), bruno.getIdUsuario(), "De outra pergunta"));

            // a nota e as avaliacoes sao ajustadas pelo codigo da avaliacao (pessoa 3);
            // aqui simulamos esse ajuste usando atualizarResposta
            Resposta a = banco.buscarResposta(r1);
            a.setNota((short) 7);
            a.setAvaliacoes((short) 13);
            banco.atualizarResposta(a);
            Resposta b = banco.buscarResposta(r2);
            b.setNota((short) 12);
            b.setAvaliacoes((short) 20);
            banco.atualizarResposta(b);
            Resposta c = banco.buscarResposta(r3);
            c.setNota((short) -2);
            c.setAvaliacoes((short) 2);
            banco.atualizarResposta(c);

            List<Resposta> lista = banco.respostasDaPergunta(pergunta.getIdPergunta());
            assertEquals(3, lista.size());
            assertEquals(r2, lista.get(0).getIdResposta());
            assertEquals(r1, lista.get(1).getIdResposta());
            assertEquals(r3, lista.get(2).getIdResposta());
            assertEquals(12, lista.get(0).getNota());
            assertEquals(20, lista.get(0).getAvaliacoes());
            assertEquals(-2, lista.get(2).getNota());
        }
    }

    @Test
    public void somenteAutorDaRespostaOuDaPerguntaPodeArquivar() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Usuario carla = novoUsuario(banco, "Carla", "carla@exemplo.com");
            Pergunta pergunta = novaPergunta(banco, ana, "Pergunta da Ana");

            int doBruno = banco.criarResposta(new Resposta(pergunta.getIdPergunta(), bruno.getIdUsuario(), "Do Bruno")).getIdResposta();
            int daCarla = banco.criarResposta(new Resposta(pergunta.getIdPergunta(), carla.getIdUsuario(), "Da Carla")).getIdResposta();

            // um terceiro nao pode arquivar
            esperarRecusa(() -> banco.arquivarResposta(doBruno, carla.getIdUsuario()), "permissão");
            assertEquals(2, banco.respostasDaPergunta(pergunta.getIdPergunta()).size());

            // o autor da resposta arquiva a propria
            banco.arquivarResposta(doBruno, bruno.getIdUsuario());
            assertEquals(1, banco.respostasDaPergunta(pergunta.getIdPergunta()).size());
            assertTrue(banco.respostasDoUsuario(bruno.getIdUsuario()).isEmpty());

            // arquivar nao apaga: o registro continua existindo, so fica inativo
            Resposta arquivada = banco.buscarResposta(doBruno);
            assertNotNull(arquivada);
            assertFalse(arquivada.isAtiva());

            // arquivar de novo e recusado
            esperarRecusa(() -> banco.arquivarResposta(doBruno, bruno.getIdUsuario()), "não encontrada");

            // o autor da pergunta arquiva a resposta de outra pessoa
            banco.arquivarResposta(daCarla, ana.getIdUsuario());
            assertTrue(banco.respostasDaPergunta(pergunta.getIdPergunta()).isEmpty());
            assertTrue(banco.respostasDoUsuario(carla.getIdUsuario()).isEmpty());
        }
    }

    @Test
    public void naoDeveCriarRespostaInvalida() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Pergunta pergunta = novaPergunta(banco, ana, "Pergunta da Ana");
            int idPergunta = pergunta.getIdPergunta();
            int idBruno = bruno.getIdUsuario();

            esperarRecusa(() -> banco.criarResposta(new Resposta(idPergunta, 999, "Texto")), "não existe");
            esperarRecusa(() -> banco.criarResposta(new Resposta(999, idBruno, "Texto")), "não existe");
            esperarRecusa(() -> banco.criarResposta(new Resposta(idPergunta, idBruno, "   ")), "vazia");
            esperarRecusa(() -> banco.criarResposta(
                    new Resposta(idPergunta, idBruno, "a".repeat(20001))), "longa demais");

            // pergunta arquivada nao recebe respostas
            pergunta.setAtiva(false);
            banco.atualizarPergunta(pergunta);
            esperarRecusa(() -> banco.criarResposta(new Resposta(idPergunta, idBruno, "Texto")), "arquivada");

            assertTrue(banco.respostasDoUsuario(idBruno).isEmpty());
            assertTrue(banco.respostasDaPergunta(idPergunta).isEmpty());
        }
    }

    @Test
    public void deveGuardarRespostaLongaComAcentos() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Pergunta pergunta = novaPergunta(banco, ana, "Pergunta da Ana");
            String longa = "é".repeat(20000);
            int id = banco.criarResposta(new Resposta(pergunta.getIdPergunta(), ana.getIdUsuario(), longa)).getIdResposta();
            assertEquals(longa, banco.buscarResposta(id).getResposta());
        }
    }

    @Test
    public void indicesDevemSobreviverAoSplitEAoReabrir() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        int idPergunta;
        int idBruno;
        int idCarla;
        int primeira;

        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Usuario carla = novoUsuario(banco, "Carla", "carla@exemplo.com");
            idBruno = bruno.getIdUsuario();
            idCarla = carla.getIdUsuario();
            idPergunta = novaPergunta(banco, ana, "Pergunta da Ana").getIdPergunta();

            primeira = banco.criarResposta(new Resposta(idPergunta, idBruno, "Resposta 1 do Bruno")).getIdResposta();
            for (int i = 2; i <= 12; i++) {
                banco.criarResposta(new Resposta(idPergunta, idBruno, "Resposta " + i + " do Bruno"));
            }
            for (int i = 1; i <= 3; i++) {
                banco.criarResposta(new Resposta(idPergunta, idCarla, "Resposta " + i + " da Carla"));
            }
            assertEquals(15, banco.respostasDaPergunta(idPergunta).size());
            assertEquals(12, banco.respostasDoUsuario(idBruno).size());
            assertEquals(3, banco.respostasDoUsuario(idCarla).size());
            banco.arquivarResposta(primeira, idBruno);
        }

        // depois do split das paginas das arvores e de reabrir, tudo continua valendo
        try (BancoDados banco = new BancoDados(pasta)) {
            assertEquals(14, banco.respostasDaPergunta(idPergunta).size());
            assertEquals(11, banco.respostasDoUsuario(idBruno).size());
            assertEquals(3, banco.respostasDoUsuario(idCarla).size());
            assertFalse(banco.buscarResposta(primeira).isAtiva());
        }
    }
}
