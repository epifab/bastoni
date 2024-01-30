package bastoni.domain.repos

import bastoni.domain.logic.GameStateMachine
import cats.effect.{Concurrent, Ref, Sync}
import cats.syntax.all.*
import io.circe.{Decoder, Encoder, JsonObject, KeyEncoder}
import org.typelevel.log4cats.Logger
import io.circe.syntax.EncoderOps

trait KeyValueRepo[F[_], K, V]:
  def get(key: K): F[Option[V]]
  def set(key: K, value: V): F[Unit]
  def remove(key: K): F[Unit]

object KeyValueRepo:

  private class InMemoryKeyValueRepo[F[_]: Concurrent, K, V](data: Ref[F, Map[K, V]]) extends KeyValueRepo[F, K, V]:
    override def get(key: K): F[Option[V]]      = data.get.map(_.get(key))
    override def set(key: K, value: V): F[Unit] = data.update(_ + (key -> value))
    override def remove(key: K): F[Unit]        = data.update(_ - key)

  def inMemory[F[_]: Concurrent, K, V]: F[KeyValueRepo[F, K, V]] =
    Ref.of[F, Map[K, V]](Map.empty).map(new InMemoryKeyValueRepo(_))
end KeyValueRepo
