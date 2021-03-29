package com.myagent.web.controller;

import com.alibaba.fastjson.JSON;
import com.myagent.web.builder.ValueBuilder;
import com.myagent.web.model.RemoteValue;
import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.tools.jdi.SocketAttachingConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class SendAgentController {

    private static final String URL_FORMART = "http://%s:%s/resource/class/%s";

    @Value("${server.host:localhost}")
    private String host;

    @Value("${server.port:80}")
    private String port;

    @Value("${java.library.path}")
    private String libPath;

    @Autowired
    private List<ValueBuilder> valueBuilders;

    private static VirtualMachine virtualMachine = null;

    @GetMapping("/send")
    public void sendAgent(@RequestParam String className, @RequestParam Integer line) {
        try {
            // 获取SocketAttachingConnector,连接其它JVM称之为附加(attach)操作
            if (virtualMachine == null) {
                VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
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
                hostArg.setValue("emcooperate-5.gz00b.dev.alipay.net");
                portArg.setValue("8000");

                virtualMachine = sac.attach(arguments);

            }

            EventRequestManager eventRequestManager = virtualMachine.eventRequestManager();
            ClassType clazz = (ClassType) virtualMachine.classesByName(className).get(0);
            Location location = clazz.locationsOfLine(line).get(0);
            BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.enable();


            EventQueue eventQueue = virtualMachine.eventQueue();
            EventSet eventSet = eventQueue.remove();
            try {
                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext()) {
                    Event event = eventIterator.next();
                    try {
                        execute(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                eventSet.resume();
            }

        } catch (IOException
                | IllegalConnectorArgumentsException
                | AbsentInformationException
                | InterruptedException e) {
            e.printStackTrace(System.err);
        } finally {
            if (virtualMachine != null) {
                virtualMachine.dispose();
                virtualMachine = null;
            }
        }
    }

    private void execute(Event event)
            throws IncompatibleThreadStateException, AbsentInformationException {
        if (event instanceof BreakpointEvent) {

            BreakpointEvent breakpointEvent = (BreakpointEvent) event;
            ThreadReference threadReference = breakpointEvent.thread();
            StackFrame stackFrame = threadReference.frame(0);

            stackFrame.visibleVariables();

            // 获取date变量
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            localVariables.forEach(
                    var -> {
                        com.sun.jdi.Value value = stackFrame.getValue(var);
                        Object o = covertValue(var.name(), value, 0);
                        store(o);
                    });
        }
    }

    private void store(Object o) {
        System.out.println(JSON.toJSONString(o));
    }

    private RemoteValue covertValue(String name, com.sun.jdi.Value value, int num) {
        if (!CollectionUtils.isEmpty(valueBuilders)) {
            ValueBuilder valueBuilder = valueBuilders.stream().filter(e -> e.support(name, value)).findAny().orElse(null);
            if (valueBuilder != null) {
                return valueBuilder.build(name, value);
            }
        }
        if (num >= 3) {
            return new RemoteValue(name, value == null ? null : value.type().name(), "......");
        }
        if (value instanceof IntegerValue) {
            return new RemoteValue(name, value.type().name(), ((IntegerValue) value).value());
        }
        if (value instanceof ByteValue) {
            return new RemoteValue(name, value.type().name(), ((ByteValue) value).value());
        }
        if (value instanceof LongValue) {
            return new RemoteValue(name, value.type().name(), ((LongValue) value).value());
        }
        if (value instanceof BooleanValue) {
            return new RemoteValue(name, value.type().name(), ((BooleanValue) value).value());
        }
        if (value instanceof DoubleValue) {
            return new RemoteValue(name, value.type().name(), ((DoubleValue) value).value());
        }
        if (value instanceof CharValue) {
            return new RemoteValue(name, value.type().name(), ((CharValue) value).value());
        }
        if (value instanceof ShortValue) {
            return new RemoteValue(name, value.type().name(), ((ShortValue) value).value());
        }
        if (value instanceof FloatValue) {
            return new RemoteValue(name, value.type().name(), ((FloatValue) value).value());
        }

        if (value instanceof StringReference) {
            return new RemoteValue(name, value.type().name(), ((StringReference) value).value());
        }

        if (value instanceof ArrayReference) {
            ArrayReference arrayReference = (ArrayReference) value;
            List<com.sun.jdi.Value> values = arrayReference.getValues();
            Object[] array = new Object[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = covertValue(null, values.get(i), num + 1);
            }
            return new RemoteValue(name, value.type().name(), array);
        }

        if (value instanceof ClassObjectReference) {
            RemoteValue remoteValue = new RemoteValue(name, value.type().name(), new ArrayList<>());
            List<RemoteValue> variables = remoteValue.getVariables();
            ClassObjectReference objectReference = (ClassObjectReference) value;
            List<Field> fields = objectReference.referenceType().visibleFields();
            Map<Field, com.sun.jdi.Value> values = objectReference.getValues(fields);
            values.forEach((f, v) -> variables.add(covertValue(f.name(), v, num + 1)));
            return remoteValue;
        }

        if (value instanceof ObjectReference) {
            RemoteValue remoteValue = new RemoteValue(name, value.type().name(), new ArrayList<>());
            List<RemoteValue> variables = remoteValue.getVariables();
            ObjectReference objectReference = (ObjectReference) value;
            List<Field> fields = objectReference.referenceType().visibleFields();
            Map<Field, com.sun.jdi.Value> values = objectReference.getValues(fields);
            values.forEach((f, v) -> variables.add(covertValue(f.name(), v, num + 1)));
            return remoteValue;
        }

        return new RemoteValue(name, value == null ? null : value.type().name(), null);
    }
}
