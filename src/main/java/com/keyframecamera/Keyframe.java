package com.keyframecamera;

import java.util.UUID;
import lombok.Data;

@Data
public class Keyframe
{
	private String id;
	private double focalX;
	private double focalY;
	private double focalZ;
	private double pitch;
	private double yaw;
	private int scale;
	private EaseType ease;

	public Keyframe(double focalX, double focalY, double focalZ, double pitch, double yaw, int scale, EaseType ease)
	{
		this.id = UUID.randomUUID().toString();
		this.focalX = focalX;
		this.focalY = focalY;
		this.focalZ = focalZ;
		this.pitch = pitch;
		this.yaw = yaw;
		this.scale = scale;
		this.ease = ease;
	}

	private static final double RADIANS_TO_JAU_FACTOR = 2048.0 / (2 * Math.PI);

	public static int radiansToJau(double radians)
	{
		return (int) Math.round(radians * RADIANS_TO_JAU_FACTOR) % 2048;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	public String toString()
	{
		return "Keyframe{" +
			"id='" + id + '\'' +
			", focalX=" + focalX +
			", focalY=" + focalY +
			", focalZ=" + focalZ +
			", pitch=" + pitch +
			", yaw=" + yaw +
			", scale=" + scale +
			", ease=" + ease +
			'}';
	}

}
