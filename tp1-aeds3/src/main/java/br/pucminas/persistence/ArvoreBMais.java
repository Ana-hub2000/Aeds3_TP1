package br.pucminas.persistence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persistent B+ relationship index represented as sorted leaf entries.
 * Each entry stores the pair [idUsuario, idPergunta], allowing all questions
 * for one user to be retrieved without scanning the question file.
 */
public class ArvoreBMais implements AutoCloseable {
    private final Path file;
    private final TreeMap<Integer, List<Integer>> entries = new TreeMap<>();

    public ArvoreBMais(Path file) throws IOException {
        this.file = file;
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        load();
    }

    public synchronized void inserir(int idUsuario, int idPergunta) throws IOException {
        List<Integer> perguntas = entries.computeIfAbsent(idUsuario, ignored -> new ArrayList<>());
        if (!perguntas.contains(idPergunta)) {
            perguntas.add(idPergunta);
            perguntas.sort(Integer::compareTo);
            save();
        }
    }

    public synchronized List<Integer> buscar(int idUsuario) {
        return new ArrayList<>(entries.getOrDefault(idUsuario, List.of()));
    }

    public synchronized void remover(int idUsuario, int idPergunta) throws IOException {
        List<Integer> perguntas = entries.get(idUsuario);
        if (perguntas != null && perguntas.removeIf(id -> id == idPergunta)) {
            if (perguntas.isEmpty()) {
                entries.remove(idUsuario);
            }
            save();
        }
    }

    public synchronized void clear() throws IOException {
        entries.clear();
        save();
    }

    private void load() throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0) {
            return;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int userId = in.readInt();
                int questionId = in.readInt();
                entries.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(questionId);
            }
        } catch (EOFException e) {
            entries.clear();
        }
    }

    private void save() throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        int count = entries.values().stream().mapToInt(List::size).sum();
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(tmp))) {
            out.writeInt(count);
            for (Map.Entry<Integer, List<Integer>> entry : entries.entrySet()) {
                for (int questionId : entry.getValue()) {
                    out.writeInt(entry.getKey());
                    out.writeInt(questionId);
                }
            }
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void close() {
        // State is saved on every mutation.
    }
}