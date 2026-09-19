package br.pucminas.model;

import br.pucminas.persistence.Registro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Usuario implements Registro {
    private int idUsuario;
    private String nome;
    private String email;
    private String hashSenha;
    private String perguntaSecreta;
    private String hashRespostaSecreta;

    public Usuario() {
    }

    public Usuario(String nome, String email, String hashSenha,
                   String perguntaSecreta, String hashRespostaSecreta) {
        this.nome = nome;
        this.email = email;
        this.hashSenha = hashSenha;
        this.perguntaSecreta = perguntaSecreta;
        this.hashRespostaSecreta = hashRespostaSecreta;
    }

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
}