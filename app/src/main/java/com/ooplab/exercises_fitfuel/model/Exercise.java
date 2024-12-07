package com.ooplab.exercises_fitfuel.model;

public class Exercise {
    private String id;
    private String name;
    private String parentId;

    public Exercise(String id, String name, String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
}
