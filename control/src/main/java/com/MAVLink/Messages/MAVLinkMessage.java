
package com.MAVLink.Messages;

import java.io.Serializable;

import com.MAVLink.MAVLinkPacket;

public abstract class MAVLinkMessage implements Serializable {
    private static final long serialVersionUID = -7754622750478538539L;
    // The MAVLink message classes have been changed to implement Serializable,
    // this way is possible to pass a mavlink message trought the Service-Acctivity interface

    /**
     * Simply a common interface for all MAVLink Messages
     */

    public static int sysid;//����ϵͳ��id��������ͬһ���������ֲ�ͬ���������ڴ˹̶�255
    public static int compid;//ֲ�ͬ������ڴ˹̶�190
    public int msgid;

    public abstract MAVLinkPacket pack();

    public abstract void unpack(MAVLinkPayload payload);
}
