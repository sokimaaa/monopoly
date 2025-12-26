package com.sokima.monopoly
package application.port

import domain.{Board, Property, PropertyId}

// todo: think about moving this to other layer (domain/service?)
trait BoardFactory {

  def createBoard(): Board

  def extractProperties(board: Board): Map[PropertyId, Property]
}
