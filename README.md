# SnowWars
SnowWars is a little plugin that I made very quickly, made for making little minigames called "Snow Wars".
It is buggy. Don't expected a plugin for production, unless you know very well how to use it.
It doesn't do everything alone

**This is SnowWars WorldEdit version, so it depends on WorldEdit *(currently not working
with FastAsyncWorldEdit)***

## How to use:
1. Set the main spawn point with `/snowwars setmainspawn`
2. Use WorldEdit to select the region you want to copy at each game start and go to
   the spawn point of that region, then do `/snowwars setsource`
3. Set as many spawn points as there are players with `/snowwars addspawn`
4. Ask your players to join with `/snowwars join`. You can join for them with `/snowwars join <name>`, or join
   for everyone in the current world with `*` as name
5. Start the game with `/snowwwars start`
6. Optionally stop with `/snowwars stop`
7. `/snowwars reload` might help for some bugs

### French messages:
```yaml
messages:
  playerDiedBroadcast: '§c§l{player}§c est mort !§e Il lui reste §l{lives}§e vies. §6§l{remaining}§6 joueurs restant !'
  playerKilledBroadcast: '§c§l{killer}§c a tué §l{victim}§c !§e Il lui reste §l{lives}§e vies. §6§l{remaining}§6 joueurs restant !'
  playerDiedTitle: '§4Tu es mort !'
  playerDiedSubtitle: '§6§l{lives}§6 vies restantes.§3 Réapparition dans §l{time}§3.'
  playerDiedForeverSubtitle: '§cTu ne pourras plus réapparaître, tu n''as plus de vies.'
  noPerm: '§cVous n''avez pas la permssion {perm}'
  join: '§aTu as rejoint le jeu de §lSnow Wars§a !'
  quit: '§eTu as quitté la partie.'
  alreadyJoined: '§eTu avais déjà rejoint, mais je te retéléporte ici si tu veux.'
  youResuscitated: '§2Tu a été ressuscité !'
  playerWon: '§b§l§n{player}§b a gagné !'
  alreadyStarted: '§eTu ne peux pas faire ça vu que la partie a déjà commencé !'
  alreadyStartedSpectator: '§eTu as rejoint en tant que spéctateur étant donné que le jeu a déjà  commencé.'
  livesLeft: '§aVies restantes:'
  bossBar: '§bPonts de glace: §l{time}s§b restantes !'
```
