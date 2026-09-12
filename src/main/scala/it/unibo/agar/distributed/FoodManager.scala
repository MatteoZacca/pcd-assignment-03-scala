package it.unibo.agar.distributed

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.model.Food
import scala.concurrent.duration.*

object FoodManager:
  private val tickFood = 1.seconds

  def apply(gmProxy: ActorRef[GameMessage]): Behavior[FoodMessage] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>
      timers.startTimerAtFixedRate(GenerateFood, tickFood)

      def active(): Behavior[FoodMessage] =
        Behaviors.receiveMessage {
          case GenerateFood =>
            gmProxy ! NewFood(Food(newFoodId))
            Behaviors.same
        }

      active()
    }
  }

  private def newFoodId: String = "f" + java.util.UUID.randomUUID().toString