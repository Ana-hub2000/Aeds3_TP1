package br.pucminas.persistence;

import aed3.ElementoLista;
import aed3.ListaInvertida;
import br.pucminas.model.Pergunta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class IndicePalavrasChave implements AutoCloseable {
    private final ListaInvertida lista;
    private final boolean precisaReconstruir;

    public IndicePalavrasChave(Path diretorio) throws Exception {
        Path dicionario = diretorio.resolve("palavrasChave.dicionario.db");
        Path blocos = diretorio.resolve("palavrasChave.blocos.db");
        precisaReconstruir = !Files.exists(dicionario) || !Files.exists(blocos)
                || Files.size(dicionario) < Integer.BYTES;
        if (precisaReconstruir) {
            // Nós reiniciamos os dois arquivos juntos quando falta uma parte do índice.
            Files.newOutputStream(dicionario).close();
            Files.newOutputStream(blocos).close();
        }
        lista = new ListaInvertida(5, dicionario.toString(), blocos.toString());
    }

    public boolean precisaReconstruir() {
        return precisaReconstruir;
    }

    public void incluir(Pergunta pergunta) throws Exception {
        if (!pergunta.isAtiva()) {
            return;
        }
        // Nós contamos cada pergunta salva como um documento para o cálculo do IDF.
        lista.incrementaEntidades();
        String[] palavras = TratamentoTexto.palavrasValidas(pergunta.getPalavrasChave());
        if (palavras.length == 0) {
            return;
        }
        float frequenciaTF = 1.0f / palavras.length;
        int idPergunta = pergunta.getIdPergunta();
        for (String termo : palavras) {
            // Nós somamos cada ocorrência: assim, termos repetidos têm TF maior sem duplicar o ID.
            ElementoLista e = lista.read(termo, idPergunta);
            if (e == null) {
                lista.create(termo, new ElementoLista(idPergunta, frequenciaTF));
            } else {
                lista.update(termo, new ElementoLista(idPergunta, frequenciaTF + e.getFrequencia()));
            }
        }
    }

    public void remover(Pergunta pergunta) throws Exception {
        if (!pergunta.isAtiva()) {
            return;
        }
        for (String termo : new LinkedHashSet<>(List.of(
                TratamentoTexto.palavrasValidas(pergunta.getPalavrasChave())))) {
            lista.delete(termo, pergunta.getIdPergunta());
        }
        lista.decrementaEntidades();
    }

    public List<Integer> buscar(String consulta) throws Exception {
        Map<Integer, Double> pontuacoes = new HashMap<>();
        int total = lista.numeroEntidades();
        if (total == 0) {
            return new ArrayList<>();
        }
        // Nós consideramos cada termo da consulta uma vez, mesmo se ele for digitado repetido.
        for (String termo : new LinkedHashSet<>(List.of(TratamentoTexto.palavrasValidas(consulta)))) {
            ElementoLista[] ocorrencias = lista.read(termo);
            if (ocorrencias.length == 0) {
                continue;
            }
            double idf = Math.log10((double) lista.numeroEntidades() / ocorrencias.length);
            for (ElementoLista ocorrencia : ocorrencias) {
                pontuacoes.merge(ocorrencia.getId(), ocorrencia.getFrequencia() * idf, Double::sum);
            }
        }
        List<Integer> ids = new ArrayList<>(pontuacoes.keySet());
        ids.sort((a, b) -> {
            int comparacao = Double.compare(pontuacoes.get(b), pontuacoes.get(a));
            return comparacao != 0 ? comparacao : Integer.compare(a, b);
        });
        return ids;
    }

    @Override
    public void close() throws Exception {
        lista.close();
    }
}