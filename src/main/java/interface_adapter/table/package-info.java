/**
 * The state of the game screen and what the game's presenters share: the
 * table view model, the immutable state it holds, the events of that state,
 * the conversion of card text to card image ids, and the helper through which
 * every game presenter publishes a new state.
 *
 * <p>Starting a hand, making a bet, and receiving the computer response
 * all update the same {@link interface_adapter.table.TableViewModel}.
 *
 * <p>Classes here may use {@code use_case.table} and {@code interface_adapter}.
 * They must not refer to entities, views, data access classes, {@code app},
 * or Swing.
 */
package interface_adapter.table;
