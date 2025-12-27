package com.sokima.monopoly
package domain

case class Board(squares: Vector[Square]) {
  def size: Int = squares.length
  def getSquare(position: Position): Option[Square] =
    squares.find(_.position == position)

  def updateProperty(property: Property): Board =
    copy(
      squares = squares.map {
        case Square.PropertySquare(existing) if existing.id == property.id =>
          Square.PropertySquare(property)
        case other => other
      }
    )

  def releaseProperties(propertyIds: Set[PropertyId]): (Board, List[Property]) = {
    var released: List[Property] = Nil
    val updatedSquares = squares.map {
      case Square.PropertySquare(property) if propertyIds.contains(property.id) =>
        val updated = property.clearOwner
        released = updated :: released
        Square.PropertySquare(updated)
      case other => other
    }
    (copy(squares = updatedSquares), released.reverse)
  }

  def jailPosition: Option[Position] =
    squares.collectFirst { case Square.Jail(position) => position }
}
