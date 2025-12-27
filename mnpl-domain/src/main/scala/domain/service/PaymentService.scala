package com.sokima.monopoly
package domain.service

import domain.{Game, Money, Player, PlayerId, Property}

sealed trait Payee

object Payee {
  case object Bank extends Payee
  case class Player(playerId: PlayerId) extends Payee
}

case class PaymentResult(
    game: Game,
    events: List[GameEvent],
    releasedProperties: List[Property]
)

class PaymentService {

  def pay(
      game: Game,
      fromPlayerId: PlayerId,
      to: Payee,
      amount: Money
  ): Either[String, PaymentResult] =
    game.players.find(_.id == fromPlayerId) match {
      case None => Left("Payer not found")
      case Some(payer) =>
        val payerAfterLiquidation =
          if (payer.canAfford(amount)) payer else attemptLiquidation(payer, amount)
        val gameAfterLiquidation = game.updatePlayer(payerAfterLiquidation)

        to match {
          case Payee.Bank =>
            if (payerAfterLiquidation.canAfford(amount)) {
              val updatedPayer = payerAfterLiquidation.pay(amount)
              val updatedGame  = gameAfterLiquidation.updatePlayer(updatedPayer)
              Right(PaymentResult(updatedGame, Nil, Nil))
            } else {
              val (bankruptGame, releasedProperties) =
                gameAfterLiquidation.markPlayerBankrupt(fromPlayerId)
              Right(
                PaymentResult(
                  bankruptGame,
                  List(GameEvent.PlayerBankrupt(fromPlayerId)),
                  releasedProperties
                )
              )
            }

          case Payee.Player(payeeId) =>
            game.players.find(_.id == payeeId) match {
              case None => Left("Payee not found")
              case Some(payee) =>
                val updatedPayer  = payerAfterLiquidation.pay(amount)
                val updatedPayee  = payee.receive(amount)
                val updatedGame   = gameAfterLiquidation
                  .updatePlayer(updatedPayer)
                  .updatePlayer(updatedPayee)
                Right(PaymentResult(updatedGame, Nil, Nil))
            }
        }
    }

  private def attemptLiquidation(player: Player, amount: Money): Player =
    player
}
