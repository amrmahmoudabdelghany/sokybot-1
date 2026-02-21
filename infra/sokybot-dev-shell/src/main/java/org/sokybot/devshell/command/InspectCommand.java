package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.gamemodel.model.ITrainer;

@Service
@Command(scope = "dev", name = "inspect", description = "Dump bot game model state")
public class InspectCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                println("State for bot '%s' in group '%s':", botName, groupName);
                println("--------------------------------------------------");
                println("Engine status: %s", bot.isRunning() ? "RUNNING" : "STOPPED");
                
                ITrainer trainer = bot.getGameModel().getTrainer();
                if (trainer != null) {
                    println("Level:  %d / %d", trainer.getLevel(), trainer.getMaxLvl());
                    println("Health: %d / %d (%d%%)", trainer.getCurrentHP(), trainer.getMaxHP(), trainer.getHPPercentage());
                    println("Mana:   %d / %d (%d%%)", trainer.getCurrentMP(), trainer.getMaxMP(), trainer.getMPPercentage());
                    println("Gold:   %,d", trainer.getGold());
                    println("SP:     %,d", trainer.getSkillPoint());
                    println("Pos:    (X: %d, Y: %d, Z: %.1f)", 
                        trainer.getX(), trainer.getY(), trainer.getZOffset());
                } else {
                    println("Game model state not available (model not initialized).");
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
