package it.unibo.agar.view

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import it.unibo.agar.model.{Food, Player, World}
import it.unibo.agar.distributed.*

import java.awt.Graphics2D
import scala.swing.*
import scala.swing.Dialog.Options

class LocalView(userId: String, 
                gmProxy: ActorRef[GameMessage],
                width: Int,
                height: Int,
                dummyPlayers: Seq[Player],
                dummyFoods: Seq[Food]
               ) extends MainFrame:

  private var worldOpt: Option[World] = None
  
  title = s"Agar.io - Local View - ($userId)"
  preferredSize = new Dimension(400, 400)

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit = worldOpt match 
      case Some(world) =>
        val (offsetX, offsetY) = world.players.find(_.id == userId) match
          case Some(p) => (p.x - size.width / 2.0, p.y - size.height / 2.0)
          case None    => (0.0, 0.0)
        
        AgarViewUtils.drawWorld(g, world, offsetX, offsetY)

      case None => 
        g.drawString("Loading World...", size.width / 2, size.height / 2)

    reactions += { case e: event.MouseMoved =>
      val mousePos = e.point
      //val playerOpt = manager.getWorld.players.find(_.id == playerId)
      //playerOpt.foreach: player =>
      val dx = (mousePos.x - size.width / 2) * 0.01
      val dy = (mousePos.y - size.height / 2) * 0.01
      //manager.movePlayerDirection(playerId, dx, dy)
      gmProxy ! UserMove(userId, dx, dy)
      /** devo dire allo user actor che lo user ha inserito gli input */
    }

  def updateWorldLocalView(newWorld: Option[World]): Unit =
    Swing.onEDT:
      worldOpt = newWorld
      repaint()

  def showPlayerEaten(): Unit =
    Swing.onEDT:
      Dialog.showMessage(
      contents.head,
      message = "You have been eaten !",
      title = "Eaten",
      Dialog.Message.Info
      )

  def showGameOver(winner: String): Unit =
    Swing.onEDT:
      Dialog.showMessage(
      contents.head,
      message = s"Game Over ! Winner: $winner",
      title = "Game Over",
      Dialog.Message.Info
      )

