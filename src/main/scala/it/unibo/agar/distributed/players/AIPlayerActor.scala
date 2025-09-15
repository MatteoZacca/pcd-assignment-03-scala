package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

import it.unibo.agar.controller.Main
import it.unibo.agar.distributed.*
import it.unibo.agar.model.{AIMovement, World}


import scala.concurrent.duration.*

object AIPlayerActor:
  private val tickAi = 50.millis

  def apply(aiId: String): Behavior[Any] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>
      var playing:Boolean = false
      var world: Option[World] = None

      val listingAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case GameManager.GameManagerKey.Listing(listings) => WrappedListingGameManager(listings)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(GameManager.GameManagerKey, listingAdapter)
      timers.startTimerAtFixedRate(Tick, tickAi)

      def active(gameManagers: Set[ActorRef[GameMessage]]): Behavior[Any] =
        Behaviors.receiveMessage {
          case WrappedListingGameManager(listings) =>
            val newManagers = listings -- gameManagers
            newManagers.foreach { l =>
              l ! RegisterView(ctx.self)
              l ! RegisterPlayer(aiId, ctx.self)
            }
            active(gameManagers ++ newManagers)

          case RegisteredPlayer(playingFlag) =>
            playing = playingFlag
            Behaviors.same
            
          case WorldSnapshot(newWorld) =>
            if (playing && newWorld.players.exists(_.id == aiId)) {
              Behaviors.stopped
            }
            world = Some(newWorld)
            Behaviors.same
            
          case Tick =>
            gameManagers.flatMap { gm =>
              world.flatMap { w =>
                AIMovement.getAIMove(aiId, w).map { direction =>
                  gm ! AIPlayerMove(aiId, direction)
                }
              }
            }
            Behaviors.same
            
        }

      active(Set.empty)
    }
  }

