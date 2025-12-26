package com.sokima.monopoly
package domain.service

import domain.*

class SquareActionService(propertyService: PropertyService) {

  def determineAction(square: Square, game: Game): SquareAction = {
    square match {
      case Square.PropertySquare(property) =>
        property.ownerId match {
          case None => SquareAction.PropertyAvailable(property)
          case Some(ownerId) if ownerId == game.currentPlayer.id => SquareAction.NoAction
          case Some(ownerId) => SquareAction.PayRentAction(property, ownerId)
        }

      case Square.Tax(_, amount) => SquareAction.PayTaxAction(amount)
      case Square.GoToJail(_) => SquareAction.GoToJailAction
      case _ => SquareAction.NoAction
    }
  }
}
