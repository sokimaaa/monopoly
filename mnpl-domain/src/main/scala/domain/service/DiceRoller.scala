package com.sokima.monopoly
package domain.service

import domain.DiceRoll

trait DiceRoller {
  def roll(): DiceRoll
}

class RandomDiceRoller extends DiceRoller {
  private val random = new scala.util.Random()

  def roll(): DiceRoll = {
    val d1 = random.nextInt(6) + 1
    val d2 = random.nextInt(6) + 1
    DiceRoll(d1, d2)
  }
}
