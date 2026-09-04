/**
 * Objetos de valor del bounded context para Gestión de Unidades.
 *
 * <p>Inmutables, sin identidad y sin {@code @Id}. Se mapean con {@code @Embeddable}
 * dentro de la tabla de su agregado. La lógica que valida o calcula sobre sus
 * atributos vive aquí, nunca en un servicio de aplicación.
 */
package pe.edu.unc.elmirador.unidades.models.vo;
