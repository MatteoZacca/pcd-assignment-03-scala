package it.unibo.agar.distributed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior

import it.unibo.agar.model.{Food, Player, World}
import it.unibo.agar.controller.Main
import it.unibo.agar.distributed.AgarioProtocol.*

object GameCoordinator:

  def apply(players: Seq[Player], food: Seq[Food]): Behavior[GameMessage] =>
    Behaviors.setup: ctx =>





