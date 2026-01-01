package com.sokima.monopoly
package infrastructure.adapter.cli

import application.port.GameRepository
import application.usecase.*
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.input.{KeyStroke, KeyType}
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.TerminalSize
import domain.{Game, GameId, Money, Player, PlayerId, Square}

import java.util.concurrent.atomic.AtomicReference

class TuiGameController(
    createGameUseCase: CreateGameUseCase,
    rollDiceUseCase: RollDiceUseCase,
    buyPropertyUseCase: BuyPropertyUseCase,
    declinePropertyUseCase: DeclinePropertyUseCase,
    auctionBidUseCase: AuctionBidUseCase,
    payJailFineUseCase: PayJailFineUseCase,
    endTurnUseCase: EndTurnUseCase,
    gameRepository: GameRepository
) {

  private val playerPalette = Vector(
    PlayerStyle("🎩", TextColor.ANSI.YELLOW),
    PlayerStyle("🚗", TextColor.ANSI.CYAN),
    PlayerStyle("🐶", TextColor.ANSI.GREEN),
    PlayerStyle("🐱", TextColor.ANSI.MAGENTA)
  )

  private var terminalStarted = false
  private var screen: TerminalScreen = _
  private var gui: MultiWindowTextGUI = _

  private val boardComponent = new TuiBoardComponent()
  private var playerStyles: Map[PlayerId, PlayerStyle] = Map.empty
  private var turnActive: Boolean = false
  private var currentGameId: Option[GameId] = None
  private var sidebarWidth: Int = 26

  private var playerLabels: List[(PlayerId, Label)] = Nil
  private var infoLabel: Label = _
  private var auctionLabel: Label = _
  private var messageLabel: Label = _
  private var buttons: ActionButtons = _

  def displayWelcome(): Unit = {}

  def startNewGame(): Either[String, GameId] = {
    ensureUi()
    val players = promptPlayers()
    players match {
      case None =>
        stopUi()
        Left("Game creation canceled")
      case Some(names) =>
        createGameUseCase.execute(names) match {
          case Right(game) =>
            playerStyles = assignPlayerStyles(game.players)
            boardComponent.updatePlayerStyles(playerStyles)
            Right(game.id)
          case Left(error) =>
            showError(error)
            stopUi()
            Left(error)
        }
    }
  }

  def gameLoop(gameId: GameId): Unit = {
    ensureUi()
    val gameOpt = gameRepository.findById(gameId)
    if (gameOpt.isEmpty) {
      stopUi()
      return
    }

    currentGameId = Some(gameId)
    val window = buildMainWindow(gameId, gameOpt.get)
    gui.addWindowAndWait(window)
    stopUi()
  }

  private def ensureUi(): Unit = {
    if (!terminalStarted) {
      val terminal = new DefaultTerminalFactory().createTerminal()
      screen = new TerminalScreen(terminal)
      screen.startScreen()
      gui = new MultiWindowTextGUI(screen)
      terminalStarted = true
    }
  }

  private def stopUi(): Unit = {
    if (terminalStarted) {
      screen.stopScreen()
      terminalStarted = false
    }
  }

  private def buildMainWindow(gameId: GameId, game: Game): BasicWindow = {
    val window = new BasicWindow("Monopoly") {
      override def handleInput(key: KeyStroke): Boolean = {
        if (key.getKeyType == KeyType.Character) {
          key.getCharacter.charValue().toLower match {
            case 'r' => handleRoll(gameId)
            case 'b' => handleBuy(gameId)
            case 'd' => handleDecline(gameId)
            case 'a' => handleBid(gameId)
            case 'f' => handleFold(gameId)
            case 'p' => handlePayJail(gameId)
            case 'e' => handleEndTurn(gameId)
            case 's' => handleStatus(gameId)
            case 'q' => handleQuit()
            case _   => ()
          }
          true
        } else super.handleInput(key)
      }
    }

    val root = new Panel()
    root.setLayoutManager(new BorderLayout())

    updateLayoutSizing()
    val sidebar = buildSidebar(game)
    val center = buildCenter()
    val helpBar = buildHelpBar()

    root.addComponent(sidebar, BorderLayout.Location.LEFT)
    root.addComponent(center, BorderLayout.Location.CENTER)
    root.addComponent(helpBar, BorderLayout.Location.BOTTOM)
    window.setComponent(root)

    refreshView(game, Some("✅ Game started. Press [r] to roll."))

    window
  }

  private def buildSidebar(game: Game): Panel = {
    val panel = new Panel()
    panel.setLayoutManager(new LinearLayout(Direction.VERTICAL))
    panel.setPreferredSize(new TerminalSize(sidebarWidth, 0))

    val title = new Label("Players")
    title.setForegroundColor(TextColor.ANSI.CYAN)
    panel.addComponent(title)

    playerLabels = game.players.map { player =>
      val label = new Label("")
      panel.addComponent(label)
      player.id -> label
    }

    panel.addComponent(new EmptySpace(new TerminalSize(0, 1)))
    panel
  }

  private def buildCenter(): Panel = {
    val panel = new Panel()
    panel.setLayoutManager(new LinearLayout(Direction.VERTICAL))

    val title = new Label("🎩 MONOPOLY — Classic Board")
    title.setForegroundColor(TextColor.ANSI.YELLOW)
    panel.addComponent(title)
    panel.addComponent(boardComponent)

    infoLabel = new Label("")
    auctionLabel = new Label("")
    messageLabel = new Label("")
    messageLabel.setForegroundColor(TextColor.ANSI.GREEN)

    val actions = buildActionBar()

    panel.addComponent(infoLabel)
    panel.addComponent(auctionLabel)
    panel.addComponent(actions)
    panel.addComponent(messageLabel)
    panel
  }

  private def buildActionBar(): Panel = {
    val panel = new Panel(new GridLayout(5))

    val rollBtn = new Button("🎲 Roll", () => handleRollFromButton())
    val buyBtn = new Button("🏠 Buy", () => handleBuyFromButton())
    val declineBtn = new Button("🔨 Decline", () => handleDeclineFromButton())
    val bidBtn = new Button("💸 Bid", () => handleBidFromButton())
    val foldBtn = new Button("🙅 Fold", () => handleFoldFromButton())
    val payBtn = new Button("🔓 Pay Jail", () => handlePayJailFromButton())
    val endBtn = new Button("⏭ End", () => handleEndTurnFromButton())
    val statusBtn = new Button("📊 Status", () => handleStatusFromButton())
    val quitBtn = new Button("🚪 Quit", () => handleQuit())

    buttons = ActionButtons(rollBtn, buyBtn, declineBtn, bidBtn, foldBtn, payBtn, endBtn, statusBtn, quitBtn)

    panel.addComponent(rollBtn)
    panel.addComponent(buyBtn)
    panel.addComponent(declineBtn)
    panel.addComponent(bidBtn)
    panel.addComponent(foldBtn)
    panel.addComponent(payBtn)
    panel.addComponent(endBtn)
    panel.addComponent(statusBtn)
    panel.addComponent(quitBtn)
    panel
  }

  private def buildHelpBar(): Label = {
    val label = new Label(
      "Keys: r Roll | b Buy | d Decline | a Bid | f Fold | p Pay Jail | e End | s Status | q Quit"
    )
    label.setForegroundColor(TextColor.ANSI.CYAN)
    label
  }

  private def refreshView(game: Game, message: Option[String] = None): Unit = {
    updateLayoutSizing()
    boardComponent.updateGame(game)
    updateSidebar(game)
    updateInfo(game)
    updateAuction(game)
    message.foreach(messageLabel.setText)
    updateButtons(game)
    screen.refresh()

    if (game.isFinished) {
      showWinner(game)
    }
  }

  private def updateSidebar(game: Game): Unit = {
    val currentId = game.currentPlayer.id
    playerLabels = game.players.map { player =>
      val label = playerLabels.find(_._1 == player.id).map(_._2).getOrElse(new Label(""))
      val style = playerStyles.getOrElse(player.id, PlayerStyle("🙂", TextColor.ANSI.WHITE))
      val status = if (player.isBankrupt) "💀" else if (player.inJail) "🔒" else "🟢"
      val indicator = if (player.id == currentId) "▶" else " "
      label.setForegroundColor(style.color)
      val text = f"$indicator ${style.icon} ${player.name}%-8s $$${player.balance.amount}%4d $status"
      label.setText(fitSidebar(text))
      player.id -> label
    }
  }

  private def updateInfo(game: Game): Unit = {
    val player = game.currentPlayer
    val squareName = squareNameAt(game)
    infoLabel.setText(
      s"Turn: ${player.name} | Position: $squareName | Balance: $$${player.balance.amount}"
    )
  }

  private def updateAuction(game: Game): Unit = {
    game.auction match {
      case None =>
        auctionLabel.setText("")
      case Some(auction) =>
        val propertyName = game.board.squares.collectFirst {
          case Square.PropertySquare(prop) if prop.id == auction.propertyId => prop.name
        }.getOrElse("Unknown Property")
        val highestBidder = auction.highestBidderId
          .flatMap(id => game.players.find(_.id == id).map(_.name))
          .getOrElse("None")
        auctionLabel.setText(
          s"Auction: $propertyName | Highest: $$${auction.highestBid.amount} by $highestBidder"
        )
    }
  }

  private def updateButtons(game: Game): Unit = {
    val inAuction = game.auction.isDefined
    buttons.roll.setEnabled(!turnActive && !inAuction)
    buttons.buy.setEnabled(turnActive && !inAuction)
    buttons.decline.setEnabled(turnActive && !inAuction)
    buttons.bid.setEnabled(inAuction)
    buttons.fold.setEnabled(inAuction)
    buttons.payJail.setEnabled(game.currentPlayer.inJail && !turnActive && !inAuction)
    buttons.end.setEnabled((turnActive || game.currentPlayer.isBankrupt) && !inAuction)
    buttons.status.setEnabled(true)
    buttons.quit.setEnabled(true)
  }

  private def handleRoll(gameId: GameId): Unit =
    withGame(gameId) { _ =>
      if (!turnActive) {
        rollDiceUseCase.execute(gameId) match {
          case Right((_, roll, _)) =>
            turnActive = true
            refreshGame(gameId, Some(s"🎲 Rolled ${roll.die1} + ${roll.die2} = ${roll.total}"))
          case Left(error) =>
            refreshGame(gameId, Some(s"❌ $error"))
        }
      } else refreshGame(gameId, Some("❌ Already rolled. End turn first."))
    }

  private def handleBuy(gameId: GameId): Unit =
    withGame(gameId) { _ =>
      if (turnActive) {
        buyPropertyUseCase.execute(gameId) match {
          case Right(_) =>
            endTurnUseCase.execute(gameId)
            turnActive = false
            refreshGame(gameId, Some("✅ Property bought. Turn ended."))
          case Left(error) =>
            refreshGame(gameId, Some(s"❌ $error"))
        }
      } else refreshGame(gameId, Some("❌ Roll first."))
    }

  private def handleDecline(gameId: GameId): Unit =
    withGame(gameId) { _ =>
      if (turnActive) {
        declinePropertyUseCase.execute(gameId) match {
          case Right(_) =>
            refreshGame(gameId, Some("🔨 Property declined. Auction started."))
          case Left(error) =>
            refreshGame(gameId, Some(s"❌ $error"))
        }
      } else refreshGame(gameId, Some("❌ Roll first."))
    }

  private def handleBid(gameId: GameId): Unit =
    withGame(gameId) { game =>
      if (game.auction.isDefined) {
        promptBid(game, requireAmount = true).foreach { bid =>
          auctionBidUseCase.placeBid(gameId, bid.playerId, bid.amount) match {
            case Right(_) =>
              refreshGame(gameId, Some(s"✅ Bid placed: $$${bid.amount.amount}"))
            case Left(error) =>
              refreshGame(gameId, Some(s"❌ $error"))
          }
        }
      } else refreshGame(gameId, Some("❌ No auction in progress."))
    }

  private def handleFold(gameId: GameId): Unit =
    withGame(gameId) { game =>
      if (game.auction.isDefined) {
        promptBid(game, requireAmount = false).foreach { bid =>
          auctionBidUseCase.fold(gameId, bid.playerId) match {
            case Right(_) =>
              refreshGame(gameId, Some("✅ Bidder folded."))
            case Left(error) =>
              refreshGame(gameId, Some(s"❌ $error"))
          }
        }
      } else refreshGame(gameId, Some("❌ No auction in progress."))
    }

  private def handlePayJail(gameId: GameId): Unit =
    withGame(gameId) { game =>
      if (game.currentPlayer.inJail && !turnActive) {
        payJailFineUseCase.execute(gameId) match {
          case Right(_) =>
            refreshGame(gameId, Some("✅ Jail fine paid."))
          case Left(error) =>
            refreshGame(gameId, Some(s"❌ $error"))
        }
      } else refreshGame(gameId, Some("❌ Cannot pay jail fine now."))
    }

  private def handleEndTurn(gameId: GameId): Unit =
    withGame(gameId) { game =>
      if (turnActive || game.currentPlayer.isBankrupt) {
        endTurnUseCase.execute(gameId)
        turnActive = false
        refreshGame(gameId, Some("⏭ Turn ended."))
      } else refreshGame(gameId, Some("❌ Roll first."))
    }

  private def handleStatus(gameId: GameId): Unit =
    withGame(gameId)(showStatus)

  private def handleQuit(): Unit = {
    gui.getActiveWindow.close()
  }

  private def handleRollFromButton(): Unit = handleAction(handleRoll)
  private def handleBuyFromButton(): Unit = handleAction(handleBuy)
  private def handleDeclineFromButton(): Unit = handleAction(handleDecline)
  private def handleBidFromButton(): Unit = handleAction(handleBid)
  private def handleFoldFromButton(): Unit = handleAction(handleFold)
  private def handlePayJailFromButton(): Unit = handleAction(handlePayJail)
  private def handleEndTurnFromButton(): Unit = handleAction(handleEndTurn)
  private def handleStatusFromButton(): Unit = handleAction(handleStatus)

  private def handleAction(handler: GameId => Unit): Unit =
    currentGameId.foreach(handler)

  private def refreshGame(gameId: GameId, message: Option[String]): Unit =
    gameRepository.findById(gameId).foreach(refreshView(_, message))

  private def withGame(gameId: GameId)(f: Game => Unit): Unit =
    gameRepository.findById(gameId).foreach(f)

  private def promptPlayers(): Option[List[String]] = {
    val window = new BasicWindow("New Game")
    val root = new Panel(new LinearLayout(Direction.VERTICAL))
    val form = new Panel(new GridLayout(2))

    form.addComponent(new Label("Players (2-4):"))
    val countBox = new TextBox(new TerminalSize(4, 1))
    countBox.setText("2")
    form.addComponent(countBox)

    val nameBoxes = (1 to 4).map { idx =>
      form.addComponent(new Label(s"Player $idx name:"))
      val box = new TextBox(new TerminalSize(18, 1))
      form.addComponent(box)
      box
    }.toList

    val errorLabel = new Label("")
    errorLabel.setForegroundColor(TextColor.ANSI.RED)

    val result = new AtomicReference[Option[List[String]]](None)

    val startBtn = new Button("Start", () => {
      val count = countBox.getText.trim.toIntOption.getOrElse(0)
      val names = nameBoxes.map(_.getText.trim).filter(_.nonEmpty).take(count)
      if (count < 2 || count > 4) {
        errorLabel.setText("Please enter 2-4 players.")
      } else if (names.size < count) {
        errorLabel.setText("Provide all player names.")
      } else {
        result.set(Some(names))
        window.close()
      }
    })

    val cancelBtn = new Button("Cancel", () => window.close())

    val buttons = new Panel(new GridLayout(2))
    buttons.addComponent(startBtn)
    buttons.addComponent(cancelBtn)

    root.addComponent(form)
    root.addComponent(errorLabel)
    root.addComponent(buttons)
    window.setComponent(root)
    gui.addWindowAndWait(window)

    result.get()
  }

  private def promptBid(game: Game, requireAmount: Boolean): Option[BidInput] = {
    val window = new BasicWindow(if (requireAmount) "Place Bid" else "Fold Bidder")
    val root = new Panel(new LinearLayout(Direction.VERTICAL))
    val playersHint = new Label(
      game.players.map(p => s"${p.id.value}: ${p.name}").mkString("Players: ", " | ", "")
    )
    playersHint.setForegroundColor(TextColor.ANSI.CYAN)
    val form = new Panel(new GridLayout(2))

    form.addComponent(new Label("Bidder id:"))
    val bidderBox = new TextBox(new TerminalSize(14, 1))
    form.addComponent(bidderBox)

    val amountBox =
      if (requireAmount) {
        form.addComponent(new Label("Amount:"))
        val box = new TextBox(new TerminalSize(10, 1))
        form.addComponent(box)
        Some(box)
      } else None

    val errorLabel = new Label("")
    errorLabel.setForegroundColor(TextColor.ANSI.RED)
    val result = new AtomicReference[Option[BidInput]](None)

    val okBtn = new Button("OK", () => {
      val bidderText = bidderBox.getText.trim
      val bidderOpt = game.players.find(_.id.value == bidderText).map(_.id)
      if (bidderOpt.isEmpty) {
        errorLabel.setText("Unknown bidder id.")
      } else {
        val amount = amountBox.flatMap(_.getText.trim.toIntOption).getOrElse(0)
        if (requireAmount && amount <= 0) {
          errorLabel.setText("Enter a valid amount.")
        } else {
          result.set(Some(BidInput(bidderOpt.get, Money(amount))))
          window.close()
        }
      }
    })

    val cancelBtn = new Button("Cancel", () => window.close())
    val buttons = new Panel(new GridLayout(2))
    buttons.addComponent(okBtn)
    buttons.addComponent(cancelBtn)

    root.addComponent(playersHint)
    root.addComponent(form)
    root.addComponent(errorLabel)
    root.addComponent(buttons)
    window.setComponent(root)
    gui.addWindowAndWait(window)
    result.get()
  }

  private def showStatus(game: Game): Unit = {
    val window = new BasicWindow("Players Status")
    val panel = new Panel(new LinearLayout(Direction.VERTICAL))
    game.players.foreach { player =>
      val style = playerStyles.getOrElse(player.id, PlayerStyle("🙂", TextColor.ANSI.WHITE))
      val status = if (player.isBankrupt) "💀" else if (player.inJail) "🔒" else "🟢"
      val label = new Label(s"${style.icon} ${player.name} | $$${player.balance.amount} | $status")
      label.setForegroundColor(style.color)
      panel.addComponent(label)
    }
    window.setComponent(panel)
    gui.addWindowAndWait(window)
  }

  private def showWinner(game: Game): Unit = {
    val winner = game.activePlayers.headOption.map(_.name).getOrElse("No one")
    val window = new BasicWindow("Game Over")
    val panel = new Panel(new LinearLayout(Direction.VERTICAL))
    val label = new Label(s"🏆 $winner wins! 🏆")
    label.setForegroundColor(TextColor.ANSI.YELLOW)
    panel.addComponent(label)
    panel.addComponent(new Button("Close", () => window.close()))
    window.setComponent(panel)
    gui.addWindowAndWait(window)
    handleQuit()
  }

  private def showError(message: String): Unit = {
    val window = new BasicWindow("Error")
    val panel = new Panel(new LinearLayout(Direction.VERTICAL))
    val label = new Label(s"❌ $message")
    label.setForegroundColor(TextColor.ANSI.RED)
    panel.addComponent(label)
    panel.addComponent(new Button("Close", () => window.close()))
    window.setComponent(panel)
    gui.addWindowAndWait(window)
  }

  private def squareNameAt(game: Game): String =
    game.board.getSquare(game.currentPlayer.position).map {
      case Square.PropertySquare(prop) => prop.name
      case Square.Go(_)                => "GO"
      case Square.Jail(_)              => "Jail"
      case Square.FreeParking(_)       => "Free Parking"
      case Square.GoToJail(_)          => "Go To Jail"
      case Square.Tax(_, _)            => "Tax"
      case Square.PercentTax(_, _)     => "Tax"
      case Square.Chance(_)            => "Chance"
      case Square.CommunityChest(_)    => "Community Chest"
    }.getOrElse("Unknown")

  private def assignPlayerStyles(players: List[Player]): Map[PlayerId, PlayerStyle] =
    players.zipWithIndex.map { case (player, idx) =>
      player.id -> playerPalette(idx % playerPalette.size)
    }.toMap

  private def updateLayoutSizing(): Unit = {
    val screenSize = screen.getTerminalSize
    val columns = screenSize.getColumns
    val rows = screenSize.getRows

    sidebarWidth = math.min(26, math.max(20, columns / 4))
    val availableWidth = math.max(0, columns - sidebarWidth - 4)
    val availableHeight = math.max(0, rows - 10)

    val cellWidth = math.max(5, availableWidth / 11)
    val cellHeight = math.max(3, availableHeight / 11)

    boardComponent.updateCellSize(cellWidth, cellHeight)
  }

  private def fitSidebar(text: String): String = {
    val max = math.max(0, sidebarWidth - 2)
    if (text.length <= max) text else text.take(max)
  }

  private case class ActionButtons(
      roll: Button,
      buy: Button,
      decline: Button,
      bid: Button,
      fold: Button,
      payJail: Button,
      end: Button,
      status: Button,
      quit: Button
  )

  private case class BidInput(playerId: PlayerId, amount: Money)
}
