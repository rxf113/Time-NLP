package com.time.nlp;

public class TimerModel {

    private String name;

    private String value;

    public TimerModel(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TimerModel{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}