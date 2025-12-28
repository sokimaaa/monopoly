package com.sokima.monopoly
package domain.service

import domain.Money

case class GameConfig(
    startingBalance: Money = Money(1500),
    goSalary: Money = Money(200),
    boardSize: Int = 40,
    deckSeed: Long = 1L,
    bankHouses: Int = 32,
    bankHotels: Int = 12
)
