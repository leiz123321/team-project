/**
 * Computer turns in the betting use case (UC2). The interactor asks for the
 * opponent's decision, uses the built-in strategy when no usable decision
 * comes back, applies the action, and finishes the hand when it is over.
 *
 * <p>Contents: an input boundary, the interactor, an output boundary, the data
 * access interfaces the opponent turn needs, and the exception a decision source
 * throws when it cannot decide. The output data is the shared
 * {@code use_case.table.TableOutputData}; there is no input data.
 *
 * <p>Classes here may use {@code entity}, {@code use_case.table}, and this
 * package.
 */
package use_case.opponent_action;
