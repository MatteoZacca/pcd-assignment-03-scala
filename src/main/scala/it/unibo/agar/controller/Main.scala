package it.unibo.agar.controller

import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, ClusterSingleton, SingletonActor}

import it.unibo.agar.{seeds, startupWithRole}
import it.unibo.agar.distributed.players.*
import it.unibo.agar.distributed.{FoodManager, GameManager, GlobalViewActor}
import it.unibo.agar.model.{AIMovement, GameInitializer, Player}
import it.unibo.agar.view.GlobalView

import scala.swing.Swing
import scala.concurrent.duration.*
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
        val supervisedFoodManager = Behaviors
          .supervise(fm)
          .onFailure[Exception](SupervisorStrategy.restart)
        ClusterSingleton(ctx.system).init(SingletonActor(supervisedFoodManager, "FoodManager"))

        val gm = GameManager(width, height, initialPlayers, initialFoods, speed, initialMass)
        val supervisedGameManager = Behaviors
          .supervise(gm)
          .onFailure[Exception](
            SupervisorStrategy.restartWithBackoff(
              minBackoff = 1.seconds,
              maxBackoff = 30.seconds, 
              randomFactor = 0.2
            )
          )
        val gmRef = ClusterSingleton(ctx.system).init(SingletonActor(gm, "GameManager"))

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
      SingletonActor(Behaviors.empty, "GameManager")
    ) /* Akka riconosce che nel cluster c'è già un singleton registrato con il nome 
    GameManager, quindi otteniamo un ClusterSingletonProxy */
    system.systemActorOf(UserActor(userId, gmProxy), "actor-" + userId)

  /** runMain it.unibo.agar.controller.mainAIPlayer */
  @main def mainAIPlayer(): Unit =
    val ports = (25252, 25253)
    (1 to AIPlayers).map( n =>
      val port = if (n == 1) ports._1 else ports._2
      val system = startupWithRole("aiplayer", port)(Behaviors.empty)
      system.systemActorOf(AIPlayerActor(s"ai-$n"), s"ai-player-$n")
    )





