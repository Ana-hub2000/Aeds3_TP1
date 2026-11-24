package br.pucminas.menu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import br.pucminas.model.Avaliacao;
import br.pucminas.model.Pergunta;
import br.pucminas.model.Resposta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import br.pucminas.security.Seguranca;

/**
 * Interface de texto do sistema: mostra as telas ao usuário e chama os métodos do
 * {@link BancoDados}. A saída usa System.out com códigos ANSI de cor (o projeto não
 * usa a API do Lanterna diretamente) e a entrada é lida com Scanner.
 *
 * Organização do arquivo:
 *   1. Início: login, novo usuário e recuperação de senha
 *   2. Menu principal e busca de perguntas (recentes e por palavras-chave)
 *   3. Pergunta: detalhes, respostas e avaliação
 *   4. Minha área: dados, perguntas, respostas e avaliações
 *   5. Apoio: pausas, molduras e leitura de dados
 */
public class Menu {
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private static final String RESET = "\u001B[0m";
    private static final String NEGRITO = "\u001B[1m";
    private static final String BORDA = "\u001B[1;38;5;39m";
    private static final String TITULO_COR = "\u001B[1;97m";
    private static final String OPCAO_COR = "\u001B[38;5;222m";
    private static final String RODAPE_COR = "\u001B[38;5;131m";
    private static final String VERDE = "\u001B[1;38;5;114m";
    private static final String VERMELHO = "\u001B[1;38;5;203m";

    private static final long ATRASO_MS = 1800;
    private static final long ATRASO_REGISTRO_MS = 7000;
    private static final int POR_PAGINA = 5;
    private static final int LARGURA_TEXTO = 60;

    private final BancoDados banco;
    private final Scanner entrada;

    public Menu(BancoDados banco, Scanner entrada) {
        this.banco = banco;
        this.entrada = entrada;
    }

    // ==================== 1. Início ====================

    /** Tela inicial: login, cadastro e saída. */
    public void executar() {
        boolean continuar = true;
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("AJUDA AÍ 1.0", "1 - Login", "2 - Novo usuário", "0 - Sair");
            switch (opcao()) {
                case 1 -> login();
                case 2 -> novoUsuario();
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
        aguardar();
        limparTela();
        System.out.println("Até mais!");
    }

    private void novoUsuario() {
        limparTela();
        moldura("Novo usuário");
        String nome = textoObrigatorio("Nome: ");
        String email = textoObrigatorio("E-mail: ");
        String senha = textoObrigatorio("Senha: ");
        String pergunta = textoObrigatorio("Pergunta secreta: ");
        String resposta = textoObrigatorio("Resposta secreta: ");

        try {
            banco.criarUsuario(new Usuario(nome, email, Seguranca.hashSenha(senha),
                    pergunta, Seguranca.hashResposta(resposta)));
            sucesso("Usuário cadastrado com sucesso.");
            aguardarRegistro();
        } catch (IllegalArgumentException e) {
            erro(e.getMessage());
            aguardar();
        }
    }

    private void login() {
        boolean primeiraVez = true;
        while (true) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Login");
            String email = textoObrigatorio("E-mail: ");
            String senha = textoObrigatorio("Senha: ");
            Usuario usuario = banco.buscarUsuarioPorEmail(email);
            if (usuario != null && Seguranca.corresponde(senha, usuario.getHashSenha())) {
                sucesso("Bem-vindo(a), " + usuario.getNome() + "!");
                aguardar();
                menuPrincipal(usuario);
                return;
            }
            aguardar();
            limparTela();
            moldura("Login inválido", "1 - Tentar novamente", "2 - Recuperar senha", "0 - Voltar");
            int escolha = opcao();
            if (escolha == 2) {
                recuperarSenha();
                return;
            }
            if (escolha == 0) {
                return;
            }
        }
    }

    private void recuperarSenha() {
        limparTela();
        moldura("Recuperar senha");
        Usuario usuario = banco.buscarUsuarioPorEmail(textoObrigatorio("E-mail: "));
        if (usuario == null) {
            erro("Usuário não encontrado.");
            aguardar();
            return;
        }
        System.out.println("Pergunta: " + usuario.getPerguntaSecreta());
        String resposta = textoObrigatorio("Resposta: ");
        if (!Seguranca.hashResposta(resposta).equals(usuario.getHashRespostaSecreta())) {
            erro("Resposta secreta incorreta.");
            aguardar();
            return;
        }
        usuario.setHashSenha(Seguranca.hashSenha(textoObrigatorio("Nova senha: ")));
        banco.atualizarUsuario(usuario, usuario.getEmail());
        sucesso("Senha alterada com sucesso.");
        aguardarRegistro();
    }

