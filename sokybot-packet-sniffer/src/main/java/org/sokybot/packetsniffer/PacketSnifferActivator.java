package org.sokybot.packetsniffer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.sokybot.IGroupContext;
import org.sokybot.IGroupListener;
import org.sokybot.IMachineContext;
import org.sokybot.IMachineListener;
import org.sokybot.ISokybotContext;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.packetsniffer.packettracer.PacketTracerModel;

public class PacketSnifferActivator implements ServiceListener, BundleActivator, IGroupListener, IMachineListener {

	private BundleContext bndCtx;

	private ISokybotContext appCtx;

	private final Map<String, IPacketSubscription> subscriptions = new HashMap<>();

	public PacketSnifferActivator() {
	
		System.out.println("PacketSnifferActivator:: ON CREATE BUNDLE ACTIVATOR") ; 
	; 
	}
    
	@Override
	public void stop(BundleContext context) throws Exception {

		// unwire link with group ctx
		// unwire link with
		this.bndCtx = null;
		uninstallPacketSniffer(appCtx);
		
		System.out.println("On Stop Packet Sniffer");
	}

	@Override
	public void onGroupInstalled(IGroupContext groupCtx) {

		installPacketSniffer(groupCtx);
	}

	@Override
	public void onGroupUninstalled(IGroupContext groupCtx) {

	}

	@Override
	public void onMachineInstalled(IMachineContext machineCtx) {

		installPacketSniffer(machineCtx);

	}

	@Override
	public void start(BundleContext context) throws Exception {

		System.out.println("Starting PacketSniffer Bundle");
	 
		
		this.bndCtx = context;

		String filter = "(objectClass=" + ISokybotContext.class.getName() + ")";

		ServiceReference<ISokybotContext> ref = this.bndCtx.getServiceReference(ISokybotContext.class);

		this.appCtx = this.bndCtx.getService(ref);

		if (this.appCtx != null) {
			installPacketSniffer(this.appCtx);

		}
		this.bndCtx.addServiceListener(this, filter);
	}

	private void installPacketSniffer(ISokybotContext ctx) {

		if (ctx.isRunning()) {
			Stream.of(this.appCtx.getGroups()).filter((g) -> g.isRunning()).forEach(this::installPacketSniffer);
		
			ctx.addGroupListener(this);
		}
	}

	private void uninstallPacketSniffer(ISokybotContext ctx) {

		Stream.of(this.appCtx.getGroups()).filter((g) -> g.isRunning()).forEach(this::uninstallPacketSniffer);

		ctx.removeGroupListener(this);
	}

	private void uninstallPacketSniffer(IGroupContext ctx) {
		Stream.of(ctx.getMachines()).filter((m) -> m.isRunning()).forEach(this::uninstallPacketSniffer);

		ctx.removeMachineListener(this);

	}

	private void uninstallPacketSniffer(IMachineContext ctx) {

		String machineName = ctx.fullName();
		IPacketSubscription s = this.subscriptions.get(machineName);

		if (s != null) {
			s.cancel();
			this.subscriptions.remove(machineName);
		}

		ctx.machinePageViewer().removePage("Packet Sniffer");

	}

	private void installPacketSniffer(IGroupContext ctx) {

		Stream.of(ctx.getMachines()).filter((m)->m.isRunning()).forEach(this::installPacketSniffer);
		ctx.addMachineListener(this);
	}

	private void installPacketSniffer(IMachineContext ctx) {
		System.out.println("Install Packet Sniffer On Machine Named : " + ctx.fullName()) ; 
			PacketSniffer ps = new PacketSniffer();

			IPacketSubscription subscription = ctx.packetPublisher().subscribe(ps, IPacketPublisher.ANY);

			this.subscriptions.put(ctx.fullName(), subscription);

			ctx.machinePageViewer().registerPage("Packet Sniffer", null, ps);
		
	}

	@Override
	public void serviceChanged(ServiceEvent event) {

		if (this.bndCtx != null) {
			Object appCtxObj = this.bndCtx.getService(event.getServiceReference());

			if (appCtxObj instanceof ISokybotContext) {

				if (event.getType() == ServiceEvent.REGISTERED) {

					this.appCtx = (ISokybotContext) appCtxObj;
					installPacketSniffer(this.appCtx);
				}
			}

		}
	}

}
