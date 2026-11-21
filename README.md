# Texture Mixer (Fabric, Minecraft 26.2)

Build:   JAVA_HOME=<JDK 25> ./gradlew build     -> build/libs/texturemixer-1.0.0.jar
Testen:  ./gradlew runClient

Ingame: /texturemixer  (oder /tm)
Pro Kategorie auf den Button klicken = naechstes Pack. "Anwenden" erzeugt
resourcepacks/TextureMixer, aktiviert es ganz oben und laedt neu.
Kategorien/Pfade/Vorschau-Texturen: Category.java
Menue: links Kategorie, rechts Kacheln (echte Texturen aus den Packs), roter Rahmen = gewaehlt.

## Giveaways & Auktionen (clientseitig)
Item in die Hand nehmen, dann:
  /giveaway start <minuten> <keyword>     Beitritt: Spieler schreibt dir (/msg) das Keyword
  /giveaway end | cancel | reroll | list
  /auction start <minuten> <mindestgebot> Gebote: "bid 500", "bid 10k", "bid 1.5m" per /msg
  /auction end | cancel
  /eventannounce                          Chat-Ansagen/Auto-Antworten an/aus
Rechts oben erscheint ein Hologramm-Panel (schwebendes 3D-Item, Teilnehmer/Gebote live).
Nur DU siehst es (Client-Mod). Achtung: Automatische Chat-Ansagen koennen auf manchen Servern gegen Regeln verstossen.

## Presets
  /tmpreset save <name>    speichert die zuletzt angewendete Auswahl
  /tmpreset load <name>    laedt, baut das Pack neu und aktiviert es
  /tmpreset list | delete <name>

## Alles in einem Menue
/tm oeffnet das Hauptmenue mit Tabs: Texturen | Presets | Giveaway | Auktion.
Giveaway/Auktion: Dauer, Keyword bzw. Mindestgebot, Start/Beenden/Abbrechen/Neu losen,
Hologramm- und Ansagen-Schalter sowie Live-Status direkt im Menue.
