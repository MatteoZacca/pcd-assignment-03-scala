package it.unibo.agar.view

import it.unibo.agar.distributed.GameManager
import it.unibo.agar.model.{Food, Player, World}

import java.awt.Color
import java.awt.Graphics2D

import scala.swing.*

class GlobalView(width: Int, height: Int, initialPlayers: Seq[Player], initialFood: Seq[Food]) extends MainFrame:

  private var world: World = World(width, height, initialPlayers, initialFood)
  
  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      AgarViewUtils.drawWorld(g, world)
  
  /** GlobalViewActor talks with GlobalView through updateGlobalWord */
  def updateWordGlobalView(newWorld: World): Unit =
    /** this is the same as calling SwingUtilities.invokeLater */
    Swing.onEDT: 
      world = newWorld
      repaint()

  def endGame(winner: String): Unit =
    Swing.onEDT:
      Dialog.showMessage(
        contents.head,
        message = s"Game Over ! Winner: $winner",
        title = "Game Over",
        Dialog.Message.Info
      )
      
