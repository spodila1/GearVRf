package org.gearvrf;

import org.gearvrf.jassimp.AiWrapperProvider;
import org.gearvrf.utility.Log;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A single bone of a mesh.<p>
 *
 * A bone has a name by which it can be found in the frame hierarchy and by
 * which it can be addressed by animations. In addition it has a number of
 * influences on vertices. <p>
 *
 * This class is designed to be mutable, i.e., the returned collections are
 * writable and may be modified. <p>
 *
 * Instantaneous pose of the bone during animation is not part of this class,
 * but in {@code GVRSkeleton}. This allows multiple instances of the same mesh
 * and bones.
 */
public final class GVRBone extends GVRComponent implements PrettyPrint {
    /**
     * Constructor.
     */
    public GVRBone(GVRContext gvrContext) {
        super(gvrContext, NativeBone.ctor());
    }

    static public long getComponentType() {
        return NativeBone.getComponentType();
    }

    /**
     * Sets the name of the bone.
     * 
     * @param name the name of the bone.
     */
    public void setName(String name) {
        mName = name == null ? null : new String(name);
        NativeBone.setName(getNative(), mName);
    }

    /**
     * Returns the name of the bone.
     *
     * @return the name
     */
    public String getName() {
        // Name is currently read-only for native code. So it is
        // not updated from native object.
        return mName;
    }

    public void setOffsetMatrix(float[] offsetMatrix) {
        NativeBone.setOffsetMatrix(getNative(), offsetMatrix);
    }

    /**
     * Sets the final transform of the bone during animation.
     *
     * @param finalTransform The transform matrix representing
     * the bone's pose after computing the skeleton.
     */
    public void setFinalTransformMatrix(float[] finalTransform) {
        NativeBone.setFinalTransformMatrix(getNative(), finalTransform);
    }

    /**
     * Sets the final transform of the bone during animation.
     *
     * @param finalTransform The transform matrix representing
     * the bone's pose after computing the skeleton.
     */
    public void setFinalTransformMatrix(Matrix4f finalTransform) {
        float[] mat = new float[16];
        finalTransform.get(mat);
        NativeBone.setFinalTransformMatrix(getNative(), mat);
    }

    /**
     * Gets the final transform of the bone.
     *
     * @return the 4x4 matrix representing the final transform of the
     * bone during animation, which comprises bind pose and skeletal
     * transform at the current time of the animation.
     */
    public Matrix4f getFinalTransformMatrix() {
        final FloatBuffer fb = ByteBuffer.allocateDirect(4*4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        NativeBone.getFinalTransformMatrix(getNative(), fb);
        return new Matrix4f(fb);
    }

    /**
     * Returns the offset matrix.<p>
     *
     * The offset matrix is a 4x4 matrix that transforms from mesh space to
     * bone space in bind pose.<p>
     *
     * This method is part of the wrapped API (see {@link AiWrapperProvider}
     * for details on wrappers).
     *
     * @return the offset matrix
     */
    public Matrix4f getOffsetMatrix() {
        Matrix4f offsetMatrix = new Matrix4f();
        offsetMatrix.set(NativeBone.getOffsetMatrix(getNative()));
        return offsetMatrix;
    }

    /**
     * Gets the scene object of this bone.
     *
     * @return the scene object that represents this bone in a
     *         hierarchy of bones.
     */
    public GVRSceneObject getSceneObject() {
        return mSceneObject;
    }

    /**
     * Sets the scene object of this bone.
     *
     * @param sceneObject The scene object that represents this
     * bone in a hierarchy of bones.
     */
    public void setSceneObject(GVRSceneObject sceneObject) {
        mSceneObject = sceneObject;
    }

    /**
     * Pretty-print the object.
     */
    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(GVRBone.class.getSimpleName());
        sb.append(" [name=" + getName()
                + ", offsetMatrix=" + getOffsetMatrix()
                + ", finalTransformMatrix=" + getFinalTransformMatrix() // crashes debugger
                + "]");
        sb.append(System.lineSeparator());
    }

    /**
     * Returns a string representation of the object.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

    /**
     * Name of the bone.
     */
    private String mName;

    /**
     * The scene object that represents the transforms of the
     * bone.
     */
    private GVRSceneObject mSceneObject;
}

class NativeBone {
    static native long ctor();
    static native long getComponentType();
    static native void setName(long object, String mName);
    static native void setOffsetMatrix(long object, float[] offsetMatrix);
    static native float[] getOffsetMatrix(long object);
    static native void setFinalTransformMatrix(long object, float[] offsetMatrix);
    static native void getFinalTransformMatrix(long object, FloatBuffer output);
}