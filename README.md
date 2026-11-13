# Poker vs. Computer

## Team Information

### Team Members
- Lei Zhang
- Qijian Yang
- Aalim
- Agrim Rana

---

## Project Description

### Domain

A desktop application that lets one person play poker against a computer
opponent. Besides playing, the user can change how the computer plays, look
back at hands they have already played, and ask for the chance of winning in
a situation they describe.

The game is heads-up (one human against one computer) No-Limit Texas Hold'em.

---

## User Stories

### User Story 1

As a player, I want to play a game of poker against the computer so that I can
practise without needing other people.

### User Story 2

As a player, I want to change how the computer opponent behaves so that I can
practise against different playing styles.

### User Story 3

As a player, I want to review my previous hands so that I can see how to
improve.

### User Story 4

As a player, I want to enter a specific situation and see my chance of winning
so that I can learn which hands are worth playing.

---

## Use Cases

### Use Case 1: Start a new hand

1. The user chooses to start a hand.
2. The system shuffles the deck, posts the blinds, and deals two hole cards to each player.
3. The system shows the user's cards, both players' chips, the pot, and the actions the user may take.
4. If either player has no chips left, the system reports that the match is over instead.

### Use Case 2: Take a betting action

1. The user chooses to fold, check, call, bet, or raise, giving an amount where one is needed.
2. If the action is not allowed in the current situation, the system tells the user why and nothing changes.
3. Otherwise the system applies the action, and the computer opponent then acts according to its current configuration.
4. When a betting round ends, the system reveals the next community cards.
5. When the hand ends, either by a fold or by a showdown after the last betting round, the system announces the winner, awards the pot, and saves a record of the hand.

### Use Case 3: Configure the computer opponent

1. The user opens the opponent settings and sees the current configuration.
2. The user picks a playing style and adjusts its settings.
3. The system saves the configuration. It applies from the next hand on and is still there after the program is closed and reopened.

### Use Case 4: Review past hands

1. The user opens the history and sees a list of hands already played, each with its date, result, and chips won or lost.
2. The user selects a hand.
3. The system shows that hand's hole cards, community cards, every action in order, and the outcome.
4. If no hands have been played yet, the system says so.

### Use Case 5: Calculate the chance of winning

1. The user enters their own hole cards and between zero and five community cards.
2. If the input is not valid, for example the same card entered twice, the system tells the user.
3. Otherwise the system shows the chances of winning, tying, and losing, based only on the cards the user entered.

---

## Minimal Viable Product (MVP)

All five use cases above form the MVP. Each team member leads the use cases
of one user story.

| Team Member | MVP Use Case |
|-------------|--------------|
| Qijian Yang | Use Case 1: Start a new hand, and Use Case 2: Take a betting action (User Story 1) |
| Lei Zhang | Use Case 3: Configure the computer opponent (User Story 2) |
| Aalim | Use Case 4: Review past hands (User Story 3) |
| Agrim Rana | Use Case 5: Calculate the chance of winning (User Story 4) |

---

## Entities

### Card

One playing card, with a rank and a suit.

### Deck

The 52 distinct cards. It can be shuffled and dealt from.

### HandValue

How strong a five-card poker hand is, so that any two hands can be compared.

### Player

A participant in the game: a name, a chip count, hole cards, and whether they
have folded. A player cannot bet more chips than they have. A player can work
out their chance of winning from the cards they can see: their own hole cards
and the community cards revealed so far.

### Action

One betting decision: fold, check, call, bet, or raise, with an amount for
bet and raise.

### Game

One hand in progress: the deck, the two players, the community cards, the pot,
the current betting round, and whose turn it is. It decides which actions are
allowed, when a betting round ends, and who wins the pot.

### HandRecord

A finished hand as it is kept in the history: the cards, every action in
order, the winner, and the chips won or lost.

### BotProfile

The computer opponent's chosen playing style and its settings.

---

## API Usage

**API Name:** A large language model (LLM) API; provider to be decided

**Documentation:** To be decided

**How it is used in the program:** The API plays the computer opponent. On the
computer's turn the program sends what the computer player can see (its own
hole cards, the revealed community cards, the pot, the betting so far, and the
actions it is allowed to take) together with the playing style chosen in Use
Case 3, and the API answers with the action to take. If the API cannot be
reached, a built-in opponent plays instead so the game can always continue.

---

## Design Diagrams

The architecture, package layout, and dependency rules are described in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

### UML Class Diagram

See the class diagram in
[docs/ARCHITECTURE.md, section 7.2](docs/ARCHITECTURE.md#72-class-diagram-for-one-use-case).

### Sequence Diagrams

See the sequence diagram in
[docs/ARCHITECTURE.md, section 7.3](docs/ARCHITECTURE.md#73-sequence-diagram).

---

## How to Run

Requirements: JDK 11 or newer, and Maven (bundled with IntelliJ IDEA).

1. Clone the repository and open it in IntelliJ IDEA as a Maven project.
2. Run `app.Main`.

The application is still under development; `app.Main` does not start a
playable game yet.

---

## Notes

- Team norms are in [TeamContract.md](TeamContract.md).
- Screenshots will be added once the user interface exists.
