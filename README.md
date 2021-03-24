# SnowWars
SnowWars is a little plugin that I made very quickly, made for making little minigames called "Snow Wars".
It is very buggy. Don't expected a plugin for production, unless you know very well how to use it.
It doesn't do everything alone

## How to use:
1. et the main spawn point with `/snowwars setmainspawn`
2. Set as many spawn points as there are players with `/snowwars addspawn`
3. Ask your players to join with `/snowwars join`. You can join for them with `/snowwars join <name>`, or join
   for everyone in the current world with `*` as name
4. Start the game with `/snowwwars start`
5. Optionally stop with `/snowwars stop`

### French messages:
```yaml
messages:
  playerDiedBroadcast: '§c§l{player}§c est mort !§e Il lui reste §l{lives}§e vies. §6§l{remaining}§6 joueurs restant !'
  playerKilledBroadcast: '§c§l{killer}§c a tué §l{victim}§c !§e Il lui reste §l{lives}§e vies. §6§l{remaining}§6 joueurs restant !'
  playerDiedTitle: '§4Tu es mort !'
  playerDiedSubtitle: '§6Il te reste §l{lives}§6 vies.§3 Tu pourras Réapparaître dans §l{time}§3.'
  playerDiedForeverSubtitle: '§cTu ne pourras plus réapparaître, tu n''as plus de vies.'
  noPerm: '§cVous n''avez pas la permssion {perm}'
  join: '§aTu as rejoint le jeu de §lSnow Wars§a !'
  alreadyJoined: '§eTu avais déjà rejoint, mais je te retéléporte ici si tu veux.'
  youResuscitated: '§2Tu a été ressuscité !'
  playerWon: '§b§l§n{player}§b a gagné !'
  alreadyStarted: '§eTu ne peux pas faire ça vu que la partie a déjà commencé !'
```
