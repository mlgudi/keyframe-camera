package com.keyframecamera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.api.WorldView;

public class Sequence
{
	private final Client client;
	private final KeyframeCameraConfig config;

	@Getter
	private final List<Keyframe> keyframes = new ArrayList<>();
	private final List<Long> keyframeTimestamps = new ArrayList<>();
	private final HashMap<Keyframe, Integer> keyframeIndexMap = new HashMap<>();

	@Getter
	@Setter
	private boolean preserveLocation = true;

	@Getter
	@Setter
	private int worldViewId;

	@Getter
	@Setter
	private int baseX;

	@Getter
	@Setter
	private int baseZ;

	public Sequence(Client client, KeyframeCameraConfig config)
	{
		this.client = client;
		this.config = config;
	}

	public Keyframe get(int index)
	{
		return keyframes.get(index);
	}

	public long getTimestamp(int index)
	{
		return keyframeTimestamps.get(index);
	}

	public long getTimestamp(Keyframe keyframe)
	{
		return keyframeTimestamps.get(keyframeIndexMap.get(keyframe));
	}

	public Keyframe getNext(Keyframe keyframe)
	{
		return keyframes.get(keyframeIndexMap.get(keyframe) + 1);
	}

	public boolean isLast(int index)
	{
		return index == keyframes.size() - 1;
	}

	public boolean isLast(Keyframe keyframe)
	{
		return isLast(keyframeIndexMap.get(keyframe));
	}

	public boolean missing(Keyframe keyframe)
	{
		return !keyframeIndexMap.containsKey(keyframe);
	}

	public int indexOf(Keyframe keyframe)
	{
		return keyframeIndexMap.get(keyframe);
	}

	public void add(Keyframe keyframe, long timestamp)
	{
		keyframes.add(keyframe);
		keyframeIndexMap.put(keyframe, keyframes.size() - 1);
		keyframeTimestamps.add(timestamp);
	}

	public void add(Keyframe keyframe)
	{
		long timestamp = size() == 0 ? 0 : getSequenceDuration() + config.defaultKeyframeDuration();
		add(keyframe, timestamp);
	}

	public int add()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return -1;
		}

		WorldView worldView = client.getTopLevelWorldView();
		if (baseX == 0 || baseZ == 0)
		{
			worldViewId = worldView.getId();
			baseX = worldView.getBaseX();
			baseZ = worldView.getBaseY();
		}

		Keyframe keyframe = new Keyframe(
			client.getCameraFocalPointX(),
			client.getCameraFocalPointY(),
			client.getCameraFocalPointZ(),
			client.getCameraFpPitch(),
			client.getCameraFpYaw(),
			getScale(),
			config.defaultKeyframeEase()
		);

		int xOff = baseX - worldView.getBaseX();
		int zOff = baseZ - worldView.getBaseY();

		keyframe.setFocalX(keyframe.getFocalX() - (xOff * 128));
		keyframe.setFocalZ(keyframe.getFocalZ() - (zOff * 128));

		add(keyframe);

		return keyframes.size() - 1;
	}

	public void remove(Keyframe keyframe)
	{
		int index = keyframeIndexMap.get(keyframe);
		long duration = getKeyframeDuration(keyframe);

		for (int i = index; i < keyframeTimestamps.size(); i++)
		{
			keyframeTimestamps.set(i, keyframeTimestamps.get(i) - duration);
		}

		keyframes.remove(index);
		keyframeIndexMap.remove(keyframe);
		keyframeTimestamps.remove(index);

		for (int i = index; i < keyframes.size(); i++)
		{
			Keyframe kf = keyframes.get(i);
			keyframeIndexMap.put(kf, i);
		}
	}

	public void swap(Keyframe a, Keyframe b)
	{
		if (a == b || missing(a) || missing(b))
		{
			return;
		}

		int indexA = indexOf(a);
		int indexB = indexOf(b);

		long timestampA = keyframeTimestamps.get(indexA);
		long durationB = isLast(b) ? getTimestamp(b) - timestampA : getKeyframeDuration(b);
		keyframeTimestamps.set(indexB, timestampA + durationB);

		Collections.swap(keyframes, indexA, indexB);

		keyframeIndexMap.put(a, indexB);
		keyframeIndexMap.put(b, indexA);
	}

	public void duplicate(Keyframe keyframe)
	{
		if (missing(keyframe))
		{
			return;
		}

		add(
			new Keyframe(
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

	public void overwrite(Keyframe keyframe)
	{
		if (missing(keyframe))
		{
			return;
		}
		keyframe.setFocalX(client.getCameraFocalPointX());
		keyframe.setFocalY(client.getCameraFocalPointY());
		keyframe.setFocalZ(client.getCameraFocalPointZ());
		keyframe.setPitch(client.getCameraFpPitch());
		keyframe.setYaw(client.getCameraFpYaw());
		keyframe.setScale(getScale());
	}

	private int getScale()
	{
		return client.getVarcIntValue(VarClientInt.CAMERA_ZOOM_FIXED_VIEWPORT);
	}

	public int size()
	{
		return keyframes.size();
	}

	public long getKeyframeDuration(Keyframe keyframe)
	{
		if (size() < 2)
		{
			return 0;
		}

		if (isLast(keyframe))
		{
			return 0;
		}

		int index = keyframeIndexMap.get(keyframe);
		return keyframeTimestamps.get(index + 1) - keyframeTimestamps.get(index);
	}

	public void setKeyframeDuration(Keyframe keyframe, long duration)
	{
		int index = keyframeIndexMap.get(keyframe);
		long oldDuration = getKeyframeDuration(keyframe);
		long delta = duration - oldDuration;

		for (int i = index + 1; i < keyframeTimestamps.size(); i++)
		{
			keyframeTimestamps.set(i, keyframeTimestamps.get(i) + delta);
		}
	}

	public long getSequenceDuration()
	{
		if (keyframeTimestamps.isEmpty() || keyframeTimestamps.size() == 1)
		{
			return 0;
		}
		return keyframeTimestamps.get(keyframeTimestamps.size() - 1);
	}

	double t(Keyframe keyframe, long elapsed)
	{
		int keyframeElapsed = (int) (elapsed - keyframeTimestamps.get(keyframeIndexMap.get(keyframe)));
		return (double) keyframeElapsed / (double) getKeyframeDuration(keyframe);
	}

}
