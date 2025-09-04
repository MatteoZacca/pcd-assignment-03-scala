package it.unibo.agar.controller

import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.Cluster

import it.unibo.agar.distributed.GameManager
import it.unibo.agar.model.AIMovement
import it.unibo.agar.model.GameInitializer
import it.unibo.agar.model.MockGameStateManager
import it.unibo.agar.model.World
import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT

object Main extends SimpleSwingApplication:

  private val width = 1000
  private val height = 1000

  private val numPlayers = 4
  private val players = GameInitializer.initialPlayers(numPlayers, width, height)

  private val numFoods = 100
  private val foods = GameInitializer.initialFoods(numFoods, width, height)

  private val manager = new MockGameStateManager(World(width, height, players, foods))

  private val timer = new Timer()
  private val task: TimerTask = new TimerTask:
    override def run(): Unit =
      AIMovement.moveAI("p1", manager)
      manager.tick()
      onEDT(Window.getWindows.foreach(_.repaint()))
  timer.scheduleAtFixedRate(task, 0, 30) // every 30ms

  override def top: Frame =
    // Open both views at startup
    new GlobalView(manager).open()
    new LocalView(manager, "p1").open()
    new LocalView(manager, "p2").open()
    // No launcher window, just return an empty frame (or null if allowed)
    new Frame { visible = false }

  // mainManager port = 25251
  @main def mainManager(port: String): Unit =
    val system = startup("manager", port)(
      Behaviors.setup { ctx =>
        val gm = GameManager(width, height, players, foods)
        ClusterSingleton(ctx.system).init(SingletonActor(gm, "GameManager"))
        
        new GlobalView(gm).open
        
        Behaviors.empty
      }
    )
    
  /** Idea:   nel momento in cui creo il manager, creo anche la global view, quindi compare
   * schermata del mondo, ma senza giocatori */

  // userId examples: user-1, user-2,...
  @main def mainUser(userId: String, port: Int): Unit =
    val system = startup(userId, port)(Behaviors.empty)
    val gmProxy = ClusterSingleton(system).init(
      SingletonActor(Behaviors.empty)
    )
    new LocalView().open()

  @main def mainAIPlayer(): Unit =
    startup("aiplayer-1", 0)(Behaviors.empty)
    startup("aiplayer-2", 0)(Behaviors.empty)


