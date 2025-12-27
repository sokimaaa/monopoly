package com.sokima.monopoly
package domain.service

import domain.*

class SquareActionService(propertyService: PropertyService) {

  def determineAction(square: Square, game: Game): SquareAction =
    square match {
      case Square.PropertySquare(property) =>
        property.ownerId match {
          case None => SquareAction.PropertyAvailable(property)
          case Some(ownerId) if ownerId == game.currentPlayer.id => SquareAction.NoAction
          case Some(ownerId) =>
            game.players.find(_.id == ownerId) match {
              case Some(owner) if owner.isBankrupt => SquareAction.PropertyAvailable(property)
              case Some(_)                         => SquareAction.PayRentAction(property, ownerId)
              case None                            => SquareAction.PropertyAvailable(property)
            }
        }

      case Square.Tax(_, amount) => SquareAction.PayTaxAction(amount)
      case Square.PercentTax(_, percent) =>
        val tax = Money((game.currentPlayer.balance.amount * percent) / 100)
        SquareAction.PayTaxAction(tax)
      case Square.GoToJail(_)    => SquareAction.GoToJailAction
      case _                     => SquareAction.NoAction
    }
}
