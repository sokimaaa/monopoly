package com.sokima.monopoly
package domain

sealed trait GameStatus

object GameStatus {
  case object Running extends GameStatus
  case object Finished extends GameStatus
}
