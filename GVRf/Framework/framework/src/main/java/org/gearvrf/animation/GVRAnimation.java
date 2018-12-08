/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.animation;

import org.gearvrf.GVRDrawFrameListener;
import org.gearvrf.GVRHybridObject;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRShaderData;
import org.gearvrf.GVRTransform;
import org.gearvrf.animation.keyframe.GVRSkeletonAnimation;
import org.gearvrf.utility.Log;

import android.graphics.Color;

/**
 * The root of the GVRF animation tree.
 *
 * This class (and the {@linkplain GVRAnimationEngine engine}) supply the common
 * functionality: descendants are tiny classes that contain compiled (ie, no
 * runtime reflection is used) code to change individual properties. Most
 * animations involve a {@linkplain GVRTransform scene object's position,}
 * {@linkplain GVRMaterial a scene object's surface appearance,} or an optional
 * {@linkplain GVRShaderData "post effect":} accordingly, most actual animations
 * descend from {@link GVRTransformAnimation}, {@link GVRMaterialAnimation}, or
 * {@link GVRPostEffectAnimation} and not directly from {@link GVRAnimation}.
 *
 * <p>
 * All animations have at least three or more required parameters: the object to
 * animate, the duration, and any animation parameters. In many cases, there are
 * overloaded methods to specify the animation parameters in convenient ways:
 * for example, you can specify a color as an Android {@link Color} or as a
 * {@code float[3]} of GL-compatible 0 to 1 values. In addition, all the stock
 * animations that animate a type like, say, {@link GVRMaterial} 'know how' to
 * find the {@link GVRMaterial} inside a {@link GVRSceneObject}.
 *
 * <p>
 * This means that most animations have two or four overloaded constructors.
 * This is trouble for the animation developer - who must keep four sets of
 * JavaDoc in sync - but probably clear enough for the animation user. However,
 * there are also four optional parameters: the interpolator, the repeat type,
 * the repeat count, and an on-finished callback. Adding these to an overload
 * tree would, well, overload both developers and users!
 *
 * <p>
 * Thus, animations use a sort of Builder Pattern: you set the optional
 * parameters <i>via</i> set() methods that return {@code this}, so you can
 * chain them like
 *
 * <pre>
 *
 * new GVRScaleAnimation(sceneObject, 1.5f, 2f) //
 *         .setRepeatMode(GVRRepetitionType.PINGPONG) //
 *         .start({@linkplain GVRAnimationEngine animationEngine});
 * </pre>
 *
 * which will 'pulse' the size of the {@code sceneObject} from its current level
 * to double size, and back.
 *
 * <p>
 * Animations run in a {@link GVRDrawFrameListener}, so they happen before your
 * {@link GVRMain#onStep()} handler, which happens before GVRF
 * renders the scene graph. This has two consequences:
 * <ul>
 * <li>When you start an animation in an {@link GVRMain#onStep()} handler, it
 * starts running on the <em>next</em> frame.
 * <li>If you start multiple animations (with the same duration and the same
 * interpolator!) in the same {@code onStep()} handler, they will always be in
 * sync. That is, you can 'compose' animations simply by starting them together;
 * you do not need to write a composite animation that animates multiple
 * properties in a single method.
 * </ul>
 */
public abstract class GVRAnimation {

    /**
     * The default repeat count only applies to the two repeat modes, not to the
     * default run-once mode.
     *
     * The default repeat count is 2, so that a ping pong animation will return
     * to the start state, even if you don't
     * {@linkplain GVRAnimation#setRepeatCount(int) setRepeatCount(2).}
     */
    public static final int DEFAULT_REPEAT_COUNT = 2;
    public static final boolean sDebug = true;

    // Immutable values, passed to constructor
    protected GVRHybridObject mTarget;
    protected float mDuration;

