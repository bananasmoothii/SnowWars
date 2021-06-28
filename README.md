# SnowWars
SnowWars is a little plugin that I made, made for making little minigames called "Snow Wars".

The goal is to pick up as much snow as you can with you shovel to throw snowballs on you opponents,
as well as craft snow blocks (4 snowballs) to make bridges. By default, snow blocks can only be placed
on chiseled quartz and other snow blocks.

**This is SnowWars WorldEdit version, so it depends on WorldEdit *(currently not working
with FastAsyncWorldEdit)***

## How to use (and command description):
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
4. Set a vote location with `/snowwars setvotelocation <map name>`. If a player is close to a vote location (distance
   is configurable), he will be considered as voting for that map. If at least half of players plus one voted for a
   map, it will be the map they will play on.
5. Ask your players to join with `/snowwars join`. You can join for them with `/snowwars join <name>`, or join
   for everyone in the current world with `*` as name
6. Start the game with `/snowwwars start` or `forcestart`
7. Optionally stop with `/snowwars stop`
8. `/snowwars reload` might help for some bugs
9. `/snowwars addlive <player> <lives>` will add (or remove if negative) lives to that player, respawn him if
   he can or stop the game if needed. example: `/snowwars addlive Bananasmoothii -1`
10. `/snowwars stats` displays stats about the currently running game.

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
- `snowwars.setvotelocation`
- `snowwars.anticheat.bypass`
- `snowwars.anticheat.notify` (notifies you when someone got punished for cheating)

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
  hasVoted: '{player}§r a voté pour §3{map name}'
  notEnoughSpawnPoints: '§eDésolé, §o{map name}§e n''a pas assez de points de spawn pour {players} joueurs.'
  noRunningGame: '§eDésolé, il n''y a actuellement pas de jeu en cours.'
  statsHeader: '§b§nVoici comment le jeu de Snow Wars se déroule:'
  statsLine: '§9{player}§9 : §l{lives}§9 vies'
```
