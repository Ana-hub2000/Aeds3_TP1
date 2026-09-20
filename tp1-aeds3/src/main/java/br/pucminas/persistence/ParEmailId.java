package br.pucminas.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 Par (e-mail, idUsuario) usado como indice indireto na Arvore B+ do professor.

 E uma adaptacao da classe ParNomeId (aed3), que so guardava 26 bytes de nome.
 Aumentamos o campo para 60 bytes porque e-mail costuma ser maior que isso.
 A comparacao e exata (sem busca por prefixo), ja que e-mail e chave unica.
*/
public class ParEmailId implements aed3.InterfaceArvoreBMais<ParEmailId> {

    private static final int TAMANHO_EMAIL = 60;
    private final short TAMANHO = TAMANHO_EMAIL + 4;

    private String email;
    private int id;

    public ParEmailId() {
        this("", -1);
    }

    public ParEmailId(String email) {
        this(email, -1);
    }

    public ParEmailId(String email, int id) {
        this.email = email == null ? "" : email;
        if (this.email.getBytes().length > TAMANHO_EMAIL) {
            throw new IllegalArgumentException("E-mail longo demais (max. " + TAMANHO_EMAIL + " bytes).");
        }
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public int getId() {
        return id;
    }

    @Override
    public ParEmailId clone() {
        return new ParEmailId(this.email, this.id);
    }

    @Override
    public short size() {
        return this.TAMANHO;
    }

    @Override
    public int compareTo(ParEmailId outro) {
        int comparacao = this.email.compareTo(outro.email);
        if (comparacao != 0) {
            return comparacao;
        }
        // id == -1 significa "qualquer id", usado nas buscas
        return this.id == -1 ? 0 : this.id - outro.id;
    }

    @Override
    public String toString() {
        return email + ";" + id;
    }

    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        byte[] fixo = new byte[TAMANHO_EMAIL];
        byte[] bytesEmail = email.getBytes();
        int i = 0;
        while (i < bytesEmail.length && i < TAMANHO_EMAIL) {
            fixo[i] = bytesEmail[i];
            i++;
        }
        while (i < TAMANHO_EMAIL) {
            fixo[i] = ' ';
            i++;
        }
        dos.write(fixo);
        dos.writeInt(id);
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        byte[] fixo = new byte[TAMANHO_EMAIL];
        dis.readFully(fixo);
        this.email = new String(fixo).trim();
        this.id = dis.readInt();
    }
}
