package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.distributed.*
import it.unibo.agar.distributed.GameManager
import it.unibo.agar.controller.Main
import it.unibo.agar.view.LocalView

import scala.swing.Swing.*

object UserActor:

  def apply(userId: String, gmProxy: ActorRef[GameMessage]): Behavior[ViewMessage] =
    Behaviors.setup { ctx =>
      val localView = new LocalView(userId, gmProxy, Main.width, Main.height, Seq.empty, Seq.empty)
      onEDT:
        localView.open()

      gmProxy ! RegisterView(ctx.self)
      gmProxy ! RegisterPlayer(userId, ctx.self)

      Behaviors.receiveMessage {
        case WorldSnapshot(world) =>
          onEDT {
            localView.updateWorldLocalView(Some(world))
          }
          Behaviors.same

        case GameOver(winner) =>
          onEDT {
            localView.endGame(winner)
          }
          Behaviors.stopped
      }
    }
