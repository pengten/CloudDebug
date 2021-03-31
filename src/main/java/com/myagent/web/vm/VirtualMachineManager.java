package com.myagent.web.vm;

import com.myagent.web.AttachException;
import com.myagent.web.cache.LocalCache;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.tools.jdi.SocketAttachingConnector;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class VirtualMachineManager {

    private static final LocalCache<VirtualMachine> vmCache = new LocalCache<>();

    public static VirtualMachine attach(String host, String port) {
        String cacheKey = host + ":" + port;
        VirtualMachine virtualMachine = vmCache.get(cacheKey);
        if (virtualMachine == null) {
            synchronized (vmCache) {
                if (vmCache.get(cacheKey) == null) {
                    try {
                        virtualMachine = create(host, port);
                    } catch (IOException | IllegalConnectorArgumentsException e) {
                        throw new AttachException("连接远程虚拟机失败！", e);
                    }
                    vmCache.put(cacheKey, virtualMachine);
                }
            }
        }
        return virtualMachine;
    }

    private static VirtualMachine create(String host, String port) throws IOException, IllegalConnectorArgumentsException {
        com.sun.jdi.VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        List<AttachingConnector> connectors = vmm.attachingConnectors();
        SocketAttachingConnector sac = null;
        for (AttachingConnector ac : connectors) {
            if (ac instanceof SocketAttachingConnector) {
                sac = (SocketAttachingConnector) ac;
            }
        }
        assert sac != null;
        // 设置好主机地址，端口信息
        Map<String, Connector.Argument> arguments = sac.defaultArguments();
        Connector.Argument hostArg = arguments.get("hostname");
        Connector.Argument portArg = arguments.get("port");
        hostArg.setValue(host);
        portArg.setValue(port);
        return sac.attach(arguments);
    }
}
