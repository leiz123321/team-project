/**
 * Data access: the classes that read and write files and call external
 * services. Each one implements a data access interface declared by a use
 * case.
 *
 * <p>This is the only package that may use JSON and HTTP libraries. Classes
 * here must not refer to interface adapters or views.
 */
package data_access;
