package com.keyframecamera;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class Playback
{

    KeyframeCameraPlugin plugin;
    KeyframeCameraConfig config;
    ConfigManager configManager;
    Client client;
    ClientThread clientThread;
    Sequence sequence;

    @Getter
    private int currentKeyframeIndex = 0;
    private Keyframe currentKeyframe;

    private long startTime = 0;
    private long pauseStartTime = 0;
    private long totalPauseTime = 0;

    public Playback(KeyframeCameraPlugin plugin, KeyframeCameraConfig config, ConfigManager configManager, Client client, ClientThread clientThread)
    {
        this.plugin = plugin;
        this.sequence = plugin.getSequence();
        this.config = config;
        this.configManager = configManager;
        this.client = client;
        this.clientThread = clientThread;
    }

    public boolean isPlaying() { return config.playing(); }
    public boolean isPaused() { return config.paused(); }

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

    public void play()
    {
        sequence = plugin.getSequence();
        if (sequence == null) return;
        if (sequence.size() < 2) return;
        if (config.playing()) return;
        if (!plugin.freeCamEnabled()) plugin.toggleCameraMode();

        currentKeyframe = null;
        startTime = System.currentTimeMillis();
        totalPauseTime = 0;
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", true);
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);
    }

    public void togglePause()
    {
        if (!config.playing()) return;
        if (config.paused())
        {
            totalPauseTime += System.currentTimeMillis() - pauseStartTime;
            configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);
        }
        else
        {
            pauseStartTime = System.currentTimeMillis();
            configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", true);
        }
    }

    public void stop()
    {
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", false);
        configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);
    }

    private void resetPlayback()
    {
        startTime = System.currentTimeMillis();
        pauseStartTime = 0;
        totalPauseTime = 0;
        currentKeyframe = sequence.get(0);
        currentKeyframeIndex = 0;
    }

    public void tick()
    {
        if (!isPlaying() || isPaused()) return;
        if (sequence.size() < 2) return;

        if (currentKeyframe == null) {
            resetPlayback();
        }

        if (sequence.isLast(currentKeyframe) && isPlaying())
        {
            if (config.loop()) {
                resetPlayback();
            } else {
                stop();
            }
        }

        long elapsed = elapsed();

        if (elapsed >= sequence.getTimestamp(currentKeyframeIndex + 1))
        {
            currentKeyframeIndex++;
            currentKeyframe = sequence.get(currentKeyframeIndex);
            plugin.redrawPanel();
        }

        if (sequence.isLast(currentKeyframe)) {
			setCameraToKeyframe(currentKeyframe);
            return;
        }

        double t = sequence.t(currentKeyframe, elapsed());
        Keyframe interpolatedKeyframe = Ease.interpolate(currentKeyframe, sequence.getNext(currentKeyframe), t);

		setCameraToKeyframe(interpolatedKeyframe);
	}

	public void setCameraToKeyframe(Keyframe keyframe)
	{
		if (client.getCameraMode() != 1)
		{
			client.setCameraMode(1);
		}

		clientThread.invoke(() -> {
			client.setCameraFocalPointX(keyframe.getFocalX());
			client.setCameraFocalPointY(keyframe.getFocalY());
			client.setCameraFocalPointZ(keyframe.getFocalZ());
			client.setCameraPitchTarget(Keyframe.radiansToJau(keyframe.getPitch()));
			client.setCameraYawTarget(Keyframe.radiansToJau(keyframe.getYaw()) % 2047);
			client.runScript(ScriptID.CAMERA_DO_ZOOM, keyframe.getScale(), keyframe.getScale());
		});
	}

}
