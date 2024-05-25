package com.keyframecamera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Sequence
{

    private final KeyframeCameraConfig config;

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final List<Long> keyframeTimestamps = new ArrayList<>();
    private final HashMap<Keyframe, Integer> keyframeIndexMap = new HashMap<>();

    public Sequence(KeyframeCameraConfig config)
    {
        this.config = config;
    }

    public List<Keyframe> getKeyframes() { return keyframes; }

    public Keyframe get(int index) { return keyframes.get(index); }

    public long getTimestamp(int index) { return keyframeTimestamps.get(index); }
    public long getTimestamp(Keyframe keyframe) { return keyframeTimestamps.get(keyframeIndexMap.get(keyframe)); }

    public Keyframe getNext(Keyframe keyframe)
    {
        return keyframes.get(keyframeIndexMap.get(keyframe) + 1);
    }

    public boolean isLast(int index) { return index == keyframes.size() - 1; }
    public boolean isLast(Keyframe keyframe) { return isLast(keyframeIndexMap.get(keyframe)); }

    public boolean contains(Keyframe keyframe) { return keyframeIndexMap.containsKey(keyframe); }
    public int indexOf(Keyframe keyframe) { return keyframeIndexMap.get(keyframe); }

    public void add(Keyframe keyframe, long timestamp)
    {
        keyframes.add(keyframe);
        keyframeIndexMap.put(keyframe, keyframes.size() - 1);
        keyframeTimestamps.add(timestamp);
    }

    public void add(Keyframe keyframe) {
        long timestamp = size() == 0 ? 0 : getSequenceDuration() + config.defaultKeyframeDuration();
        add(keyframe, timestamp);
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

    public void swap(Keyframe a, Keyframe b) {
        if (a == b || !contains(a) || !contains(b)) return;

        int indexA = indexOf(a);
        int indexB = indexOf(b);

        long timestampA = keyframeTimestamps.get(indexA);
        long durationB = getKeyframeDuration(b);
        keyframeTimestamps.set(indexB, timestampA + durationB);

        Collections.swap(keyframes, indexA, indexB);

        keyframeIndexMap.put(a, indexB);
        keyframeIndexMap.put(b, indexA);
    }

    public int size() { return keyframes.size(); }

    public long getKeyframeDuration(Keyframe keyframe) {
        if (size() < 2) return 0;

        if (isLast(keyframe)) {
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
        if (keyframeTimestamps.isEmpty() || keyframeTimestamps.size() == 1) return 0;
        return keyframeTimestamps.get(keyframeTimestamps.size() - 1);
    }

    double t(Keyframe keyframe, long elapsed) {
        int keyframeElapsed = (int) (elapsed - keyframeTimestamps.get(keyframeIndexMap.get(keyframe)));
        return (double) keyframeElapsed / (double) getKeyframeDuration(keyframe);
    }

}
