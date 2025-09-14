package it.unibo.agar.distributed

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView
import akka.actor.typed.ActorRef
import it.unibo.agar.distributed.*

import scala.swing.Swing.*

object GlobalViewActor:

  def apply(globalView: GlobalView, gmProxy: ActorRef[GameMessage]): Behavior[ViewMessage] =
    Behaviors.setup { ctx =>
      gmProxy ! RegisterView(ctx.self)

      Behaviors.receiveMessage:
        case WorldSnapshot(world) => 
          globalView.updateWordGlobalView(world)
          Behaviors.same

        case GameOver(winner) =>
          globalView.endGame(winner)
          Behaviors.stopped

    }

