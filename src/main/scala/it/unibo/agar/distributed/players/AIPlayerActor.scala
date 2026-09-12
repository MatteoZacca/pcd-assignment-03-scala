package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

import it.unibo.agar.distributed.*
import it.unibo.agar.model.{AIMovement, World}

import scala.concurrent.duration.*

object AIPlayerActor:
  private val tickAI = 60.millis

  def apply(aiId: String, gmProxy: ActorRef[GameMessage]): Behavior[AIPlayerMsg] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>

      // 1. Immediately register using the proxy (no Receptionist needed)
      gmProxy ! RegisterView(ctx.self)
      gmProxy ! RegisterPlayer(aiId, ctx.self)

      timers.startTimerAtFixedRate(Tick, tickAI)

      // 2. Purely functional state loop (no 'var' used)
      def active(playing: Boolean, world: Option[World]): Behavior[AIPlayerMsg] =
        Behaviors.receiveMessage {

          case WorldSnapshot(newWorld) =>
            if (playing && !newWorld.players.exists(_.id == aiId)) {
              ctx.log.info(s"\n\n[${ctx.self.path.name}] log: $aiId has been eaten\n")
              gmProxy ! EatenPlayerLeft(aiId, Cluster(ctx.system).selfMember.address)
              Behaviors.stopped
            } else {
              active(playing, Some(newWorld))
            }

          case RegisteredPlayer(playingFlag) =>
            active(playingFlag, world)

          case GameOver(winner) =>
            ctx.log.info(s"\n\n[${ctx.self.path}] received GameOver msg, Winner: $winner\n\n")
            gmProxy ! GameOverPlayerLeft(aiId, Cluster(ctx.system).selfMember.address)
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