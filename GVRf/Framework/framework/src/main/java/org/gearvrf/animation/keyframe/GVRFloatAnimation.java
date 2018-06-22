package org.gearvrf.animation.keyframe;

import org.gearvrf.PrettyPrint;
import org.gearvrf.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 
 * Describes the animation of a set of floating point values.
 */
public final class GVRFloatAnimation implements PrettyPrint
{
    private static final String TAG = GVRFloatAnimation.class.getSimpleName();

    public static class FloatKeyInterpolator
    {
        private final int mFloatsPerKey;
        private final float[] mKeyData;
        private final float mDuration;
        private int mLastKeyIndex;

        public FloatKeyInterpolator(float[] keyData, int keySize)
        {
            mKeyData = keyData;
            mFloatsPerKey = keySize;
            mLastKeyIndex = -1;
            mDuration = keyData[keyData.length - keySize] - keyData[0];
        }

        protected float[] interpolate(float time, float[] destValues)
        {
            int index = getKeyIndex(time);
            float curTime = getTime(index);
            float nextTime = getTime(index + 1);

            if ((index >= 0) &&
                (curTime <= time) &&
                (time < nextTime))
            {
                // interpolate
                float deltaTime = nextTime - curTime;
                float factor = (time - curTime) / deltaTime;

                interpolateValues(index, destValues, factor);
            }
            else
            {
                // time is out of range of animation time frame
                if (time <= getTime(0))
                {
                    getValues(0, destValues);
                }
                else
                {
                    getValues(getNumKeys() - 1, destValues);
                }
            }
            return destValues;
        }

        public float getDuration()
        {
            return mDuration;
        }

        public int getKeyOffset(int keyIndex)
        {
            int index;

            if (keyIndex < 0)
            {
                return -1;
            }
            index = keyIndex * mFloatsPerKey;
            if (index + mFloatsPerKey > mKeyData.length)
            {
                return -1;
            }
            return index;
        }

        public int getNumKeys()
        {
            return mKeyData.length / mFloatsPerKey;
        }

        public float getTime(int keyIndex)
        {
            int ofs = getKeyOffset(keyIndex);
            if (ofs >= 0)
            {
                return mKeyData[ofs];
            }
            return -1.0f;
        }

        public boolean setValues(int keyIndex, float[] values)
        {
            int ofs = getKeyOffset(keyIndex);
            if (ofs >= 0)
            {
                System.arraycopy(values, 0, mKeyData, ofs + 1, mFloatsPerKey - 1);
                return true;
            }
            return false;
        }

        public boolean interpolateValues(int keyIndex, float[] values, float factor)
        {
            int firstOfs = getKeyOffset(keyIndex);
            int lastOfs = getKeyOffset(keyIndex + 1);

            for (int i = 1; i < mFloatsPerKey; ++i)
            {
                values[i - 1] = (1.0f - factor) * mKeyData[firstOfs + i] + factor * mKeyData[lastOfs + i];
            }
            return true;
        }

        public boolean getValues(int keyIndex, float[] values)
        {
            int ofs = getKeyOffset(keyIndex);
            if (ofs >= 0)
            {
                System.arraycopy(mKeyData, ofs + 1, values, 0, mFloatsPerKey - 1);
                return true;
            }
            return false;
        }

    final private FloatKeyInterpolator mFloatInterpolator;

    /**
     * Constructor.
     *
     * @param keyData animation key data
     * @param keySize number of floats per key
     */
    public GVRFloatAnimation(float[] keyData, int keySize)
    {
        if (keySize <= 2)
        {
            throw new IllegalArgumentException("The number of floats per key must be > 1, the key includes time");
        }
        if ((keyData == null) || (keyData.length < keySize))
        {
            throw new IllegalArgumentException("Not enough key data");
        }
        mFloatInterpolator = new FloatKeyInterpolator(keyData, keySize);
    }

    /**
     * Obtains the transform for a specific time in animation.
     * 
     * @param animationTime The time in animation.
     * 
     * @return The transform.
     */
    public void animate(float animationTime, float[] destValues)
    {
        mFloatInterpolator.interpolate(animationTime * mFloatInterpolator.getDuration(), destValues);
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(GVRFloatAnimation.class.getSimpleName());
        sb.append(" [ Key[" + mFloatInterpolator.getNumKeys() + "]");
        sb.append(System.lineSeparator());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

}

