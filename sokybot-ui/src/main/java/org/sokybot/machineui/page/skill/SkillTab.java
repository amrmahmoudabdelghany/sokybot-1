package org.sokybot.machineui.page.skill;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.settings.Settings;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.machinegroup.gamemodel.skill.SkillType;
import org.sokybot.machineui.model.MachineViewModel;
import org.sokybot.persistence.entities.MonsterType;
import org.sokybot.runtime.IMachineContext;

import info.clearthought.layout.TableLayout;

public class SkillTab extends JPanel implements ItemListener, ListSelectionListener, ActionListener, EventHandler {
    private static final long serialVersionUID = 1L;

    protected final IMachineContext context;
    protected final MachineViewModel viewModel;

    // UI Components
    protected JComboBox<MasteryEntry> comMasteryFilter;
    protected SkillTableModel tblSkillModel;
    protected JTable tblSkill;
    protected JCheckBox checkBuffSkills;
    protected JCheckBox checkAttackSkills;
    protected JCheckBox checkHideLowLv;

    protected JComboBox<MonsterTypeEntry> comAttackSkillFilter;
    protected SkillTableModel tblAttackSkillModel;
    protected JTable tblAttackSkill;
    protected JButton btnAddAttackSkill;
    protected JButton btnRemoveAttackSkill;
    protected JButton btnMoveUpAttackSkill;
    protected JButton btnMoveDownAttackSkill;
    protected JButton btnSaveAttackSkill;
    protected JCheckBox checkNoAttack;

    protected JComboBox<Skill> comBuffSkillFilter;
    protected SkillTableModel tblBuffSkillModel;
    protected JTable tblBuffSkill;
    protected JButton btnAddBuffSkill;
    protected JButton btnRemoveBuffSkill;
    protected JButton btnMoveUpBuffSkill;
    protected JButton btnMoveDownBuffSkill;

    private SkillTabComponentFactory compFactory;
    private BundleContext bundleContext;
    private org.osgi.framework.ServiceRegistration<?> eventRegistration;

