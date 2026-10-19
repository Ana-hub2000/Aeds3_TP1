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


/* 
 essa classe serve para mosatrar ao usuario o menu do sistema, e para interagir com ele, chamando os metodos do banco de dados,
 foi feita utilizando maven e lanterna, porem por utulizar o lanterna, foi necessario fazer algumas adaptações, como por exemplo,
  utilizar o System.out.println para mostrar as mensagens na tela, e o Scanner para ler a entrada do usuario.
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

    private final BancoDados banco;
    private final Scanner entrada;
     private Usuario usuarioLogado;

    public Menu(BancoDados banco, Scanner entrada) {
        this.banco = banco;
        this.entrada = entrada;
    }

    private void limparTela() {
        System.out.print("\033[2J\033[3J\033[H");
        System.out.flush();
    }

    private void aguardar() {
        try {
            new ProcessBuilder("stty", "-echo").inheritIO().start().waitFor();
            Thread.sleep(ATRASO_MS);
            while (System.in.available() > 0) System.in.read();
        } catch (Exception ignored) {
        } finally {
            try {
                new ProcessBuilder("stty", "echo").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
    }

    private void aguardarRegistro() {
        System.out.println(RODAPE_COR + "Salvando no sistema..." + RESET);
        try {
            new ProcessBuilder("stty", "-echo").inheritIO().start().waitFor();
            Thread.sleep(ATRASO_REGISTRO_MS);
            while (System.in.available() > 0) System.in.read();
        } catch (Exception ignored) {
        } finally {
            try {
                new ProcessBuilder("stty", "echo").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
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

    /**
     * É a tela inicial com as opções de:
     * * Login
     * * Cadastro
     * * Sair
     * É um modal de redirecionamento de tela
     */
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
                if (usuario != null && Seguranca.corresponde(senha, usuario.getHashSenha())) {
    sucesso("Bem-vindo(a), " + usuario.getNome() + "!");
    aguardar();
    usuarioLogado = usuario;
    menuPrincipal(usuario);
    return;
}
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
                case 2 -> buscarPerguntas();
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }

    // ==================== TP2 - Pessoa 1: busca e paginação ====================

    private static final int POR_PAGINA = 5;

    /** Menu de busca: A = mais recentes, B = palavras-chave (outra etapa). */
    private void buscarPerguntas() {
        while (true) {
            limparTela();
            moldura("Buscar perguntas", "A - Perguntas mais recentes",
                    "B - Buscar por palavras-chave", "0 - Voltar");
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim().toUpperCase();
            switch (op) {
                case "A" -> listarRecentesPaginado();
                case "B" -> { erro("Busca por palavras-chave ainda não implementada."); aguardar(); }
                case "0" -> { return; }
                default -> { erro("Opção inválida."); aguardar(); }
            }
        }
    }

    /** Lista as perguntas (ordem decrescente da Árvore B+), 5 por página. */
    private void listarRecentesPaginado() {
        List<Pergunta> todas = banco.perguntasRecentes();
        int totalPaginas = Math.max(1, (todas.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pagina = 0;
        while (true) {
            limparTela();
            int ini = pagina * POR_PAGINA;
            int fim = Math.min(ini + POR_PAGINA, todas.size());
            List<String> linhas = new ArrayList<>();
            if (todas.isEmpty()) {
                linhas.add("Nenhuma pergunta cadastrada.");
            }
            for (int i = ini; i < fim; i++) {
                Pergunta p = todas.get(i);
                linhas.add("[ID " + p.getIdPergunta() + "] " + p.getPergunta());
                linhas.add("   " + DATA.format(Instant.ofEpochMilli(p.getCriacao()))
                        + " | Nota: " + p.getNota());
            }
            linhas.add("");
            linhas.add("Página " + (pagina + 1) + " de " + totalPaginas);
            linhas.add("> - Próxima | < - Anterior | ID - Ver detalhes | 0 - Voltar");
            moldura("Perguntas mais recentes", linhas);
            System.out.print("Opção: ");
            String op = entrada.nextLine().trim();
            if (op.equals("0")) return;
            if (op.equals(">")) {
                if (pagina < totalPaginas - 1) pagina++; else { erro("Já está na última página."); aguardar(); }
            } else if (op.equals("<")) {
                if (pagina > 0) pagina--; else { erro("Já está na primeira página."); aguardar(); }
            } else {
                try {
                  mostrarDetalhes(Integer.parseInt(op), usuarioLogado);
                } catch (NumberFormatException e) {
                    erro("Opção inválida.");
                    aguardar();
                }
            }
        }
    }

    /** Mostra todos os dados de uma pergunta e, abaixo, suas respostas. */
  private void mostrarDetalhes(int id, Usuario usuario) {
    while (true) {
        Pergunta p = banco.buscarPergunta(id);
        if (p == null || !p.isAtiva()) { erro("Pergunta não encontrada."); aguardar(); return; }
        Usuario autor = banco.buscarUsuario(p.getIdUsuario());
        List<Resposta> respostas = banco.respostasDaPergunta(id);
        List<String> linhas = new ArrayList<>();
        linhas.add("Início > Buscar perguntas > Pergunta");
        linhas.add("");
        linhas.addAll(quebrar(p.getPergunta(), 60));
        linhas.add("");
        linhas.add("Criada por " + (autor == null ? "desconhecido" : autor.getNome()) + " em " + DATA.format(Instant.ofEpochMilli(p.getCriacao())));
        linhas.add("Palavras-chave: " + p.getPalavrasChave());
        linhas.add("");
        linhas.add("RESPOSTAS");
        linhas.add("---------");
        if (respostas.isEmpty()) linhas.add("Nenhuma resposta cadastrada.");
        for (int i = 0; i < respostas.size(); i++) {
            Resposta r = respostas.get(i);
            Usuario a = banco.buscarUsuario(r.getIdUsuario());
            List<String> partes = quebrar(r.getResposta(), 60);
            linhas.add("");
            linhas.add("(" + (i + 1) + ") " + partes.get(0));
            for (int j = 1; j < partes.size(); j++) linhas.add("    " + partes.get(j));
            linhas.add("    Respondido por " + (a == null ? "desconhecido" : a.getNome()));
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
            case "R":
                return;
            case "P":
                responderPergunta(p, autor, usuario);
                break;
            case "A":
                selecionarRespostaParaAvaliar(respostas, usuario);
                break;
            default:
                erro("Opção inválida.");
                aguardar();
                break;
        }
    }
}
private void responderPergunta(Pergunta p, Usuario criador, Usuario usuario) {
    limparTela();
    moldura("AJUDA AÍ 1.0",
            "> Início > Buscar perguntas > Pergunta > Responder", "",
            p.getPergunta(),
            "Criada por " + (criador == null ? "desconhecido" : criador.getNome()) + " em " + DATA.format(Instant.ofEpochMilli(p.getCriacao())), "",
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
  
private void listarRespostasDaPergunta(int idPergunta, Usuario usuario) {
    List<Resposta> respostas = banco.respostasDaPergunta(idPergunta);

    limparTela();
    moldura("Pergunta", "RESPOSTAS");

    if (respostas.isEmpty()) {
        System.out.println("Nenhuma resposta cadastrada.");
    } else {
        for (int i = 0; i < respostas.size(); i++) {
            Resposta resposta = respostas.get(i);
            Usuario autor = banco.buscarUsuario(resposta.getIdUsuario());

            System.out.println("\n(" + (i + 1) + ") " + resposta.getResposta());
            System.out.println("Respondido por "
                    + (autor == null ? "desconhecido" : autor.getNome()));
            System.out.println("Nota: " + resposta.getNota()
                    + " (" + resposta.getAvaliacoes() + " avaliações)");
        }
    }

    System.out.println("\n(A) Avaliar uma resposta");
    System.out.println("(R) Retornar");
    System.out.print("\nOpção: ");

    String opcao = entrada.nextLine().trim();

    switch (opcao.toUpperCase()) {
        case "A" -> selecionarRespostaParaAvaliar(respostas, usuario);
        case "R" -> {
            // Retorna à tela anterior.
        }
        default -> erro("Opção inválida.");
    }
}


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
                case 3 -> System.out.println("Esta funcionalidade ainda não foi implementada.");
               case 4 -> minhasAvaliacoes(usuario);
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }
    private void minhasAvaliacoes(Usuario usuario) {
    while (true) {
        List<Avaliacao> lista = banco.avaliacoesDoUsuario(usuario.getIdUsuario());
        List<String> linhas = new ArrayList<>();
        if (lista.isEmpty()) linhas.add("Você ainda não avaliou nenhuma resposta.");
        for (int i = 0; i < lista.size(); i++) {
            Avaliacao a = lista.get(i);
            Resposta r = banco.buscarResposta(a.get_IdResposta());
            linhas.add("(" + (i + 1) + ") " + (r == null ? "(resposta não encontrada)" : quebrar(r.getResposta(), 50).get(0)));
            linhas.add("    " + (a.get_avaliacao() ? "Positiva" : "Negativa"));
        }
        linhas.add("");
        linhas.add("(C) Cancelar uma avaliação");
        linhas.add("(R) Retornar");
        limparTela();
        moldura("Minhas avaliações", linhas);
        System.out.print("Opção: ");
        String op = entrada.nextLine().trim().toUpperCase();
        if (op.equals("R")) return;
        if (!op.equals("C")) { erro("Opção inválida."); aguardar(); continue; }
        if (lista.isEmpty()) { erro("Não há avaliações para cancelar."); aguardar(); continue; }
        int n = inteiro("Número da avaliação (0 para voltar): ");
        if (n == 0) continue;
        if (n < 1 || n > lista.size()) { erro("Avaliação inválida."); aguardar(); continue; }
        try {
            banco.cancelarAvaliacao(lista.get(n - 1).getId(), usuario.getIdUsuario());
            sucesso("Avaliação cancelada.");
        } catch (IllegalArgumentException e) {
            erro(e.getMessage());
        }
        aguardar();
    }
}
    /*
     * cria a interface com os dados do usuário
    */
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

private void testarAvaliacoes(Usuario usuario) {
    while (true) {
        limparTela();
        moldura("Minhas avaliações",
                "1 - Nova avaliação",
                "0 - Voltar");

        switch (opcao()) {
            case 1 -> novaAvaliacao(usuario);
            case 0 -> {
                return;
            }
            default -> {
                erro("Opção inválida.");
                aguardar();
            }
        }
    }
}

private void novaAvaliacao(Usuario usuario) {
    limparTela();
    moldura("Nova avaliação");

    int idResposta = inteiro("ID da resposta: ");

    System.out.println("1 - Positiva");
    System.out.println("2 - Negativa");

    int escolha = inteiro("Sua avaliação: ");

    if (escolha != 1 && escolha != 2) {
        erro("Escolha inválida.");
        aguardar();
        return;
    }

    boolean positiva = escolha == 1;

    Avaliacao avaliacao = new Avaliacao(
            -1,
            usuario.getIdUsuario(),
            idResposta,
            positiva
    );

    limparTela();
    moldura("Conferir avaliação",
            "ID da avaliação: " + avaliacao.getId(),
            "ID do usuário: " + avaliacao.get_IdUsuario(),
            "ID da resposta: " + avaliacao.get_IdResposta(),
            "Tipo: " + (avaliacao.get_avaliacao()
                    ? "Positiva" : "Negativa"));

    System.out.println();
    System.out.println("1 - Confirmar teste");
    System.out.println("0 - Cancelar");

    if (opcao() == 1) {
        try {
            byte[] dados = avaliacao.serialize();

            Avaliacao recuperada = new Avaliacao();
            recuperada.deserialize(dados);

            sucesso("Serialização conferida!");
            System.out.println("Tamanho: " + dados.length + " bytes");
            System.out.println("ID do usuário recuperado: "
                    + recuperada.get_IdUsuario());
            System.out.println("ID da resposta recuperado: "
                    + recuperada.get_IdResposta());
            System.out.println("Avaliação recuperada: "
                    + (recuperada.get_avaliacao()
                    ? "Positiva" : "Negativa"));
        } catch (Exception e) {
            erro("Erro ao testar a avaliação: " + e.getMessage());
        }
    } else {
        System.out.println("Teste cancelado.");
    }

    System.out.println("Pressione Enter para continuar...");
    entrada.nextLine();
}
private void avaliarResposta(Usuario usuario, Resposta resposta) {
    System.out.println("(A) Concordar com a resposta");
    System.out.println("(B) Discordar da resposta");
    System.out.println("(R) Retornar ao menu anterior");
    System.out.print("\nOpção: ");

    String opcao = entrada.nextLine().trim();

    switch (opcao.toUpperCase()) {
        case "A", "B" -> {
            boolean positiva = opcao.equalsIgnoreCase("A");

            Avaliacao avaliacao = new Avaliacao(
                -1,
                usuario.getIdUsuario(),
                resposta.getIdResposta(),
                positiva
            );

            try {
               banco.criarAvaliacao(avaliacao);
sucesso("Avaliação registrada com sucesso!");
aguardar();
} catch (IllegalArgumentException e) {
    erro(e.getMessage());
    aguardar();
}
        }
        case "R" -> {
            // Volta à tela anterior.
        }
        default -> erro("Opção inválida.");
    }
}


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
                    linhas.add("   Palavras-chave: " + pergunta.getPalavrasChave() + " | Nota: "
                            + pergunta.getNota() + " | Criada: " + DATA.format(Instant.ofEpochMilli(pergunta.getCriacao())));
                }
            }
            linhas.add("");
            linhas.add("1 - Incluir pergunta");
            linhas.add("2 - Alterar pergunta");
            linhas.add("3 - Arquivar pergunta");
            linhas.add("0 - Voltar");
            moldura("Minhas perguntas", linhas);
            switch (opcao()) {
                case 1 -> incluirPergunta(usuario);
                case 2 -> editarPergunta(perguntas);
                case 3 -> arquivarPergunta(perguntas);
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }
    private Pergunta selecionarPergunta(List<Pergunta> perguntas) {
    if (perguntas.isEmpty()) { erro("Nenhuma pergunta disponível."); aguardar(); return null; }
    int n = inteiro("Número da pergunta (0 para voltar): ");
    if (n < 1 || n > perguntas.size()) return null;
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
    private List<String> quebrar(String texto, int max) {
    List<String> out = new ArrayList<>();
    String atual = "";
    for (String w : texto.split(" ")) {
        if (!atual.isEmpty() && atual.length() + 1 + w.length() > max) { out.add(atual); atual = w; }
        else atual = atual.isEmpty() ? w : atual + " " + w;
    }
    out.add(atual);
    return out;
}

   
private void selecionarRespostaParaAvaliar(
        List<Resposta> respostas, Usuario usuario) {

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

    Resposta resposta = respostas.get(numero - 1);
    avaliarResposta(usuario, resposta);
}


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
}
