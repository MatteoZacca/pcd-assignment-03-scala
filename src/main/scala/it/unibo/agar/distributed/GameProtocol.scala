package it.unibo.agar.distributed

import akka.actor.Address
import akka.actor.typed.ActorRef
import it.unibo.agar.Message
import it.unibo.agar.model.{Direction, Food, World}
  
/** GameManager messages */
sealed trait GameMessage extends Message
case class RegisterView(view: ActorRef[StandardViewMessage]) extends GameMessage
case class RegisterPlayer(userId: String, replyTo: ActorRef[LocalViewMsg]) extends GameMessage
case class NewFood(food: Food) extends GameMessage
case class PlayerMove(aiId: String, direction: Direction) extends GameMessage
case class PlayerLeft(id: String, nodeAddress: Address) extends GameMessage
case object Tick extends GameMessage with AIPlayerMsg 


sealed trait FoodMessage extends Message
case object GenerateFood extends FoodMessage

final case class WrappedListingGameManager(refs: Set[ActorRef[GameMessage]]) 
  extends FoodMessage 
    with AIPlayerMsg

/** GlobalViewActor, UserActor and AIPlayerActor messages */
sealed trait ViewMessage extends Message
trait GlobalViewMsg extends ViewMessage
trait UserMsg extends ViewMessage
trait AIPlayerMsg extends ViewMessage

sealed trait StandardViewMessage extends GlobalViewMsg with UserMsg with AIPlayerMsg
final case class WorldSnapshot(world: World) extends StandardViewMessage
final case class GameOver(winner: String) extends StandardViewMessage

sealed trait LocalViewMsg extends UserMsg with AIPlayerMsg
final case class RegisteredPlayer(playing: Boolean) extends LocalViewMsg


  
  
  
