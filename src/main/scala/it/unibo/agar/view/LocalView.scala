package it.unibo.agar.view

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.agar.model.{World, Food, Player}
import it.unibo.agar.distributed.GameProtocol.*
import it.unibo.agar.controller.Main

import java.awt.Graphics2D
import scala.swing.*

class LocalView(userId: String, 
                gmProxy: ActorRef[GameMessage],
                width: Int,
                height: Int,
                mockPlayers: Seq[Player], 
                mockFoods: Seq[Food]
               ) extends MainFrame:

  private var world: World = World(width, height, mockPlayers, mockFoods)
  
  title = s"Agar.io - Local View - ($userId)"
  preferredSize = new Dimension(400, 400)

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      val playerOpt = world.players.find(_.id == userId)
      val (offsetX, offsetY) = playerOpt
        .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
        .getOrElse((0.0, 0.0))
      AgarViewUtils.drawWorld(g, world, offsetX, offsetY)

    reactions += { case e: event.MouseMoved =>
      val mousePos = e.point
      //val playerOpt = manager.getWorld.players.find(_.id == playerId)
      //playerOpt.foreach: player =>
      val dx = (mousePos.x - size.width / 2) * 0.01
      val dy = (mousePos.y - size.height / 2) * 0.01
      //manager.movePlayerDirection(playerId, dx, dy)
      gmProxy ! UserInputMsg(userId, dx, dy)
        
      repaint()
    }

  def updateWorldLocalView(newWorld: World): Unit =
    world = newWorld

