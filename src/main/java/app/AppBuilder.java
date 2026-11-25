package app;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import data_access.FileHandHistoryDataAccessObject;
import data_access.InMemoryGameSessionDataAccessObject;
import data_access.LlmOpponentDataAccessObject;
import entity.BotProfile;
import interface_adapter.opponent_action.OpponentActionController;
import interface_adapter.opponent_action.OpponentActionPresenter;
import interface_adapter.player_action.PlayerActionController;
import interface_adapter.player_action.PlayerActionPresenter;
import interface_adapter.start_hand.StartHandController;
import interface_adapter.start_hand.StartHandPresenter;
import interface_adapter.table.TableViewModel;
import use_case.opponent_action.OpponentActionDecisionDataAccessInterface;
import use_case.opponent_action.OpponentActionInteractor;
import use_case.player_action.PlayerActionInteractor;
import use_case.start_hand.StartHandInteractor;
import view.GameTableView;

/** Connects the two-player table to its use cases and data sources. */
public class AppBuilder {
    public static GameTableView createTable() {
        return createTable(Path.of("data", "hand_history.json"), new LlmOpponentDataAccessObject());
    }

    /** Creates an independent table using the supplied history file and opponent. */
    public static GameTableView createTable(Path historyFile, OpponentActionDecisionDataAccessInterface decisions) {
        InMemoryGameSessionDataAccessObject session = new InMemoryGameSessionDataAccessObject();
        FileHandHistoryDataAccessObject history = new FileHandHistoryDataAccessObject(historyFile);
        TableViewModel model = new TableViewModel();
        Random random = new Random();
        Clock clock = Clock.systemUTC();
        Supplier<String> handIds = () -> UUID.randomUUID().toString();

        StartHandInteractor start = new StartHandInteractor(session, new StartHandPresenter(model), random);
        PlayerActionInteractor player = new PlayerActionInteractor(session, history,
                new PlayerActionPresenter(model), handIds, clock);
        OpponentActionInteractor opponent = new OpponentActionInteractor(session, history,
                () -> new BotProfile("Balanced", 0.5), decisions, new OpponentActionPresenter(model),
                random, handIds, clock);
        return new GameTableView(model, new StartHandController(start), new PlayerActionController(player),
                new OpponentActionController(opponent));
    }
}
