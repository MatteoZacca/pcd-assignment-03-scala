package it.unibo.agar.distributed

import akka.actor.typed.ActorRef

import it.unibo.agar.Message
import it.unibo.agar.model.{Direction, Food, World}
  
sealed trait GameMessage extends Message
case class RegisterView(view: ActorRef[ViewMessage]) extends GameMessage
case class RegisterPlayer(userId: String, replyTo: ActorRef[ViewMessage]) extends GameMessage
case class UserInputMsg(playerId: String, dx: Double, dy: Double) extends GameMessage
case class NewFood(food: Food) extends GameMessage
case object Tick extends GameMessage
case class AIPlayerMove(aiId: String, direction: Direction)extends GameMessage

sealed trait FoodMessage extends Message
case class WrappedListingGameManager(refs: Set[ActorRef[GameMessage]]) extends FoodMessage
case object GenerateFood extends FoodMessage

sealed trait ViewMessage extends Message
case class WorldSnapshot(world: World)extends ViewMessage
case class GameOver(winner: String) extends ViewMessage
case class RegisteredPlayer(playing: Boolean) extends ViewMessage

/*
/** GlobalViewActor and UserActor messages */
sealed trait ViewMessage extends Message
trait GlobalViewActorMsg extends ViewMessage
trait UserActorMsg extends ViewMessage

enum StandardViewMessage extends GlobalViewActorMsg, UserActorMsg:
  case WorldSnapshot(world: World)
  case GameOver(winner: String)

enum LocalViewActorMsg extends UserActorMsg:
  case RegisteredPlayer(playing: Boolean)
  
 */
  
  
  
