package it.unibo.agar.distributed

import it.unibo.agar.Message

object AgarioProtocol:
  
  // GameCoordinator protocol
  enum GameMessage extends Message:
    case Ciao
  
  // Player Creation
  enum CreatePlayerMsg extends Message:
    case CreateUser
    case CreateAI
    
  
