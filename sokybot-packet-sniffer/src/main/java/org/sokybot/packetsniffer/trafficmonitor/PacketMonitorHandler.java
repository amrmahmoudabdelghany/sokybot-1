package org.sokybot.packetsniffer.trafficmonitor;

import java.awt.event.ActionEvent;

import javax.swing.JButton;

import org.sokybot.packetsniffer.IPacketSnifferService;


public class PacketMonitorHandler implements IPacketMonitorHandler {


	private TablePacket selectedPacket ; 

	private IPacketSnifferService service ; 
	
	//private IPacketDetailsTap detailsTap;

	public PacketMonitorHandler(IPacketSnifferService service) {

		this.service = service ; 
		//this.detailsTap = detailsTap;

		/////////////////////////////// DEBUG//////////////////////////////
/*
		Timer timer = new Timer();
		Random rnd = new Random();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (isRunning) {
					ImmutablePacket packet = ImmutablePacket.wrap(
							new byte[] { (byte) 0x7F, (byte) 0x03, (byte) 0x1A, (byte) 0x7D, (byte) 0x06, (byte) 0x13,
									(byte) 0x1C, (byte) 0x0F, (byte) 0x18, (byte) 0x0B, (byte) 0x0C, (byte) 0x0E,
									(byte) 0x62, (byte) 0x7A, (byte) 0x18, (byte) 0x04, (byte) 0x0D, (byte) 0x0C,
									(byte) 0x09, (byte) 0x14, (byte) 0x09, (byte) 0x02, (byte) 0x00, (byte) 0x04,
									(byte) 0x12, (byte) 0x1C, (byte) 0x36 },
							((rnd.nextInt() % 2) == 0) ? PacketDirection.SERVER : PacketDirection.CLIENT,
							((rnd.nextInt() % 2) == 0) ? Encoding.PLAIN : Encoding.ENCRYPTED);

					model.addRow(packet);
				}
			}
		}, 1000, 1000);
*/
/////////////////////////////////////////////////////////////////////////////////////////////		

	}

	@Override
	public void handle(ActionEvent userAction) {

		UserAction action = UserAction.valueOf(userAction.getActionCommand());

		switch (action) {
		case RESUME:

			this.service.resumeMonitoring();
			if (userAction.getSource() instanceof JButton) {
				JButton btnOperate = (JButton) userAction.getSource();
				btnOperate.setText("Pasue");
				btnOperate.setActionCommand(UserAction.PAUSE.toString());
			}
			break;
		case PAUSE:
		
			this.service.pasueMonitoring();
			if (userAction.getSource() instanceof JButton) {
				JButton btnOperate = (JButton) userAction.getSource();
				btnOperate.setText("Resume");
				btnOperate.setActionCommand(UserAction.RESUME.toString());
			}
			break;

		case CLEAR:
			this.service.clearMonitor();
			break;

		case ANALYZE:

			
			
			this.service.analyze();
			break;


		case IGNORE:
 
			if(this.selectedPacket != null) { 
				this.service.ignore(this.selectedPacket.getPacket().getPacketSource() , this.selectedPacket.getPacket().getOpcode());
			}
			
			break ; 
		default:
			break;
		}

	}

	@Override
	public void onSelectPacket(TablePacket packet) {

	  this.selectedPacket = packet ; 
	}

	
	//@Override
	//public void onRecivePacket(String name  , ImmutablePacket packet) {
	
		///if (isRunning) 
	//		this.model.addRow(TablePacket.createTablePacket(name, packet));
	//}
}
