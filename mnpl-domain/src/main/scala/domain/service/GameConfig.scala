package com.sokima.monopoly
package domain.service

import domain.{GameRules, Money}

case class GameConfig
(
  startingBalance: Money = Money(1500),
  goSalary: Money = Money(200),
  boardSize: Int = 40,
  rules: GameRules = GameRules.MoveOnly
)
