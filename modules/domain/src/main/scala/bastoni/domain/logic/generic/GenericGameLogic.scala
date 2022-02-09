package bastoni.domain.logic
package generic

import bastoni.domain.logic.generic.MatchState.WaitingForPlayers
import bastoni.domain.model.*
import bastoni.domain.model.Command.{Act, Continue, Ok}
import bastoni.domain.model.Delay.syntax.afterGameOver
import bastoni.domain.model.Event.PlayerConfirmed
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

import scala.annotation.tailrec

abstract class GenericGameLogic extends GameLogic[MatchState]:

  type GameState
  given stateEncoder: Encoder[GameState]
  given stateDecoder: Decoder[GameState]

  protected val uneventful: List[StateMachineOutput] = Nil

  protected def newMatch(players: List[MatchPlayer]): MatchState.InProgress
  protected def newGame(players: List[MatchPlayer]): GameState
  protected def statusFor(state: GameState): GameStatus
  protected val playGameStep: (GameState, StateMachineInput) => (GameState, List[StateMachineOutput])

  override def initialState(players: List[MatchPlayer]): MatchState = newMatch(players)
  override def isFinal(state: MatchState): Boolean = state == MatchState.Terminated

  override val playStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) = {

    case (inProgress: MatchState.InProgress, message) =>

      val gameState: GameState = inProgress.gameState.as[GameState].getOrElse(throw new IllegalStateException("Game state corrupted"))

      val (updatedGameState, events) = playGameStep(gameState, message)

      statusFor(updatedGameState) match {

        case GameStatus.Completed(playersUpdate) =>

          val updatedPlayers = inProgress.players.map(possiblyOutdated => playersUpdate.find(_.is(possiblyOutdated)).getOrElse(possiblyOutdated))

          def nextGame: MatchState.GameOver = {
            val shiftedRound = updatedPlayers.shift
            MatchState.GameOver(
              Act(shiftedRound.last.id, Action.ShuffleDeck, timeout = None),
              inProgress.nextGame(newGame(shiftedRound).asJson, shiftedRound)
            )
          }

          val sortedTeams: List[MatchScore] = MatchScore.forTeams(Teams(updatedPlayers)).sortBy(-_.points)

          val winners: Option[MatchScore] = inProgress.matchType match {
            case MatchType.FixedRounds(remainingGames) =>
              sortedTeams match {
                case winningTeam :: secondTeam :: _ if winningTeam.points > remainingGames + secondTeam.points => Some(winningTeam)
                case _ => None
              }

            case MatchType.PointsBased(pointsToWin) =>
              sortedTeams match {
                case winnerTeam :: secondTeam :: _
                  if winnerTeam.points >= pointsToWin && winnerTeam.points > secondTeam.points =>
                  Some(winnerTeam)
                case _ => None
              }
          }

          val nextState = winners.fold(nextGame)(score => MatchState.GameOver(Event.MatchCompleted(score.playerIds), MatchState.Terminated))
          val playerIds = updatedPlayers.map(_.id)
          WaitingForPlayers(playerIds.toSet, nextState) -> (events ++ playerIds.map(id => Command.Act(id, Action.Confirm, timeout = None)))

        case GameStatus.Aborted => MatchState.GameOver(Event.MatchAborted, MatchState.Terminated) -> (events :+ Event.GameAborted :+ Continue.afterGameOver)

        case GameStatus.InProgress => inProgress.copy(gameState = updatedGameState.asJson) -> events
      }

    case (MatchState.WaitingForPlayers(players, next), Ok(playerId)) if players.contains(playerId) =>
      val remainingPlayers = players - playerId
      val newState = if (remainingPlayers.isEmpty) next else WaitingForPlayers(remainingPlayers, next)
      newState -> (PlayerConfirmed(playerId) :: Option.when(remainingPlayers.isEmpty)(Continue).toList)

    case (MatchState.GameOver(event, state), Continue) => state -> List(event)

    case (state, _) => state -> uneventful
  }

  @tailrec
  private def playSteps(state: MatchState, input: List[StateMachineInput], previousEvents: List[StateMachineOutput] = Nil): (MatchState, List[StateMachineOutput]) =
    input match {
      case Nil => state -> previousEvents
      case head :: tail =>
        val (intermediateState, events) = playStep(state, head)
        playSteps(intermediateState, tail, previousEvents ++ events)
    }

  def playSteps(state: MatchState, input: StateMachineInput, otherInput: StateMachineInput*): (MatchState, List[StateMachineOutput]) =
    playSteps(state, input :: otherInput.toList, previousEvents = uneventful)
