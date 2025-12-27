package com.sokima.monopoly
package domain

import scala.util.Random

case class Deck(
    cards: Vector[Card],
    drawIndex: Int,
    discard: Vector[Card],
    seed: Long,
    shuffleCount: Int
) {
  def draw(): (Card, Deck) = {
    val prepared =
      if (drawIndex >= cards.length) reshuffle else this

    val card = prepared.cards(prepared.drawIndex)
    val nextDiscard =
      if (card == Card.GetOutOfJailFree) prepared.discard
      else prepared.discard :+ card

    val updated = prepared.copy(
      drawIndex = prepared.drawIndex + 1,
      discard = nextDiscard
    )
    (card, updated)
  }

  def returnCard(card: Card): Deck =
    copy(discard = discard :+ card)

  private def reshuffle: Deck = {
    val nextSeed = seed + shuffleCount + 1
    val shuffled = Deck.shuffle(discard, nextSeed)
    copy(cards = shuffled, drawIndex = 0, discard = Vector.empty, shuffleCount = shuffleCount + 1)
  }
}

object Deck {
  def apply(cards: Vector[Card], seed: Long): Deck =
    Deck(
      cards = shuffle(cards, seed),
      drawIndex = 0,
      discard = Vector.empty,
      seed = seed,
      shuffleCount = 0
    )

  def shuffle(cards: Vector[Card], seed: Long): Vector[Card] =
    Random(seed).shuffle(cards)
}
