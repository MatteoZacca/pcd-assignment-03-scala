package it.unibo.agar.distributed

import akka.actor.typed.receptionist.{ServiceKey, Receptionist}
import Receptionist.{Register, Subscribe, Listing}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}

import it.unibo.agar.model.{EatingManager, Food, Player, World}
import it.unibo.agar.distributed.GameMessage

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
        val views: mutable.Set[ActorRef[ViewMessage]] = mutable.Set.empty

        timers.startTimerAtFixedRate(Tick, 30.millis)
        ctx.log.info(s"GameManager started")

        Behaviors.receiveMessage {
          case RegisterView(view) =>
            views += view
            ctx.log.info(s"Registered view, total views: ${views.size}")
            Behaviors.same

          case RegisterPlayer(userId, replyTo) =>
            val player = Player(userId, Random.nextInt(width), Random.nextInt(height), initialMass)
              world = world.copy(players = world.players :+ player)
              ctx.log.info(s"Registered player ===> ${player.id}")
              // If actor A sends two messages to actor B, in order m1 then m2, 
              // then B will always process m1 before m2
              replyTo ! RegisteredPlayer(true) 
              views.foreach(_ ! WorldSnapshot(world)) /** è giusto mandarlo solo allo userActor appena 
           registrato e non anche alle altre views? */
            Behaviors.same

          case UserInputMsg(pid, dx, dy) =>
            directions = directions.updated(pid, (dx, dy))
            world.playerById(pid) match
              case Some(player) =>
                val newX = (player.x + dx * speed).max(0).min(width)
                val newY = (player.y + dy * speed).max(0).min(height)
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
            world.players.find(_.mass > 10000) match {
              case Some(winner) =>
                views.foreach(_ ! GameOver(winner.id))
                Behaviors.stopped // any messages still in the mailbox become dead letters
                // and the actor cannot receive new messages anymore

              case None =>
                views.foreach(_ ! WorldSnapshot(world))
                Behaviors.same
            }
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

        world.players.foreach(p => println(s"\n\n $p mass: ${p.mass} \n\n"))

        world = world
          .updatePlayer(playerEatPlayers)
          .removePlayers(playersEatable)
          .removeFoods(foodEatable)
      }
    }
    world


