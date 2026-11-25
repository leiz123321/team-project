/**
 * Interface adapters for the computer-response step of the betting flow:
 * its controller and its presenter. The presenter updates the table view model
 * that the whole game screen shares, so this step has no view model or
 * state of its own.
 *
 * <p>Classes here may use {@code use_case.opponent_action},
 * {@code use_case.table}, and {@code interface_adapter}. They must not refer
 * to entities, views, data access classes, {@code app}, or Swing.
 */
package interface_adapter.opponent_action;
