package org.sokybot.service;

import org.sokybot.network.IPacketPublisher;

public interface IMachineService {

    String getName();

    IBotStateController getStateController();

    IPacketPublisher getPacketPublisher();

    INavigationService getNavigation();
    
    ISettingsService getSettingsService();
    
}