    // ==================== 2. Menu principal e busca ====================

    private void menuPrincipal(Usuario usuario) {
        boolean continuar = true;
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Menu principal", "1 - Minha área", "2 - Buscar perguntas", "0 - Sair");
            switch (opcao()) {
                case 1 -> minhaArea(usuario);
                case 2 -> buscarPerguntas(usuario);
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }

    /** Menu de busca: A = mais recentes, B = palavras-chave. */
    private void buscarPerguntas(Usuario usuario) {
        while (true) {
            limparTela();
            moldura("Buscar perguntas", "A - Perguntas mais recentes",
                    "B - Buscar por palavras-chave", "0 - Voltar");
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim().toUpperCase();
            switch (op) {
                case "A" -> listarPerguntasPaginado(banco.perguntasRecentes(),
                        "Perguntas mais recentes", "Nenhuma pergunta cadastrada.", usuario);
                case "B" -> buscarPorPalavrasChave(usuario);
                case "0" -> { return; }
                default -> { erro("Opção inválida."); aguardar(); }
            }
        }
    }

    /** Busca por palavras-chave; os resultados seguem a ordem de relevância (TF x IDF). */
    private void buscarPorPalavrasChave(Usuario usuario) {
        String consulta = textoObrigatorio("Palavras-chave: ");
        List<Pergunta> encontradas = new ArrayList<>();
        for (int id : banco.buscarPerguntasPorPalavrasChave(consulta)) {
            Pergunta pergunta = banco.buscarPergunta(id);
            if (pergunta != null && pergunta.isAtiva()) {
                encontradas.add(pergunta);
            }
        }
        // A ordem recebida do índice é mantida: as perguntas não são reordenadas por data.
        listarPerguntasPaginado(encontradas, "Resultados por relevância",
                "Nenhuma pergunta encontrada.", usuario);
    }

    /** Mostra a lista de perguntas, 5 por página, com navegação e acesso aos detalhes. */
    private void listarPerguntasPaginado(List<Pergunta> todas, String titulo,
                                         String mensagemVazia, Usuario usuario) {
        int totalPaginas = Math.max(1, (todas.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pagina = 0;
        while (true) {
            limparTela();
            int inicio = pagina * POR_PAGINA;
            int fim = Math.min(inicio + POR_PAGINA, todas.size());
            List<String> linhas = new ArrayList<>();
            if (todas.isEmpty()) {
                linhas.add(mensagemVazia);
            }
            for (int i = inicio; i < fim; i++) {
                Pergunta p = todas.get(i);
                linhas.add("[ID " + p.getIdPergunta() + "] " + p.getPergunta());
                linhas.add("   " + formatarData(p.getCriacao()) + " | Nota: " + p.getNota());
            }
            linhas.add("");
            linhas.add("Página " + (pagina + 1) + " de " + totalPaginas);
            linhas.add("> - Próxima | < - Anterior | ID - Ver detalhes | 0 - Voltar");
            moldura(titulo, linhas);
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim();
            if (op.equals("0")) return;
            if (op.equals(">")) {
                if (pagina < totalPaginas - 1) pagina++;
                else { erro("Já está na última página."); aguardar(); }
            } else if (op.equals("<")) {
                if (pagina > 0) pagina--;
                else { erro("Já está na primeira página."); aguardar(); }
            } else {
                try {
                    mostrarDetalhes(Integer.parseInt(op), usuario);
                } catch (NumberFormatException e) {
                    erro("Opção inválida.");
                    aguardar();
                }
            }
        }
    }

    // ==================== 3. Pergunta: detalhes, respostas e avaliação ====================

    /** Mostra os dados de uma pergunta e, abaixo, suas respostas ativas por nota. */
    private void mostrarDetalhes(int id, Usuario usuario) {
        while (true) {
            Pergunta p = banco.buscarPergunta(id);
            if (p == null || !p.isAtiva()) {
                erro("Pergunta não encontrada.");
                aguardar();
                return;
            }
            Usuario autor = banco.buscarUsuario(p.getIdUsuario());
            List<Resposta> respostas = banco.respostasDaPergunta(id);

            List<String> linhas = new ArrayList<>();
            linhas.add("Início > Buscar perguntas > Pergunta");
            linhas.add("");
            linhas.addAll(quebrar(p.getPergunta(), LARGURA_TEXTO));
            linhas.add("");
            linhas.add("Criada por " + nomeDoUsuario(autor) + " em " + formatarData(p.getCriacao()));
            linhas.add("Palavras-chave: " + p.getPalavrasChave());
            linhas.add("");
            linhas.add("RESPOSTAS");
            linhas.add("---------");
            if (respostas.isEmpty()) {
                linhas.add("Nenhuma resposta cadastrada.");
            }
            for (int i = 0; i < respostas.size(); i++) {
                Resposta r = respostas.get(i);
                Usuario respondedor = banco.buscarUsuario(r.getIdUsuario());
                List<String> partes = quebrar(r.getResposta(), LARGURA_TEXTO);
                linhas.add("");
                linhas.add("(" + (i + 1) + ") " + partes.get(0));
                for (int j = 1; j < partes.size(); j++) {
                    linhas.add("    " + partes.get(j));
                }
                linhas.add("    Respondido por " + nomeDoUsuario(respondedor));
                linhas.add("    Nota: " + r.getNota() + " (" + r.getAvaliacoes() + " avaliações)");
            }
            linhas.add("");
            linhas.add("(P) Responder à pergunta");
            linhas.add("(A) Avaliar uma resposta");
            linhas.add("(R) Retornar");

            limparTela();
            moldura("Pergunta #" + id, linhas);
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim().toUpperCase();
            switch (op) {
                case "R" -> { return; }
                case "P" -> responderPergunta(p, autor, usuario);
                case "A" -> selecionarRespostaParaAvaliar(respostas, usuario);
                default -> { erro("Opção inválida."); aguardar(); }
            }
        }
    }

    private void responderPergunta(Pergunta p, Usuario criador, Usuario usuario) {
        limparTela();
        moldura("AJUDA AÍ 1.0",
                "> Início > Buscar perguntas > Pergunta > Responder", "",
                p.getPergunta(),
                "Criada por " + nomeDoUsuario(criador) + " em " + formatarData(p.getCriacao()), "",
                "Deixe em branco para cancelar.");
        System.out.print("Resposta: ");
        String texto = entrada.nextLine().trim();
        if (texto.isEmpty()) return;
        try {
            banco.criarResposta(new Resposta(p.getIdPergunta(), usuario.getIdUsuario(), texto));
            sucesso("Resposta incluída com sucesso.");
            aguardarRegistro();
        } catch (IllegalArgumentException e) {
            erro(e.getMessage());
            aguardar();
        }
    }

    private void selecionarRespostaParaAvaliar(List<Resposta> respostas, Usuario usuario) {
        if (respostas.isEmpty()) {
            erro("Não há respostas para avaliar.");
            aguardar();
            return;
        }
        int numero = inteiro("Número da resposta para avaliar (0 para voltar): ");
        if (numero == 0) {
            return;
        }
        if (numero < 1 || numero > respostas.size()) {
            erro("Resposta inválida.");
            aguardar();
            return;
        }
        avaliarResposta(usuario, respostas.get(numero - 1));
    }

    private void avaliarResposta(Usuario usuario, Resposta resposta) {
        System.out.println("(A) Concordar com a resposta");
        System.out.println("(B) Discordar da resposta");
        System.out.println("(R) Retornar ao menu anterior");
        System.out.print("\nOpção: ");
        String op = entrada.nextLine().trim().toUpperCase();

        switch (op) {
            case "A", "B" -> {
                boolean positiva = op.equals("A");
                Avaliacao avaliacao = new Avaliacao(-1, usuario.getIdUsuario(),
                        resposta.getIdResposta(), positiva);
                try {
                    banco.criarAvaliacao(avaliacao);
                    sucesso("Avaliação registrada com sucesso!");
                } catch (IllegalArgumentException e) {
                    erro(e.getMessage());
                }
                aguardar();
            }
            case "R" -> { /* volta à tela anterior */ }
            default -> { erro("Opção inválida."); aguardar(); }
        }
    }

    // ==================== 4. Minha área ====================

    private void minhaArea(Usuario usuario) {
        boolean continuar = true;
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Minha área", "1 - Meus dados", "2 - Minhas perguntas", "3 - Minhas respostas",
                    "4 - Minhas avaliações", "0 - Retornar");
            switch (opcao()) {
                case 1 -> meusDados(usuario);
                case 2 -> minhasPerguntas(usuario);
                case 3 -> minhasRespostas(usuario);
                case 4 -> minhasAvaliacoes(usuario);
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }

    // ---------- Meus dados ----------

    private void meusDados(Usuario usuario) {
        boolean continuar = true;
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Meus dados",
                    "Nome: " + usuario.getNome(), "E-mail: " + usuario.getEmail(), "",
                    "1 - Alterar nome", "2 - Alterar e-mail", "3 - Alterar senha",
                    "4 - Alterar pergunta/resposta de recuperação", "0 - Voltar");
            int escolha = opcao();
            String emailAnterior = usuario.getEmail();
            try {
                switch (escolha) {
                    case 1 -> usuario.setNome(textoObrigatorio("Novo nome: "));
                    case 2 -> usuario.setEmail(textoObrigatorio("Novo e-mail: "));
                    case 3 -> usuario.setHashSenha(
                            Seguranca.hashSenha(textoObrigatorio("Nova senha: ")));
                    case 4 -> {
                        usuario.setPerguntaSecreta(textoObrigatorio("Nova pergunta secreta: "));
                        usuario.setHashRespostaSecreta(
                                Seguranca.hashResposta(textoObrigatorio("Nova resposta secreta: ")));
                    }
                    case 0 -> {
                        continuar = false;
                        continue;
                    }
                    default -> {
                        erro("Opção inválida.");
                        continue;
                    }
                }
                banco.atualizarUsuario(usuario, emailAnterior);
                sucesso("Dados atualizados.");
                aguardarRegistro();
            } catch (IllegalArgumentException e) {
                erro(e.getMessage());
            }
        }
    }

    // ---------- Minhas perguntas ----------

    private void minhasPerguntas(Usuario usuario) {
        boolean continuar = true;
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            List<Pergunta> perguntas = banco.perguntasDoUsuario(usuario.getIdUsuario());
            List<String> linhas = new ArrayList<>();
            if (perguntas.isEmpty()) {
                linhas.add("Você ainda não possui perguntas ativas.");
            } else {
                for (int i = 0; i < perguntas.size(); i++) {
                    Pergunta pergunta = perguntas.get(i);
                    linhas.add((i + 1) + ". " + pergunta.getPergunta());
                    linhas.add("   Palavras-chave: " + pergunta.getPalavrasChave()
                            + " | Nota: " + pergunta.getNota()
                            + " | Criada: " + formatarData(pergunta.getCriacao()));
                }
            }
            linhas.add("");
            linhas.add("1 - Incluir pergunta");
            linhas.add("2 - Alterar pergunta");
            linhas.add("3 - Arquivar pergunta");
            linhas.add("4 - Ver respostas recebidas");
            linhas.add("0 - Voltar");
            moldura("Minhas perguntas", linhas);
            switch (opcao()) {
                case 1 -> incluirPergunta(usuario);
                case 2 -> editarPergunta(perguntas);
                case 3 -> arquivarPergunta(perguntas);
                case 4 -> respostasRecebidasDaPergunta(perguntas, usuario);
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }

    /** Pede o número de uma pergunta da lista; devolve null se o usuário voltar ou errar. */
    private Pergunta selecionarPergunta(List<Pergunta> perguntas) {
        if (perguntas.isEmpty()) {
            erro("Nenhuma pergunta disponível.");
            aguardar();
            return null;
        }
        int n = inteiro("Número da pergunta (0 para voltar): ");
        if (n == 0) {
            return null;
        }
        if (n < 1 || n > perguntas.size()) {
            erro("Pergunta inválida.");
            aguardar();
            return null;
        }
        return perguntas.get(n - 1);
    }

    private void incluirPergunta(Usuario usuario) {
        String texto = textoObrigatorio("Texto da pergunta: ");
        String palavras = textoObrigatorio("Palavras-chave (separadas por ponto e vírgula): ");
        banco.criarPergunta(new Pergunta(usuario.getIdUsuario(), texto, palavras));
        sucesso("Pergunta incluída com sucesso.");
        aguardarRegistro();
    }

    private void editarPergunta(List<Pergunta> perguntas) {
        Pergunta pergunta = selecionarPergunta(perguntas);
        if (pergunta == null) {
            return;
        }
        pergunta.setPergunta(textoObrigatorio("Novo texto (Enter para manter): ",
                pergunta.getPergunta()));
        pergunta.setPalavrasChave(textoObrigatorio("Novas palavras-chave (Enter para manter): ",
                pergunta.getPalavrasChave()));
        pergunta.setAlteracao(System.currentTimeMillis());
        banco.atualizarPergunta(pergunta);
        sucesso("Pergunta alterada.");
        aguardarRegistro();
    }

    private void arquivarPergunta(List<Pergunta> perguntas) {
        Pergunta pergunta = selecionarPergunta(perguntas);
        if (pergunta == null) {
            return;
        }
        pergunta.setAtiva(false);
        pergunta.setAlteracao(System.currentTimeMillis());
        banco.atualizarPergunta(pergunta);
        sucesso("Pergunta arquivada. O registro não foi excluído.");
        aguardarRegistro();
    }

    /** Respostas de outros usuários a uma das minhas perguntas; o autor pode arquivá-las. */
    private void respostasRecebidasDaPergunta(List<Pergunta> perguntas, Usuario usuario) {
        Pergunta pergunta = selecionarPergunta(perguntas);
        if (pergunta == null) {
            return;
        }

        while (true) {
            List<Resposta> respostas = banco.respostasDaPergunta(pergunta.getIdPergunta());

            List<String> linhas = new ArrayList<>();
            linhas.add("Pergunta: " + pergunta.getPergunta());
            linhas.add("");
            if (respostas.isEmpty()) {
                linhas.add("Esta pergunta ainda não recebeu respostas ativas.");
            }
            for (int i = 0; i < respostas.size(); i++) {
                Resposta resposta = respostas.get(i);
                Usuario autor = banco.buscarUsuario(resposta.getIdUsuario());
                linhas.add((i + 1) + ". " + resposta.getResposta());
                linhas.add("   Autor: " + nomeDoUsuario(autor));
                linhas.add("   Nota: " + resposta.getNota()
                        + " | Avaliações: " + resposta.getAvaliacoes());
            }
            linhas.add("");
            linhas.add("A - Arquivar resposta de outro usuário");
            linhas.add("R - Voltar");

            limparTela();
            moldura("Respostas recebidas", linhas);
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim().toUpperCase();

            if (op.equals("R")) {
                return;
            }
            if (!op.equals("A")) {
                erro("Opção inválida.");
                aguardar();
                continue;
            }
            if (respostas.isEmpty()) {
                erro("Não há respostas disponíveis.");
                aguardar();
                continue;
            }

            int n = inteiro("Número da resposta (0 para voltar): ");
            if (n == 0) {
                continue;
            }
            if (n < 1 || n > respostas.size()) {
                erro("Resposta inválida.");
                aguardar();
                continue;
            }

            Resposta resposta = respostas.get(n - 1);
            if (resposta.getIdUsuario() == usuario.getIdUsuario()) {
                erro("Nesta tela, você só pode arquivar respostas de outros usuários.");
                aguardar();
                continue;
            }
            if (confirmarArquivamento()) {
                try {
                    banco.arquivarResposta(resposta.getIdResposta(), usuario.getIdUsuario());
                    sucesso("Resposta arquivada.");
                } catch (IllegalArgumentException e) {
                    erro(e.getMessage());
                }
                aguardar();
            }
        }
    }

    // ---------- Minhas respostas ----------

    private void minhasRespostas(Usuario usuario) {
        while (true) {
            List<Resposta> respostas = banco.respostasDoUsuario(usuario.getIdUsuario());

            List<String> linhas = new ArrayList<>();
            if (respostas.isEmpty()) {
                linhas.add("Você ainda não possui respostas ativas.");
            }
            for (int i = 0; i < respostas.size(); i++) {
                Resposta resposta = respostas.get(i);
                linhas.add((i + 1) + ". " + resposta.getResposta());
                linhas.add("   Pergunta: " + textoDaPergunta(resposta.getIdPergunta()));
                linhas.add("   Nota: " + resposta.getNota()
                        + " | Avaliações: " + resposta.getAvaliacoes());
            }
            linhas.add("");
            linhas.add("D - Ver detalhes de uma resposta");
            linhas.add("A - Arquivar uma resposta");
            linhas.add("R - Voltar");

            limparTela();
            moldura("Minhas respostas", linhas);
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim().toUpperCase();

            if (op.equals("R")) {
                return;
            }
            if (!op.equals("D") && !op.equals("A")) {
                erro("Opção inválida.");
                aguardar();
                continue;
            }
            if (respostas.isEmpty()) {
                erro("Não há respostas disponíveis.");
                aguardar();
                continue;
            }

            int n = inteiro("Número da resposta (0 para voltar): ");
            if (n == 0) {
                continue;
            }
            if (n < 1 || n > respostas.size()) {
                erro("Resposta inválida.");
                aguardar();
                continue;
            }

            Resposta resposta = respostas.get(n - 1);
            if (op.equals("D")) {
                mostrarDetalhesDaMinhaResposta(resposta);
            } else if (confirmarArquivamento()) {
                try {
                    banco.arquivarResposta(resposta.getIdResposta(), usuario.getIdUsuario());
                    sucesso("Resposta arquivada. O registro não foi excluído.");
                } catch (IllegalArgumentException e) {
                    erro(e.getMessage());
                }
                aguardar();
            }
        }
    }

    private void mostrarDetalhesDaMinhaResposta(Resposta resposta) {
        List<String> detalhes = new ArrayList<>();
        detalhes.add("Pergunta: " + textoDaPergunta(resposta.getIdPergunta()));
        detalhes.add("");
        detalhes.add("Sua resposta:");
        detalhes.addAll(quebrar(resposta.getResposta(), LARGURA_TEXTO));
        detalhes.add("");
        detalhes.add("Nota: " + resposta.getNota());
        detalhes.add("Avaliações: " + resposta.getAvaliacoes());

        limparTela();
        moldura("Detalhes da resposta #" + resposta.getIdResposta(), detalhes);
        aguardar();
    }

    // ---------- Minhas avaliações ----------

    private void minhasAvaliacoes(Usuario usuario) {
        while (true) {
            List<Avaliacao> lista = banco.avaliacoesDoUsuario(usuario.getIdUsuario());

            List<String> linhas = new ArrayList<>();
            if (lista.isEmpty()) {
                linhas.add("Você ainda não avaliou nenhuma resposta.");
            }
            for (int i = 0; i < lista.size(); i++) {
                Avaliacao avaliacao = lista.get(i);
                Resposta resposta = banco.buscarResposta(avaliacao.get_IdResposta());

                linhas.add("(" + (i + 1) + ") " + (avaliacao.get_avaliacao() ? "Concordo" : "Discordo"));
                if (resposta == null) {
                    linhas.add("    Resposta não encontrada.");
                    continue;
                }
                linhas.add("    Pergunta: " + textoDaPergunta(resposta.getIdPergunta()));
                linhas.add("    Resposta: " + resposta.getResposta());
                linhas.add("    Nota atual da resposta: " + resposta.getNota());
            }
            linhas.add("");
            linhas.add("(C) Cancelar uma avaliação");
            linhas.add("(R) Retornar");

            limparTela();
            moldura("Minhas avaliações", linhas);
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim().toUpperCase();

            if (op.equals("R")) {
                return;
            }
            if (!op.equals("C")) {
                erro("Opção inválida.");
                aguardar();
                continue;
            }
            if (lista.isEmpty()) {
                erro("Não há avaliações para cancelar.");
                aguardar();
                continue;
            }

            int n = inteiro("Número da avaliação (0 para voltar): ");
            if (n == 0) {
                continue;
            }
            if (n < 1 || n > lista.size()) {
                erro("Avaliação inválida.");
                aguardar();
                continue;
            }
            try {
                banco.cancelarAvaliacao(lista.get(n - 1).getId(), usuario.getIdUsuario());
                sucesso("Avaliação cancelada.");
            } catch (IllegalArgumentException e) {
                erro(e.getMessage());
            }
            aguardar();
        }
    }

    // ==================== 5. Apoio ====================

    // ---------- Pausas ----------

    private void aguardar() {
        pausar(ATRASO_MS);
    }

    private void aguardarRegistro() {
        System.out.println(RODAPE_COR + "Salvando no sistema..." + RESET);
        pausar(ATRASO_REGISTRO_MS);
    }

    /**
     * Pausa para o usuário ler a mensagem: desliga o eco do teclado (stty), espera e
     * descarta o que foi digitado durante a espera. Se o terminal não tiver o comando
     * stty (por exemplo, o PowerShell do Windows), a pausa é pulada.
     */
    private void pausar(long milissegundos) {
        try {
            new ProcessBuilder("stty", "-echo").inheritIO().start().waitFor();
            Thread.sleep(milissegundos);
            while (System.in.available() > 0) System.in.read();
        } catch (Exception ignored) {
        } finally {
            try {
                new ProcessBuilder("stty", "echo").inheritIO().start().waitFor();
            } catch (Exception ignored) {
            }
        }
    }

    // ---------- Saída ----------

    private void limparTela() {
        System.out.print("\033[2J\033[3J\033[H");
        System.out.flush();
    }

    private void sucesso(String msg) {
        System.out.println(VERDE + msg + RESET);
    }

    private void erro(String msg) {
        System.out.println(VERMELHO + msg + RESET);
    }

    private void moldura(String titulo, String... linhas) {
        moldura(titulo, List.of(linhas));
    }

    private void moldura(String titulo, List<String> linhas) {
        int largura = titulo.length();
        for (String l : linhas) largura = Math.max(largura, l.length());
        largura += 4;
        String linhaBorda = "═".repeat(largura);
        System.out.println(BORDA + "╔" + linhaBorda + "╗" + RESET);
        System.out.println(BORDA + "║ " + RESET + TITULO_COR + centralizar(titulo, largura - 2) + RESET + BORDA + " ║" + RESET);
        System.out.println(BORDA + "╠" + linhaBorda + "╣" + RESET);
        for (String l : linhas) {
            System.out.println(BORDA + "║ " + RESET + OPCAO_COR + alinharEsquerda(l, largura - 2) + RESET + BORDA + " ║" + RESET);
        }
        System.out.println(BORDA + "╚" + linhaBorda + "╝" + RESET);
        System.out.println(RODAPE_COR + NEGRITO + alinharDireita("PUC Minas", largura + 2) + RESET);
    }

    private String centralizar(String texto, int largura) {
        int espacos = largura - texto.length();
        int esquerda = espacos / 2;
        return " ".repeat(Math.max(0, esquerda)) + texto + " ".repeat(Math.max(0, espacos - esquerda));
    }

    private String alinharEsquerda(String texto, int largura) {
        return texto + " ".repeat(Math.max(0, largura - texto.length()));
    }

    private String alinharDireita(String texto, int largura) {
        return " ".repeat(Math.max(0, largura - texto.length())) + texto;
    }

    /** Divide um texto em linhas de no máximo {@code max} caracteres, quebrando entre palavras. */
    private List<String> quebrar(String texto, int max) {
        List<String> linhas = new ArrayList<>();
        String atual = "";
        for (String palavra : (texto == null ? "" : texto).split(" ")) {
            if (!atual.isEmpty() && atual.length() + 1 + palavra.length() > max) {
                linhas.add(atual);
                atual = palavra;
            } else {
                atual = atual.isEmpty() ? palavra : atual + " " + palavra;
            }
        }
        linhas.add(atual);
        return linhas;
    }

    private String formatarData(long milissegundos) {
        return DATA.format(Instant.ofEpochMilli(milissegundos));
    }

    private String nomeDoUsuario(Usuario usuario) {
        return usuario == null ? "desconhecido" : usuario.getNome();
    }

    private String textoDaPergunta(int idPergunta) {
        Pergunta pergunta = banco.buscarPergunta(idPergunta);
        return pergunta == null ? "Não encontrada" : pergunta.getPergunta();
    }

    // ---------- Entrada ----------

    private int opcao() {
        return inteiro("Opção: ");
    }

    private int inteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String valor = entrada.nextLine().trim();
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                erro("Digite um número válido.");
            }
        }
    }

    private String textoObrigatorio(String mensagem) {
        return textoObrigatorio(mensagem, null);
    }

    /** Lê um texto não vazio; se {@code valorPadrao} existir, Enter vazio mantém esse valor. */
    private String textoObrigatorio(String mensagem, String valorPadrao) {
        while (true) {
            System.out.print(mensagem);
            String valor = entrada.nextLine().trim();
            if (valor.isEmpty() && valorPadrao != null) {
                return valorPadrao;
            }
            if (!valor.isEmpty()) {
                return valor;
            }
            erro("Este campo é obrigatório.");
        }
    }

    private boolean confirmarArquivamento() {
        System.out.print("Confirma o arquivamento? (S/N): ");
        return entrada.nextLine().trim().equalsIgnoreCase("S");
    }
}
