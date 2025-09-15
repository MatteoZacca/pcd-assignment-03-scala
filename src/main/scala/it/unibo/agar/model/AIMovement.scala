package it.unibo.agar.model

import it.unibo.agar.model.Direction

/** Object responsible for AI movement logic, separate from the game state management */
object AIMovement:
  

  /** Finds the nearest food for a given player in the world
    * @param player
    *   the ID of the player for whom to find the nearest food
    * @param world
    *   the current game world containing players and food
    * @return
    */
  def nearestFood(player: String, world: World): Option[Food] =
    world.foods
      .sortBy(food => world.playerById(player).map(p => p.distanceTo(food)).getOrElse(Double.MaxValue))
      .headOption

  /** Moves the AI toward the nearest food
    *
    * 
    *   
    */
  def getAIMove(aiId: String, world: World): Option[Direction] =
    val aiOpt = world.playerById(aiId)
    val foodOpt = nearestFood(aiId, world)
    (aiOpt, foodOpt) match
      case (Some(ai), Some(food)) =>
        val dx = food.x - ai.x
        val dy = food.y - ai.y
        val distance = math.hypot(dx, dy)
        if (distance > 0) {
          val normalizedDx = dx / distance
          val normalizedDy = dy / distance
          Some((normalizedDx, normalizedDy))
        } else {
          None
        }
          
      case _ => None // Do nothing if AI or food doesn't exist
