package com.myagent.web.builder;

import com.myagent.web.model.RemoteValue;

public interface ValueBuilder {

    boolean support(String name, com.sun.jdi.Value value);

    RemoteValue build(String name, com.sun.jdi.Value value);
}
