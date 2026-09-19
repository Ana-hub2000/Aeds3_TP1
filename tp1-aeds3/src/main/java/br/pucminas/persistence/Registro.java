package br.pucminas.persistence;

/**
 * Common contract for records stored by {@link CRUD2}.
 */
public interface Registro {
    int getId();

    void setId(int id);

    byte[] toByteArray();
}