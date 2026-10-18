import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * CaText RPG demo: a Java console RPG vertical slice.
 * The demo covers character creation, travel from the first town to the second,
 * two combats, one recruitable ally, inn saves, and file loading.
 */
public class CaTextRPG {
    private static final Scanner INPUT = new Scanner(System.in);
    private static final Random RANDOM = new Random();
    private static final Path SAVE_FILE = Paths.get("catext-save.txt");

    private enum ClassType {
        FIGHTER("Fighter", "Champion", 10, 15, 13, 14, 10, 12, 8, "Shield Bash", false),
        ROGUE("Rogue", "Thief", 8, 8, 15, 14, 13, 12, 10, "Precise Strike", false),
        WIZARD("Wizard", "School of Evocation", 6, 8, 14, 13, 15, 12, 10, "Arcane Bolt", true),
        CLERIC("Cleric", "Life Domain", 8, 14, 8, 13, 10, 15, 12, "Mending Light", true),
        BARBARIAN("Barbarian", "Path of the Berserker", 12, 15, 13, 14, 8, 12, 10, "Raging Blow", false),
        RANGER("Ranger", "Hunter", 10, 12, 15, 13, 10, 14, 8, "Marked Shot", false),
        BARD("Bard", "College of Lore", 8, 8, 14, 13, 10, 12, 15, "Rousing Verse", true);

        final String displayName;
        final String archetype;
        final int hitDie;
        final int strength;
        final int dexterity;
        final int constitution;
        final int intelligence;
        final int wisdom;
        final int charisma;
        final String skillName;
        final boolean magicUser;

        ClassType(String displayName, String archetype, int hitDie, int strength, int dexterity,
                  int constitution, int intelligence, int wisdom, int charisma,
                  String skillName, boolean magicUser) {
            this.displayName = displayName;
            this.archetype = archetype;
            this.hitDie = hitDie;
            this.strength = strength;
            this.dexterity = dexterity;
            this.constitution = constitution;
            this.intelligence = intelligence;
            this.wisdom = wisdom;
            this.charisma = charisma;
            this.skillName = skillName;
            this.magicUser = magicUser;
        }
    }

    private static class Item {
        final String name;
        final int healing;

        Item(String name, int healing) {
            this.name = name;
            this.healing = healing;
        }
    }

    private static class Combatant {
        String name;
        ClassType classType;
        int level = 1;
        int strength;
        int dexterity;
        int constitution;
        int intelligence;
        int wisdom;
        int charisma;
        int maxHealth;
        int health;
        boolean playerControlled;
        boolean ally;
        boolean enemy;

        Combatant(String name, ClassType classType, boolean playerControlled, boolean ally, boolean enemy) {
            this.name = name;
            this.classType = classType;
            this.playerControlled = playerControlled;
            this.ally = ally;
            this.enemy = enemy;
            this.strength = classType.strength;
            this.dexterity = classType.dexterity;
            this.constitution = classType.constitution;
            this.intelligence = classType.intelligence;
            this.wisdom = classType.wisdom;
            this.charisma = classType.charisma;
            this.maxHealth = roll(classType.hitDie) + constitution;
            this.health = maxHealth;
        }

        int attack() {
            return strength + dexterity / 2;
        }

        int defense() {
            return dexterity + constitution / 2;
        }

        int magic() {
            return Math.max(intelligence, Math.max(wisdom, charisma));
        }

        int magicDefense() {
            return wisdom + constitution / 2;
        }

        int speed() {
            return dexterity;
        }

        boolean alive() {
            return health > 0;
        }

        void heal(int amount) {
            health = Math.min(maxHealth, health + amount);
        }
    }

    private static class GameState {
        String location = "Moonwhisker Town";
        boolean allyRescued;
        boolean chocolateFound;
        boolean chocolateUsed;
        Combatant player;
        List<Combatant> party = new ArrayList<>();
        List<Item> inventory = new ArrayList<>();
        GameState innSnapshot;
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("       CATEXT RPG: THE DEMON DOG        ");
        System.out.println("========================================");
        System.out.println("A serious adventure with room for hope.");

