import org.sokybot.gamemodel.IGameModel
import org.sokybot.gamemodel.model.*

def model = binding.getVariable("model") as IGameModel
def entityType = (binding.getVariable("entityType") ?: "trainer").toString().toLowerCase()
def out = new StringBuilder()

switch (entityType) {
    case "trainer":
        def trainer = model.getTrainer()
        if (trainer == null) {
            out << "Trainer not available (not logged in?).\n"
            break
        }
        out << "=== Trainer ===\n"
        out << "  UniqueId: ${trainer.getUniqueId()}\n"
        out << "  Level:    ${trainer.getLevel()} / ${trainer.getMaxLvl()}\n"
        out << "  Health:   ${trainer.getCurrentHP()} / ${trainer.getMaxHP()} (${trainer.getHPPercentage()}%)\n"
        out << "  Mana:     ${trainer.getCurrentMP()} / ${trainer.getMaxMP()} (${trainer.getMPPercentage()}%)\n"
        out << "  Gold:     ${String.format('%,d', trainer.getGold())}\n"
        out << "  SP:       ${String.format('%,d', trainer.getSkillPoint())}\n"
        out << "  Position: (X: ${trainer.getX()}, Y: ${trainer.getY()}, Z: ${String.format('%.1f', trainer.getZOffset())})\n"

        def skills = trainer.getSkills()
        if (skills != null && !skills.isEmpty()) {
            out << "  Skills (${skills.size()}):\n"
            skills.each { skill ->
                if (skill != null) {
                    def status = skill.isEnabled() ? "[enabled]" : "[disabled]"
                    out << "    - ${skill.getName()} (Lv.${skill.getSkillLvl()}) ${status}\n"
                }
            }
        }

        def inventory = trainer.getInventory()
        if (inventory != null && !inventory.isEmpty()) {
            out << "  Inventory (${inventory.size()} items):\n"
            inventory.each { item ->
                if (item != null) {
                    out << "    [${String.format('%02d', item.getSlot())}] ${item.getName()} x${item.getStackCount()} (ref: ${item.getLongId()})\n"
                }
            }
        }
        break

    case "monsters":
        def monsters = model.findAll(IMonster)
        if (!monsters) {
            out << "No monsters in view.\n"
            break
        }
        out << "=== Monsters (${monsters.size()}) ===\n"
        monsters.values().each { m ->
            out << "  [${m.getUniqueId()}] alive=${m.isAlive()} type=${m.getMonsterType()} pos=(${m.getX()},${m.getY()}) target=${m.getTargetId()}\n"
        }
        break

    case "items":
        def items = model.findAll(IItem)
        if (!items) {
            out << "No items tracked.\n"
            break
        }
        out << "=== Items (${items.size()}) ===\n"
        items.values().each { item ->
            out << "  [slot ${String.format('%02d', item.getSlot())}] ${item.getName()} x${item.getStackCount()} (ref: ${item.getLongId()})\n"
        }
        break

    case "spawns":
        def spawns = model.findAll(ISpawn)
        if (!spawns) {
            out << "No spawns in model.\n"
            break
        }
        out << "=== Spawns (${spawns.size()}) ===\n"
        spawns.values().each { s ->
            out << "  [${s.getUniqueId()}] ${s.getClass().getSimpleName()} pos=(${s.getX()},${s.getY()})\n"
        }
        break

    default:
        try {
            int id = Integer.parseInt(entityType)
            model.find(id).ifPresentOrElse({ spawn ->
                out << "=== Entity #${id} (${spawn.getClass().getSimpleName()}) ===\n"
                out << "  UniqueId: ${spawn.getUniqueId()}\n"
                out << "  Position: (${spawn.getX()}, ${spawn.getY()})\n"
                out << "  Type:     ${spawn.getClass().getName()}\n"
            }, {
                out << "ERROR: No entity found with ID ${id}.\n"
            })
        } catch (NumberFormatException e) {
            out << "ERROR: Unknown entity type '${entityType}'. Use: trainer, monsters, items, spawns, or a numeric ID.\n"
        }
}

out.toString()
