package it.unibo.agar.distributed

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import it.unibo.agar.view.GlobalView
import it.unibo.agar.distributed.StandardViewMessage

object GlobalViewActor:

  def apply(globalView: GlobalView, gmProxy: ActorRef[GameMessage]): Behavior[GlobalViewMsg] =
    Behaviors.setup { ctx =>
      gmProxy ! RegisterView(ctx.self)

      Behaviors.receiveMessage:
        case WorldSnapshot(world) => 
          globalView.updateWordGlobalView(world)
          Behaviors.same

        case GameOver(winner) =>
          ctx.log.info(s"\n\n[${ctx.self.path}] received GameOver msg, Winner: $winner\n\n")
          globalView.endGame(winner)
          Behaviors.stopped

    }