    // Defaulted values, which should be set before start()
    protected GVRInterpolator mInterpolator = null;
    protected int mRepeatMode = GVRRepeatMode.ONCE;
    protected int mRepeatCount = DEFAULT_REPEAT_COUNT;
    protected float animationOffset = 0;
    protected float animationSpeed = 1;
    protected GVROnFinish mOnFinish = null;
    protected float mCurrentTime = 0;
    protected float mStartTime = 0;
    protected String mName = null;
    static float timeper = 0;
    private int countfind=0;
    static int counter = 0;
    static int counterS = 0;
    boolean flagB = false;
    static int sizeofAnim = 0;

    /**
     * This is derived from {@link #mOnFinish}. Doing the {@code instanceof}
     * test in {@link #setOnFinish(GVROnFinish)} means we <em>don't</em> have to
     * do it on every call, in {@link #onDrawFrame(float)}
     */
    protected GVROnRepeat mOnRepeat = null;

    // Running state
    protected float mElapsedTime = 0f;
    protected int mIterations = 0;

    protected boolean isFinished = false;

    /**
     * Base constructor.
     *
     * Sets required fields, initializes optional fields to default values.
     *
     * @param target
     *            The object to animate. Note that this constructor makes a
     *            <em>private<em> copy of the {@code target}
     *            parameter: It is up to descendant classes to cast the
     *            {@code target} to the expected type and save the typed value.
     * @param duration
     *            The animation duration, in seconds.
     */
    protected GVRAnimation(GVRHybridObject target, float duration) {
        mTarget = target;
        mDuration = duration;

    }

    /**
     * Many animations can take multiple target types: for example,
     * {@link GVRMaterialAnimation material animations} can work directly with
     * {@link GVRMaterial} targets, but also 'know how' to get a
     * {@link GVRMaterial} from a {@link GVRSceneObject}. They can, of course,
     * just expose multiple constructors, but that makes for a combinatorial
     * explosion when the other parameters also 'want' to be overloaded. This
     * method allows them to just take a {@link GVRHybridObject} and throw an
     * exception if they get a type they can't handle; it also returns the
     * matched type (which may not be equal to {@code target.getClass()}) so
     * that calling code doesn't have to do {@code instanceof} tests.
     *
     * @param target
     *            A {@link GVRHybridObject} instance
     * @param supported
     *            An array of supported types
     * @return The element of {@code supported} that matched
     * @throws IllegalArgumentException
     *             If {@code target} is not an instance of any of the
     *             {@code supported} types
     */
    protected static Class<?> checkTarget(GVRHybridObject target,
                                          Class<?>... supported) {
        for (Class<?> type : supported) {
            if (type.isInstance(target)) {
                return type;
            }
        }
        // else
        throw new IllegalArgumentException();
    }

    /**
     * Set the interpolator.
     *
     * By default, animations proceed linearly: at X percent of the duration,
     * the animated property will be at X percent of the way from the start
     * state to the stop state. Specifying an explicit interpolator lets the
     * animation do other things, like accelerate and decelerate, overshoot,
     * bounce, and so on.
     *
     * @param interpolator
     *            An interpolator instance. {@code null} gives you the default,
     *            linear animation.
     * @return {@code this}, so you can chain setProperty() calls.
     */
    public GVRAnimation setInterpolator(GVRInterpolator interpolator) {
        mInterpolator = interpolator;
        return this;
    }

    /**
     * Set the repeat type.
     *
     * In the default {@linkplain GVRRepeatMode#ONCE run-once} mode, animations
     * run once, ignoring the {@linkplain #getRepeatCount() repeat count.} In
     * {@linkplain GVRRepeatMode#PINGPONG ping pong} and
     * {@linkplain GVRRepeatMode#REPEATED repeated} modes, animations do honor
     * the repeat count, which {@linkplain #DEFAULT_REPEAT_COUNT defaults} to 2.
     *
     * @param repeatMode
     *            One of the {@link GVRRepeatMode} constants
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code repetitionType} is not one of the
     *             {@link GVRRepeatMode} constants
     */
    public GVRAnimation setRepeatMode(int repeatMode) {
        if (GVRRepeatMode.invalidRepeatMode(repeatMode)) {
            throw new IllegalArgumentException(repeatMode
                    + " is not a valid repetition type");
        }
        mRepeatMode = repeatMode;
        return this;
    }

