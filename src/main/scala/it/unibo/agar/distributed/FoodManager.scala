package it.unibo.agar.distributed

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

import it.unibo.agar.distributed.{GameManager, GameMessage}
import it.unibo.agar.model.Food

import scala.concurrent.duration.*

object FoodManager:
  private val tickFood = 1.seconds

  def apply(): Behavior[FoodMessage] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>
      /** è una buona bets practice mettere listing adapter già dopo timers? 
       * o è meglio metterlo prima?  */
      val listingAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case GameManager.GameManagerKey.Listing(listings) => WrappedListingGameManager(listings)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(GameManager.GameManagerKey, listingAdapter)
      timers.startTimerAtFixedRate(GenerateFood, tickFood)
      
      def active(gameManagers: Set[ActorRef[GameMessage]]): Behavior[FoodMessage] =
        Behaviors.receiveMessage {
          case WrappedListingGameManager(listings) =>
            active(gameManagers ++ listings)
            // Behaviors.same here wouldn't update Food

          case GenerateFood =>
            gameManagers.foreach(_ ! NewFood(Food(newFoodId)))
            Behaviors.same
        }

      active(Set.empty)
    }
  }

  private def newFoodId: String = "f" + java.util.UUID.randomUUID().toString