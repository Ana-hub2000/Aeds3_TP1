package br.pucminas.menu;

import br.pucminas.model.Pergunta;
import br.pucminas.model.Usuario;
import br.pucminas.repository.BancoDados;
import br.pucminas.security.Seguranca;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Menu {
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final BancoDados banco;
    private final Scanner entrada;

    public Menu(BancoDados banco, Scanner entrada) {
        this.banco = banco;
        this.entrada = entrada;
    }

    public void executar() {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n=== AJUDA AÍ 1.0 ===");
            System.out.println("1 - Login");
            System.out.println("2 - Novo usuário");
            System.out.println("0 - Sair");
            switch (opcao()) {
                case 1 -> login();
                case 2 -> novoUsuario();
                case 0 -> continuar = false;
                default -> System.out.println("Opção inválida.");
            }
        }
        System.out.println("Até mais!");
    }

    private void novoUsuario() {
        System.out.println("\n--- Novo usuário ---");
        String nome = textoObrigatorio("Nome: ");
        String email = textoObrigatorio("E-mail: ");
        String senha = textoObrigatorio("Senha: ");
        String pergunta = textoObrigatorio("Pergunta secreta: ");
        String resposta = textoObrigatorio("Resposta secreta: ");

        try {
            banco.criarUsuario(new Usuario(nome, email, Seguranca.hashSenha(senha),
                    pergunta, Seguranca.hashResposta(resposta)));
            System.out.println("Usuário cadastrado com sucesso.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void login() {
        while (true) {
            System.out.println("\n--- Login ---");
            String email = textoObrigatorio("E-mail: ");
            String senha = textoObrigatorio("Senha: ");
            Usuario usuario = banco.buscarUsuarioPorEmail(email);
            if (usuario != null && Seguranca.corresponde(senha, usuario.getHashSenha())) {
                System.out.println("Bem-vindo(a), " + usuario.getNome() + "!");
                menuPrincipal(usuario);
                return;
            }

            System.out.println("E-mail ou senha inválidos.");
            System.out.println("1 - Tentar novamente");
            System.out.println("2 - Recuperar senha");
            System.out.println("0 - Voltar");
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
        System.out.println("\n--- Recuperar senha ---");
        Usuario usuario = banco.buscarUsuarioPorEmail(textoObrigatorio("E-mail: "));
        if (usuario == null) {
            System.out.println("Usuário não encontrado.");
            return;
        }
        System.out.println("Pergunta: " + usuario.getPerguntaSecreta());
        String resposta = textoObrigatorio("Resposta: ");
        if (!Seguranca.hashResposta(resposta).equals(usuario.getHashRespostaSecreta())) {
            System.out.println("Resposta secreta incorreta.");
            return;
        }
        usuario.setHashSenha(Seguranca.hashSenha(textoObrigatorio("Nova senha: ")));
        banco.atualizarUsuario(usuario, usuario.getEmail());
        System.out.println("Senha alterada com sucesso.");
    }

    private void menuPrincipal(Usuario usuario) {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n--- Menu principal ---");
            System.out.println("1 - Minha área");
            System.out.println("2 - Buscar perguntas");
            System.out.println("0 - Sair");
            switch (opcao()) {
                case 1 -> minhaArea(usuario);
                case 2 -> System.out.println("A busca global ainda não foi implementada.");
                case 0 -> continuar = false;
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void minhaArea(Usuario usuario) {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n--- Minha área ---");
            System.out.println("1 - Meus dados");
            System.out.println("2 - Minhas perguntas");
            System.out.println("3 - Minhas respostas");
            System.out.println("4 - Meus votos");
            System.out.println("0 - Voltar");
            switch (opcao()) {
                case 1 -> meusDados(usuario);
                case 2 -> minhasPerguntas(usuario);
                case 3, 4 -> System.out.println("Esta funcionalidade ainda não foi implementada.");
                case 0 -> continuar = false;
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void meusDados(Usuario usuario) {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n--- Meus dados ---");
            System.out.println("Nome: " + usuario.getNome());
            System.out.println("E-mail: " + usuario.getEmail());
            System.out.println("1 - Alterar nome");
            System.out.println("2 - Alterar e-mail");
            System.out.println("3 - Alterar senha");
            System.out.println("4 - Alterar pergunta/resposta de recuperação");
            System.out.println("0 - Voltar");
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
                        System.out.println("Opção inválida.");
                        continue;
                    }
                }
                banco.atualizarUsuario(usuario, emailAnterior);
                System.out.println("Dados atualizados.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void minhasPerguntas(Usuario usuario) {
        boolean continuar = true;
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
            switch (opcao()) {
                case 1 -> incluirPergunta(usuario);
                case 2 -> editarPergunta(perguntas);
                case 3 -> arquivarPergunta(perguntas);
                case 0 -> continuar = false;
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void incluirPergunta(Usuario usuario) {
        String texto = textoObrigatorio("Texto da pergunta: ");
        String palavras = textoObrigatorio("Palavras-chave (separadas por ponto e vírgula): ");
        banco.criarPergunta(new Pergunta(usuario.getIdUsuario(), texto, palavras));
        System.out.println("Pergunta incluída com sucesso.");
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
        System.out.println("Pergunta alterada.");
    }

    private void arquivarPergunta(List<Pergunta> perguntas) {
        Pergunta pergunta = selecionarPergunta(perguntas);
        if (pergunta == null) {
            return;
        }
        pergunta.setAtiva(false);
        pergunta.setAlteracao(System.currentTimeMillis());
        banco.atualizarPergunta(pergunta);
        System.out.println("Pergunta arquivada. O registro não foi excluído.");
    }

    private Pergunta selecionarPergunta(List<Pergunta> perguntas) {
        if (perguntas.isEmpty()) {
            System.out.println("Não há perguntas para selecionar.");
            return null;
        }
        int numero = inteiro("Número da pergunta: ");
        if (numero < 1 || numero > perguntas.size()) {
            System.out.println("Número inválido.");
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
                System.out.println("Digite um número válido.");
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
            System.out.println("Este campo é obrigatório.");
        }
    }
}