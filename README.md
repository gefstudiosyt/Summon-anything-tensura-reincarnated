# Summon Anything (Fabric, Minecraft 1.21.1)

Lets Tensura's **Summon Otherworlder** spell summon **any mob from any mod**, using a list you edit.
Needs Fabric API and Tensura: Reincarnated 2.0.1.5 (Fabric). **Untested** - see "If the build fails".

## Get the mod file (no coding needed)
1. Make a free account at github.com and click **New repository**. Name it `summon-anything`, then **Create repository**.
2. Click **uploading an existing file**. Unzip this project first, open the folder, select EVERYTHING inside
   (including the hidden `.github` folder) and drag it into the page. Click **Commit changes**.
   - If the `.github` folder did not upload: click **Add file > Create new file**, type
     `.github/workflows/build.yml` as the name (typing the slashes makes the folders), paste the contents of
     that file from this project, and commit.
3. Click the **Actions** tab. A run called "Build mod" starts by itself (about 5 minutes).
   If it asks you to enable workflows, click the green button.
4. When it shows a green check, click the run, scroll down to **Artifacts**, and download `summon-anything-mod`.
   Unzip it - the `summon-anything-1.0.0.jar` inside is the mod.
5. Put that `.jar` in your Minecraft `mods` folder (next to Tensura and ManasCore). Start the game once.

## Magic Station and Mob Summon Book
Craft a **Magic Station**: obsidian in every slot except the top-middle (a Book) and the center (an Eye of Ender).

Right-click the station to open its screen. Put a **Summon Otherworlder spell book** (Tensura Magic Tome for that
spell) in the tome slot - no other book is accepted. Search or scroll the list (it shows every mob from every
installed mod), click a mob, press **Create**, then take the Mob Summon Book from the bottom slot.
Only the spell book is used up (free in creative).

**Iron golems and snow golems can never be summoned** by this mod - you have to get them normally. More mobs can be
blocked with `blockedMobs` in the config.

You get a **Mob Summon Book**. Right-click with it to summon that mob in front of you. Hostile mobs will be hostile to you.
In `config/summonanything.json`:
- `bookMagiculeCost` (default 500): magicule taken from you per summon. `0` = free. Needs Tensura; creative is free.
- `bookUses` (default 10): how many summons before the book breaks. `0` = unlimited. Needs a game restart to change.
- `bookCooldownSeconds` (default 60): wait time between summons.
The station and book pictures are in `src/main/resources/assets/summonanything/textures` - replace them to change the look.

## Choose your mobs for the spell - edit the file
After the first launch, open `config/summonanything.json` in Notepad:

```json
"extraMobChance": 0.5,
"mobs": [
  { "id": "minecraft:villager", "weight": 1.0 },
  { "id": "modid:mob_name",     "weight": 2.0 }
]
```
- `extraMobChance`: chance (0 to 1) that a cast gives one of your mobs instead of a normal Tensura otherworlder.
- `weight`: bigger = more likely compared with the other mobs in the list. Any numbers work.
- Find IDs: F3+H, then hold a spawn egg; or type `/summon ` and press Tab.
- The file reloads while the game runs. Unknown IDs are skipped (check `logs/latest.log`).
- Hostile mobs will be hostile to you. The spell's normal 50% fail chance still comes from Tensura's own config.

## If the build fails
Open the failed run in the Actions tab, copy the red error text, and send it to whoever helped you write this.

## Sacrifice Circle (refill a book's uses)
Craft a **Sacrifice Circle**: redstone in the four corners, obsidian on the four sides, an Ender Pearl in the middle.
Place it flat on the ground. Lure mobs onto it (within `sacrificeRadius`, default 3 blocks), hold your **Mob Summon Book**
in your main hand and right-click the circle. The mobs are sacrificed and each one gives the book `sacrificeUsesPerMob`
uses back (default 1), up to full. Only as many mobs as needed are taken. Named mobs, tamed pets and players are safe.
Does nothing for books with unlimited uses (`bookUses` 0).
