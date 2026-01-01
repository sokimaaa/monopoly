package com.sokima.monopoly
package infrastructure.adapter.cli

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.{AbstractComponent, ComponentRenderer, TextGUIGraphics}
import com.googlecode.lanterna.{TerminalPosition, TerminalSize}
import domain.{ColorGroup, Game, PlayerId, Square}

final case class PlayerStyle(icon: String, color: TextColor)

final class TuiBoardComponent(
    initialCellWidth: Int = 10,
    initialCellHeight: Int = 4
) extends AbstractComponent[TuiBoardComponent] {

  private var game: Option[Game] = None
  private var playerStyles: Map[PlayerId, PlayerStyle] = Map.empty
  private var cellWidth: Int = initialCellWidth
  private var cellHeight: Int = initialCellHeight

  private val minCellWidth = 5
  private val minCellHeight = 3

  def updateGame(newGame: Game): Unit = {
    game = Some(newGame)
    invalidate()
  }

  def updatePlayerStyles(styles: Map[PlayerId, PlayerStyle]): Unit = {
    playerStyles = styles
    invalidate()
  }

  def updateCellSize(width: Int, height: Int): Unit = {
    cellWidth = math.max(minCellWidth, width)
    cellHeight = math.max(minCellHeight, height)
    invalidate()
  }

  override protected def createDefaultRenderer(): ComponentRenderer[TuiBoardComponent] =
    new ComponentRenderer[TuiBoardComponent] {
      override def getPreferredSize(component: TuiBoardComponent): TerminalSize =
        new TerminalSize(cellWidth * 11, cellHeight * 11)

      override def drawComponent(graphics: TextGUIGraphics, component: TuiBoardComponent): Unit = {
        val size = graphics.getSize
        graphics.setBackgroundColor(TextColor.ANSI.BLACK)
        graphics.setForegroundColor(TextColor.ANSI.WHITE)
        graphics.fillRectangle(new TerminalPosition(0, 0), size, ' ')
        if (size.getColumns < minBoardWidth || size.getRows < minBoardHeight) {
          graphics.putString(1, 1, "Terminal too small for board")
          graphics.putString(1, 2, "Resize to see full map")
        } else {
          game.foreach(drawBoard(graphics, _))
        }
      }
    }

  private def minBoardWidth: Int = minCellWidth * 11
  private def minBoardHeight: Int = minCellHeight * 11

  private def drawBoard(graphics: TextGUIGraphics, currentGame: Game): Unit = {
    val playersByPosition = currentGame.players.groupBy(_.position.value)
    currentGame.board.squares.foreach { square =>
      val posValue = square.position.value
      val (col, row) = positionToCell(posValue)
      val players = playersByPosition.getOrElse(posValue, Nil).map { player =>
        playerStyles.getOrElse(player.id, PlayerStyle("🙂", TextColor.ANSI.WHITE))
      }
      drawCell(graphics, col, row, square, players)
    }
  }

  private def drawCell(
      graphics: TextGUIGraphics,
      col: Int,
      row: Int,
      square: Square,
      players: List[PlayerStyle]
  ): Unit = {
    val x = col * cellWidth
    val y = row * cellHeight
    drawBorder(graphics, x, y)

    val label = fitLabel(squareLabel(square), cellWidth - 2)
    val labelColor = squareLabelColor(square)
    graphics.setForegroundColor(labelColor)
    graphics.putString(x + 1, y + 1, label)

    if (players.nonEmpty) {
      var currentX = x + 1
      val maxIcons = math.max(1, (cellWidth - 2) / 2)
      players.take(maxIcons).foreach { style =>
        graphics.setForegroundColor(style.color)
        graphics.putString(currentX, y + 2, style.icon)
        currentX += 2
      }
    }
  }

  private def drawBorder(graphics: TextGUIGraphics, x: Int, y: Int): Unit = {
    graphics.setForegroundColor(TextColor.ANSI.WHITE)
    val right = x + cellWidth - 1
    val bottom = y + cellHeight - 1

    graphics.setCharacter(x, y, '+')
    graphics.setCharacter(right, y, '+')
    graphics.setCharacter(x, bottom, '+')
    graphics.setCharacter(right, bottom, '+')

    (x + 1 until right).foreach { col =>
      graphics.setCharacter(col, y, '-')
      graphics.setCharacter(col, bottom, '-')
    }
    (y + 1 until bottom).foreach { row =>
      graphics.setCharacter(x, row, '|')
      graphics.setCharacter(right, row, '|')
    }
  }

  private def positionToCell(position: Int): (Int, Int) = {
    if (position <= 10) (10 - position, 10)
    else if (position <= 19) (0, 10 - (position - 10))
    else if (position == 20) (0, 0)
    else if (position <= 29) (position - 20, 0)
    else if (position == 30) (10, 0)
    else (10, position - 30)
  }

  private def squareLabel(square: Square): String = square match {
    case Square.PropertySquare(prop) =>
      abbreviateName(prop.name)
    case Square.Go(_) =>
      "GO 🏁"
    case Square.Jail(_) =>
      "JAIL 🔒"
    case Square.FreeParking(_) =>
      "FREE 🅿"
    case Square.GoToJail(_) =>
      "GO JAIL"
    case Square.Tax(_, amount) =>
      s"TAX $${amount.amount}"
    case Square.PercentTax(_, percent) =>
      s"TAX ${percent}%"
    case Square.Chance(_) =>
      "CHANCE ❓"
    case Square.CommunityChest(_) =>
      "CHEST 🎁"
  }

  private def squareLabelColor(square: Square): TextColor = square match {
    case Square.PropertySquare(prop) =>
      prop.colorGroup.map(colorForGroup).getOrElse(TextColor.ANSI.WHITE)
    case Square.Go(_) =>
      TextColor.ANSI.GREEN
    case Square.Jail(_) =>
      TextColor.ANSI.RED
    case Square.GoToJail(_) =>
      TextColor.ANSI.RED
    case Square.FreeParking(_) =>
      TextColor.ANSI.CYAN
    case Square.Chance(_) =>
      TextColor.ANSI.MAGENTA
    case Square.CommunityChest(_) =>
      TextColor.ANSI.YELLOW
    case _ =>
      TextColor.ANSI.WHITE
  }

  private def colorForGroup(group: ColorGroup): TextColor = group match {
    case ColorGroup.Brown =>
      TextColor.ANSI.YELLOW
    case ColorGroup.LightBlue =>
      TextColor.ANSI.CYAN
    case ColorGroup.Pink =>
      TextColor.ANSI.MAGENTA
    case ColorGroup.Orange =>
      TextColor.ANSI.RED
    case ColorGroup.Red =>
      TextColor.ANSI.RED
    case ColorGroup.Yellow =>
      TextColor.ANSI.YELLOW
    case ColorGroup.Green =>
      TextColor.ANSI.GREEN
    case ColorGroup.DarkBlue =>
      TextColor.ANSI.BLUE
  }

  private def abbreviateName(name: String): String = {
    val normalized = name
      .replace("Avenue", "Ave")
      .replace("Place", "Pl")
      .replace("Railroad", "RR")
      .replace("Company", "Co")
      .replace("Electric", "Elec")
      .replace("Pennsylvania", "Penn")
      .replace("Connecticut", "Conn")
      .replace("Mediterranean", "Med")
      .replace("Community", "Comm")

    val words = normalized.split(" ").filter(_.nonEmpty)
    val full = words.mkString(" ")
    val maxLength = cellWidth - 2
    if (full.length <= maxLength) full
    else {
      val compact = words.map(_.take(3)).mkString(" ")
      if (compact.length <= maxLength) compact else compact.take(maxLength)
    }
  }

  private def fitLabel(label: String, maxWidth: Int): String = {
    if (label.length <= maxWidth) label.padTo(maxWidth, ' ')
    else label.take(maxWidth)
  }
}
