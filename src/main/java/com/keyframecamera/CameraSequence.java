package com.keyframecamera;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Slf4j
public class CameraSequence
{
    @Getter
    @Setter
    private String name;
    private final Client client;
    private final KeyframeCameraConfig config;
    private final ConfigManager configManager;

    List<Keyframe> keyframes = new ArrayList<>();

    private int currentKeyframeIndex = 0;
    private long currentKeyframeStartTime = 0;

    private long startTime = 0;
    private long pauseStartTime = 0;
    private long totalPauseTime = 0;

    public CameraSequence(Client client, KeyframeCameraConfig config, ConfigManager configManager, String name)
    {
        this.client = client;
        this.config = config;
        this.configManager = configManager;
        this.name = name;
    }

    public List<Keyframe> getKeyframes()
    {
        return keyframes;
    }

    public Keyframe getKeyframe(int index)
    {
        return keyframes.get(index);
    }

    public long elapsed()
    {
        if (!config.playing())
        {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime - totalPauseTime;

        if (elapsedTime < 0)
        {
            return 0;
        }

        return elapsedTime;
    }

    public void addKeyframe()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        keyframes.add(
            new Keyframe(
                config.defaultKeyframeDuration(),
                client.getCameraFocalPointX(),
                client.getCameraFocalPointY(),
                client.getCameraFocalPointZ(),
                client.getCameraPitch(),
                client.getCameraYaw(),
                config.defaultKeyframeEase()
            )
        );
    }

    public void duplicateKeyframe(int index)
    {
        if (index < 0 || index >= keyframes.size())
        {
            return;
        }

        Keyframe keyframe = keyframes.get(index);
        keyframes.add(
            new Keyframe(
                keyframe.getDuration(),
                keyframe.getFocalX(),
                keyframe.getFocalY(),
                keyframe.getFocalZ(),
                keyframe.getPitch(),
                keyframe.getYaw(),
                keyframe.getEase()
            )
        );
    }

    public void overwriteKeyframe(int index)
    {
        Keyframe keyframe = keyframes.get(index);
        keyframes.set(
            index,
            new Keyframe(
                keyframe.getDuration(),
                client.getCameraFocalPointX(),
                client.getCameraFocalPointY(),
                client.getCameraFocalPointZ(),
                client.getCameraPitch(),
                client.getCameraYaw(),
                keyframe.getEase()
            )
        );
    }

    public void deleteKeyframe(int index)
    {
        if (index < 0 || index >= keyframes.size())
        {
            return;
        }

        keyframes.remove(index);
    }

    public void play()
    {
        if (client.getCameraMode() != 1)
        {
            client.setCameraMode(1);
        }
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", true);
        startTime = System.currentTimeMillis();
        totalPauseTime = 0;
        currentKeyframeIndex = 0;
    }

    public void stop()
    {
        startTime = 0;
        pauseStartTime = 0;
        totalPauseTime = 0;
        currentKeyframeIndex = 0;
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", false);
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);
    }

    public void togglePause()
    {
        if (!config.playing())
        {
            return;
        }

        if (config.paused())
        {
            if (client.getCameraMode() != 1)
            {
                client.setCameraMode(1);
            }
            totalPauseTime += System.currentTimeMillis() - pauseStartTime;
            configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);
        }
        else
        {
            pauseStartTime = System.currentTimeMillis();
            configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", true);
        }
    }

    public void tick() {
        if (!config.playing() || config.paused()) {
            return;
        }

        if (client.getCameraMode() != 1) {
            stop();
            return;
        }

        Keyframe currentKeyframe = keyframes.get(currentKeyframeIndex);

        if (elapsed() >= currentKeyframeStartTime + currentKeyframe.getDuration()) {
            if (keyframes.get(keyframes.size() - 1) == currentKeyframe) {
                if (config.loop()) {
                    startTime = System.currentTimeMillis();
                    totalPauseTime = 0;
                    currentKeyframeIndex = 0;
                    currentKeyframe = keyframes.get(0);
                } else {
                    stop();
                    return;
                }
            } else {
                currentKeyframe = keyframes.get(keyframes.indexOf(currentKeyframe) + 1);
                currentKeyframeStartTime = elapsed();
            }
        }

        long keyframeElapsed = elapsed() - currentKeyframeStartTime;
        Keyframe nextKeyframe = currentKeyframeIndex + 1 < keyframes.size() ? keyframes.get(currentKeyframeIndex + 1) : null;
        Keyframe interpolatedFrame = Ease.interpolate(currentKeyframe, nextKeyframe, keyframeElapsed);

        if (client.getCameraMode() != 1)
        {
            client.setCameraMode(1);
        }
        client.setCameraFocalPointX(interpolatedFrame.getFocalX());
        client.setCameraFocalPointY(interpolatedFrame.getFocalY());
        client.setCameraFocalPointZ(interpolatedFrame.getFocalZ());
        client.setCameraPitchTarget((int) interpolatedFrame.getPitch());
        client.setCameraYawTarget((int) interpolatedFrame.getYaw());
    }

    public void moveKeyframe(boolean up, int index)
    {
        if (index < 0 || index >= keyframes.size()) {
            return;
        }

        if (up && index == 0) {
            return;
        }

        if (!up && index == keyframes.size() - 1) {
            return;
        }

        Keyframe keyframe = keyframes.remove(index);
        keyframes.add(up ? index - 1 : index + 1, keyframe);
    }

    public void setCameraToKeyframe(Keyframe keyframe)
    {
        if (client.getCameraMode() != 1)
        {
            client.setCameraMode(1);
        }
        client.setCameraFocalPointX(keyframe.getFocalX());
        client.setCameraFocalPointY(keyframe.getFocalY());
        client.setCameraFocalPointZ(keyframe.getFocalZ());
        client.setCameraPitchTarget((int) keyframe.getPitch());
        client.setCameraYawTarget((int) keyframe.getYaw());
    }

    public void save()
    {
        Path sequencePath = KeyframeCameraPlugin.SEQUENCE_DIR.resolve(name + ".txt");

        File file = new File(sequencePath.toString());
        try {
            file.createNewFile();
        } catch (Exception e) {
            log.warn("Failed to create sequence file");
            return;
        }

        try {
            StringBuilder kf = new StringBuilder();
            for (Keyframe keyframe : keyframes) {
                kf.append(keyframe.toString()).append("\n");
            }
            java.nio.file.Files.write(sequencePath, kf.toString().getBytes());
        } catch (Exception e) {
            log.warn("Failed to write keyframes to sequence file");
        }
    }

    public static CameraSequence load(Client client, KeyframeCameraConfig config, ConfigManager configManager, String name)
    {
        CameraSequence sequence = new CameraSequence(client, config, configManager, new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime()));
        Path sequencePath = KeyframeCameraPlugin.SEQUENCE_DIR.resolve(name);
        try {
            List<String> lines = java.nio.file.Files.readAllLines(sequencePath);
            for (String line : lines) {
                String[] parts = line.split(",");
                sequence.keyframes.add(new Keyframe(
                    Long.parseLong(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5]),
                    EaseType.valueOf(parts[6])
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to load sequence file");
            return null;
        }
        return sequence;
    }
}
