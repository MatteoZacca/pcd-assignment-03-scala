package it.unibo.agar.distributed

import akka.actor.typed.ActorRef

import it.unibo.agar.model.World

object GameProtocol:
  sealed trait Msg
  
  sealed trait GameMessage extends Msg
  case class RegisterView(view: ActorRef[WorldSnapshot]) extends GameMessage
  case class RegisterPlayer(userId: String, replyTo: Option[ActorRef[WorldSnapshot]] = None) extends GameMessage
  case class WorldSnapshot(world: World) extends GameMessage
  case class UserInputMsg(playerId: String, dx: Double, dy: Double) extends GameMessage
  case object Tick extends GameMessage

  
  
  
  
