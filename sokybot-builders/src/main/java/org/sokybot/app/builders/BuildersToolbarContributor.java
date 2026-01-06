package org.sokybot.app.builders;

import javax.swing.JButton;
import javax.swing.JDialog;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.ISokybotContext;
import org.sokybot.service.IMainFrameConfigurator;

@Component(immediate = true)
public class BuildersToolbarContributor {

    private ISokybotContext sokybotContext;
    private IMainFrameConfigurator frameConfigurator;

    @Reference
    public void setSokybotContext(ISokybotContext ctx) {
        this.sokybotContext = ctx;
    }

    @Reference
    public void setFrameConfigurator(IMainFrameConfigurator configurator) {
        this.frameConfigurator = configurator;
    }

    @Activate
    public void activate() {
        System.out.println("BuildersToolbarContributor: Activating...");

        // Button for Creating New Group
        JButton groupBtn = new JButton("New Group");
        groupBtn.setToolTipText("Create a new Bot Group");
        groupBtn.addActionListener(e -> {
            SilkroadFileBrowser browser = new SilkroadFileBrowser();
            MachineGroupBuilderDialog dialog = new MachineGroupBuilderDialog(browser, sokybotContext);
            frameConfigurator.displayDialog(dialog);
        });
        frameConfigurator.addToolbarButton(groupBtn);

        // Button for Creating New Machine
        JButton machineBtn = new JButton("New Machine");
        machineBtn.setToolTipText("Create a new Machine instance");
        machineBtn.addActionListener(e -> {
            MachineBuilderDialog dialog = new MachineBuilderDialog(sokybotContext);
            frameConfigurator.displayDialog(dialog);
        });
        frameConfigurator.addToolbarButton(machineBtn);
    }
}
