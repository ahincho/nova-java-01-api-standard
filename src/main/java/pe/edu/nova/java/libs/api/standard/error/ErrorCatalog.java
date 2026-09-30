package pe.edu.nova.java.libs.api.standard.error;

/**
 * Puerto que decide el código y el mensaje que ve el cliente.
 * <p>
 * Nova trae {@link NovaErrorCatalog}: el código propio del error si la respuesta es 4xx y lo trae,
 * y si no, el del catálogo de la plataforma, con un mensaje genérico en español para todo 5xx. Una
 * organización pone el suyo, con sus propios códigos y textos, o envuelve el de Nova para cambiar
 * solo una parte, como el idioma de los mensajes genéricos.
 * <p>
 * El error trae lo que va solo al log: el proveedor ({@link NovaError#upstream()}), la causa y el
 * mensaje real de una falla interna. Con el catálogo de Nova ningún 5xx los muestra; en uno propio esa
 * regla pasa a ser de quien lo escribe, y ningún código ni mensaje de un 5xx debe contarlos.
 */
@FunctionalInterface
public interface ErrorCatalog {

    /**
     * Retorna el código y el mensaje que ve el cliente.
     *
     * @param error  el error
     * @param status el status con que se va a responder, el que decidió el {@link ErrorStatusMapper}
     * @return la entrada del catálogo para ese error
     */
    CatalogEntry describe(NovaError error, int status);
}
