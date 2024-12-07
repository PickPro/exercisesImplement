package com.ooplab.exercises_fitfuel.model;

// ExerciseType.java
public class ExerciseType {
    private String id;
    private String name;
    private boolean hasSubTypes;

    public ExerciseType(String id, String name, boolean hasSubTypes) {
        this.id = id;
        this.name = name;
        this.hasSubTypes = hasSubTypes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean hasSubTypes() { return hasSubTypes; }
}