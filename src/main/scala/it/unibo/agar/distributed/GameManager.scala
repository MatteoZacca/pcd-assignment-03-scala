package it.unibo.agar.distributed

import akka.cluster.typed.{Cluster, Leave}
import akka.actor.typed.receptionist.{ServiceKey, Receptionist}
import Receptionist.{Register, Subscribe, Listing}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}

import it.unibo.agar.model.{Direction, EatingManager, Food, Player, World}
import it.unibo.agar.distributed.GameMessage
import it.unibo.agar.distributed.StandardViewMessage
import it.unibo.agar.distributed.LocalViewMsg

import scala.collection.mutable
import scala.concurrent.duration.*
import scala.util.Random


object GameManager:

  val GameManagerKey: ServiceKey[GameMessage] = ServiceKey[GameMessage]("game-manager")

  def apply(
             width: Int,
             height: Int,
             initialPlayers: Seq[Player],
             initialFoods: Seq[Food],
             speed: Double,
             initialMass: Double
           ): Behavior[GameMessage] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timers =>
        ctx.system.receptionist ! Receptionist.Register(GameManagerKey, ctx.self)
        var world: World = World(width, height, initialPlayers, initialFoods)
        val views: mutable.Set[ActorRef[StandardViewMessage]] = mutable.Set.empty
        val endGameThreshold: Int = 10_000

        timers.startTimerAtFixedRate(Tick, 30.millis)

        Behaviors.receiveMessage {
          case RegisterView(view) =>
            views += view
            ctx.log.info(s"\n\nRegistered view: $view, total views: ${views.size}\n")
            Behaviors.same

          /* --------------------------------------------------------------------- */

          case RegisterPlayer(userId, replyTo) =>
            val player = Player(userId, Random.nextInt(width), Random.nextInt(height), initialMass)
              world = world.copy(players = world.players :+ player)
              ctx.log.info(s"Registered player ===> ${player.id}")
              // If actor A sends two messages to actor B, in order m1 then m2, 
              // then B will always process m1 before m2
              replyTo ! RegisteredPlayer(true) 
              views.foreach(_ ! WorldSnapshot(world))
            Behaviors.same

          /* --------------------------------------------------------------------- */

          case NewFood(food: Food) =>
            world = world.copy(foods = world.foods :+ food)
            views.foreach(_ ! WorldSnapshot(world))
            Behaviors.same

          /* --------------------------------------------------------------------- */

          case PlayerMove(id, (dx, dy)) =>
            directions = directions.updated(id, (dx, dy))
            world.playerById(id) match
              case Some(player) =>
                val newX = (player.x + dx * speed).max(0).min(width)
                val newY = (player.y + dy * speed).max(0).min(height)
                val moved = player.copy(x = newX, y = newY)
                world = world.updatePlayer(moved)
                /** Updating world and Snapshot */
                world = updateWorld(world)
                weHaveChampion(world, views, endGameThreshold)

              case None =>
                Behaviors.same

          /* --------------------------------------------------------------------- */

          case EatenPlayerLeft(idPlayer, nodeAddress) =>
            ctx.log.info(s"\n\n${ctx.self.path.name} received PlayerLeft msg, " +
              s"$idPlayer is preparing to leave the cluster")
            Cluster(ctx.system).manager ! Leave(nodeAddress)
            Behaviors.same

          /* --------------------------------------------------------------------- */

          case Tick =>
            world = updateWorld(world)
            weHaveChampion(world, views, endGameThreshold)

          /* --------------------------------------------------------------------- */

          case _ =>
            Behaviors.same
        }
      }
  }

  /** private variable and methods */
  private var directions: Map[String, (Double, Double)] = Map.empty

  private def updateWorld(w: World): World =
    var world = w
    world.players.foreach { player =>
      world.playerById(player.id).foreach { p =>
        val foodEatable = world.foods.filter(food => EatingManager.canEatFood(p, food))
        val playerEatFood = foodEatable.foldLeft(p)((pl, food) => pl.grow(food))
        val playersEatable = world
          .playersExcludingSelf(p)
          .filter(otherPlayer => EatingManager.canEatPlayer(playerEatFood, otherPlayer))
        val playerEatPlayers = playersEatable.foldLeft(playerEatFood)((p, other) => p.grow(other))

        //world.players.foreach(p => println(s"\n$p mass: ${p.mass} \n"))
        world = world
          .updatePlayer(playerEatPlayers)
          .removePlayers(playersEatable)
          .removeFoods(foodEatable)
      }
    }
    world

  private def weHaveChampion(world: World, views: mutable.Set[ActorRef[StandardViewMessage]], endGameThreshold: Int): Behavior[GameMessage] =
    world.players.find(_.mass > endGameThreshold) match {
      case Some(winner) =>
        views.foreach(_ ! GameOver(winner.id))
        val received: Int = 0
        gameOverStatus(world.players.size, received) // any messages still in the mailbox become dead letters
      // and the actor cannot receive new messages anymore

      case None =>
        views.foreach(_ ! WorldSnapshot(world))
        Behaviors.same
    }

  /** When GameManager find a winner, it changes behavior */
  private def gameOverStatus(expected: Int, received: Int): Behavior[GameMessage] =
    Behaviors.setup { ctx =>

      Behaviors.receiveMessage {
        case GameOverPlayerLeft(id, nodeAddress) =>
          val updated = received + 1
          ctx.log.info(s"\n\nGameManager received GameOverPlayerLeft: $updated/$expected}\n, " +
            s"$id is preparing to leave the cluster")
          Cluster(ctx.system).manager ! Leave(nodeAddress)
          if (updated == expected) {
            Behaviors.stopped
          } else {
            gameOverStatus(expected, updated)
          }

        case _ => // Ignore any other message
          Behaviors.same
      }
    }



