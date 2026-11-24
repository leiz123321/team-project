/**
 * Use case: Start a new hand. The interactor deals the next hand of the match,
 * or the first hand of a new match: it shuffles the deck, posts the blinds,
 * and deals the hole cards. It never makes the computer opponent act.
 *
 * <p>Contents: an input boundary, input data, the interactor, an output
 * boundary, and the data access interface for the match being played. The
 * output data is the shared {@code use_case.table.TableOutputData}.
 *
 * <p>Classes here may use {@code entity}, {@code use_case.table}, and this
 * package.
 */
package use_case.start_hand;
