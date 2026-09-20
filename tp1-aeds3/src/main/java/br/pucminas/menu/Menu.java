package br.pucminas.menu;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import br.pucminas.security.Seguranca;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
<<<<<<< HEAD
import java.util.List;
import java.util.Scanner;

=======
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
/* 
 essa classe serve para mosatrar ao usuario o menu do sistema, e para interagir com ele, chamando os metodos do banco de dados,
 foi feita utilizando maven e lanterna, porem por utulizar o lanterna, foi necessario fazer algumas adaptações, como por exemplo,
  utilizar o System.out.println para mostrar as mensagens na tela, e o Scanner para ler a entrada do usuario.
*/
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
public class Menu {
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());
<<<<<<< HEAD
=======
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
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b

    private final BancoDados banco;
    private final Scanner entrada;

    public Menu(BancoDados banco, Scanner entrada) {
        this.banco = banco;
        this.entrada = entrada;
    }

<<<<<<< HEAD
    public void executar() {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n=== AJUDA AÍ 1.0 ===");
            System.out.println("1 - Login");
            System.out.println("2 - Novo usuário");
            System.out.println("0 - Sair");
=======
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

    public void executar() {
        boolean continuar = true;
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("AJUDA AÍ 1.0", "1 - Login", "2 - Novo usuário", "0 - Sair");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            switch (opcao()) {
                case 1 -> login();
                case 2 -> novoUsuario();
                case 0 -> continuar = false;
<<<<<<< HEAD
                default -> System.out.println("Opção inválida.");
            }
        }
=======
                default -> erro("Opção inválida.");
            }
        }
        aguardar();
        limparTela();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        System.out.println("Até mais!");
    }

    private void novoUsuario() {
<<<<<<< HEAD
        System.out.println("\n--- Novo usuário ---");
=======
        limparTela();
        moldura("Novo usuário");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        String nome = textoObrigatorio("Nome: ");
        String email = textoObrigatorio("E-mail: ");
        String senha = textoObrigatorio("Senha: ");
        String pergunta = textoObrigatorio("Pergunta secreta: ");
        String resposta = textoObrigatorio("Resposta secreta: ");

        try {
            banco.criarUsuario(new Usuario(nome, email, Seguranca.hashSenha(senha),
                    pergunta, Seguranca.hashResposta(resposta)));
<<<<<<< HEAD
            System.out.println("Usuário cadastrado com sucesso.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
=======
            sucesso("Usuário cadastrado com sucesso.");
            aguardarRegistro();
        } catch (IllegalArgumentException e) {
            erro(e.getMessage());
            aguardar();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        }
    }

    private void login() {
<<<<<<< HEAD
        while (true) {
            System.out.println("\n--- Login ---");
=======
        boolean primeiraVez = true;
        while (true) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Login");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            String email = textoObrigatorio("E-mail: ");
            String senha = textoObrigatorio("Senha: ");
            Usuario usuario = banco.buscarUsuarioPorEmail(email);
            if (usuario != null && Seguranca.corresponde(senha, usuario.getHashSenha())) {
<<<<<<< HEAD
                System.out.println("Bem-vindo(a), " + usuario.getNome() + "!");
                menuPrincipal(usuario);
                return;
            }

            System.out.println("E-mail ou senha inválidos.");
            System.out.println("1 - Tentar novamente");
            System.out.println("2 - Recuperar senha");
            System.out.println("0 - Voltar");
=======
                sucesso("Bem-vindo(a), " + usuario.getNome() + "!");
                aguardar();
                menuPrincipal(usuario);
                return;
            }
            aguardar();
            limparTela();
            moldura("Login inválido", "1 - Tentar novamente", "2 - Recuperar senha", "0 - Voltar");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
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
<<<<<<< HEAD
        System.out.println("\n--- Recuperar senha ---");
        Usuario usuario = banco.buscarUsuarioPorEmail(textoObrigatorio("E-mail: "));
        if (usuario == null) {
            System.out.println("Usuário não encontrado.");
=======
        limparTela();
        moldura("Recuperar senha");
        Usuario usuario = banco.buscarUsuarioPorEmail(textoObrigatorio("E-mail: "));
        if (usuario == null) {
            erro("Usuário não encontrado.");
            aguardar();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            return;
        }
        System.out.println("Pergunta: " + usuario.getPerguntaSecreta());
        String resposta = textoObrigatorio("Resposta: ");
        if (!Seguranca.hashResposta(resposta).equals(usuario.getHashRespostaSecreta())) {
<<<<<<< HEAD
            System.out.println("Resposta secreta incorreta.");
=======
            erro("Resposta secreta incorreta.");
            aguardar();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            return;
        }
        usuario.setHashSenha(Seguranca.hashSenha(textoObrigatorio("Nova senha: ")));
        banco.atualizarUsuario(usuario, usuario.getEmail());
<<<<<<< HEAD
        System.out.println("Senha alterada com sucesso.");
=======
        sucesso("Senha alterada com sucesso.");
        aguardarRegistro();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    private void menuPrincipal(Usuario usuario) {
        boolean continuar = true;
<<<<<<< HEAD
        while (continuar) {
            System.out.println("\n--- Menu principal ---");
            System.out.println("1 - Minha área");
            System.out.println("2 - Buscar perguntas");
            System.out.println("0 - Sair");
=======
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Menu principal", "1 - Minha área", "2 - Buscar perguntas", "0 - Sair");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            switch (opcao()) {
                case 1 -> minhaArea(usuario);
                case 2 -> System.out.println("A busca global ainda não foi implementada.");
                case 0 -> continuar = false;
<<<<<<< HEAD
                default -> System.out.println("Opção inválida.");
=======
                default -> erro("Opção inválida.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            }
        }
    }

    private void minhaArea(Usuario usuario) {
        boolean continuar = true;
<<<<<<< HEAD
        while (continuar) {
            System.out.println("\n--- Minha área ---");
            System.out.println("1 - Meus dados");
            System.out.println("2 - Minhas perguntas");
            System.out.println("3 - Minhas respostas");
            System.out.println("4 - Meus votos");
            System.out.println("0 - Voltar");
=======
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Minha área", "1 - Meus dados", "2 - Minhas perguntas", "3 - Minhas respostas",
                    "4 - Meus votos", "0 - Voltar");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            switch (opcao()) {
                case 1 -> meusDados(usuario);
                case 2 -> minhasPerguntas(usuario);
                case 3, 4 -> System.out.println("Esta funcionalidade ainda não foi implementada.");
                case 0 -> continuar = false;
<<<<<<< HEAD
                default -> System.out.println("Opção inválida.");
=======
                default -> erro("Opção inválida.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            }
        }
    }

    private void meusDados(Usuario usuario) {
        boolean continuar = true;
<<<<<<< HEAD
        while (continuar) {
            System.out.println("\n--- Meus dados ---");
            System.out.println("Nome: " + usuario.getNome());
            System.out.println("E-mail: " + usuario.getEmail());
            System.out.println("1 - Alterar nome");
            System.out.println("2 - Alterar e-mail");
            System.out.println("3 - Alterar senha");
            System.out.println("4 - Alterar pergunta/resposta de recuperação");
            System.out.println("0 - Voltar");
=======
        boolean primeiraVez = true;
        while (continuar) {
            if (!primeiraVez) aguardar();
            primeiraVez = false;
            limparTela();
            moldura("Meus dados",
                    "Nome: " + usuario.getNome(), "E-mail: " + usuario.getEmail(), "",
                    "1 - Alterar nome", "2 - Alterar e-mail", "3 - Alterar senha",
                    "4 - Alterar pergunta/resposta de recuperação", "0 - Voltar");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
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
<<<<<<< HEAD
                        System.out.println("Opção inválida.");
=======
                        erro("Opção inválida.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
                        continue;
                    }
                }
                banco.atualizarUsuario(usuario, emailAnterior);
<<<<<<< HEAD
                System.out.println("Dados atualizados.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
=======
                sucesso("Dados atualizados.");
                aguardarRegistro();
            } catch (IllegalArgumentException e) {
                erro(e.getMessage());
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            }
        }
    }

    private void minhasPerguntas(Usuario usuario) {
        boolean continuar = true;
<<<<<<< HEAD
        while (continuar) {
            List<Pergunta> perguntas = banco.perguntasDoUsuario(usuario.getIdUsuario());
            System.out.println("\n--- Minhas perguntas ---");
            if (perguntas.isEmpty()) {
                System.out.println("Você ainda não possui perguntas ativas.");
            } else {
                for (int i = 0; i < perguntas.size(); i++) {
                    Pergunta pergunta = perguntas.get(i);
                    System.out.printf("%d. %s%n   Palavras-chave: %s | Nota: %d | Criada: %s%n",
                            i + 1, pergunta.getPergunta(), pergunta.getPalavrasChave(),
                            pergunta.getNota(), DATA.format(Instant.ofEpochMilli(pergunta.getCriacao())));
                }
            }
            System.out.println("1 - Incluir pergunta");
            System.out.println("2 - Alterar pergunta");
            System.out.println("3 - Arquivar pergunta");
            System.out.println("0 - Voltar");
=======
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
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            switch (opcao()) {
                case 1 -> incluirPergunta(usuario);
                case 2 -> editarPergunta(perguntas);
                case 3 -> arquivarPergunta(perguntas);
                case 0 -> continuar = false;
<<<<<<< HEAD
                default -> System.out.println("Opção inválida.");
=======
                default -> erro("Opção inválida.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            }
        }
    }

    private void incluirPergunta(Usuario usuario) {
        String texto = textoObrigatorio("Texto da pergunta: ");
        String palavras = textoObrigatorio("Palavras-chave (separadas por ponto e vírgula): ");
        banco.criarPergunta(new Pergunta(usuario.getIdUsuario(), texto, palavras));
<<<<<<< HEAD
        System.out.println("Pergunta incluída com sucesso.");
=======
        sucesso("Pergunta incluída com sucesso.");
        aguardarRegistro();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
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
<<<<<<< HEAD
        System.out.println("Pergunta alterada.");
=======
        sucesso("Pergunta alterada.");
        aguardarRegistro();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    private void arquivarPergunta(List<Pergunta> perguntas) {
        Pergunta pergunta = selecionarPergunta(perguntas);
        if (pergunta == null) {
            return;
        }
        pergunta.setAtiva(false);
        pergunta.setAlteracao(System.currentTimeMillis());
        banco.atualizarPergunta(pergunta);
<<<<<<< HEAD
        System.out.println("Pergunta arquivada. O registro não foi excluído.");
=======
        sucesso("Pergunta arquivada. O registro não foi excluído.");
        aguardarRegistro();
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    private Pergunta selecionarPergunta(List<Pergunta> perguntas) {
        if (perguntas.isEmpty()) {
<<<<<<< HEAD
            System.out.println("Não há perguntas para selecionar.");
=======
            erro("Não há perguntas para selecionar.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
            return null;
        }
        int numero = inteiro("Número da pergunta: ");
        if (numero < 1 || numero > perguntas.size()) {
<<<<<<< HEAD
            System.out.println("Número inválido.");
=======
            erro("Número inválido.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
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
<<<<<<< HEAD
                System.out.println("Digite um número válido.");
=======
                erro("Digite um número válido.");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
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
<<<<<<< HEAD
            System.out.println("Este campo é obrigatório.");
        }
    }
}
=======
            erro("Este campo é obrigatório.");
        }
    }
}
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
