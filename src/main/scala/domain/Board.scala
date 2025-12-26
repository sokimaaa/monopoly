package com.sokima.monopoly
package domain

case class Board(squares: Vector[Square]) {
  def size: Int = squares.length
  def getSquare(position: Position): Option[Square] =
    squares.find(_.position == position)
}
