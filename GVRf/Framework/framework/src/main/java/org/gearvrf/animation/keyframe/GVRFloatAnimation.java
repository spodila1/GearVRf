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
        private float[] mKeys;
        private int mFloatsPerKey;
        private int mLastKeyIndex;
        private float[] mCurrentValues;

        public FloatKeyInterpolator(float[] keys, int floatsPerKey)
        {
            mKeys = keys;
            mFloatsPerKey = floatsPerKey;
            mLastKeyIndex = -1;
            mCurrentValues = new float[floatsPerKey - 1];
        }

        protected float[] interpolate(float time)
        {
            int index = getKeyIndex(time);
            int nextIndex = index;

            if (index != -1 &&
                (mKeys[index] <= time) &&
                (time < mKeys[nextIndex]))
            {
                // interpolate
                float deltaTime = mKeys[nextIndex] - mKeys[index];
                float factor = (time - mKeys[index] / deltaTime);
                for (int i = 1; i < mFloatsPerKey; ++i)
                {
                    mCurrentValues[i] = factor * mKeys[index + i] + (1.0f - factor) * mKeys[nextIndex + i];
                }
            }
            else
            {
                // time is out of range of animation time frame
                if (time <= mKeys[0])
                {
                    System.arraycopy(mKeys, 1, mCurrentValues, 0, mFloatsPerKey - 1);
                }
                else
                {
                    System.arraycopy(mKeys, mKeys.length - mFloatsPerKey + 1, mCurrentValues, 0, mFloatsPerKey - 1);
                }
            }
            return mCurrentValues;
        }

        protected int getKeyIndex(float time)
        {
            // Try cached key first
            if (mLastKeyIndex != -1)
            {
                if ((mKeys[mLastKeyIndex] <= time) &&
                    (time < mKeys[mLastKeyIndex + mFloatsPerKey]))
                {
                    return mLastKeyIndex;
                }
                // Try neighboring keys
                if (((mLastKeyIndex + 2 * mFloatsPerKey) < mKeys.length) &&
                     (mKeys[mLastKeyIndex + mFloatsPerKey] <= time) &&
                     (time < mKeys[mLastKeyIndex + 2 * mFloatsPerKey]))
                {
                    return mLastKeyIndex += mFloatsPerKey;
                }
                if ((mLastKeyIndex >= 1) &&
                    ((mKeys[mLastKeyIndex - mFloatsPerKey]) <= time) &&
                    (time < mKeys[mLastKeyIndex]))
                {
                    return mLastKeyIndex -= mFloatsPerKey;
                }
            }

            // Binary search for the interval
            // Each of the index i represents an interval I(i) = [time(i), time(i + 1)).
            int low = 0, high = mKeys.length - 2 * mFloatsPerKey;
            // invariant: I(low)...I(high) contains time if time can be found
            // post-condition: |high - low| <= 1, only need to check I(low) and I(low + 1)
            while (high - low > 1)
            {
                int mid = ((low + high) / 2) * mFloatsPerKey;
                if (time < mKeys[mid])
                {
                    high = mid;
                }
                else if (time >= mKeys[mid + mFloatsPerKey])
                {
                    low = mid + 1;
                }
                else
                    {
                    // time in I(mid) by definition
                    return mLastKeyIndex = mid;
                }
            }
            if ((mKeys[low] <= time) &&
                (time < mKeys[low + mFloatsPerKey]))
            {
                return mLastKeyIndex = low;
            }
            if ((low + 2 * mFloatsPerKey < mKeys.length) &&
                (mKeys[low + mFloatsPerKey] <= time) &&
                (time < mKeys[low + 2 * mFloatsPerKey]))
            {
                return mLastKeyIndex = low + mFloatsPerKey;
            }
            Log.v(TAG, "Warning: interpolation failed at time " + time);
            return mLastKeyIndex = -1;
        }
    };

    private int mFloatsPerKey;
    protected float[] mKeys;
    private final FloatKeyInterpolator mFloatInterpolator;

    /**
     * Constructor.
     *
     * @param keyData animation key data
     * @param keySize number of floats per key
     */
    public GVRFloatAnimation(float[] keyData, int keySize)
    {
        mFloatsPerKey = keySize;
        mKeys = keyData;
        mFloatInterpolator = new FloatKeyInterpolator(mKeys, keySize);
    }

    /**
     * Returns the number of keys.
     * 
     * @return the number of keys
     */
    public int getNumKeys() {
        return mKeys.length / mFloatsPerKey;
    }

    /**
     * Returns the time component of the specified key.
     * 
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public double getTime(int keyIndex) {
        return mKeys[keyIndex * mFloatsPerKey];
    }

    /**
     * Returns the scaling factor as vector.<p>
     * 
     * @param keyIndex the index of the scale key
     * 
     * @return the scaling factor as vector
     */
    public void getKey(int keyIndex, float[] values)
    {
        int index = keyIndex * mFloatsPerKey;
        System.arraycopy(mKeys, index + 1, values, 0, values.length);
    }

    public void setKey(int keyIndex, float time, final float[] values)
    {
        int index = keyIndex * mFloatsPerKey;
        mKeys[index] = time;
        System.arraycopy(values, 0, mKeys, index + 1, values.length);
    }

    public void setKeys(float[] keys)
    {
        mKeys = keys;
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
        float[] currentValues = mFloatInterpolator.interpolate(animationTime);
        System.arraycopy(currentValues, 0, destValues, 0, mFloatsPerKey - 1);
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(GVRFloatAnimation.class.getSimpleName());
        sb.append(" [ Keys=" + mKeys.length + "]");
        sb.append(System.lineSeparator());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

}

