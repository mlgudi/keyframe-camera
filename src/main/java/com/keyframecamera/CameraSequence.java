package com.keyframecamera;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientInt;
import net.runelite.client.callback.ClientThread;
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

    @Getter
    private final KeyframeCameraPlugin plugin;
    private final Client client;
    private final ClientThread clientThread;
    private final KeyframeCameraConfig config;
    private final ConfigManager configManager;

    List<Keyframe> keyframes = new ArrayList<>();

    @Getter
    private int currentKeyframeIndex = 0;
    private long currentKeyframeStartTime = 0;

    private long startTime = 0;
    private long pauseStartTime = 0;
    private long totalPauseTime = 0;

    public CameraSequence(KeyframeCameraPlugin plugin, Client client, ClientThread clientThread, KeyframeCameraConfig config, ConfigManager configManager, String name)
    {
        this.plugin = plugin;
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.configManager = configManager;
        this.name = name;
    }

    private int getScale()
    {
        return client.getVarcIntValue(VarClientInt.CAMERA_ZOOM_FIXED_VIEWPORT);
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

    public int addKeyframe()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return -1;
        }

        keyframes.add(
            new Keyframe(
                config.defaultKeyframeDuration(),
                client.getCameraFocalPointX(),
                client.getCameraFocalPointY(),
                client.getCameraFocalPointZ(),
                client.getCameraPitch(),
                client.getCameraYaw(),
                getScale(),
                config.defaultKeyframeEase()
            )
        );

        return keyframes.size() - 1;
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
                keyframe.getScale(),
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
                getScale(),
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
            clientThread.invoke(() -> client.setCameraMode(1));
        }
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", true);
        startTime = System.currentTimeMillis();
        totalPauseTime = 0;
        currentKeyframeIndex = 0;
        currentKeyframeStartTime = 0;
    }

    public void stop()
    {
        startTime = 0;
        pauseStartTime = 0;
        totalPauseTime = 0;
        currentKeyframeIndex = 0;
        currentKeyframeStartTime = 0;
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
            if (currentKeyframeIndex + 1 >= keyframes.size()) {
                if (config.loop()) {
                    startTime = System.currentTimeMillis();
                    totalPauseTime = 0;
                    currentKeyframeIndex = 0;
                    currentKeyframeStartTime = 0;
                    currentKeyframe = keyframes.get(0);
                } else {
                    stop();
                    return;
                }
            } else {
                currentKeyframeIndex++;
                currentKeyframe = keyframes.get(currentKeyframeIndex);
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

        setCameraToKeyframe(interpolatedFrame);
    }

    public boolean isPlaying()
    {
        return config.playing();
    }

    public boolean moveKeyframe(boolean up, int index)
    {
        if (index < 0 || index >= keyframes.size()) {
            return false;
        }

        if (up && index == 0) {
            return false;
        }

        if (!up && index == keyframes.size() - 1) {
            return false;
        }

        Keyframe keyframe = keyframes.remove(index);
        keyframes.add(up ? index - 1 : index + 1, keyframe);
        return true;
    }

    public void setCameraToKeyframe(Keyframe keyframe)
    {
        if (client.getCameraMode() != 1) {
            client.setCameraMode(1);
        }

        clientThread.invoke(() -> {
            client.setCameraFocalPointX(keyframe.getFocalX());
            client.setCameraFocalPointY(keyframe.getFocalY());
            client.setCameraFocalPointZ(keyframe.getFocalZ());
            client.setCameraPitchTarget((int) keyframe.getPitch());
            client.setCameraYawTarget((int) keyframe.getYaw());
            client.runScript(ScriptID.CAMERA_DO_ZOOM, keyframe.getScale(), keyframe.getScale());
        });
    }

    public void wipe()
    {
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", false);
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);

        keyframes.clear();

        startTime = 0;
        pauseStartTime = 0;
        totalPauseTime = 0;
        currentKeyframeIndex = 0;
        currentKeyframeStartTime = 0;
    }

    public void save()
    {
        Path sequencePath = KeyframeCameraPlugin.SEQUENCE_DIR.resolve(name + ".txt");
        try {
            StringBuilder kf = new StringBuilder();
            for (Keyframe keyframe : keyframes) {
                kf.append(keyframe.toString()).append("\n");
            }
            java.nio.file.Files.write(sequencePath, kf.toString().getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            plugin.sendChatMessage("Failed to save sequence: " + name);
        }
    }

    public static CameraSequence load(KeyframeCameraPlugin plugin, Client client, ClientThread clientThread, KeyframeCameraConfig config, ConfigManager configManager, String name)
    {
        CameraSequence sequence = new CameraSequence(plugin, client, clientThread, config, configManager, new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime()));
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
                        Integer.parseInt(parts[6]),
                        EaseType.valueOf(parts[7])
                ));
            }
        } catch (Exception e) {
            plugin.sendChatMessage("Failed to load sequence: " + name);
            return plugin.getSequence();
        }

        return sequence;
    }
}
