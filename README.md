# SnowWars
SnowWars is a little plugin that I made very quickly, made for making little minigames called "Snow Wars".
It is buggy. Don't expected a plugin for production, unless you know very well how to use it.
It doesn't do everything alone

**This is SnowWars WorldEdit version, so it depends on WorldEdit *(currently not working
with FastAsyncWorldEdit)***

## How to use:
1. Set the main spawn point with `/snowwars setmainspawn`
2. Add one or more play maps to play in:
   1. Select your map with WorldEdit (only cuboid regions are allowed) and run `/snowwars addmap <map name>` at a
      specific point in the map
   2. Go to the location where you want to play with that map (can be in another world) and run `/snowwars completemap`.
      Each time the game will start (or you run `/snowwars refreshmap`), the plugin will do as if you ran `//copy -e`
      at the location where you ran `/snowwars addmap` and `//paste -e` at the location where you ran
      `/snowwars completemap`. The `-e` flags means it will (try to) copy entities.
   3. If you messed up, there is the `/snowwars deletemap <map name>` command.
3. Set as many spawn points as there are players with `/snowwars addspawn <map name>`
4. Ask your players to join with `/snowwars join`. You can join for them with `/snowwars join <name>`, or join
   for everyone in the current world with `*` as name
5. Start the game with `/snowwwars start` or `forcestart`
6. Optionally stop with `/snowwars stop`
7. `/snowwars reload` might help for some bugs
8. `/snowwars addlive <player> <lives>` will add (or remove if negative) lives to that player, respawn him if
   he can or stop the game if needed. example: `/snowwars addlive Bananasmoothii -1`

Players can run `/snowwars join`, `start` and `quit` without admin perms

## Permissions:
- `snowwars.join.others`
- `snowwars.forcestart`
- `snowwars.stop`
- `snowwars.setmainspawn`
- `snowwars.addspawn`
- `snowwars.reload`
- `snowwars.addmap` (works for `/snowwars completemap` too)
- `snowwars.refreshmap`
- `snowwars.deletemap`
- `snowwars.iceevent`
- `snowwars.addlive`
- `snowwars.choosemap` (allows you to specify a map in `/snowwars start` and `/snowwars forcestart`)
- `snowwars.teleport` (allows you to be teleported to another world without quitting the game or to be teleported to the
  "snowwars" world)

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
  notEnoughPlayers: '§cPas asser de joueurs pour commencer.'
  startingIn: '§bAttention, début dans §9§l{time}s§b !'
  pleaseUseJoin: '§cErreur: merci d''utiliser §r§n/snowwars join§c pour rejoindre ce monde.'
  pleaseUseQuit: '§cErreur: merci d''utiliser §r§n/snowwars quit§c pour aller dans un autre monde.'
  defaultWinner: '§c<personne>'
  startTitle: '§b§nFIGHT !'
  startSubtitle: '§b§lMap: §r§l{map name}'
```