        GameState state = loadGame();
        if (state == null) {
            state = new GameState();
            state.player = createCharacter();
            state.party.add(state.player);
            state.inventory.add(new Item("Healing Berry", 8));
            state.inventory.add(new Item("Healing Berry", 8));
            restAtInn(state);
        } else {
            System.out.println("Save loaded. Welcome back, " + state.player.name + ".");
        }

        runDemo(state);
        System.out.println("Thanks for playing the CaText RPG demo.");
    }

    private static Combatant createCharacter() {
        System.out.print("Enter your adventurer's name: ");
        String name = INPUT.nextLine().trim();
        if (name.isEmpty()) {
            name = "Aster";
        }

        System.out.println("\nChoose a class:");
        ClassType[] classes = ClassType.values();
        for (int i = 0; i < classes.length; i++) {
            ClassType type = classes[i];
            System.out.printf("%d. %s - %s; skill: %s%n", i + 1, type.displayName, type.archetype, type.skillName);
        }
        int choice = readNumber(1, classes.length);
        ClassType selected = classes[choice - 1];
        Combatant player = new Combatant(name, selected, true, false, false);

        System.out.println("\nYour class data comes from the referenced class sheets.");
        System.out.println("Starting ability values:");
        printStats(player);
        System.out.println("Initial health roll: 1d" + selected.hitDie + " + Constitution = " + player.maxHealth);
        System.out.println("Primary attributes have a game ceiling of 20.");
        return player;
    }

    private static void runDemo(GameState state) {
        while (true) {
            if (state.location.equals("Second Town")) {
                System.out.println("\nYou reach Sunclaw Town, the second town.");
                System.out.println("The demo ends here. The road ahead leads toward the Demon Dog.");
                saveGame(state);
                return;
            }

            System.out.println("\n--- " + state.location + " ---");
            System.out.println("The Demon Dog has returned. You want to save cat-people, but you refuse to believe every dog-person is your enemy.");
            System.out.println("1. Travel toward Sunclaw Town");
            System.out.println("2. View party and inventory");
            System.out.println("3. Rest at the inn");
            System.out.println("4. Save game");
            System.out.println("5. Load game");
            System.out.println("6. Quit");
            int choice = readNumber(1, 6);

            if (choice == 1) {
                travel(state);
            } else if (choice == 2) {
                showParty(state);
            } else if (choice == 3) {
                restAtInn(state);
            } else if (choice == 4) {
                saveGame(state);
            } else if (choice == 5) {
                GameState loaded = loadGame();
                if (loaded != null) {
                    state = loaded;
                }
            } else {
                return;
            }
        }
    }

    private static void travel(GameState state) {
        if (state.location.equals("Moonwhisker Town")) {
            state.location = "Old Pine Road";
            System.out.println("\nYou leave Moonwhisker Town and follow the old road.");
            ascii("  /\\_/\\    The road is quiet... too quiet.", " ( o.o )");
            if (!combat(state, makeEnemies("Road Patrol", ClassType.ROGUE, 1))) {
                return;
            }
            if (!state.allyRescued) {
                rescueAlly(state);
            }
        } else if (state.location.equals("Old Pine Road")) {
            System.out.println("\nA barricade blocks the road to Sunclaw Town.");
            if (combat(state, makeEnemies("Dog-people Vanguard", ClassType.FIGHTER, 2))) {
                state.location = "Second Town";
                state.chocolateFound = true;
                System.out.println("You find a strange golden chocolate in a fallen supply crate.");
                System.out.println("It may weaken the Demon Dog when used at the beginning of the final battle.");
            }
        }
    }

    private static void rescueAlly(GameState state) {
        System.out.println("\nBeyond the road, a young dog-person and a cat-person are cornered by monsters.");
        System.out.println("Your childhood friend taught you that people are more than their faction.");
        System.out.println("1. Rescue the trapped cat-person");
        System.out.println("2. Leave them and continue");
        int choice = readNumber(1, 2);
        if (choice == 1) {
            if (combat(state, makeEnemies("Forest Monsters", ClassType.BARBARIAN, 1))) {
                Combatant ally = new Combatant("Mira the Ranger", ClassType.RANGER, true, true, false);
                state.party.add(ally);
                state.allyRescued = true;
                System.out.println("Mira joins your party. You now control every party member during combat.");
                saveGame(state);
            }
        } else {
            System.out.println("You continue, but the road feels heavier behind you.");
        }
    }

