package it.unibo.agar.distributed

import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.Cluster

import it.unibo.agar.model.Player
import it.unibo.agar.model.Food


object GameManager:
  
  def apply(width: Int, height: Int, players: Seq[Player], foods: Seq[Food]): Behavior[Nothing] = Behaviors.setup { ctx =>
    
  }

