package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

import it.unibo.agar.controller.Main
import it.unibo.agar.distributed.*
import it.unibo.agar.distributed.UserMsg
import it.unibo.agar.distributed.StandardViewMessage
import it.unibo.agar.distributed.LocalViewMsg
import it.unibo.agar.view.LocalView

import scala.swing.Swing.*

object UserActor:

  def apply(userId: String, gmProxy: ActorRef[GameMessage]): Behavior[UserMsg] =
    Behaviors.setup { ctx =>
      var playing: Boolean = false
      val localView = new LocalView(userId, gmProxy, Main.width, Main.height, Seq.empty, Seq.empty)
      onEDT:
        localView.open()

      gmProxy ! RegisterView(ctx.self)
      gmProxy ! RegisterPlayer(userId, ctx.self)

      Behaviors.receiveMessage {
        case WorldSnapshot(world) =>
          if (playing && !world.players.exists(_.id == userId)) {
            localView.showPlayerEaten()
            ctx.log.info(s"\n\n [${ctx.self.path.name}] log: $userId has been eaten \n\n")
            playing = false
            gmProxy ! EatenPlayerLeft(userId, Cluster(ctx.system).selfMember.address)
            Behaviors.stopped
          } else {
            localView.updateWorldLocalView(Some(world))
            Behaviors.same
          }

        /* --------------------------------------------------------------------- */

        case RegisteredPlayer(playFlag) =>
          playing = playFlag
          Behaviors.same

        /* --------------------------------------------------------------------- */

        case GameOver(winner) =>
          ctx.log.info(s"\n\n ${ctx.self.path} received GameOver msg, Winner: $winner\n\n")
          localView.showGameOver(winner)
          gmProxy ! GameOverPlayerLeft(userId, Cluster(ctx.system).selfMember.address)
          Behaviors.stopped
      }
    }
