# Terminate any stale Java/sbt background servers holding the pipe lock
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

Write-Host "Starting all Agar.io nodes in a single SBT instance..." -ForegroundColor Cyan

sbt "; bgRunMain it.unibo.agar.controller.mainManager; bgRunMain it.unibo.agar.controller.mainAIPlayer; bgRunMain it.unibo.agar.controller.mainUser user-1; bgRunMain it.unibo.agar.controller.mainUser user-2; shell"