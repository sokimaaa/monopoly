package com.sokima.monopoly
package domain

sealed trait SquareAction

object SquareAction {
  case object NoAction extends SquareAction

  case class PropertyAvailable(property: Property) extends SquareAction

  case class PayRentAction(property: Property, landlordId: PlayerId) extends SquareAction

  case class PayTaxAction(amount: Money) extends SquareAction

  case object GoToJailAction extends SquareAction
}
