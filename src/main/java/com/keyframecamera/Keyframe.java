package com.keyframecamera;

import lombok.Data;

import java.util.UUID;

@Data
public class Keyframe
{
    private String id;
    private long duration;
    private double focalX;
    private double focalY;
    private double focalZ;
    private double pitch;
    private double yaw;
    private int scale;
    private EaseType ease;

    public Keyframe(long duration, double focalX, double focalY, double focalZ, double pitch, double yaw, int scale, EaseType ease)
    {
        this.id = UUID.randomUUID().toString();
        this.duration = duration;
        this.focalX = focalX;
        this.focalY = focalY;
        this.focalZ = focalZ;
        this.pitch = pitch;
        this.yaw = yaw;
        this.scale = scale;
        this.ease = ease;
    }

    public String toString()
    {
        return String.format(
                "%d,%f,%f,%f,%f,%f,%d,%s",
                duration,
                focalX,
                focalY,
                focalZ,
                pitch,
                yaw,
                scale,
                ease
        );
    }
}
