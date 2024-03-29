package bastoni.backend

import bastoni.domain.model.*
import bastoni.domain.model.Event.PlayerJoinedTable
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import cats.effect.IO
import cats.implicits.showInterpolator
import org.http4s.syntax.all.uri
import org.http4s.Uri
import org.scalatest.freespec.AsyncFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.Assertion

class ConnectionSpec extends AsyncFreeSpecLike with AsyncIOSpec with Matchers:
  override given ioRuntime: IORuntime = cats.effect.unsafe.IORuntime.global

  val user: User     = User(UserId.newId, "John Doe")
  val roomId: RoomId = RoomId.newId

  def runTest(f: GameControllerClient[IO] => IO[Assertion]): IO[Assertion] =
    (for
      runner <- fs2.Stream.resource(BackendApp.resource)
      client <- fs2.Stream
        .resource(GameControllerClientBuilder[IO](uri"ws://localhost:9090"))
        .concurrently(runner)
      connected <- fs2.Stream.resource(client.connect(roomId))
      result    <- fs2.Stream.eval(f(connected))
    yield result).compile.lastOrError

  "A player can connect" in {
    runTest(client =>
      for
        authToken              <- InsecureAuthController.tokenize(Account(user))
        authResponse           <- client.askTo(FromPlayer.Authenticate(authToken))
        connectResponse        <- client.askTo(FromPlayer.Connect)
        joinRoomResponse       <- client.askTo(FromPlayer.JoinTable)
        updatedConnectResponse <- client.askTo(FromPlayer.Connect)

        _ <- IO(
          authResponse shouldBe ToPlayer.Authenticated(user)
        )
        _ <- IO(
          connectResponse shouldBe ToPlayer.Connected(
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
        _ <- IO(joinRoomResponse match
          case ToPlayer.GameEvent(event: PlayerJoinedTable) =>
            event.user shouldBe user
          case somethingElse =>
            fail(s"Unexpected event $somethingElse")
        )
        done <-
          updatedConnectResponse match
            case ToPlayer.Connected(snapshot) =>
              IO(snapshot.players.get(user.id) shouldBe Some(user)) *>
                IO(assert(snapshot.seats.exists(_.playerOption.exists(_.is(user)))))
            case unexpected =>
              IO(fail(s"Unexpected event $unexpected"))
      yield done
    )
  }

end ConnectionSpec
