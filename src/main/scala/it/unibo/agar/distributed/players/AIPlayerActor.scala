package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import it.unibo.agar.distributed.*
import it.unibo.agar.model.{AIMovement, World}

import scala.concurrent.duration.*

object AIPlayerActor:
  private val tickAI = 60.millis

  def apply(aiId: String, gmProxy: ActorRef[GameMessage]): Behavior[AIPlayerMsg] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>

      gmProxy ! RegisterView(ctx.self)
      gmProxy ! RegisterPlayer(aiId, ctx.self)

      timers.startTimerAtFixedRate(Tick, tickAI)

      def active(playing: Boolean, world: Option[World]): Behavior[AIPlayerMsg] =
        Behaviors.receiveMessage {

          case WorldSnapshot(newWorld) =>
            if (playing && !newWorld.players.exists(_.id == aiId)) {
              ctx.log.info(s"\n\n[${ctx.self.path.name}] log: $aiId has been eaten\n")
              Behaviors.stopped
            } else {
              active(playing, Some(newWorld))
            }

          case RegisteredPlayer(playingFlag) =>
            active(playingFlag, world)

          case GameOver(winner) =>
            ctx.log.info(s"\n\n[${ctx.self.path}] received GameOver msg, Winner: $winner\n\n")
            ctx.system.terminate()
            Behaviors.stopped

          case Tick =>
            for {
              w <- world
              direction <- AIMovement.getAIMove(aiId, w)
            } do {
              gmProxy ! PlayerMove(aiId, direction)
            }
            Behaviors.same
        }

      active(playing = false, world = None)
    }
  }