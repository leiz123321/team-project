/**
 * Use case: Take a betting action. The interactor applies the human player's
 * action to the hand in progress, says why when the action is not allowed,
 * and keeps a record of the hand when the action ends it. It never makes the
 * computer opponent act.
 *
 * <p>Contents: an input boundary, input data, the interactor, an output
 * boundary, and the data access interfaces for the match being played and for
 * the history of finished hands. The output data is the shared
 * {@code use_case.table.TableOutputData}.
 *
 * <p>Classes here may use {@code entity}, {@code use_case.table}, and this
 * package.
 */
package use_case.player_action;
