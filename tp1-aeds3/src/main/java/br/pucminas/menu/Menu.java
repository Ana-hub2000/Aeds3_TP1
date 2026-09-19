package br.pucminas.menu;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import br.pucminas.security.Seguranca;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    private final BancoDados banco;
    private final Scanner entrada;

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
        } catch (IllegalArgumentException e) {
            erro(e.getMessage());
        }
        aguardar();
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
        aguardar();
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
                case 2 -> System.out.println("A busca global ainda não foi implementada.");
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
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
                    "4 - Meus votos", "0 - Voltar");
            switch (opcao()) {
                case 1 -> meusDados(usuario);
                case 2 -> minhasPerguntas(usuario);
                case 3, 4 -> System.out.println("Esta funcionalidade ainda não foi implementada.");
                case 0 -> continuar = false;
                default -> erro("Opção inválida.");
            }
        }
    }

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
            } catch (IllegalArgumentException e) {
                erro(e.getMessage());
            }
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

    private void incluirPergunta(Usuario usuario) {
        String texto = textoObrigatorio("Texto da pergunta: ");
        String palavras = textoObrigatorio("Palavras-chave (separadas por ponto e vírgula): ");
        banco.criarPergunta(new Pergunta(usuario.getIdUsuario(), texto, palavras));
        sucesso("Pergunta incluída com sucesso.");
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
    }

    private Pergunta selecionarPergunta(List<Pergunta> perguntas) {
        if (perguntas.isEmpty()) {
            erro("Não há perguntas para selecionar.");
            return null;
        }
        int numero = inteiro("Número da pergunta: ");
        if (numero < 1 || numero > perguntas.size()) {
            erro("Número inválido.");
            return null;
        }
        return perguntas.get(numero - 1);
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
                return valor;2
                
            }
            erro("Este campo é obrigatório.");
        }
    }
}