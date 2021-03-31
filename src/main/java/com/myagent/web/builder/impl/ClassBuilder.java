package com.myagent.web.builder.impl;

import com.myagent.web.builder.ValueBuilder;
import com.myagent.web.model.RemoteValue;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.Value;
import org.springframework.stereotype.Component;

@Component
public class ClassBuilder implements ValueBuilder {
    @Override
    public boolean support(String name, Value value) {
        return value != null && Class.class.getName().equals(value.type().name());
    }

    @Override
    public RemoteValue build(String name, Value value) {
        return new RemoteValue(name, value.type().name(), ((ClassObjectReference) value).reflectedType().name());
    }
}
