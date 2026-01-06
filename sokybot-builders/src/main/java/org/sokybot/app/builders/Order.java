package org.sokybot.app.builders;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Order {

    public static enum OrderType {
        RESET,
        CREATE;
    }

    @Setter(value = AccessLevel.NONE)
    private final OrderType orderType;

    private Order(OrderType orderType) {
        this.orderType = orderType;
    }

    private String groupName;
    private String machineName;
    private String description;

    public void setGroupName(String groupName) {
        if (orderType == OrderType.RESET)
            throw new IllegalStateException("Trying to change group name while reset machine settings");
        this.groupName = groupName;
    }

    public void setMachineName(String machineName) {
        if (orderType == OrderType.RESET)
            throw new IllegalStateException("Trying to change machine name while reset machine settings");
        this.machineName = machineName;
    }

    public static Order getResetOrder(String groupName, String machineName) {
        if (groupName == null || groupName.isBlank())
            throw new IllegalArgumentException("Group name is required");
        if (machineName == null || machineName.isBlank())
            throw new IllegalArgumentException("Machine name is required");

        Order order = new Order(OrderType.RESET);
        order.groupName = groupName;
        order.machineName = machineName;
        order.description = "Reset machine settings";
        return order;
    }
}
