# UWNHRando
Uncharted Waters: New Horizons randomizer for the SNES
Requires a 64-bit JVM ver. 1.6.0 or later and a copy of the original ROM

Note:  The latest version can now process headerless ROMs.

Version 1.01
 - Added a makeLog.txt for further tracking ROM generation errors
 - Increased the ROM generation timeout from 20 seconds to 2 minutes (for slower machines)
 - Made a minor change to continent placement to increase randomness
 - Fixed archipelago placement (so that they are not all on the top left corner)

Version 1.0
 - Randomize the world map and your initial ship
 - Option to view minimap of newly created world
 - Theme editor allows you to rename Kingdoms, trade regions, all ports, discoveries, and commodities
 - Randomize port market types, shipyard types, specialty good and graphical tileset (a.k.a. "culture")

Known issues:
 - Junk may appear at the top of the in-game minimap.  The latest version significantly reduces this.
 - There is a (very) minimal chance that ports may be placed in an inaccessible location.
 - The game may softlock if you change the graphical tileset ("Culture") of capitals.
 - Randomizing the map may make the game impossible to beat.  
   (Especially for Pietro, who needs to sail from Genoa to Lisbon right at the start of the scenario).
