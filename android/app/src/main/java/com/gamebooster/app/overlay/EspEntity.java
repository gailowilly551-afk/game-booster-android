package com.gamebooster.app.overlay;

/**
 * Data model representing a tracked game entity for ESP rendering.
 */
public class EspEntity {
    public String name;
    public float health;
    public float maxHealth;
    public float distance;
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean isEnemy;
    public int color;

    public EspEntity(String name, float health, float maxHealth, float distance,
                     float x, float y, float width, float height, boolean isEnemy, int color) {
        this.name = name;
        this.health = health;
        this.maxHealth = maxHealth;
        this.distance = distance;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isEnemy = isEnemy;
        this.color = color;
    }

    public float getHealthPercentage() {
        if (maxHealth <= 0f) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, health / maxHealth));
    }
}