    private static List<Combatant> makeEnemies(String groupName, ClassType type, int level) {
        List<Combatant> enemies = new ArrayList<>();
        Combatant first = new Combatant(groupName + " Alpha", type, false, false, true);
        first.level = level;
        first.maxHealth += level * 3;
        first.health = first.maxHealth;
        enemies.add(first);
        if (level > 1) {
            Combatant second = new Combatant(groupName + " Scout", ClassType.ROGUE, false, false, true);
            second.maxHealth += 2;
            second.health = second.maxHealth;
            enemies.add(second);
        }
        return enemies;
    }

    private static boolean combat(GameState state, List<Combatant> enemies) {
        saveGame(state);
        ascii("  .-._.-.   COMBAT", " /  o o  /", "|    ^    |", " \\  '-'  /");
        System.out.println("Combat begins. The game saved before combat.");
        List<Item> combatItems = new ArrayList<>();
        List<Combatant> all = new ArrayList<>();
        all.addAll(state.party);
        all.addAll(enemies);
        all.sort(Comparator.comparingInt(Combatant::speed).reversed());

        while (hasLiving(state.party) && hasLiving(enemies)) {
            for (Combatant actor : all) {
                if (!actor.alive()) {
                    continue;
                }
                if (!hasLiving(state.party) || !hasLiving(enemies)) {
                    break;
                }
                if (actor.enemy) {
                    enemyTurn(actor, state.party);
                } else {
                    playerTurn(actor, state, enemies, combatItems);
                }
            }
        }

        if (hasLiving(state.party)) {
            System.out.println("You survived the battle.");
            return true;
        }

        System.out.println("\nYour party has fallen. You wake at the last inn you saved at.");
        if (state.innSnapshot != null) {
            restore(state, state.innSnapshot);
        }
        for (Item item : combatItems) {
            if (RANDOM.nextBoolean()) {
                state.inventory.add(item);
            }
        }
        System.out.println("Some items used during combat were returned by chance.");
        return false;
    }

    private static void playerTurn(Combatant actor, GameState state, List<Combatant> enemies, List<Item> combatItems) {
        if (!actor.alive()) {
            return;
        }
        System.out.println("\n" + actor.name + "'s turn. HP " + actor.health + "/" + actor.maxHealth);
        System.out.println("1. Attack");
        System.out.println("2. Use items");
        System.out.println("3. Retreat");
        int choice = readNumber(1, 3);
        if (choice == 1) {
            attackMenu(actor, enemies);
        } else if (choice == 2) {
            useItem(state, actor, combatItems);
        } else {
            int chance = Math.max(25, Math.min(90, 60 + actor.speed() - enemies.get(0).speed()));
            if (RANDOM.nextInt(100) < chance) {
                System.out.println(actor.name + " escapes. The party retreats to the last inn.");
                if (state.innSnapshot != null) {
                    restore(state, state.innSnapshot);
                }
                for (Combatant enemy : enemies) {
                    enemy.health = 0;
                }
            } else {
                System.out.println(actor.name + " fails to retreat.");
            }
        }
    }

    private static void attackMenu(Combatant actor, List<Combatant> enemies) {
        System.out.println("1. Basic attack");
        System.out.println("2. " + actor.classType.skillName);
        int choice = readNumber(1, 2);
        Combatant target = chooseTarget(enemies);
        int amount;
        if (choice == 1) {
            amount = actor.attack() - target.defense();
            amount = Math.max(0, amount);
            target.health -= amount;
            System.out.println(actor.name + " deals " + amount + " melee damage to " + target.name + ".");
        } else if (actor.classType.magicUser) {
            amount = Math.max(0, actor.magic() - target.magicDefense());
            target.health -= amount;
            System.out.println(actor.name + " uses " + actor.classType.skillName + " for " + amount + " magic damage.");
        } else {
            amount = Math.max(0, actor.attack() - target.defense());
            target.health -= amount;
            System.out.println(actor.name + " uses " + actor.classType.skillName + " for " + amount + " melee damage.");
        }
        if (!target.alive()) {
            System.out.println(target.name + " is defeated.");
        }
    }

