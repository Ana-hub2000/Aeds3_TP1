package br.pucminas.persistence;

import aed3.InterfaceArvoreBMais;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 Par (data de criação, idPergunta) armazenado na Árvore B+ de perguntas.

 O compareTo é INVERTIDO na data: a pergunta mais recente é considerada
 "menor", então ela fica na primeira folha da árvore. Assim, percorrer as
 folhas da esquerda para a direita já entrega a ordem DECRESCENTE de data.
 Em caso de empate na data, desempata pelo ID (também decrescente).
*/
public class ParDataId implements InterfaceArvoreBMais<ParDataId> {

    private static final short TAMANHO = 12; // long (8) + int (4)

    private long data;
    private int id;

    public ParDataId() {
        this(-1L, -1);
    }

    public ParDataId(long data, int id) {
        this.data = data;
        this.id = id;
    }

    public long getData() {
        return data;
    }

    public int getId() {
        return id;
    }

    @Override
    public int compareTo(ParDataId outro) {
        if (this.data != outro.data) {
            return Long.compare(outro.data, this.data); // decrescente
        }
        // id == -1 permite buscar todas as perguntas de uma mesma data
        return this.id == -1 ? 0 : Integer.compare(outro.id, this.id);
    }

    @Override
    public ParDataId clone() {
        return new ParDataId(data, id);
    }

    @Override
    public short size() {
        return TAMANHO;
    }

    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(data);
        dos.writeInt(id);
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        this.data = dis.readLong();
        this.id = dis.readInt();
    }

    @Override
    public String toString() {
        return data + ";" + id;
    }
}
