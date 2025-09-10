package it.unibo.agar.distributed

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView
import akka.actor.typed.ActorRef
import it.unibo.agar.distributed.GameProtocol.*

import scala.swing.Swing

object GlobalViewActor:

  def apply(globalView: GlobalView, gmProxy: ActorRef[GameMessage]): Behavior[WorldSnapshot] =
    Behaviors.setup { ctx =>
      gmProxy ! RegisterView(ctx.self)

      Behaviors.receiveMessage:
        case WorldSnapshot(world) =>
          Swing.onEDT {
            globalView.updateWordGlobalView(world)
          }
          Behaviors.same
    }

