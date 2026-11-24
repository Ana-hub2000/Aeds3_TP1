package br.pucminas;

import br.pucminas.model.Avaliacao;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Resposta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// Testes das avaliações de respostas (nota, total de avaliações e regras de uso)
public class AvaliacaoTest {
    @Rule
    public TemporaryFolder pastaTemporaria = new TemporaryFolder();

    private static Usuario novoUsuario(BancoDados banco, String nome, String email) {
        return banco.criarUsuario(new Usuario(nome, email, "hash", "Pergunta secreta?", "hash"));
    }

    private static void esperarRecusa(Runnable acao, String trechoDaMensagem) {
        try {
            acao.run();
            fail("Deveria ter recusado a operação");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(trechoDaMensagem));
        }
    }

    @Test
    public void avaliarAjustaNotaETotalDeAvaliacoes() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Usuario carla = novoUsuario(banco, "Carla", "carla@exemplo.com");
            Usuario dani = novoUsuario(banco, "Dani", "dani@exemplo.com");
            Pergunta pergunta = banco.criarPergunta(new Pergunta(ana.getIdUsuario(), "Pergunta", "teste"));
            int idResposta = banco.criarResposta(new Resposta(
                    pergunta.getIdPergunta(), bruno.getIdUsuario(), "Resposta")).getIdResposta();

            banco.criarAvaliacao(new Avaliacao(-1, ana.getIdUsuario(), idResposta, true));
            banco.criarAvaliacao(new Avaliacao(-1, carla.getIdUsuario(), idResposta, true));
            banco.criarAvaliacao(new Avaliacao(-1, dani.getIdUsuario(), idResposta, false));

            Resposta lida = banco.buscarResposta(idResposta);
            assertEquals(1, lida.getNota());
            assertEquals(3, lida.getAvaliacoes());
            assertEquals(1, banco.avaliacoesDoUsuario(ana.getIdUsuario()).size());
        }
    }

    @Test
    public void naoDeveAvaliarMinhaRespostaDuasVezesNemRespostaArquivada() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Pergunta pergunta = banco.criarPergunta(new Pergunta(ana.getIdUsuario(), "Pergunta", "teste"));
            int idResposta = banco.criarResposta(new Resposta(
                    pergunta.getIdPergunta(), bruno.getIdUsuario(), "Resposta")).getIdResposta();
            int idAna = ana.getIdUsuario();
            int idBruno = bruno.getIdUsuario();

            esperarRecusa(() -> banco.criarAvaliacao(new Avaliacao(-1, idBruno, idResposta, true)),
                    "própria resposta");

            banco.criarAvaliacao(new Avaliacao(-1, idAna, idResposta, true));
            esperarRecusa(() -> banco.criarAvaliacao(new Avaliacao(-1, idAna, idResposta, false)),
                    "já avaliou");
            esperarRecusa(() -> banco.criarAvaliacao(new Avaliacao(-1, idAna, 999, true)),
                    "não encontrada");
            esperarRecusa(() -> banco.criarAvaliacao(new Avaliacao(-1, 999, idResposta, true)),
                    "não existe");

            banco.arquivarResposta(idResposta, idBruno);
            Usuario carla = novoUsuario(banco, "Carla", "carla@exemplo.com");
            int idCarla = carla.getIdUsuario();
            esperarRecusa(() -> banco.criarAvaliacao(new Avaliacao(-1, idCarla, idResposta, true)),
                    "não encontrada");

            // as tentativas recusadas não mexeram na nota
            Resposta lida = banco.buscarResposta(idResposta);
            assertEquals(1, lida.getNota());
            assertEquals(1, lida.getAvaliacoes());
        }
    }

    @Test
    public void cancelarDesfazNotaETotalESoOAutorCancela() throws Exception {
        Path pasta = pastaTemporaria.newFolder("dados").toPath();
        int idResposta;
        int idAna;
        try (BancoDados banco = new BancoDados(pasta)) {
            Usuario ana = novoUsuario(banco, "Ana", "ana@exemplo.com");
            Usuario bruno = novoUsuario(banco, "Bruno", "bruno@exemplo.com");
            Usuario carla = novoUsuario(banco, "Carla", "carla@exemplo.com");
            idAna = ana.getIdUsuario();
            int idCarla = carla.getIdUsuario();
            Pergunta pergunta = banco.criarPergunta(new Pergunta(idAna, "Pergunta", "teste"));
            idResposta = banco.criarResposta(new Resposta(
                    pergunta.getIdPergunta(), bruno.getIdUsuario(), "Resposta")).getIdResposta();

            Avaliacao positiva = banco.criarAvaliacao(new Avaliacao(-1, idAna, idResposta, true));
            banco.criarAvaliacao(new Avaliacao(-1, idCarla, idResposta, false));
            assertEquals(0, banco.buscarResposta(idResposta).getNota());
            assertEquals(2, banco.buscarResposta(idResposta).getAvaliacoes());

            // outra pessoa não cancela a avaliação da Ana
            esperarRecusa(() -> banco.cancelarAvaliacao(positiva.getId(), idCarla), "não encontrada");

            banco.cancelarAvaliacao(positiva.getId(), idAna);
            assertEquals(-1, banco.buscarResposta(idResposta).getNota());
            assertEquals(1, banco.buscarResposta(idResposta).getAvaliacoes());
            assertTrue(banco.avaliacoesDoUsuario(idAna).isEmpty());

            // depois de cancelar, a Ana pode avaliar de novo (agora negativa)
            banco.criarAvaliacao(new Avaliacao(-1, idAna, idResposta, false));
            assertEquals(-2, banco.buscarResposta(idResposta).getNota());
            assertEquals(2, banco.buscarResposta(idResposta).getAvaliacoes());
        }

        try (BancoDados banco = new BancoDados(pasta)) {
            assertEquals(-2, banco.buscarResposta(idResposta).getNota());
            assertEquals(1, banco.avaliacoesDoUsuario(idAna).size());
            assertNotNull(banco.buscarAvaliacao(banco.avaliacoesDoUsuario(idAna).get(0).getId()));
        }
    }
}
