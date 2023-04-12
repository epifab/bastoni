package bastoni.backend

import bastoni.domain.model.{RoomId, RoomPlayerView, User, UserId}
import bastoni.domain.model.EmptySeat
import bastoni.domain.model.Event.PlayerJoinedRoom
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.domain.AsyncIOFreeSpec
import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.concurrent.SignallingRef
import org.http4s.syntax.all.uri
import org.http4s.Uri
import org.scalatest.{stats, Assertion}

import scala.concurrent.ExecutionContext

class ConnectionSpec extends AsyncIOFreeSpec:

  val user: User     = User(UserId.newId, "John Doe")
  val roomId: RoomId = RoomId.newId

  def runTest(f: GameControllerClient[IO] => IO[Assertion]): IO[Assertion] =
    (for
      runner <- fs2.Stream.resource(BackendApp.resource)
      client <- fs2.Stream
        .resource(GameControllerClientBuilder[IO](uri"ws://localhost:9090"))
        .concurrently(runner)
      connected <- fs2.Stream.resource(client.connect(user, roomId))
      result    <- fs2.Stream.eval(f(connected))
    yield result).compile.lastOrError

  "A player can connect" in {
    runTest(client =>
      for
        _         <- client.send1(FromPlayer.Connect)
        response1 <- client.receive1
        _         <- client.send1(FromPlayer.JoinRoom)
        response2 <- client.receive1
        _         <- client.send1(FromPlayer.Connect)
        response3 <- client.receive1

        _ <- IO(
          response1 shouldBe ToPlayer.Connected(
            user,
            RoomPlayerView(
              me = user.id,
              seats = List(
                EmptySeat(0, Nil, Nil),
                EmptySeat(1, Nil, Nil),
                EmptySeat(2, Nil, Nil),
                EmptySeat(3, Nil, Nil)
              ),
              deck = Nil,
              board = Nil,
              matchInfo = None,
              dealerIndex = None,
              players = Map.empty
            )
          )
        )
        _ <- IO(response2 match
          case ToPlayer.GameEvent(event: PlayerJoinedRoom) =>
            event.user shouldBe user
          case somethingElse =>
            fail(s"Unexpected event $somethingElse")
        )
        done <-
          response3 match
            case ToPlayer.Connected(_, snapshot) =>
              IO(snapshot.players.get(user.id) shouldBe Some(user)) *>
                IO(assert(snapshot.seats.exists(_.playerOption.exists(_.is(user)))))
            case unexpected =>
              IO(fail(s"Unexpected event $unexpected"))
      yield done
    )
  }

end ConnectionSpec
