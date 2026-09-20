package br.pucminas.model;

<<<<<<< HEAD
import br.pucminas.persistence.Registro;

=======
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
<<<<<<< HEAD
import java.io.IOException;

public class Pergunta implements Registro {
=======

// Mesma ideia do Usuario: implementa a interface da disciplina.
public class Pergunta implements aed3.InterfaceRegistro {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    private int idPergunta;
    private int idUsuario;
    private long criacao;
    private long alteracao;
    private short nota;
    private String pergunta;
    private String palavrasChave;
    private boolean ativa;

    public Pergunta() {
<<<<<<< HEAD
=======
        this(-1, "", "");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    public Pergunta(int idUsuario, String pergunta, String palavrasChave) {
        long agora = System.currentTimeMillis();
<<<<<<< HEAD
=======
        this.idPergunta = -1;
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        this.idUsuario = idUsuario;
        this.criacao = agora;
        this.alteracao = agora;
        this.nota = 0;
        this.pergunta = pergunta;
        this.palavrasChave = palavrasChave;
        this.ativa = true;
    }

<<<<<<< HEAD
    public static Pergunta fromByteArray(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Pergunta pergunta = new Pergunta();
            pergunta.idPergunta = in.readInt();
            pergunta.idUsuario = in.readInt();
            pergunta.criacao = in.readLong();
            pergunta.alteracao = in.readLong();
            pergunta.nota = in.readShort();
            pergunta.pergunta = in.readUTF();
            pergunta.palavrasChave = in.readUTF();
            pergunta.ativa = in.readBoolean();
            return pergunta;
        } catch (IOException e) {
            throw new IllegalArgumentException("Registro de pergunta inválido", e);
        }
    }

    @Override
    public byte[] toByteArray() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(idPergunta);
            out.writeInt(idUsuario);
            out.writeLong(criacao);
            out.writeLong(alteracao);
            out.writeShort(nota);
            out.writeUTF(safe(pergunta));
            out.writeUTF(safe(palavrasChave));
            out.writeBoolean(ativa);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível serializar pergunta", e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
=======
    @Override
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(idPergunta);
        dos.writeInt(idUsuario);
        dos.writeLong(criacao);
        dos.writeLong(alteracao);
        dos.writeShort(nota);
        dos.writeUTF(texto(pergunta));
        dos.writeUTF(texto(palavrasChave));
        dos.writeBoolean(ativa);
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        this.idPergunta = dis.readInt();
        this.idUsuario = dis.readInt();
        this.criacao = dis.readLong();
        this.alteracao = dis.readLong();
        this.nota = dis.readShort();
        this.pergunta = dis.readUTF();
        this.palavrasChave = dis.readUTF();
        this.ativa = dis.readBoolean();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    @Override
    public int getId() {
        return idPergunta;
    }

    @Override
    public void setId(int id) {
        this.idPergunta = id;
    }

    public int getIdPergunta() {
        return idPergunta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public long getCriacao() {
        return criacao;
    }

    public long getAlteracao() {
        return alteracao;
    }

    public void setAlteracao(long alteracao) {
        this.alteracao = alteracao;
    }

    public short getNota() {
        return nota;
    }

    public void setNota(short nota) {
        this.nota = nota;
    }

    public String getPergunta() {
        return pergunta;
    }

    public void setPergunta(String pergunta) {
        this.pergunta = pergunta;
    }

    public String getPalavrasChave() {
        return palavrasChave;
    }

    public void setPalavrasChave(String palavrasChave) {
        this.palavrasChave = palavrasChave;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
