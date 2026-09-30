package pe.edu.nova.java.libs.api.standard.error;

/**
 * El tipo de un error dentro de su capa: la clasificación de la tabla de ADR-031.
 * <p>
 * Es cerrado a propósito. Cada capa trae su enumeración ({@link DomainError.Type},
 * {@link ApplicationError.Type}, {@link InfrastructureError.Type} y {@link PlatformError.Type}) y
 * ningún servicio puede sumar un tipo: si cada uno inventa el suyo, el tablero vuelve a agrupar por
 * cadenas que no coinciden.
 */
public sealed interface ErrorType
        permits DomainError.Type, ApplicationError.Type, InfrastructureError.Type, PlatformError.Type {

    /**
     * La capa a la que pertenece el tipo.
     *
     * @return la capa
     */
    Layer layer();

    /**
     * El nombre del tipo, como {@code NOT_FOUND}. Lo implementa cada enumeración.
     *
     * @return el nombre del tipo
     */
    String name();
}
