package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.javadsl.Behaviors
import it.unibo.agar.distributed.GameManager.{WorldSnapshot, RegisterView, RegisterPlayer}
import it.unibo.agar.distributed.GameManager.Command
import it.unibo.agar.view.LocalView

import scala.swing.Swing.*

object UserActor:
  
  def apply(gmProxy: ActorRef[Command], userId: String): Behavior[Any] =
    Behaviors.setup { ctx =>
      val view = new LocalView(userId, Some(gmProxy)) /** perchè some? */
      onEDT(view.open())

      val adapter = ctx.spawn(
        Behaviors.receiveMessage {
          case WorldSnapshot(world) =>
            onEDT {
              view.snapUpdateWorld(world)
            }
            Behaviors.same
        },
        s"view-adapter-${userId}"
      )
      
      gmProxy ! RegisterView(adapter)
      gmProxy ! RegisterPlayer(userId, Some(adapter))
      
      Behaviors.empty
    }
