package com.webgame.backend.databases;

public class BetInfo {
    private int horse;
    private int amount;

    public BetInfo(int horse, int amount) {
        this.horse = horse;
        this.amount = amount;
    }

    public int getHorse() { return horse; }
    public void setHorse(int horse) { this.horse = horse; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "BetInfo{horse=" + horse + ", amount=" + amount + '}';
    }
}