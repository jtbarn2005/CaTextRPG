# CaText RPG Demo

CaText RPG is a Java text-based fantasy RPG demo set in a world of anthropomorphic animal people. The player creates an adventurer, chooses one of seven classes, travels from the first town toward the second town, fights enemies, and can rescue an ally. The demo is based on the project requirements and the referenced D&D-inspired class sheets.

## Instructions for Build and Use

Steps to build and/or run the software:

1. Install a Java Development Kit, preferably Java 17 or newer.
2. Open PowerShell in the `CaText RPG` project folder.
3. Compile and run the program with:

   ```powershell
   javac CaTextRPG.java
   java CaTextRPG
   ```

Instructions for using the software:

1. Enter a character name and select one of the seven available classes.
2. Use the numbered menus to travel, view the party, rest at the inn, save, load, or quit.
3. During combat, choose `1. Attack`, `2. Use items`, or `3. Retreat`, then follow the target prompts.
4. The player controls every active party member when their turn arrives.
5. The game automatically saves before combat and when the player rests at an inn.
6. Save data is written to `catext-save.txt` in the project folder.

## Development Environment

To recreate the development environment, you need the following software and/or libraries with the specified versions:

* Java Development Kit 17 or newer
* Java standard library only; no external libraries are required
* Windows PowerShell or another Windows terminal
* Windows console for the intended text-based interface

## Useful Websites to Learn More

I found these websites useful in developing this software:

* [Oracle Java Documentation](https://docs.oracle.com/en/java/)
* [Java Collections Framework](https://docs.oracle.com/javase/tutorial/collections/)
* [Java File I/O Tutorial](https://docs.oracle.com/javase/tutorial/essential/io/)

## Future Work

The following items I plan to fix, improve, and/or add to this project in the future:

* [ ] Add the complete route and story content beyond the second town
* [ ] Add the remaining two recruitable allies and party-swapping outside combat
* [ ] Add level progression, skill-point upgrades, and more class-specific skills
* [ ] Add sub-bosses, the Demon Dog final boss, and the chocolate weakening mechanic
* [ ] Expand branching story choices and alternate endings
