package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

import it.unibo.agar.controller.Main
import it.unibo.agar.distributed.*
import it.unibo.agar.distributed.StandardViewMessage
import it.unibo.agar.distributed.LocalViewMsg
import it.unibo.agar.model.{AIMovement, World}


import scala.concurrent.duration.*

object AIPlayerActor:
  private val tickAI = 60.millis

  def apply(aiId: String): Behavior[AIPlayerMsg] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>
      var playing: Boolean = false
      var world: Option[World] = None

      val listingAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case GameManager.GameManagerKey.Listing(listings) => WrappedListingGameManager(listings)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(GameManager.GameManagerKey, listingAdapter)
      timers.startTimerAtFixedRate(Tick, tickAI)

      def active(gameManagers: Set[ActorRef[GameMessage]]): Behavior[AIPlayerMsg] =
        Behaviors.receiveMessage {
          case WrappedListingGameManager(listings) =>
            val newManagers = listings -- gameManagers
            newManagers.foreach { l =>
              l ! RegisterView(ctx.self)
              l ! RegisterPlayer(aiId, ctx.self)
            }
            active(gameManagers ++ newManagers)

          /* --------------------------------------------------------------------- */

          case WorldSnapshot(newWorld) =>
            if (playing && !newWorld.players.exists(_.id == aiId)) {
              ctx.log.info(s"\n\n[${ctx.self.path.name}] log: $aiId has been eaten \n\n")
              playing = false
              gameManagers.foreach(_ ! PlayerLeft(aiId, Cluster(ctx.system).selfMember.address))
              Behaviors.stopped
            } else {
              world = Some(newWorld)
              Behaviors.same
            }
            
          /* --------------------------------------------------------------------- */  
            
          case RegisteredPlayer(playingFlag) =>
            playing = playingFlag
            Behaviors.same

          /* --------------------------------------------------------------------- */

          case GameOver(winner) =>
            ctx.log.info(s"\n\n[${ctx.self.path}] received GameOver msg, Winner: $winner\n\n")
            gameManagers.foreach(_ ! PlayerLeft(aiId, Cluster(ctx.system).selfMember.address))
            Behaviors.stopped
          
          /* --------------------------------------------------------------------- */
            
          case Tick =>
            for {
              gm <- gameManagers
              w  <- world
              direction <- AIMovement.getAIMove(aiId, w)
            } do {
              gm ! PlayerMove(aiId, direction)
            }
            Behaviors.same
        }

      active(Set.empty)
    }
  }

