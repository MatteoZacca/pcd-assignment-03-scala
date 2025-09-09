package it.unibo.agar.view

import it.unibo.agar.model.{MockGameStateManager, Player, World, Food}
import it.unibo.agar.distributed.GameManager

import java.awt.Color
import java.awt.Graphics2D
import scala.swing.*

class GlobalView(width: Int, height: Int, initialplayers: Seq[Player], initialfoods: Seq[Food]) extends MainFrame:

  private var world: World = World(width, height, initialplayers, initialfoods)
  
  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      AgarViewUtils.drawWorld(g, world)

  /*
  private val timer = new javax.swing.Timer(30, _ => repaint())
  timer.start()
   */
  
  /** GlobalViewActor talks with GlobalView through updateGlobalWord */
  def updateWordGlobalView(updatedWorld: World): Unit =
    world = updatedWorld