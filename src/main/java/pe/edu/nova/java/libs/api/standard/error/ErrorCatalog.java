package pe.edu.nova.java.libs.api.standard.error;

/**
 * Puerto que decide el código y el mensaje que ve el cliente.
 * <p>
 * Nova trae {@link NovaErrorCatalog}: el código y el mensaje propios del error si la respuesta es 4xx y
 * los trae; si no, los del catálogo de la plataforma para el status, que son los mismos en los tres
 * stacks. Un 5xx lleva siempre los de su status. Una organización pone el suyo, con sus propios códigos
 * y textos, o envuelve el de Nova para cambiar solo una parte, como el idioma de los mensajes genéricos.
 * <p>
 * Recibe el {@link SanitizedFailure}, no el error: sin el proveedor ni la causa, que van solo al log, y
 * sin el mensaje de un incidente. {@link SanitizedFailure#code()} y {@link SanitizedFailure#message()}
 * traen lo que responde la plataforma; un catálogo propio que necesita distinguir lo que escribió quien
 * lanzó el error tiene {@link SanitizedFailure#ownCode()} y {@link SanitizedFailure#ownMessage()}.
 * Responde igual para una excepción de un framework, que llega con su status y sin tipo.
 */
@FunctionalInterface
public interface ErrorCatalog {

    /**
     * Retorna el código y el mensaje que ve el cliente.
     *
     * @param failure el fallo saneado, con el status ya decidido
     * @return la entrada del catálogo para ese fallo
     */
    CatalogEntry describe(SanitizedFailure failure);
}
