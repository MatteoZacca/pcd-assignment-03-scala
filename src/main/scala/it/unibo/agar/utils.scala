package it.unibo.agar

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import com.typesafe.config.ConfigFactory

val seeds = List(2551, 2552) // seed used in the configuration

def startup[X](file: String = "base-cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
  // Override the configuration of the port
  val config = ConfigFactory
    // akka.remote.artery.canonical.port=25251 -> imposta la porta di rete su cui l'attore
    // comunicherà tramite il protocollo Artery
    .parseString(s"""akka.remote.artery.canonical.port=$port""")
    .withFallback(ConfigFactory.load(file))

  // Create an Akka system
  // Il nodo pinger si registra come seed node e crea un nuovo cluster, perchè è il primo ad avviarsi.
  // Il nodo ponger, che parte dopo, cercherà di agganciarsi a questo seed node 'agario-actor-system-25251',
  // per unirsi al cluster
  ActorSystem(root, file + s"-actor-system-${port}", config)

def startupWithRole[X](role: String, port: Int)(root: => Behavior[X]): ActorSystem[X] =
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
    .withFallback(ConfigFactory.load("base-cluster"))

  // Create an Akka system
  ActorSystem(root, "ClusterSystem", config)
