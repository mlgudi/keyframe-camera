package com.keyframecamera;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SequenceIO
{
    public static String serialize(Sequence sequence)
    {
        StringBuilder sb = new StringBuilder();

        for (Keyframe keyframe : sequence.getKeyframes())
        {
            sb.append(sequence.getTimestamp(keyframe)).append(",");
            sb.append(keyframe.getFocalX()).append(",");
            sb.append(keyframe.getFocalY()).append(",");
            sb.append(keyframe.getFocalZ()).append(",");
            sb.append(keyframe.getPitch()).append(",");
            sb.append(keyframe.getYaw()).append(",");
            sb.append(keyframe.getScale()).append(",");
            sb.append(keyframe.getEase().name()).append("\n");
        }

        return sb.toString();
    }

    public static Sequence deserialize(String data, KeyframeCameraConfig config)
    {
        Sequence sequence = new Sequence(config);

        for (String line : data.split("\n")) {
            String[] parts = line.split(",");
            long ms = Long.parseLong(parts[0]);
            double focalX = Double.parseDouble(parts[1]);
            double focalY = Double.parseDouble(parts[2]);
            double focalZ = Double.parseDouble(parts[3]);
            double pitch = Double.parseDouble(parts[4]);
            double yaw = Double.parseDouble(parts[5]);
            int scale = Integer.parseInt(parts[6]);
            EaseType ease = EaseType.valueOf(parts[7]);

            Keyframe keyframe = new Keyframe(focalX, focalY, focalZ, pitch, yaw, scale, ease);
            sequence.add(keyframe, ms);
        }

        return sequence;
    }

    public static boolean save(Sequence sequence, String filename)
    {
        try
        {
            Files.write(Paths.get(filename), serialize(sequence).getBytes());
            return true;
        }
        catch (IOException ignored)
        {
            return false;
        }
    }

    public static Sequence load(String filename, KeyframeCameraConfig config)
    {
        try
        {
            return deserialize(new String(Files.readAllBytes(Paths.get(filename))), config);
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

}
