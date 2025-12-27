package com.sokima.monopoly
package domain

case class TurnState(
    diceRolled: Boolean = false,
    doublesCount: Int = 0,
    extraRoll: Boolean = false
)
