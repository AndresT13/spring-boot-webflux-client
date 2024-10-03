package com.jandrest.springboot.webflux.client.app.handler;

import com.jandrest.springboot.webflux.client.app.models.Producto;
import com.jandrest.springboot.webflux.client.app.models.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.jmx.export.notification.ModelMBeanNotificationPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Component
public class ProductoHandler {


    @Autowired
    private ProductoService service;


    public Mono<ServerResponse> listar(ServerRequest request) {
        return errorHandler( ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)  // se retorma del tipo 200 y un content Typede del tipo respuesta.
                .body(service.findAll(), Producto.class));// retorna un publisher del tipo findAll()
    }

    public Mono<ServerResponse> ver(ServerRequest request){
        String id = request.pathVariable("id");  // se debe obtener el id del pathVariable como primera medida.
        return errorHandler(service.findById(id) // se debe convertir este flujo en un ServerResponse por medio de un flatMap
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p)) // syncBody o .bodyValue es un atajo para poder agregar en el cuerpo un producto común y corriente que no es un flux ni un mo no solo elobjeto producto.
                .switchIfEmpty(ServerResponse.notFound().build())
        );  // si no existe el id se puede manejar con switchIfEmpty y con ServerResponse y un .notFound  con un cuerpo sin contenido vacio
    }

    public Mono<ServerResponse> crear(ServerRequest request){
        Mono<Producto> producto = request.bodyToMono(Producto.class); // Publisher para obtener el cuerpode la solicitud HTTP del objeto Producto
        return producto.flatMap( p -> {
            if(p.getCreateAt() == null) {
                p.setCreateAt(new Date());
            }
           return  service.save(p);  // retorna y modifica el flujo con el mismo producto con el id

        }).flatMap(p-> ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(p))
                .onErrorResume(error -> {
                    WebClientResponseException errorResponse  = (WebClientResponseException) error;
                    if(errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST){
                        return  ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(errorResponse.getResponseBodyAsString());
                    }
                    return Mono.error(errorResponse);
                }); //.syncBody

    }

    public Mono<ServerResponse> editar(ServerRequest request) {
        Mono<Producto> producto = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");

        return producto.flatMap(p ->
             ServerResponse.created(URI.create("/api/client/".concat(id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body( service.update(p,id), Producto.class));

    }

    public Mono<ServerResponse> eliminar(ServerRequest request) {
        String id = request.pathVariable("id");
        return errorHandler( service.delete(id).then(ServerResponse.noContent().build()));

    }


    public Mono<ServerResponse> upload(ServerRequest request){
        String id = request.pathVariable("id");
        return errorHandler(request.multipartData().map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(file -> service.upload(file, id))
                .flatMap(p -> ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .syncBody(p))
        );
    }




    private Mono<ServerResponse> errorHandler(Mono<ServerResponse> response) {
        return response.onErrorResume(error -> {
            WebClientResponseException responseError = (WebClientResponseException) error;
            if(responseError.getStatusCode() == HttpStatus.NOT_FOUND) {
                Map<String, Object> body = new HashMap<>();
                body.put("error","No existe el producto: ".concat(Objects.requireNonNull(responseError.getMessage())));
                body.put("timestamp",new Date());
                body.put("status",responseError.getStatusCode().value());
                return ServerResponse.status(HttpStatus.NOT_FOUND).syncBody(body);
            }
            return Mono.error(responseError);
        });
    }

}
