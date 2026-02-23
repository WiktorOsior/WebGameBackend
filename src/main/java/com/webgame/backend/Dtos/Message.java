package com.webgame.backend.Dtos;

import java.io.Serializable;
import java.util.Map;

public class Message implements Serializable {
    private String type;
    private String content;
    private int value;
    private int horse;
    private String authToken;
    private Map<String, Double> map;

    public Message() {}

    public Message(String type, String content, int value, int horse) {
        this.type = type;
        this.content = content;
        this.value = value;
        this.horse = horse;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getHorse() { return horse; }
    public void setHorse(int horse) { this.horse = horse; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public Map<String, Double> getMap() { return map; }
    public void setMap(Map<String, Double> map) { this.map = map; }
}