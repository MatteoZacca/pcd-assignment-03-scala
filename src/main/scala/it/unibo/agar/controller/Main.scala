package it.unibo.agar.controller

import akka.cluster.typed.{ClusterSingleton, SingletonActor, Cluster}
import akka.cluster.singleton.ClusterSingletonProxy
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.distributed.GameManager
import it.unibo.agar.model.{AIMovement, GameInitializer, MockGameStateManager, Player, World}
import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView
import it.unibo.agar.*
import it.unibo.agar.distributed.players.*
import it.unibo.agar.distributed.GlobalViewActor
import it.unibo.agar.distributed.FoodManager

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT

object Main: //extends SimpleSwingApplication:

  val width = 1000
  val height = 1000

  private val numPlayers = 4
  private val initialplayers = Seq.empty[Player]
  //private val players = GameInitializer.initialPlayers(numPlayers, width, height)

  private val numFoods = 100
  private val initialfoods = GameInitializer.initialFoods(numFoods, width, height)
  
  private val rand = scala.util.Random
  def randomX: Double = rand.nextDouble() * width
  def randomY: Double = rand.nextDouble() * height

  //private val manager = new MockGameStateManager(World(width, height, players, foods))

  /*
  private val timer = new Timer()
  private val task: TimerTask = new TimerTask:
    override def run(): Unit =
      AIMovement.moveAI("p1", manager)
      manager.tick()
      onEDT(Window.getWindows.foreach(_.repaint()))
  timer.scheduleAtFixedRate(task, 0, 30) // every 30ms
  */

  /*
  override def top: Frame =
    // Open both views at startup
    new GlobalView(manager).open()
    new LocalView(manager, "p1").open()
    new LocalView(manager, "p2").open()
    // No launcher window, just return an empty frame (or null if allowed)
    new Frame { visible = false }
  */


  @main def mainManager(): Unit =
    // seeds.head() must return port 25251
    val system = startupWithRole("manager", 25251)(
      Behaviors.setup { ctx =>
        val globalView = new GlobalView(width, height, initialplayers, initialfoods)

        val gm = GameManager(width, height, initialplayers, initialfoods)
        val gmRef = ClusterSingleton(ctx.system).init(SingletonActor(gm, "game-manager-actor-singleton"))
        /** Pensa a una soluzione per la creazione del ClusterSingletonProxy */
        val gvActorRef = ctx.spawn(GlobalViewActor(globalView, gmRef), "global-view-actor")
        println("\n\ngvActorRef: " + gvActorRef + "\n\n")
        
        val fm = FoodManager()
        val fmRef = ClusterSingleton(ctx.system).init(SingletonActor(fm, "food-manager-actor-singleton"))
        
        onEDT:
          globalView.open()

        Behaviors.empty
      }
    )

  // userId examples: user-1, user-2,...
  @main def mainUser(userId: String): Unit =
    val system = startupWithRole("user", 0)(Behaviors.empty)
    val gmProxy = ClusterSingleton(system).init(
      SingletonActor(Behaviors.empty, "game-manager-actor-singleton")
    ) /* Akka riconosce che nel cluster c'è già un singleton registrato con il nome 
    GameManager, quindi otteniamo un ClusterSingletonProxy */
    val userActorRef = system.systemActorOf(UserActor(userId, gmProxy), "actor-" + userId)
    println("\n\nuserActorRef: " + userActorRef + "\n\n")

  // AIPlayerId examples: aiplayer-1, aiplayer-2
  // port: 0
  /*
  @main def mainAIPlayer(aiId: String, port: Int): Unit =
    val system = startupWithRole("aiplayer", 0)(Behaviors.empty)
    val gmProxy = ClusterSingleton(system).init(
      SingletonActor(Behaviors.empty, "GameManager")
    )
    system.systemActorOf(AIPlayer(aiId, gmProxy), s"$aiId")

   */




