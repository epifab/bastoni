package bastoni.domain

import cats.effect.IO
import org.typelevel.log4cats.Logger

object ScalaJsCompatibleLogger extends Logger[IO]:

  override def error(message: => String): IO[Unit] = IO.println(message)

  override def warn(message: => String): IO[Unit] = IO.println(message)

  override def info(message: => String): IO[Unit] = IO.println(message)

  override def debug(message: => String): IO[Unit] = IO.println(message)

  override def trace(message: => String): IO[Unit] = IO.println(message)

  override def error(t: Throwable)(message: => String): IO[Unit] = IO.println(message)

  override def warn(t: Throwable)(message: => String): IO[Unit] = IO.println(message)

  override def info(t: Throwable)(message: => String): IO[Unit] = IO.println(message)

  override def debug(t: Throwable)(message: => String): IO[Unit] = IO.println(message)

  override def trace(t: Throwable)(message: => String): IO[Unit] = IO.println(message)
