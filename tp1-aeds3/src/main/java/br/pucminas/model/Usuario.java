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

public class Usuario implements Registro {
=======

// Agora o Usuario segue a interface da disciplina (aed3.InterfaceRegistro),
// para poder ser gravado pela classe Arquivo do professor.
public class Usuario implements aed3.InterfaceRegistro {
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    private int idUsuario;
    private String nome;
    private String email;
    private String hashSenha;
    private String perguntaSecreta;
    private String hashRespostaSecreta;

<<<<<<< HEAD
    public Usuario() {
=======
    // O construtor vazio e obrigatorio: o Arquivo usa reflexao para criar o objeto
    public Usuario() {
        this("", "", "", "", "");
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    public Usuario(String nome, String email, String hashSenha,
                   String perguntaSecreta, String hashRespostaSecreta) {
<<<<<<< HEAD
=======
        this.idUsuario = -1;
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
        this.nome = nome;
        this.email = email;
        this.hashSenha = hashSenha;
        this.perguntaSecreta = perguntaSecreta;
        this.hashRespostaSecreta = hashRespostaSecreta;
    }

<<<<<<< HEAD
    public static Usuario fromByteArray(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Usuario usuario = new Usuario();
            usuario.idUsuario = in.readInt();
            usuario.nome = in.readUTF();
            usuario.email = in.readUTF();
            usuario.hashSenha = in.readUTF();
            usuario.perguntaSecreta = in.readUTF();
            usuario.hashRespostaSecreta = in.readUTF();
            return usuario;
        } catch (IOException e) {
            throw new IllegalArgumentException("Registro de usuário inválido", e);
        }
    }

    @Override
    public byte[] toByteArray() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(idUsuario);
            out.writeUTF(safe(nome));
            out.writeUTF(safe(email));
            out.writeUTF(safe(hashSenha));
            out.writeUTF(safe(perguntaSecreta));
            out.writeUTF(safe(hashRespostaSecreta));
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível serializar usuário", e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
=======
    @Override
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(idUsuario);
        dos.writeUTF(texto(nome));
        dos.writeUTF(texto(email));
        dos.writeUTF(texto(hashSenha));
        dos.writeUTF(texto(perguntaSecreta));
        dos.writeUTF(texto(hashRespostaSecreta));
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        this.idUsuario = dis.readInt();
        this.nome = dis.readUTF();
        this.email = dis.readUTF();
        this.hashSenha = dis.readUTF();
        this.perguntaSecreta = dis.readUTF();
        this.hashRespostaSecreta = dis.readUTF();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
    }

    @Override
    public int getId() {
        return idUsuario;
    }

    @Override
    public void setId(int id) {
        this.idUsuario = id;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashSenha() {
        return hashSenha;
    }

    public void setHashSenha(String hashSenha) {
        this.hashSenha = hashSenha;
    }

    public String getPerguntaSecreta() {
        return perguntaSecreta;
    }

    public void setPerguntaSecreta(String perguntaSecreta) {
        this.perguntaSecreta = perguntaSecreta;
    }

    public String getHashRespostaSecreta() {
        return hashRespostaSecreta;
    }

    public void setHashRespostaSecreta(String hashRespostaSecreta) {
        this.hashRespostaSecreta = hashRespostaSecreta;
    }
<<<<<<< HEAD
}
=======

    @Override
    public String toString() {
        return idUsuario + " - " + nome + " (" + email + ")";
    }
}
>>>>>>> 9dcc73d58fb43770a8a2f588cbcbe6df9f12c31b
