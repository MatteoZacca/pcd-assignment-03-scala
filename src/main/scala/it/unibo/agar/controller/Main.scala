package it.unibo.agar.controller

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{ClusterSingleton, SingletonActor, Cluster}

import it.unibo.agar.{seeds, startupWithRole}
import it.unibo.agar.distributed.players.*
import it.unibo.agar.distributed.{GameManager, GlobalViewActor, FoodManager}
import it.unibo.agar.model.{AIMovement, GameInitializer, Player}
import it.unibo.agar.view.GlobalView

import scala.swing.Swing

import java.awt.Window
import java.util.Timer
import java.util.TimerTask


object Main:

  val width = 800
  val height = 800

  private val initialPlayers = Seq.empty[Player]
  private val AIPlayers = 2
  private val initialMass = 120.0
  private val speed = 1.0

  private val numFoods = 100
  private val initialFoods = GameInitializer.initialFoods(numFoods, width, height)

  private val rand = scala.util.Random
  def randomX: Double = rand.nextDouble() * width
  def randomY: Double = rand.nextDouble() * height

  /** runMain it.unibo.agar.controller.mainManager */
  @main def mainManager(): Unit =
    // seeds.head() returns port 25251
    val system = startupWithRole("manager", seeds.head)(
      Behaviors.setup { ctx =>
        val fm = FoodManager()
        val fmRef = ClusterSingleton(ctx.system).init(SingletonActor(fm, "food-manager-actor-singleton"))

        val gm = GameManager(width, height, initialPlayers, initialFoods, speed, initialMass)
        val gmRef = ClusterSingleton(ctx.system).init(SingletonActor(gm, "game-manager-actor-singleton"))

        val globalView = new GlobalView(width, height, initialPlayers, initialFoods)
        ctx.spawn(GlobalViewActor(globalView, gmRef), "global-view-actor")

        Swing.onEDT:
          globalView.open()

        Behaviors.empty
      }
    )

  /** runMain it.unibo.agar.controller.mainUser user-n */
  // userId examples: user-1, user-2,...
  @main def mainUser(userId: String): Unit =
    val system = startupWithRole("user", 0)(Behaviors.empty)
    val gmProxy = ClusterSingleton(system).init(
      SingletonActor(Behaviors.empty, "game-manager-actor-singleton")
    ) /* Akka riconosce che nel cluster c'è già un singleton registrato con il nome 
    GameManager, quindi otteniamo un ClusterSingletonProxy */
    system.systemActorOf(UserActor(userId, gmProxy), "actor-" + userId)

  /** runMain it.unibo.agar.controller.mainAIPlayer */
  @main def mainAIPlayer(): Unit =
    (1 to AIPlayers).foreach(n =>
      val system = startupWithRole("aiplayer", 0)(Behaviors.empty)
      system.systemActorOf(AIPlayerActor(s"ai-$n"), s"ai-player-$n")
    )





