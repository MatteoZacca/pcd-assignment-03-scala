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

        val initialWorld: World = World(width, height, initialPlayers, initialFoods)
        val endGameThreshold: Int = 10_000

        timers.startTimerAtFixedRate(Tick, 30.millis)

        // Inner helper to check for a winner and transition state
        def checkChampionAndNextState(
                                       w: World,
                                       v: Set[ActorRef[StandardViewMessage]],
                                       d: Map[String, Direction]
                                     ): Behavior[GameMessage] = {
          w.players.find(_.mass > endGameThreshold) match
            case Some(winner) =>
              v.foreach(_ ! GameOver(winner.id))
              gameOverStatus(w.players.size, 0) // any messages still in the mailbox become dead letters
            // and the actor cannot receive new messages anymore
            case None =>
              v.foreach(_ ! WorldSnapshot(w))
              active(w, v, d)
        }

        def active(
            world: World,
            views: Set[ActorRef[StandardViewMessage]],
            directions: Map[String, Direction]
                  ): Behavior[GameMessage] = Behaviors.receiveMessage {
          
          case RegisterView(view) =>
            val newViews = views + view
            ctx.log.info(s"\n\nRegistered view: $view, total views: ${newViews.size}\n")
            active(world, newViews, directions)

          case RegisterPlayer(userId, replyTo) =>
            val player = Player(userId, Random.nextInt(width), Random.nextInt(height), initialMass)
            val newWorld = world.copy(players = world.players :+ player)
            ctx.log.info(s"Registered player ===> ${player.id}")
            replyTo ! RegisteredPlayer(true)
            views.foreach(_ ! WorldSnapshot(newWorld))
            active(newWorld, views, directions)
            
          case NewFood(food: Food) =>
            val newWorld = world.copy(foods = world.foods :+ food)
            views.foreach(_ ! WorldSnapshot(newWorld))
            active(newWorld, views, directions)

          case PlayerMove(id, (dx, dy)) =>
            val newDirections = directions.updated(id, (dx, dy))
            val newWorld = world.playerById(id) match
              case Some(player) =>
                val newX = (player.x + dx * speed).max(0).min(width)
                val newY = (player.y + dy * speed).max(0).min(height)
                val moved = player.copy(x = newX, y = newY)
                val updatedWorld = world.updatePlayer(moved)
                updateWorldCollisions(updatedWorld)
              case None => world

            checkChampionAndNextState(newWorld, views, newDirections)

          case EatenPlayerLeft(idPlayer, nodeAddress) =>
            ctx.log.info(s"\n\n${ctx.self.path.name} received PlayerLeft msg, " +
              s"$idPlayer is preparing to leave the cluster")
            Cluster(ctx.system).manager ! Leave(nodeAddress)
            active(world, views, directions)

          case Tick =>
            val newWorld = updateWorldCollisions(world)
            checkChampionAndNextState(newWorld, views, directions)

          case _ =>
            Behaviors.same
        }

        // Start the actor with the initial empty state
        active(initialWorld, Set.empty, Map.empty)
      }
    }

  private def updateWorldCollisions(w: World): World =
    w.players.foldLeft(w) { (currentWorld, player) =>
      currentWorld.playerById(player.id) match
        case Some(p) =>
          val foodEatable = currentWorld.foods.filter(food => EatingManager.canEatFood(p, food))
          val playerEatFood = foodEatable.foldLeft(p)((pl, food) => pl.grow(food))

          val playersEatable = currentWorld
            .playersExcludingSelf(p)
            .filter(otherPlayer => EatingManager.canEatPlayer(playerEatFood, otherPlayer))
          val playerEatPlayers = playersEatable.foldLeft(playerEatFood)((pl, other) => pl.grow(other))

          currentWorld
            .updatePlayer(playerEatPlayers)
            .removePlayers(playersEatable)
            .removeFoods(foodEatable)
        case None => currentWorld
    }

  /** When GameManager find a winner, it changes behavior */
  private def gameOverStatus(expected: Int, received: Int): Behavior[GameMessage] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case GameOverPlayerLeft(id, nodeAddress) =>
          val updated = received + 1
          ctx.log.info(s"\n\nGameManager received GameOverPlayerLeft: $updated/$expected, $id is preparing to leave the cluster")
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



