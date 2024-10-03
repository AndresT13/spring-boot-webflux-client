package com.jandrest.springboot.webflux.client.app.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Producto {
    private String id;
    private String nombre;
    private Double precio;
    private Date createAt;
    private String foto;
    private Categoria categoria;

}
