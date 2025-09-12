package it.unibo.agar.distributed

import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.actor.typed.receptionist.{ServiceKey, Receptionist}
import Receptionist.{Register, Subscribe, Listing}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.cluster.Cluster
import akka.util.Timeout

import it.unibo.agar.model.{EatingManager, Food, Player, World}
import it.unibo.agar.model.*
import it.unibo.agar.distributed.GameMessage

import scala.concurrent.duration.*
import scala.collection.mutable
import scala.util.Random


object GameManager:

  val GameManagerKey = ServiceKey[GameMessage]("game-manager")
  
  def apply(
             width: Int,
             height: Int,
             initialPlayers: Seq[Player],
             initialFoods: Seq[Food]
           ): Behavior[GameMessage] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timers =>
        ctx.system.receptionist ! Receptionist.Register(GameManagerKey, ctx.self)
        var world: World = World(width, height, initialPlayers, initialFoods)
        val views: mutable.Set[ActorRef[WorldSnapshot]] = mutable.Set.empty

        timers.startTimerAtFixedRate(Tick, 30.millis)
        ctx.log.info(s"GameManager started")

        Behaviors.receiveMessage {
          case RegisterView(view) =>
            views += view
            view ! WorldSnapshot(world)
            ctx.log.info(s"Registered view, total views: ${views.size}")
            Behaviors.same

          case RegisterPlayer(userId, replyTo) =>
            val player = Player(userId, Random.nextInt(width), Random.nextInt(height), 120.0)
              world = world.copy(players = world.players :+ player)
              ctx.log.info(s"Registered player ===> $player")
              replyTo ! WorldSnapshot(world)
            Behaviors.same

            
          case UserInputMsg(pid, dx, dy) =>
            val speed = 1.0 /** dovrebbe essere dichiarata a livello più globale */
            directions = directions.updated(pid, (dx, dy))
            world.playerById(pid) match
              case Some(player) =>
                val newX = (player.x + dx * speed).max(0).min(world.width)
                val newY = (player.y + dy * speed).max(0).min(world.height)
                val moved = player.copy(x = newX, y = newY)
                world = world.updatePlayer(moved)

              case None => 

            Behaviors.same

          case NewFood(food: Food) =>
            world = world.copy(foods = world.foods :+ food)
            views.foreach(_ ! WorldSnapshot(world))
            Behaviors.same
            

          case Tick =>
            world = updateWorld(world)
            views.foreach(_ ! WorldSnapshot(world))
            /** Idea: inviare mesasggio di Game ended */
            /**
            world.players.find(_.mass >= 1000.0).foreach( winner =>
            ctx.log.info(s"Game ended: ${winner.id}")
            ) 
            */
            Behaviors.same
        }
      }
  }

  /** private variable and methods */
  private var directions: Map[String, (Double, Double)] = Map.empty

  private def updateWorld(w: World): World =
    var world = w
    val orderedPlayers = world.players.sortBy(_.id)
    orderedPlayers.foreach { player =>
      world.playerById(player.id).foreach { p =>
        val foodEaten = world.foods.filter(food => EatingManager.canEatFood(p, food))
        val playerEatsFood = foodEaten.foldLeft(player)((pl, food) => pl.grow(food))
        val playersEaten = world
          .playersExcludingSelf(player)
          .filter(otherPlayer => EatingManager.canEatPlayer(playerEatsFood, otherPlayer))
        val playerEatsPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))
        world = world
          .updatePlayer(playerEatsPlayers)
          .removePlayers(playersEaten)
          .removeFoods(foodEaten)
      }
    }
    world


