package pe.edu.nova.java.libs.api.standard.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.error.ApiError;

/**
 * Las fábricas de error de {@link ApiResponse} no cambian con el módulo de errores por capas: sus
 * consumidores dependen de ellas, y el cambio de código lo trae el {@code ErrorSerializer}.
 */
class ApiResponseCompatibilityTest {

    @Test
    void errorWithAMessageKeepsTheErrorCode() {
        ApiResponse<Object> response = ApiResponse.error(404, "Recurso no encontrado");

        assertFalse(response.success());
        assertEquals(404, response.status());
        assertEquals(List.of(ApiError.of("ERROR", "Recurso no encontrado")), response.errors());
        assertNull(response.metadata());
    }

    @Test
    void errorWithAListKeepsTheGivenEntries() {
        List<ApiError> errors = List.of(ApiError.validationError("email", "El correo no es válido"));

        ApiResponse<Object> response = ApiResponse.error(422, errors);

        assertFalse(response.success());
        assertEquals(422, response.status());
        assertEquals(errors, response.errors());
        assertNull(response.metadata());
    }
}
