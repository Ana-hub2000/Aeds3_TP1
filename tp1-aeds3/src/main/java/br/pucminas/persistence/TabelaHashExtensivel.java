package br.pucminas.persistence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extendible hash index for a String key and an integer record id.
 * The index is indirect: the value is the id stored in a CRUD file.
 */
public class TabelaHashExtensivel implements AutoCloseable {
    private static final int BUCKET_CAPACITY = 4;

    private final Path file;
    private int globalDepth = 1;
    private List<Bucket> directory = new ArrayList<>();

    public TabelaHashExtensivel(Path file) throws IOException {
        this.file = file;
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        for (int i = 0; i < 2; i++) {
            directory.add(new Bucket(1));
        }
        load();
    }

    public synchronized Integer get(String key) {
        if (key == null) {
            return null;
        }
        return bucket(key).find(key);
    }

    public synchronized boolean contains(String key) {
        return get(key) != null;
    }

    public synchronized void put(String key, int value) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Hash index keys cannot be blank");
        }
        Bucket bucket = bucket(key);
        Integer existing = bucket.find(key);
        if (existing != null) {
            bucket.put(key, value);
            save();
            return;
        }
        if (bucket.entries.size() >= BUCKET_CAPACITY) {
            split(bucket);
            put(key, value);
            return;
        }
        bucket.put(key, value);
        save();
    }

    public synchronized void remove(String key) throws IOException {
        if (key != null && bucket(key).entries.removeIf(entry -> entry.key().equals(key))) {
            save();
        }
    }

    public synchronized void clear() throws IOException {
        globalDepth = 1;
        directory = new ArrayList<>();
        directory.add(new Bucket(1));
        directory.add(new Bucket(1));
        save();
    }

    private void split(Bucket bucket) throws IOException {
        if (bucket.localDepth == globalDepth) {
            int oldSize = directory.size();
            directory.addAll(new ArrayList<>(directory));
            globalDepth++;
            if (directory.size() != oldSize * 2) {
                throw new IllegalStateException("Could not grow hash directory");
            }
        }
        Bucket split = new Bucket(bucket.localDepth + 1);
        bucket.localDepth++;
        for (int i = 0; i < directory.size(); i++) {
            if (directory.get(i) == bucket && ((i >>> (bucket.localDepth - 1)) & 1) == 1) {
                directory.set(i, split);
            }
        }
        List<Entry> oldEntries = new ArrayList<>(bucket.entries);
        bucket.entries.clear();
        for (Entry entry : oldEntries) {
            bucket(entry.key()).put(entry.key(), entry.value());
        }
    }

    private Bucket bucket(String key) {
        int hash = spread(key.hashCode());
        int mask = (1 << globalDepth) - 1;
        return directory.get(hash & mask);
    }

    private static int spread(int hash) {
        return hash ^ (hash >>> 16);
    }

    private void load() throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0) {
            return;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                put(in.readUTF(), in.readInt());
            }
        } catch (EOFException e) {
            clear();
        }
    }

    private void save() throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        List<Entry> entries = new ArrayList<>();
        for (Bucket bucket : directory) {
            for (Entry entry : bucket.entries) {
                if (!entries.contains(entry)) {
                    entries.add(entry);
                }
            }
        }
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(tmp))) {
            out.writeInt(entries.size());
            for (Entry entry : entries) {
                out.writeUTF(entry.key());
                out.writeInt(entry.value());
            }
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void close() {
        // State is saved on every mutation.
    }

    private static final class Bucket {
        private int localDepth;
        private final List<Entry> entries = new ArrayList<>();

        private Bucket(int localDepth) {
            this.localDepth = localDepth;
        }

        private Integer find(String key) {
            for (Entry entry : entries) {
                if (entry.key().equals(key)) {
                    return entry.value();
                }
            }
            return null;
        }

        private void put(String key, int value) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).key().equals(key)) {
                    entries.set(i, new Entry(key, value));
                    return;
                }
            }
            entries.add(new Entry(key, value));
        }
    }

    private record Entry(String key, int value) {
    }
}