package org.sokybot.api.events;

/**
 * Event fired for weather updates (opcode 0x3809).
 */
public class WeatherUpdateEvent implements IGameEvent {
    
    public enum WeatherType {
        CLEAR,
        CLOUDY,
        RAIN,
        SNOW,
        FOG
    }
    
    private final String fullName;
    private final long timestamp;
    private final WeatherType weatherType;
    private final int intensity;
    
    public WeatherUpdateEvent(String fullName, WeatherType weatherType, int intensity) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.weatherType = weatherType;
        this.intensity = intensity;
    }
    
    public WeatherUpdateEvent(String fullName, byte weatherCode, int intensity) {
        this(fullName, mapWeatherType(weatherCode), intensity);
    }
    
    private static WeatherType mapWeatherType(byte code) {
        switch (code) {
            case 0: return WeatherType.CLEAR;
            case 1: return WeatherType.CLOUDY;
            case 2: return WeatherType.RAIN;
            case 3: return WeatherType.SNOW;
            case 4: return WeatherType.FOG;
            default: return WeatherType.CLEAR;
        }
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public WeatherType getWeatherType() { return weatherType; }
    public int getIntensity() { return intensity; }
    
    @Override
    public String toString() {
        return String.format("WeatherUpdateEvent[%s, weather=%s, intensity=%d]", 
            fullName, weatherType, intensity);
    }
}
