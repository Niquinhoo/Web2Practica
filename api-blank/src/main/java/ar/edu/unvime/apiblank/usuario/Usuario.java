package ar.edu.unvime.apiblank.usuario;
public record Usuario(Long id, String email, String passwordHash, Rol rol) {}