    private static void enemyTurn(Combatant enemy, List<Combatant> party) {
        Combatant target = party.stream().filter(Combatant::alive).findFirst().orElse(null);
        if (target == null) {
            return;
        }
        int amount = Math.max(0, enemy.attack() - target.defense());
        target.health -= amount;
        System.out.println(enemy.name + " attacks " + target.name + " for " + amount + " melee damage.");
        if (!target.alive()) {
            System.out.println(target.name + " falls.");
        }
    }

    private static void useItem(GameState state, Combatant actor, List<Item> combatItems) {
        if (state.inventory.isEmpty()) {
            System.out.println("No usable items remain.");
            return;
        }
        System.out.println("Choose an item:");
        for (int i = 0; i < state.inventory.size(); i++) {
            System.out.println((i + 1) + ". " + state.inventory.get(i).name + " (heal " + state.inventory.get(i).healing + ")");
        }
        int choice = readNumber(1, state.inventory.size());
        Item item = state.inventory.remove(choice - 1);
        actor.heal(item.healing);
        combatItems.add(item);
        System.out.println(actor.name + " uses " + item.name + " and heals " + item.healing + " HP.");
    }

    private static Combatant chooseTarget(List<Combatant> targets) {
        List<Combatant> living = targets.stream().filter(Combatant::alive).toList();
        System.out.println("Choose a target:");
        for (int i = 0; i < living.size(); i++) {
            Combatant target = living.get(i);
            System.out.println((i + 1) + ". " + target.name + " HP " + target.health + "/" + target.maxHealth);
        }
        return living.get(readNumber(1, living.size()) - 1);
    }

    private static void showParty(GameState state) {
        System.out.println("\nParty:");
        for (Combatant member : state.party) {
            System.out.println("- " + member.name + " the " + member.classType.displayName + " HP " + member.health + "/" + member.maxHealth + " Speed " + member.speed());
            printStats(member);
        }
        System.out.println("Inventory: " + state.inventory.size() + " item(s)");
    }

    private static void printStats(Combatant character) {
        System.out.printf("STR %d | DEX %d | CON %d | INT %d | WIS %d | CHA %d | Attack %d | Defense %d | Magic %d | Magic Defense %d | Speed %d%n",
                character.strength, character.dexterity, character.constitution, character.intelligence,
                character.wisdom, character.charisma, character.attack(), character.defense(),
                character.magic(), character.magicDefense(), character.speed());
    }

    private static void restAtInn(GameState state) {
        for (Combatant member : state.party) {
            member.health = member.maxHealth;
        }
        state.innSnapshot = snapshot(state);
        saveGame(state);
        System.out.println("You rest at the inn. The game has been saved.");
    }

    private static boolean hasLiving(List<Combatant> group) {
        return group.stream().anyMatch(Combatant::alive);
    }

    private static int roll(int sides) {
        return RANDOM.nextInt(sides) + 1;
    }

