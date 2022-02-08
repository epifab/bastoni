package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Delay.syntax.afterGameOver
import bastoni.domain.model.Command.{Act, Continue}
import cats.Applicative
import io.circe.{Decoder, Encoder}
import io.circe.syntax.EncoderOps

import scala.annotation.tailrec

trait ActiveMatch:
  def players: List[MatchPlayer]

trait GameLogic[State: Decoder: Encoder]:

  protected val uneventful: List[StateMachineOutput] = Nil

  val gameType: GameType
  def newMatch(players: List[MatchPlayer]): MatchState.InProgress
  def newGame(players: List[MatchPlayer]): State
  def statusFor(state: State): GameStatus

  val playGameStep: (State, StateMachineInput) => (State, List[StateMachineOutput])

  val playStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) = {

    case (inProgress: MatchState.InProgress, message) =>

      val gameState: State = inProgress.gameState.as[State].getOrElse(throw new IllegalStateException("Game state corrupted"))

      val (updatedGameState, events) = playGameStep(gameState, message)

      statusFor(updatedGameState) match {

        case GameStatus.Completed(updatedPlayers) =>

          def nextGame: MatchState.GameOver = {
            val shiftedRound = inProgress.players
              .shift
              .map(possiblyOutdated => updatedPlayers.find(_.is(possiblyOutdated)).getOrElse(possiblyOutdated))

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

          winners.fold(nextGame)(score => MatchState.GameOver(Event.MatchCompleted(score.playerIds), MatchState.Terminated)) -> (events :+ Continue.afterGameOver)


        case GameStatus.Aborted => MatchState.GameOver(Event.MatchAborted, MatchState.Terminated) -> (events :+ Event.GameAborted :+ Continue.afterGameOver)

        case GameStatus.InProgress => inProgress.copy(gameState = updatedGameState.asJson) -> events
      }

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

  def playStream[F[_]](users: List[User])(input: fs2.Stream[F, StateMachineInput]): fs2.Stream[F, StateMachineOutput] =
    input
      .scan[(MatchState, List[StateMachineOutput])](newMatch(users.map(user => MatchPlayer(user, 0))) -> uneventful) {
        case ((state, _), message) => playStep(state, message)
      }
      .takeThrough { case (state, _) => state != MatchState.Terminated }
      .flatMap { case (state, output) => fs2.Stream.iterable(output) }