    public SkillTab(IMachineContext context, MachineViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
        this.compFactory = new SkillTabComponentFactory(this);
        init();
    }
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        if (bundleContext != null) {
            java.util.Dictionary<String, Object> props = new java.util.Hashtable<>();
            props.put(org.osgi.service.event.EventConstants.EVENT_TOPIC, new String[] {
                "sokybot/machine/trainer/loaded",
                "sokybot/machine/trainer/skills_updated"
            });
            String filter = "(machineId=" + context.fullName() + ")";
             props.put(org.osgi.service.event.EventConstants.EVENT_FILTER, filter);
            eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
        }
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        if (eventRegistration != null) {
            eventRegistration.unregister();
            eventRegistration = null;
        }
    }

    private void init() {
        float border = 5f;
        float gab = 5;
        double size[][] = { { border, 350, gab, 300, border }, // cols
                { border, 0.35, 0.35, gab, TableLayout.FILL, border } // rows
        };

        TableLayout tableLayout = new TableLayout(size);
        setLayout(tableLayout);

        add(compFactory.createSkillTableBox(), "1 , 1 , 1 , 2 ");
        add(compFactory.createAttackSkillBox(), "3 ,  1 ");
        add(compFactory.createBuffSkillBox(), "3 , 2");
        add(compFactory.createSettingBox(), "1 , 4 , 3 , 4");

        binding();
        
        // Initial load if data exists
        if (viewModel.getTrainer() != null) {
            updateTrainerMasteries();
            updateSkillList();
            updateAttackSkillList();
        }
    }

    private void binding() {
        this.comMasteryFilter.addItemListener(this);
        this.checkAttackSkills.addItemListener(this);
        this.checkBuffSkills.addItemListener(this);
        this.comAttackSkillFilter.addItemListener(this);

        this.tblSkill.getSelectionModel().addListSelectionListener(this);
        
        this.btnAddAttackSkill.addActionListener(this);
        this.btnRemoveAttackSkill.addActionListener(this);
        this.btnMoveUpAttackSkill.addActionListener(this);
        this.btnMoveDownAttackSkill.addActionListener(this);
        
        this.tblAttackSkill.getSelectionModel().addListSelectionListener(this);
    }
    
    @Override
    public void handleEvent(Event event) {
        // React to OSGi events dispatched to this component (needs registration)
        // For now, we assume MachineUIInstance might register this, or we rely on ViewModel
        // Actually, let's use ViewModel property changes if available, but for now strict port:
        String topic = event.getTopic();
        if (topic.endsWith("TrainerLoadedEvent")) {
             updateTrainerMasteries();
             updateSkillList();
             updateAttackSkillList();
        } else if (topic.endsWith("TrainerSkillsUpdatedEvent")) {
             updateSkillList();
             updateAttackSkillList();
        }
    }

    private void updateTrainerMasteries() {
        ITrainer trainer = viewModel.getTrainer();
        if (trainer == null) return;

        comMasteryFilter.removeAllItems();
        comMasteryFilter.addItem(new MasteryEntry(-1, "All"));

        trainer.getMastryList().getAllMasteries().stream()
               .filter((m) -> m.getMasteryLevel() > 0)
               .forEach((m) -> {
            comMasteryFilter.addItem(new MasteryEntry(m.getMasteryID(), m.toString()));
        });
    }

    private void updateSkillList() {
        ITrainer trainer = viewModel.getTrainer();
        if (trainer == null) return;

        this.tblSkill.getSelectionModel().clearSelection();
        this.tblSkillModel.removeAll();
        MasteryEntry masteryArg = (MasteryEntry) this.comMasteryFilter.getSelectedItem();

        trainer.getSkills().stream().filter((s) -> {
            if (masteryArg == null || masteryArg.getId() == -1)
                return true;
            else if (masteryArg.getId() == s.getMasteryId())
                return true;
            else
                return false;
        }).filter((skill) -> {
            SkillType skillType = skill.getType();
            return (isBuffSkill(skillType) && checkBuffSkills.isSelected())
                    || (isAttackSkill(skillType) && checkAttackSkills.isSelected());
        }).map(skill -> new SkillTableEntry(null, skill.getName(), skill.getSkillLvl()))
          .forEach((skill) -> {
            this.tblSkillModel.addSkill(skill);
        });
    }

    private void updateAttackSkillList() {
        ITrainer trainer = viewModel.getTrainer();
        Settings settings = context.getSettings();
        if (trainer == null || settings == null) return;

        MonsterTypeEntry type = (MonsterTypeEntry) this.comAttackSkillFilter.getSelectedItem();
        if (type == null) return;

        this.tblAttackSkill.getSelectionModel().clearSelection();
        this.tblAttackSkillModel.removeAll();
        
        List<SkillTableEntry> skills = settings.getAttakListFor(type.getType())
                .stream().map((skillName) -> {
                    return trainer.findSkill(skillName).map((skill) -> {
                        return new SkillTableEntry(null, skill.getName(), skill.getSkillLvl());
                    }).orElse(new SkillTableEntry(null, skillName, 0));
                }).collect(Collectors.toList());
        this.tblAttackSkillModel.addAll(skills);
    }
    
    // Event Handlers
    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (source == this.comMasteryFilter || source == this.checkAttackSkills || source == this.checkBuffSkills) {
            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                updateSkillList();
            }
        } else if (source == this.comAttackSkillFilter) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateAttackSkillList();
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        ITrainer trainer = viewModel.getTrainer();
        if (trainer == null) return;

        Object source = e.getSource();

        if (source == this.tblSkill.getSelectionModel()) {
            int viewRow = this.tblSkill.getSelectedRow();
            if (viewRow < 0) return;

            SkillTableEntry skillEntry = this.tblSkillModel.getRowObject(viewRow);
            trainer.findSkill(skillEntry.getSkillName()).ifPresent((target) -> {
                SkillType skillType = target.getType();
                boolean isBuffSkill = isBuffSkill(skillType) && !isAttackSkill(skillType);
                this.btnAddAttackSkill.setEnabled(!isBuffSkill && !this.tblAttackSkillModel.contains(target.getName()));
                this.btnAddBuffSkill.setEnabled(isBuffSkill && !this.tblBuffSkillModel.contains(target.getName()));
            });

        } else if (source == this.tblAttackSkill.getSelectionModel()) {
            int selectedRow = this.tblAttackSkill.getSelectedRow();
            this.btnRemoveAttackSkill.setEnabled(selectedRow >= 0);
            this.btnMoveDownAttackSkill.setEnabled(selectedRow >= 0 && selectedRow < this.tblAttackSkill.getRowCount() - 1);
            this.btnMoveUpAttackSkill.setEnabled(selectedRow > 0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        
        if (source == this.btnAddAttackSkill) {
            SkillTableEntry selectedSkill = getSelectedSkill();
            if (selectedSkill == null) return;

            if (!this.tblAttackSkillModel.contains(selectedSkill.getSkillName())) {
                this.tblAttackSkillModel.addSkill(selectedSkill);
                // settings.addSkill... REMOVED (Draft Mode)
                this.btnAddAttackSkill.setEnabled(false);
            }
        } else if (source == this.btnRemoveAttackSkill) {
            int selectedRow = this.tblAttackSkill.getSelectedRow();
            if (selectedRow >= 0) {
                this.tblAttackSkillModel.removeRow(selectedRow);
                 // settings.remove... REMOVED (Draft Mode)
            }
        } else if (source == this.btnMoveUpAttackSkill) {
             int selectedRow = this.tblAttackSkill.getSelectedRow();
             if (selectedRow <= 0) return;
             this.tblAttackSkillModel.move(selectedRow, selectedRow - 1);
             // settings.swap... REMOVED (Draft Mode)
             this.tblAttackSkill.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
        } else if (source == this.btnMoveDownAttackSkill) {
             int selectedRow = this.tblAttackSkill.getSelectedRow();
             if (selectedRow >= this.tblAttackSkill.getRowCount() - 1) return;
             this.tblAttackSkillModel.move(selectedRow, selectedRow + 1);
             // settings.swap... REMOVED (Draft Mode)
             this.tblAttackSkill.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
        } else if (e.getActionCommand().equals("Save")) {
             saveChanges();
        }
    }
    
    private void saveChanges() {
        if (context == null) return; // Should not happen
        
        org.sokybot.settings.UpdateAttackSkillsCommand cmd = 
            new org.sokybot.settings.UpdateAttackSkillsCommand(
                context.fullName(),
                getSelectedMonsterType(),
                tblAttackSkillModel.getAllSkillNames()
            );
            
         // Send Event
         // We need EventAdmin. Since we are in UI and have BundleContext (or IMachineContext -> IEngine -> sendEvent?)
         // context.getEngine().sendEvent() sends to StateMachine. 
         // We want OSGi EventAdmin.
         // We can use context.getEngine().sendEvent() if we map it? No.
         // We injected BundleContext into SkillPage -> SkillTab.
         // We can lookup EventAdmin service.
         
         if (bundleContext != null) {
             org.osgi.framework.ServiceReference<org.osgi.service.event.EventAdmin> ref = 
                 bundleContext.getServiceReference(org.osgi.service.event.EventAdmin.class);
             if (ref != null) {
                 org.osgi.service.event.EventAdmin eventAdmin = bundleContext.getService(ref);
                 java.util.Map<String, Object> props = new java.util.HashMap<>();
                 props.put("command", cmd);
                 props.put("machineId", context.fullName());
                 
                 // Add Group/Machine Name for SettingsManager
                 Settings settings = context.getSettings();
                 if (settings != null) {
                     props.put("groupName", settings.getGroupName());
                     props.put("machineName", settings.getTrainerName());
                 }
                 
                 eventAdmin.postEvent(new Event("sokybot/command/settings/update", props));
                 bundleContext.ungetService(ref);
             }
         }
    }

    private SkillTableEntry getSelectedSkill() {
        int selectedRow = this.tblSkill.getSelectedRow();
        if (selectedRow >= 0) {
            return this.tblSkillModel.getRowObject(selectedRow);
        }
        return null;
    }

    public MonsterType getSelectedMonsterType() {
        return ((MonsterTypeEntry) this.comAttackSkillFilter.getSelectedItem()).getType();
    }

    private boolean isBuffSkill(SkillType skillType) {
        return skillType != SkillType.Other && skillType != SkillType.Passive && skillType != SkillType.Imbue;
    }

    private boolean isAttackSkill(SkillType skillType) {
        return skillType == SkillType.Other;
    }
}
