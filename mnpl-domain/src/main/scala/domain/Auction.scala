package com.sokima.monopoly
package domain

case class Auction(
    propertyId: PropertyId,
    activeBidders: Set[PlayerId],
    highestBid: Money = Money(0),
    highestBidderId: Option[PlayerId] = None
) {
  def placeBid(bidderId: PlayerId, amount: Money): Either[String, Auction] =
    if (!activeBidders.contains(bidderId)) Left("Bidder not in auction")
    else if (amount.amount <= highestBid.amount) Left("Bid too low")
    else Right(copy(highestBid = amount, highestBidderId = Some(bidderId)))

  def fold(bidderId: PlayerId): Auction =
    copy(activeBidders = activeBidders - bidderId)

  def winner: Option[PlayerId] =
    if (activeBidders.size == 1) {
      highestBidderId.orElse(activeBidders.headOption)
    } else {
      None
    }
}
