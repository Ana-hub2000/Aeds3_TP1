package br.pucminas;

import br.pucminas.menu.Menu;
import br.pucminas.repository.BancoDados;

<<<<<<< HEAD
import java.io.IOException;
=======
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
<<<<<<< HEAD
        Path diretorio = Path.of(System.getProperty("ajudaai.data", "data"));
        try (BancoDados banco = new BancoDados(diretorio);
             Scanner entrada = new Scanner(System.in)) {
            new Menu(banco, entrada).executar();
        } catch (IOException e) {
            System.err.println("Não foi possível iniciar o banco de dados: " + e.getMessage());
        }
    }
}
=======
        Path diretorio = Path.of(System.getProperty("ajudaai.data", "dados"));
        try (BancoDados banco = new BancoDados(diretorio);
             Scanner entrada = new Scanner(System.in)) {
            new Menu(banco, entrada).executar();
        } catch (Exception e) {
            System.err.println("Não foi possível iniciar o banco de dados: " + e.getMessage());
        }
    }
}
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
