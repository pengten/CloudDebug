package com.myagent.web.model;

import java.util.List;

public class RemoteValue {

    private String name;

    private String type;

    private Object value;

    private List<RemoteValue> variables;

    public RemoteValue(String name, String type, List<RemoteValue> variables) {
        this.name = name;
        this.type = type;
        this.variables = variables;
    }

    public RemoteValue(String name, String type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * Gets the value of value.
     *
     * @return the value of value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of value.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Gets the value of type.
     *
     * @return the value of type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the value of variables.
     *
     * @return the value of variables
     */
    public List<RemoteValue> getVariables() {
        return variables;
    }

    /**
     * Sets the value of variables.
     */
    public void setVariables(List<RemoteValue> variables) {
        this.variables = variables;
    }

    /**
     * Gets the value of name.
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of name.
     */
    public void setName(String name) {
        this.name = name;
    }
}
