package com.myagent.web.builder.impl;

import com.myagent.web.builder.ValueBuilder;
import com.myagent.web.model.RemoteValue;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import org.springframework.stereotype.Component;

@Component
public class IntegerBuilder implements ValueBuilder {


    @Override
    public boolean support(String name, Value value) {
        return value != null && Integer.class.getName().equals(value.type().name());
    }

    @Override
    public RemoteValue build(String name, Value value) {
        ObjectReference objectReference = (ObjectReference) value;
        Field field = objectReference.referenceType().fieldByName("value");
        IntegerValue intValue = (IntegerValue) objectReference.getValue(field);
        return new RemoteValue(name, Integer.class.getName(), intValue.value());
    }
}
