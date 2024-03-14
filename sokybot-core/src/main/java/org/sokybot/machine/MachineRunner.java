package org.sokybot.machine;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.slf4j.Logger;
import org.sokybot.IMachinePageViewer;
import org.sokybot.IPageViewer;
import org.sokybot.app.AppConstants;
import org.sokybot.app.machinebuilder.GameDataPanel;
import org.sokybot.app.machinebuilder.MachineDataPanel;
import org.sokybot.machine.dashboard.MachineDashboard;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.repo.SettingRepo;
import org.sokybot.service.ISroDAO;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.statemachine.StateMachine;

import com.formdev.flatlaf.icons.FlatSearchIcon;

@Configuration
public class MachineRunner {

	@Autowired
	ApplicationContext ctx;

	@Autowired
	Logger log;

	@Autowired
	StateMachine<MachineState, IMachineEvent> machine;

	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String machineName;

	@Value("${" + AppConstants.GROUP_NAME + "}")
	private String groupName;

	@Bean
	@Order(1)
	ApplicationRunner resetSettings() {
		return args -> {
			if (args.containsOption(AppConstants.MACHINE_PRIMARY_RESET)) {

				// Order order = Order.getResetOrder(machine.getGroup().getName(),
				// machine.getMachineName()) ;
				// this.groupCtx.getBean(MachineBuilderDialog.class ,null ,
				// order).setVisible(true);
				GameDataPanel gameDataPanel = this.ctx.getBean(GameDataPanel.class);
				setGameData(gameDataPanel);
				MachineDataPanel machineDataPanel = this.ctx.getBean(MachineDataPanel.class);
				machineDataPanel.setTrainerName(this.machineName);
				machineDataPanel.setTrainerNameEnabled(false);

				Box content = createDialogContent(gameDataPanel, machineDataPanel);
				content.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

				JPanel contentp = new JPanel();
				contentp.setLayout(new BorderLayout());
				contentp.setBorder(BorderFactory.createEtchedBorder());
				contentp.add(content, BorderLayout.CENTER);

				java.awt.Component[] comps = new java.awt.Component[] {
						new JLabel("Sokybot cannot access some essential settings , please reenter it"), contentp

				};

				JOptionPane opt = new JOptionPane(comps, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

				JDialog dialog = new JDialog();
				opt.setOptions(new String[] { "Save" });
				opt.addPropertyChangeListener((prop) -> {
					if (prop.getPropertyName() == JOptionPane.VALUE_PROPERTY) {
						Object v = prop.getNewValue();
						if (v instanceof String) {
							String optV = (String) v;
							if (optV.equals("Save")) {

								try {
									Settings s = getTargetObject(ctx.getBean(Settings.class), Settings.class);
									updateSettings(s, gameDataPanel, machineDataPanel);
									ctx.getBean(SettingRepo.class).save(s);
									dialog.dispose();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				});
				dialog.setTitle("Machine Primary Reset [" + this.machineName + "]");

				dialog.setAlwaysOnTop(true);
				dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				dialog.setLocationRelativeTo(null);
				dialog.setResizable(false);
				dialog.setContentPane(opt);
				dialog.setModal(true);
				dialog.pack();
				dialog.setVisible(true);

			}

		};
	}

	private <T> T getTargetObject(Object proxy, Class<T> targetClass) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else if (AopUtils.isCglibProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return (T) proxy;
		}
	}

	private void updateSettings(Settings s, GameDataPanel gameDataPanel, MachineDataPanel machineDataPanel) {

		s.setTargetGateway(gameDataPanel.getSelectedHost());
		if (machineDataPanel.isAutoLogin()) {
			s.setAutoLogin(machineDataPanel.isAutoLogin());
			s.setUsername(machineDataPanel.getUsername());
			s.setPassword(machineDataPanel.getPassword());
			s.setPasscode(machineDataPanel.getPasscode());
			s.setTargetAgent(machineDataPanel.getAgentServerName());
		}

	}

	private Box createDialogContent(GameDataPanel gdp, MachineDataPanel mdp) {
		Box b = Box.createVerticalBox();
		b.add(titledBorder(gdp, "Game Data [ " + this.groupName + " ]"));
		b.add(titledBorder(mdp, "Machine Data [ " + this.machineName + " ]"));

		return b;

	}

	private JPanel titledBorder(JPanel panel, String title) {
		JPanel border = new JPanel();
		border.setLayout(new BorderLayout());
		border.add(panel, BorderLayout.CENTER);
		border.setBorder(BorderFactory.createTitledBorder(title));
		return border;
	}

	private void setGameData(GameDataPanel gameData) {
		ISroDAO dao = this.ctx.getBean(ISroDAO.class);
		gameData.setVersion(dao.getVersion());
		gameData.setPort(dao.getPort());
		gameData.setDivHosts(dao.getDivHosts());
		gameData.setCountry(dao.getCountry().orElse("UNKNOWN"));
		gameData.setLanguage(dao.getLanguage().orElse("UNKNOWN"));
		gameData.setGameLocal(dao.getLocal().orElse((byte) -1));
	}

	@Bean
	@Order(2)
	ApplicationRunner installMachineGUIComponents(IMachinePageViewer machinePageViewer,
			@Qualifier("machineGroupPageViewer") IPageViewer pageViewer) {
		return args -> {

			JPanel tmpPanel = new JPanel(new BorderLayout());
			tmpPanel.add(new JLabel(machineName));
			pageViewer.registerPage(machineName, new FlatSearchIcon(), tmpPanel);
			machinePageViewer.registerDashboard("Trainer", this.ctx.getBean(MachineDashboard.class));
		};
	}

	@Bean
	@Order(3)
	ApplicationRunner installMachinePages(List<IMachinePage> pages, IMachinePageViewer pageViewer) {
		return args -> {
			pages.forEach((p) -> {
				pageViewer.registerPage(p.getName(), p.getIcon(), p);
			});
		};
	}

	@Bean
	@Order(4)
	ApplicationRunner initializeUserConfig() {
		return args -> {

			Settings config = this.ctx.getBean(Settings.class);

			if (args.containsOption(AppConstants.MACHINE_AUTO_LOGIN)) {
				config.setAutoLogin(true);
			}
			// config.setAutoLogin(args.containsOption(Constants.MACHINE_AUTO_LOGIN));

			args.getNonOptionArgs()
					.stream()
					.map((l) -> l.split("="))

					.forEach((pair) -> {
						if (pair.length == 2) {
							switch (pair[0]) {

							case AppConstants.MACHINE_TARGET_GATEWAY:
								config.setTargetGateway(pair[1]);
								break;
							case AppConstants.MACHINE_USER_NAME:
								config.setUsername(pair[1]);
								break;
							case AppConstants.MACHINE_PASSWORD:
								config.setPassword(pair[1]);
								break;
							case AppConstants.MACHINE_PASSCODE:
								config.setPasscode(pair[1]);
								break;
							case AppConstants.MACHINE_TARGET_AGENT:
								config.setTargetAgent(pair[1]);
								break;
							}
						}
					});

			// this.executor.execute(()->{

			boolean isAcceptable = machine.sendEvent(UserAction.CONFIG_COMMIT);
			log.info("CONFIG_COMMIT event is acceptable {} ", isAcceptable);
			// });
		};
	}

	@Bean
	@Order(5)
	ApplicationRunner openMapViewer(ViewerPanel vp) {

		return (args) -> {
			JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());

			frame.add(vp, BorderLayout.CENTER);

			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);

		};

	}
}
