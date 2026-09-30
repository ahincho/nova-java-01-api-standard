/**
 * Estructuras estandarizadas de errores de API y el módulo de errores por capas de ADR-031.
 * <p>
 * {@code ApiError} es una entrada de {@code errors} en el sobre, con soporte de Builder.
 * <p>
 * El modelo por capas clasifica un error por capa y tipo, sin importar ningún framework:
 * {@code DomainError}, {@code ApplicationError}, {@code InfrastructureError} y
 * {@code PlatformError}, sobre la base común {@code NovaError}. El mapeo a HTTP lo hace la
 * plataforma con tres puertos, cada uno con la implementación por defecto de Nova:
 * {@code ErrorStatusMapper} ({@code NovaErrorStatusMapper}), {@code ErrorCatalog}
 * ({@code NovaErrorCatalog}) y {@code ErrorSerializer} ({@code NovaErrorSerializer}).
 * <p>
 * Los puertos no reciben el error: reciben un {@code SanitizedFailure}, sin el proveedor ni la causa,
 * que solo ve el núcleo. {@code ErrorPorts} los junta y fija el orden en que se consultan. El
 * {@code traceId} lo da una {@code TraceIdSource}, que cada integración registra con
 * {@link java.util.ServiceLoader}.
 */
package pe.edu.nova.java.libs.api.standard.error;
