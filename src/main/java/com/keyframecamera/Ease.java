package com.keyframecamera;

public class Ease {
    public static Keyframe interpolate(Keyframe currentKeyframe, Keyframe nextKeyframe, long keyframeElapsed) {
        double t = (double) keyframeElapsed / currentKeyframe.getDuration();

        if (nextKeyframe == null) {
            return currentKeyframe;
        }

        double interpolationFactor = calculateEasing(currentKeyframe.getEase(), t);

        double currentYaw = currentKeyframe.getYaw();
        double nextYaw = nextKeyframe.getYaw();

        double yawDiff = nextYaw - currentYaw;
        if (Math.abs(yawDiff) > 1024) {
            if (yawDiff > 0) {
                currentYaw += 2048;
            } else {
                nextYaw += 2048;
            }
        }

        return new Keyframe(
                0,
                lerp(currentKeyframe.getFocalX(), nextKeyframe.getFocalX(), interpolationFactor),
                lerp(currentKeyframe.getFocalY(), nextKeyframe.getFocalY(), interpolationFactor),
                lerp(currentKeyframe.getFocalZ(), nextKeyframe.getFocalZ(), interpolationFactor),
                lerp(currentKeyframe.getPitch(), nextKeyframe.getPitch(), interpolationFactor),
                lerp(currentYaw, nextYaw, interpolationFactor) % 2048,
                (int) lerp(currentKeyframe.getScale(), nextKeyframe.getScale(), interpolationFactor),
                currentKeyframe.getEase()
        );
    }

    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static double calculateEasing(EaseType ease, double t) {
        switch (ease) {
            case LINEAR:
                return t;
            case SINE:
                return sinEaseInOut(t);
            case QUAD:
                return quadEaseInOut(t);
            case CUBIC:
                return cubicEaseInOut(t);
            case QUART:
                return quartEaseInOut(t);
            case QUINT:
                return quintEaseInOut(t);
            case EXPO:
                return expoEaseInOut(t);
            case CONSTANT:
                return 0;
            default:
                throw new IllegalArgumentException("Unknown easing type: " + ease);
        }
    }

    // Easing functions
    private static double sinEaseInOut(double t) { return (-(Math.cos(Math.PI * t) - 1) / 2); }

    private static double quadEaseInOut(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private static double cubicEaseInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private static double quartEaseInOut(double t) {
        return t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2;
    }

    private static double quintEaseInOut(double t) {
        return t < 0.5 ? 16 * t * t * t * t * t : 1 - Math.pow(-2 * t + 2, 5) / 2;
    }

    private static double expoEaseInOut(double t) {
        return t == 0 ? 0 : t == 1 ? 1 : t < 0.5 ? Math.pow(2, 20 * t - 10) / 2
                : (2 - Math.pow(2, -20 * t + 10)) / 2;
    }
}
