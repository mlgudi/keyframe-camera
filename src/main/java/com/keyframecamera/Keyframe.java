package com.keyframecamera;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Keyframe
{
    private long duration;
    private double focalX;
    private double focalY;
    private double focalZ;
    private double pitch;
    private double yaw;
    private EaseType ease;

    public String toString()
    {
        return String.format(
                "%d,%f,%f,%f,%f,%f,%s",
                duration,
                focalX,
                focalY,
                focalZ,
                pitch,
                yaw,
                ease
        );
    }
}
