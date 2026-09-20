package br.pucminas;

import br.pucminas.menu.Menu;
import br.pucminas.repository.BancoDados;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Path diretorio = Path.of(System.getProperty("ajudaai.data", "dados"));
        try (BancoDados banco = new BancoDados(diretorio);
             Scanner entrada = new Scanner(System.in)) {
            new Menu(banco, entrada).executar();
        } catch (Exception e) {
            System.err.println("Não foi possível iniciar o banco de dados: " + e.getMessage());
        }
    }
}
