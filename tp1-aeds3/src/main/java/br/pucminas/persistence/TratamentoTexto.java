package br.pucminas.persistence;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class TratamentoTexto {
    private static final HashSet<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "ao", "aos", "aquela", "aquelas", "aquele", "aqueles", "aquilo", "as",
            "ate", "com", "como", "da", "das", "de", "dela", "delas", "dele", "deles",
            "depois", "do", "dos", "e", "ela", "elas", "ele", "eles", "em", "entre",
            "era", "eram", "essa", "essas", "esse", "esses", "esta", "estao", "estas",
            "este", "estes", "eu", "foi", "foram", "ha", "isso", "isto", "ja", "lhe",
            "lhes", "mais", "mas", "me", "mesmo", "meu", "meus", "minha", "minhas",
            "muito", "na", "nas", "nao", "nem", "no", "nos", "nossa", "nossas", "nosso",
            "nossos", "num", "numa", "o", "os", "ou", "para", "pela", "pelas", "pelo",
            "pelos", "por", "porque", "qual", "quando", "que", "quem", "se", "sem",
            "ser", "seu", "seus", "so", "sobre", "somos", "sua", "suas", "tambem",
            "te", "tem", "tendo", "ter", "teu", "teus", "tu", "tua", "tuas", "um",
            "uma", "umas", "uns", "voce", "voces", "vos"));

    private TratamentoTexto() {
    }

    public static String[] palavrasValidas(String texto) {
        if (texto == null || texto.isBlank()) {
            return new String[0];
        }
        // Nós tratamos cadastro e consulta do mesmo jeito, para não perder correspondências.
        String normalizado = Normalizer.normalize(texto.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        return removerStopWords(normalizado.split("\\s+"));
    }

    public static String[] removerStopWords(String[] palavras) {
        List<String> validas = new ArrayList<>();
        for (String palavra : palavras) {
            if (!palavra.isEmpty() && !STOP_WORDS.contains(palavra)) {
                validas.add(palavra);
            }
        }
        return validas.toArray(new String[0]);
    }
}