    private static int readNumber(int min, int max) {
        while (true) {
            System.out.print("> ");
            String line = INPUT.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // The loop reports invalid input without changing game state.
            }
            System.out.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    private static void ascii(String... lines) {
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private static GameState snapshot(GameState original) {
        GameState copy = new GameState();
        copy.location = original.location;
        copy.allyRescued = original.allyRescued;
        copy.chocolateFound = original.chocolateFound;
        copy.chocolateUsed = original.chocolateUsed;
        for (Combatant member : original.party) {
            Combatant memberCopy = copyCombatant(member);
            copy.party.add(memberCopy);
            if (member == original.player) {
                copy.player = memberCopy;
            }
        }
        for (Item item : original.inventory) {
            copy.inventory.add(new Item(item.name, item.healing));
        }
        return copy;
    }

    private static Combatant copyCombatant(Combatant original) {
        Combatant copy = new Combatant(original.name, original.classType, original.playerControlled, original.ally, original.enemy);
        copy.level = original.level;
        copy.strength = original.strength;
        copy.dexterity = original.dexterity;
        copy.constitution = original.constitution;
        copy.intelligence = original.intelligence;
        copy.wisdom = original.wisdom;
        copy.charisma = original.charisma;
        copy.maxHealth = original.maxHealth;
        copy.health = original.health;
        return copy;
    }

    private static void restore(GameState destination, GameState source) {
        GameState restored = snapshot(source);
        destination.location = restored.location;
        destination.allyRescued = restored.allyRescued;
        destination.chocolateFound = restored.chocolateFound;
        destination.chocolateUsed = restored.chocolateUsed;
        destination.player = restored.player;
        destination.party = restored.party;
        destination.inventory = restored.inventory;
        destination.innSnapshot = source.innSnapshot == null ? null : snapshot(source.innSnapshot);
    }

    private static void saveGame(GameState state) {
        try (BufferedWriter writer = Files.newBufferedWriter(SAVE_FILE)) {
            writer.write("location=" + state.location + "\n");
            writer.write("allyRescued=" + state.allyRescued + "\n");
            writer.write("chocolateFound=" + state.chocolateFound + "\n");
            writer.write("playerName=" + state.player.name + "\n");
            writer.write("playerClass=" + state.player.classType.name() + "\n");
            writer.write("partyCount=" + state.party.size() + "\n");
            for (int i = 0; i < state.party.size(); i++) {
                Combatant member = state.party.get(i);
                writer.write("party" + i + "=" + member.name.replace("|", "/") + "|" + member.classType.name() + "|" + member.health + "|" + member.maxHealth + "\n");
            }
            writer.write("itemCount=" + state.inventory.size() + "\n");
            for (int i = 0; i < state.inventory.size(); i++) {
                Item item = state.inventory.get(i);
                writer.write("item" + i + "=" + item.name.replace("|", "/") + "|" + item.healing + "\n");
            }
            System.out.println("Game saved to " + SAVE_FILE.toAbsolutePath());
        } catch (IOException exception) {
            System.out.println("Save failed: " + exception.getMessage());
        }
    }

    private static GameState loadGame() {
        if (!Files.exists(SAVE_FILE)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(SAVE_FILE)) {
            Map<String, String> values = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                int split = line.indexOf('=');
                if (split > 0) {
                    values.put(line.substring(0, split), line.substring(split + 1));
                }
            }
            GameState state = new GameState();
            state.location = values.getOrDefault("location", "Moonwhisker Town");
            state.allyRescued = Boolean.parseBoolean(values.getOrDefault("allyRescued", "false"));
            state.chocolateFound = Boolean.parseBoolean(values.getOrDefault("chocolateFound", "false"));
            int partyCount = Integer.parseInt(values.getOrDefault("partyCount", "0"));
            for (int i = 0; i < partyCount; i++) {
                String[] parts = values.get("party" + i).split("\\|");
                Combatant member = new Combatant(parts[0], ClassType.valueOf(parts[1]), i == 0, i > 0, false);
                member.health = Integer.parseInt(parts[2]);
                member.maxHealth = Integer.parseInt(parts[3]);
                state.party.add(member);
                if (i == 0) {
                    state.player = member;
                }
            }
            int itemCount = Integer.parseInt(values.getOrDefault("itemCount", "0"));
            for (int i = 0; i < itemCount; i++) {
                String[] parts = values.get("item" + i).split("\\|");
                state.inventory.add(new Item(parts[0], Integer.parseInt(parts[1])));
            }
            if (state.player == null) {
                return null;
            }
            state.innSnapshot = snapshot(state);
            return state;
        } catch (Exception exception) {
            System.out.println("Save could not be loaded; starting a new game. Reason: " + exception.getMessage());
            return null;
        }
    }
}
