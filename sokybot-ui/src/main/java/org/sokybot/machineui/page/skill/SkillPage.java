package org.sokybot.machineui.page.skill;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import org.osgi.framework.BundleContext;
import org.sokybot.machineui.model.MachineViewModel;
import org.sokybot.machineui.page.IMachinePage;
import org.sokybot.runtime.IMachineContext;

public class SkillPage extends JPanel implements IMachinePage {
    private static final long serialVersionUID = 1L;
    private final SkillTab skillTab;

    public SkillPage(IMachineContext context, MachineViewModel viewModel, BundleContext bundleContext) {
        setLayout(new BorderLayout());
        this.skillTab = new SkillTab(context, viewModel);
        // We need to inject bundleContext to SkillTab for event registration
        this.skillTab.setBundleContext(bundleContext);
        add(this.skillTab, BorderLayout.CENTER);
    }

    @Override
    public String getName() {
        return "Skills";
    }

    @Override
    public javax.swing.Icon getIcon() {
        return null;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
}
