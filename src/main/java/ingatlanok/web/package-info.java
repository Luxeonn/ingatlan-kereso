/**
 * Layer 1b — Headless Spring MVC REST API controllers and structured exception handling.
 *
 * <ul>
 *   <li>{@link ingatlanok.web.RestApiController}: JSON REST endpoints backing {@code
 *       openapi/api-spec.yaml}.
 *   <li>{@link ingatlanok.web.GlobalExceptionHandler}: Fail-closed {@code @RestControllerAdvice}
 *       returning sanitized JSON error payloads.
 * </ul>
 */
package ingatlanok.web;
