package com.myagent.web;

import com.alibaba.fastjson.JSON;
import com.myagent.web.builder.ValueBuilder;
import com.myagent.web.model.RemoteValue;
import com.myagent.web.vm.VirtualMachineManager;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EventRunner implements ApplicationRunner {

    @Autowired
    private List<ValueBuilder> valueBuilders;

    private static final String[] INCLUDE_CLASS = {"^java\\..+", "^com\\.alipay\\..+"};

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Thread thread = new Thread(() -> {
            while ((true)) {
                EventSet eventSet = VirtualMachineManager.takeEvent();
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
        }, "eventRunner");
        thread.start();
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
            Map<Field, Value> values = objectReference.getValues(fields);
            values.entrySet().stream()
                    .filter(e -> variableFileter(e.getKey().typeName()))
                    .forEach(entry -> variables.add(covertValue(entry.getKey().name(), entry.getValue(), num + 1)));
            return remoteValue;
        }

        return new RemoteValue(name, value == null ? null : value.type().name(), null);
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
}
