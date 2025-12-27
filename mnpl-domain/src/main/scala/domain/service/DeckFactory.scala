package com.sokima.monopoly
package domain.service

import domain.{Card, Deck, Money, Position}

object DeckFactory {
  def chanceDeck(seed: Long): Deck =
    Deck(
      Vector(
        Card.MoveTo(Position(0), awardGo = true),
        Card.MoveTo(Position(10), awardGo = false),
        Card.MoveBack(3),
        Card.Gain(Money(50)),
        Card.Pay(Money(15)),
        Card.GoToJail,
        Card.GetOutOfJailFree,
        Card.Repairs(Money(0), Money(0))
      ),
      seed
    )

  def communityDeck(seed: Long): Deck =
    Deck(
      Vector(
        Card.Gain(Money(200)),
        Card.Gain(Money(50)),
        Card.Pay(Money(50)),
        Card.Pay(Money(100)),
        Card.GoToJail,
        Card.GetOutOfJailFree,
        Card.MoveTo(Position(0), awardGo = true),
        Card.Repairs(Money(0), Money(0))
      ),
      seed + 1
    )
}
