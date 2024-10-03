package com.jandrest.springboot.webflux.client.app.models.service;

import com.jandrest.springboot.webflux.client.app.models.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private WebClient client;


    @Override
    public Flux<Producto> findAll() {
        return client.get().accept(MediaType.APPLICATION_JSON) // aceptamos mediante las cabezeras http el tipo de contenido aplication JSON
               .exchange() //  .retrieve()
                 .flatMapMany(response -> response.bodyToFlux(Producto.class));
               // .bodyToFlux(Producto.class);
    }

    @Override
    public Mono<Producto> findById(String id) {
        Map<String,Object> params = new HashMap<String, Object>();
        params.put("id",id );

        return client.get().uri("/{id}", params)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve() // .exchange()
                //.flatMap(response -> response.bodyToMono(Producto.class));
                .bodyToMono(Producto.class);
    }

    @Override
    public Mono<Producto> save(Producto producto) {
        return client.post()
                .accept(MediaType.APPLICATION_JSON) // tipo de contenido que estamos aceptando en el response
                .contentType(MediaType.APPLICATION_JSON) // tipo de contenido que estamos enviando en el request
                .body(BodyInserters.fromValue(producto))
                .retrieve()
                .bodyToMono(Producto.class);// de esta forma obtenemos el objeto JSON que guardamos en la base de datod;

    }

    @Override
    public Mono<Producto> update(Producto producto, String id) {
        return client.put()
                .uri("/{id}", Collections.singletonMap("id",id))
                .accept(MediaType.APPLICATION_JSON) // tipo de contenido que estamos aceptando en el response
                .contentType(MediaType.APPLICATION_JSON) // tipo de contenido que estamos enviando en el request
                .body(BodyInserters.fromValue(producto))
                .retrieve()
                .bodyToMono(Producto.class);// de esta forma obtenemos el objeto JSON que guardamos en la base de datod;

    }

    @Override
    public Mono<Void> delete(String id) {
        return client.delete()
                .uri("/{id}", Collections.singletonMap("id",id))
                .retrieve()
                .bodyToMono(Void.class)
                .then();
    }

    @Override
    public Mono<Producto> upload(FilePart file, String id) {
        // se debe configurar el body del request y cuando se sube una imagen se sube del tipo request pero del tipo MultiPartFormat
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.asyncPart("file",file.content(), DataBuffer.class )
                .headers(h -> {
                    h.setContentDispositionFormData("file",file.filename());
                });
        return client.post()
                .uri("/upload/{id}", Collections.singletonMap("id", id))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts.build())
                .retrieve()
                .bodyToMono(Producto.class);
    }

}