    /**
     * Set the repeat count.
     *
     * @param repeatCount
     *            <table border="none">
     *            <tr>
     *            <td width="15%">A negative number</td>
     *            <td>Means the animation will repeat indefinitely. See the
     *            notes on {@linkplain GVROnFinish#finished(GVRAnimation)
     *            stopping an animation.}</td>
     *            </tr>
     *
     *            <tr>
     *            <td>0</td>
     *            <td>After {@link #start(GVRAnimationEngine) start()}, 0 means
     *            'stop at the end' and schedules a clean shutdown. Calling
     *            {@code setRepeatCount(0)} <em>before</em> {@code start()} is
     *            really pointless and silly ... but {@code start()} is
     *            special-cased so setting the repeat count to 0 will do what
     *            you expect.</td>
     *            </tr>
     *
     *            <tr>
     *            <td>A positive number</td>
     *            <td>Specifies a repeat count</td>
     *            </tr>
     *            </table>
     * @return {@code this}, so you can chain setProperty() calls.
     */
    public GVRAnimation setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
        return this;
    }
    /**
     * Sets the offset for the animation.
     *
     * @param startOffset animation will start at the specified offset value
     *
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code startOffset} is either negative or greater than
     *             the animation duration
     */
    public GVRAnimation setOffset(float startOffset)
    {
        if(startOffset<0 || startOffset>mDuration){
            throw new IllegalArgumentException("offset should not be either negative or greater than duration");
        }
        animationOffset = startOffset;
        mDuration =  mDuration-animationOffset;
        return this;
    }
    /**
     * Sets the speed for the animation.
     *
     * @param speed values from between 0 to 1 displays animation in slow mode
     *              values from 1 displays the animation in fast mode
     *
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code speed} is either zero or negative value
     */
    public GVRAnimation setSpeed(float speed)
    {
        if(speed<=0){
            throw new IllegalArgumentException("speed should be greater than zero");
        }
        animationSpeed =  speed;
        return this;
    }
    /**
     * Sets the duration for the animation to be played.
     *
     * @param start the animation will start playing from the specified time
     * @param end the animation will stop playing at the specified time
     *
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code start} is either negative value, greater than
     *             {@code end} value or {@code end} is greater than duration
     */
    public GVRAnimation setDuration(float start, float end)
    {
        if(start>end || start<0 || end>mDuration){
            throw new IllegalArgumentException("start and end values are wrong");
        }
        animationOffset =  start;
        mDuration = end-start;
        return this;
    }
    public GVRAnimation setStartTime(float startTime)
    {
        mStartTime = startTime;
        return this;
    }
    /**
     * Get the name of this animator.
     * <p>
     * The name is optional and may be set with {@link #setName(String) }
     * @returns string with name of animator, may be null
     * @see #setName(String)
     */
    public String getName() { return mName; }

    /**
     * Set the name of this animator.
     * @param name string with name of animator, may be null
     * @see #getName()
     */
    public void setName(String name) { mName = name; }
    /**
     * Set the on-finish callback.
     *
     * The basic {@link GVROnFinish} callback will notify you when the animation
     * runs to completion. This is a good time to do things like removing
     * now-invisible objects from the scene graph.
     *
     * <p>
     * The extended {@link GVROnRepeat} callback will be called after every
     * iteration of an indefinite (repeat count less than 0) animation, giving
     * you a way to stop the animation when it's not longer appropriate.
     *
     * @param callback
     *            A {@link GVROnFinish} or {@link GVROnRepeat} implementation.
     *            <p>
     *            <em>Note</em>: Supplying a {@link GVROnRepeat} callback will
     *            {@linkplain #setRepeatCount(int) set the repeat count} to a
     *            negative number. Calling {@link #setRepeatCount(int)} with a
     *            non-negative value after setting a {@link GVROnRepeat}
     *            callback will effectively convert the callback to a
     *            {@link GVROnFinish}.
     * @return {@code this}, so you can chain setProperty() calls.
     */
    public GVRAnimation setOnFinish(GVROnFinish callback) {
        mOnFinish = callback;

        // Do the instance-of test at set-time, not at use-time
        mOnRepeat = callback instanceof GVROnRepeat ? (GVROnRepeat) callback
                : null;
        if (mOnRepeat != null) {
            mRepeatCount = -1; // loop until iterate() returns false
        }
        return this;
    }

    /**
     * Start the animation.
     *
     * Changing properties once the animation is running can have unpredictable
     * results.
     * @param engine animation engine to start.
     * <p>
     * This method is exactly equivalent to
     * {@link GVRAnimationEngine#start(GVRAnimation)} and is provided as a
     * convenience so you can write code like
     *
     * <pre>
     *
     * GVRAnimation animation = new GVRAnimationDescendant(target, duration)
     *         .setOnFinish(callback).start(animationEngine);
     * </pre>
     *
     * instead of
     *
     * <pre>
     *
     * GVRAnimation animation = new GVRAnimationDescendant(target, duration)
     *         .setOnFinish(callback);
     * animationEngine.start(animation);
     * </pre>
     *
     * @return {@code this}, so you can save the instance at the end of a chain
     *         of calls
     */
    public GVRAnimation start(GVRAnimationEngine engine) {
        // Log.i("engine","started"+this.getName());
        engine.start(this);
        return this;
    }

    public void onStart()
    {
        mCurrentTime = 0;
        if (sDebug)
        {
            //  Log.d("ANIMATION", "%s started", getClass().getSimpleName());
        }
    }

    protected void onFinish()
    {

        if (sDebug)
        {
            //  Log.d("ANIMATION", "%s finished", getClass().getSimpleName());
        }
    }

    float newElaps = 0;

    private int animId = 0;
    private float blendDur = 0;

    static boolean[] flagArr; //= new boolean[sizeofAnim];
    public void setFlag(int size)
    {
        sizeofAnim = size;
        flagArr = new boolean[sizeofAnim];
        flagArr[0]=true;
        flagArr[1]=true;

        for(int i=2; i<size; i++)
        {
            // Log.i("printanim","pring"+size);
            flagArr[i] = false;

        }
    }
    public void setSize(int size)
    {
        sizeofAnim = size;
    }

    public void setID(int id)
    {
        animId = id;
    }
    public void setDur(float blendT)
    {
        blendDur = blendT;
    }
    public int getID()
    {
        return animId;
    }

    protected void onRepeat(float frameTime, int count, float mElapsedTime)
    {
        print=0;
        if(this.getID()%2==0) {

            if((this.getID())!=((sizeofAnim)-2)&&(this.getClass().getName().contains("GVRSkeletonAnimation"))||this.getClass().getName().contains("GVRPoseInterpolator"))
            {
                flagArr[this.getID()] = false;
                flagArr[this.getID()+1] = false;
                if(this.getClass().getName().contains("GVRPoseInterpolator"))
                {
                   flagArr[this.getID()-1] = false;
                   flagArr[this.getID()-2] = false;
                }
                else
                {
                    GVRSkeletonAnimation setTru = (GVRSkeletonAnimation)this;
                    setTru.setReturn(false);
                }
            }
            else if((this.getID()==((sizeofAnim)-2))&&(this.getClass().getName().contains("GVRSkeletonAnimation")))
            {
                flagArr[this.getID()] = false;
                flagArr[this.getID()+1] = false;
                flagArr[0] = true;
                flagArr[1] = true;
            }
        }

        if (sDebug)
        {
            //  Log.d("ANIMATION", "%s repeated %d", getClass().getSimpleName(), count);
        }

    }

    /**
     * Called by the animation engine. Uses the frame time, the interpolator,
     * and the repeat mode to generate a call to
     * {@link #animate(GVRHybridObject, float)}.
     *
     * @param frameTime
     *            elapsed time since the previous animation frame, in seconds
     * @return {@code true} to keep running the animation; {@code false} to shut
     *         it down
     */
    int print =0;
    int countr = 0;
    final boolean onDrawFrame(float frameTime) {
        GVRPoseInterpolator pose =null;
        if(this.getClass().getName().contains("GVRPoseInterpolator"))
        {
            pose = (GVRPoseInterpolator)this;
        }

        //  Log.i("callOn","Repeat "+" first "+flagArr[0]+ " inter1 "+flagArr[2]+" sec "+flagArr[4]+" inter2 "+flagArr[6]+" third "+flagArr[8]+" inter3 "+flagArr[10]+" four "+flagArr[12]);
        if(!flagArr[this.getID()]) {
            return true;
        }
        if(((this.getID())!=((sizeofAnim)-2))&&(this.getClass().getName().contains("GVRSkeletonAnimation")) &&((mElapsedTime-newElaps) >= ((this.getDuration())-(blendDur))+(frameTime)))
        {
            pose.frameTime = frameTime;
            GVRSkeletonAnimation setTru = (GVRSkeletonAnimation)this;
            setTru.setReturn(true);
            flagArr[this.getID()+2] = true;
            flagArr[this.getID()+3] = true;
            flagArr[this.getID()+4] = true;
            flagArr[this.getID()+5] = true;


        }

        final int previousCycleCount = (int) (mElapsedTime / mDuration);
        mElapsedTime += (frameTime*animationSpeed);

        if((this.getClass().getName().contains("GVRSkeletonAnimation")))
        {
            GVRSkeletonAnimation last = (GVRSkeletonAnimation)this;
            if(last.getSkelReturn()=="last")
            {
                if((0<(mElapsedTime-newElaps))&&((mElapsedTime-newElaps)<(blendDur)))
                {

                    last.setReturn(true);


                }
                else
                {
                    last.setReturn(false);
                }
            }

        }

        final int currentCycleCount = (int) (mElapsedTime / mDuration);
        final float cycleTime = (mElapsedTime % mDuration)+animationOffset;


        final boolean cycled = previousCycleCount != currentCycleCount;
        boolean stillRunning = cycled != true;

        if (cycled && mRepeatMode != GVRRepeatMode.ONCE) {
            //  Log.i("callOn","Repeat "+"name "+this.getClass().getName()+" "+mElapsedTime);
            // End of a cycle - see if we should continue
            mIterations += 1;
            if (mRepeatCount == 0) {
                stillRunning = false; // last pass
            } else if (mRepeatCount > 0) {
                stillRunning = --mRepeatCount > 0;
            } else {
                newElaps = mElapsedTime;
                onRepeat(frameTime, currentCycleCount, mElapsedTime);

                // Negative repeat count - call mOnRepeat, if we can
                if (mOnRepeat != null) {
                    stillRunning = mOnRepeat.iteration(this, mIterations);
                } else {
                    stillRunning = true; // repeat indefinitely
                }
            }
        }

        if (stillRunning) {
            final boolean countDown = mRepeatMode == GVRRepeatMode.PINGPONG
                    && (mIterations & 1) == 1;
           // Log.i("printLasttime","cycleTime "+((cycleTime))+ " duration "+mDuration);
            float elapsedRatio = //
                    countDown != true ? interpolate(cycleTime, mDuration)
                            : interpolate(mDuration - cycleTime, mDuration);
            if((this.getClass().getName().contains("GVRSkeletonAnimation"))) {
                GVRSkeletonAnimation last = (GVRSkeletonAnimation) this;

                if (last.getSkelReturn() == "first") {
                    Log.i("printLasttime","cycleTime "+((cycleTime))+ " duration "+mDuration+" countDown "+countDown);

                }
            }

            animate(mTarget, elapsedRatio);

        } else {

            float endRatio = mRepeatMode == GVRRepeatMode.ONCE ? 1f : 0f;

            endRatio = interpolate(mDuration, mDuration);
            animate(mTarget, endRatio);

            onFinish();
            if (mOnFinish != null) {
                mOnFinish.finished(this);
            }

            isFinished = true;
        }

        return stillRunning;
    }

    private float interpolate(float cycleTime, float duration) {
        float ratio = cycleTime / duration;
        return mInterpolator == null ? ratio : mInterpolator.mapRatio(ratio);
    }

    /**
     * Checks whether the animation has run to completion.
     *
     * For {@linkplain GVRRepeatMode#ONCE run-once} animations, this means only
     * that the animation has timed-out: generally, this means that the
     * (optional) onFinish callback has been invoked and the animation
     * 'unregistered' by the {@linkplain GVRAnimationEngine animation engine}
     * but it's not impossible that there is some lag between time-out and
     * finalization.
     *
     * <p>
     * For {@linkplain GVRRepeatMode#REPEATED repeated} or
     * {@linkplain GVRRepeatMode#PINGPONG ping pong} animations, this method can
     * tell you whether an animation is on its first iteration or one of the
     * repetitions. If you need to, you can terminate a repetitive animation
     * 'abruptly' by calling {@linkplain GVRAnimationEngine#stop(GVRAnimation)}
     * or 'cleanly' by calling {@link #setRepeatCount(int) setRepeatCount(0).}
     * Do note that both these approaches are sort of 'legacy' - the clean way
     * to handle indeterminate animations is to use
     * {@link #setOnFinish(GVROnFinish)} to set an {@linkplain GVROnRepeat}
     * handler, before calling {@link #start(GVRAnimationEngine)}.
     *
     * @return {@code true} if done or repeating; {@code false} if on first run.
     */
    public final boolean isFinished() {
        return isFinished;
    }

    public void reset() {
        mElapsedTime = 0;
        mIterations = 0;
        isFinished = false;
    }


    /**
     * Get the current repeat count.
     *
     * A negative number means the animation will repeat indefinitely; zero
     * means the animation will stop after the current cycle; a positive number
     * is the number of cycles after the current cycle.
     *
     * @return The current repeat count
     */
    public int getRepeatCount() {
        return mRepeatCount;
    }

    public float getAnimationOffset() {
        return animationOffset;
    }

    public float getAnimationSpeed()
    {
        return animationSpeed;
    }


    /**
     * The duration passed to {@linkplain #GVRAnimation(GVRHybridObject, float)
     * the constructor.}
     *
     * This may be useful if you have to, say, 'undo' a running animation.
     *
     * @return The duration passed to the constructor.
     */
    public float getDuration() {
        return mDuration;
    }

    /**
     * How long the animation has been running.
     *
     * This may be useful if you have to, say, 'undo' a running animation. With
     * {@linkplain #getRepeatCount() repeated animations,} this may be longer
     * than the {@linkplain #getDuration() duration.}
     *
     * @return How long the animation has been running.
     */
    public float getElapsedTime() {
        return mElapsedTime;
    }


    /*
     * Evaluates the animation at the specific time.
     * This allows the user to step the animation under program control
     * as opposed to having it run at the current frame rate.
     * Subclasses can override this function when creating new
     * types of animation. The default behavior is to call
     * {@link #animate(GVRHybridObject, float)}.
     * @param timeInSec elapsed time from animation start (seconds)
     */
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / mDuration;
        animate(mTarget, ratio);
    }

    /**
     * Override this to create a new animation. Generally, you do this by
     * changing some property of the {@code mTarget}, and letting GVRF handle
     * screen updates automatically.
     *
     * @param target
     *            The GVRF object to animate
     * @param ratio
     *            The start state is 0; the stop state is 1.
     */
    protected abstract void animate(GVRHybridObject target, float ratio);
}