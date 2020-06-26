package gmax

import io.circe.Json

object KVJson {

  sealed trait KVJsonCodec[T] {
    def kv(key: String, value: T): Json
  }

  implicit object kvString extends KVJsonCodec[String] {
    def kv(key: String, value: String): Json = Json.obj(
      (key, Json.fromString(value))
    )
  }

  implicit object kvInt extends KVJsonCodec[Int] {
    def kv(key: String, value: Int): Json = Json.obj(
      (key, Json.fromInt(value))
    )
  }

  def kvJson[A](key: String, value: A)(implicit F: KVJsonCodec[A]): Json =
    F.kv(key, value)
}
