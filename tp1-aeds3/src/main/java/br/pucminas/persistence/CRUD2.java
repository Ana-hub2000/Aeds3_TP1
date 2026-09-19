package br.pucminas.persistence;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * File-based CRUD using the record layout required by the practical assignment:
 * tombstone (lapide), record size, and the record's byte vector.
 *
 * <p>Updates append the new version after marking the previous version as
 * deleted. This keeps record sizes independent from future changes to an
 * entity's fields and makes recovery after an interrupted append predictable.</p>
 */
public class CRUD2<T extends Registro> implements AutoCloseable {
    private static final byte ACTIVE = 0;
    private static final byte DELETED = 1;

    private final Path file;
    private final Function<byte[], T> decoder;

    public CRUD2(Path file, Function<byte[], T> decoder) throws IOException {
        this.file = file;
        this.decoder = decoder;
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    public synchronized int create(T record) throws IOException {
        // IDs are owned by the CRUD; callers cannot accidentally reuse one.
        record.setId(nextId());
        append(ACTIVE, record);
        return record.getId();
    }

    public synchronized T read(int id) throws IOException {
        return latestRecords().get(id);
    }

    public synchronized List<T> readAll() throws IOException {
        return new ArrayList<>(latestRecords().values());
    }

    public synchronized boolean update(T record) throws IOException {
        Map<Integer, RecordPosition> positions = latestPositions();
        RecordPosition position = positions.get(record.getId());
        if (position == null) {
            return false;
        }
        markDeleted(position.offset());
        append(ACTIVE, record);
        return true;
    }

    public synchronized boolean delete(int id) throws IOException {
        RecordPosition position = latestPositions().get(id);
        if (position == null) {
            return false;
        }
        markDeleted(position.offset());
        return true;
    }

    public synchronized int nextId() throws IOException {
        int max = 0;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            while (raf.getFilePointer() < raf.length()) {
                long offset = raf.getFilePointer();
                RecordHeader header = readHeader(raf);
                if (header == null) {
                    break;
                }
                byte[] bytes = readBytes(raf, header.size());
                try {
                    T record = decoder.apply(bytes);
                    max = Math.max(max, record.getId());
                } catch (RuntimeException ignored) {
                    // A malformed trailing record must not prevent opening the file.
                    if (raf.getFilePointer() <= offset) {
                        break;
                    }
                }
            }
        }
        return max + 1;
    }

    private void append(byte lapide, T record) throws IOException {
        byte[] bytes = record.toByteArray();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(raf.length());
            raf.writeByte(lapide);
            raf.writeInt(bytes.length);
            raf.write(bytes);
        }
    }

    private void markDeleted(long offset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(offset);
            raf.writeByte(DELETED);
        }
    }

    private Map<Integer, T> latestRecords() throws IOException {
        Map<Integer, T> records = new LinkedHashMap<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            while (raf.getFilePointer() < raf.length()) {
                RecordHeader header = readHeader(raf);
                if (header == null) {
                    break;
                }
                byte[] bytes = readBytes(raf, header.size());
                T record = decoder.apply(bytes);
                if (header.lapide() == ACTIVE) {
                    records.remove(record.getId());
                    records.put(record.getId(), record);
                } else {
                    records.remove(record.getId());
                }
            }
        }
        return records;
    }

    private Map<Integer, RecordPosition> latestPositions() throws IOException {
        Map<Integer, RecordPosition> positions = new LinkedHashMap<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            while (raf.getFilePointer() < raf.length()) {
                long offset = raf.getFilePointer();
                RecordHeader header = readHeader(raf);
                if (header == null) {
                    break;
                }
                byte[] bytes = readBytes(raf, header.size());
                T record = decoder.apply(bytes);
                if (header.lapide() == ACTIVE) {
                    positions.put(record.getId(), new RecordPosition(offset));
                } else {
                    positions.remove(record.getId());
                }
            }
        }
        return positions;
    }

    private static RecordHeader readHeader(RandomAccessFile raf) throws IOException {
        try {
            byte lapide = raf.readByte();
            int size = raf.readInt();
            if (size < 0 || size > 100_000_000) {
                throw new IOException("Invalid record size: " + size);
            }
            return new RecordHeader(lapide, size);
        } catch (EOFException e) {
            return null;
        }
    }

    private static byte[] readBytes(RandomAccessFile raf, int size) throws IOException {
        if (raf.length() - raf.getFilePointer() < size) {
            throw new EOFException("Incomplete record");
        }
        byte[] bytes = new byte[size];
        raf.readFully(bytes);
        return bytes;
    }

    @Override
    public void close() {
        // Files are opened per operation, so no resource needs closing here.
    }

    private record RecordHeader(byte lapide, int size) {
    }

    private record RecordPosition(long offset) {
    }
}