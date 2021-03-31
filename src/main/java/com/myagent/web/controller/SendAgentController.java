package com.myagent.web.controller;

import com.alibaba.fastjson.JSON;
import com.myagent.web.builder.ValueBuilder;
import com.myagent.web.model.RemoteValue;
import com.myagent.web.vm.VirtualMachineManager;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String[] INCLUDE_CLASS = {"^java\\..+", "^com\\.alipay\\..+"};

    @GetMapping("/send")
    public void sendAgent(@RequestParam String className, @RequestParam String lines) {
        VirtualMachine virtualMachine = VirtualMachineManager.attach("emcooperate-5.gz00b.dev.alipay.net", "8000");
        try {
            EventRequestManager eventRequestManager = virtualMachine.eventRequestManager();
            ClassType clazz = (ClassType) virtualMachine.classesByName(className).get(0);
            List<Location> locations = clazz.allLineLocations();
            String[] lineArr = lines.split(",");
            locations.stream()
                    .filter(location -> Arrays.asList(lineArr).contains(Integer.toString(location.lineNumber())))
                    .forEach(location -> {
                        BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
                        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        breakpointRequest.enable();
                    });


            EventQueue eventQueue = virtualMachine.eventQueue();
            while (true) {
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
            }

        } catch (AbsentInformationException
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
            event.virtualMachine().eventRequestManager().deleteEventRequest(event.request());
            BreakpointEvent breakpointEvent = (BreakpointEvent) event;
            ThreadReference threadReference = breakpointEvent.thread();
            StackFrame stackFrame = threadReference.frame(0);
            stackFrame.visibleVariables();
            // 获取date变量
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            localVariables.stream()
                    .filter(e -> variableFileter(e.typeName()))
                    .forEach(
                            var -> {
                                com.sun.jdi.Value value = stackFrame.getValue(var);
                                Object o = covertValue(var.name(), value, 0);
                                store(o);
                            });
            StepRequest stepRequest = event.virtualMachine().eventRequestManager().createStepRequest(threadReference, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            stepRequest.enable();
        } else if (event instanceof StepEvent) {
            ThreadReference threadReference = ((StepEvent) event).thread();
            StackFrame stackFrame = threadReference.frame(0);
            stackFrame.visibleVariables();
            // 获取date变量
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            localVariables.stream()
                    .filter(e -> variableFileter(e.typeName()))
                    .forEach(
                            var -> {
                                com.sun.jdi.Value value = stackFrame.getValue(var);
                                Object o = covertValue(var.name(), value, 0);
                                store(o);
                            });
        }
    }

    private boolean variableFileter(String typeName) {
        for (String excludeClass : INCLUDE_CLASS) {
            Pattern pattern = Pattern.compile(excludeClass);
            Matcher matcher = pattern.matcher(typeName);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
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

        if (value instanceof ObjectReference) {
            RemoteValue remoteValue = new RemoteValue(name, value.type().name(), new ArrayList<>());
            List<RemoteValue> variables = remoteValue.getVariables();
            ObjectReference objectReference = (ObjectReference) value;
            List<Field> fields = objectReference.referenceType().visibleFields();
            Map<Field, com.sun.jdi.Value> values = objectReference.getValues(fields);
            values.entrySet().stream()
                    .filter(e -> variableFileter(e.getKey().typeName()))
                    .forEach(entry -> variables.add(covertValue(entry.getKey().name(), entry.getValue(), num + 1)));
            return remoteValue;
        }

        return new RemoteValue(name, value == null ? null : value.type().name(), null);
    }
}
