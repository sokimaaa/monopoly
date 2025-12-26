package com.sokima.monopoly
package domain

case class GameRules(
                      landingActionsEnabled: Boolean,
                      allowPropertyPurchases: Boolean
                    )

object GameRules {
  val MoveOnly: GameRules = GameRules(
    landingActionsEnabled = false,
    allowPropertyPurchases = false
  )
}
