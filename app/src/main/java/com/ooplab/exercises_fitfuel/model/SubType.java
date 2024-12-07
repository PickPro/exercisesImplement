package com.ooplab.exercises_fitfuel.model;

public class SubType {
    private String id;
    private String name;
    private String parentTypeId;

    public SubType(String id, String name, String parentTypeId) {
        this.id = id;
        this.name = name;
        this.parentTypeId = parentTypeId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentTypeId() { return parentTypeId; }
}
