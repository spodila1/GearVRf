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

package org.gearvrf.mixedreality;

import android.graphics.Bitmap;

import org.gearvrf.GVRBehavior;
import org.gearvrf.GVRContext;
import org.gearvrf.GVREventListeners;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.IActivityEvents;
import org.gearvrf.mixedreality.arcore.ARCoreSession;
import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * Component to enable AR functionalities on GVRf.
 */
public class GVRMixedReality extends GVRBehavior implements IMRCommon {
    static private long TYPE_MIXEDREALITY = newComponentType(GVRMixedReality.class);
    private final IActivityEvents mActivityEventsHandler;
    private final MRCommon mSession;
    private SessionState mState;
    private Vector3f mTempVec1 = new Vector3f();
    private Vector3f mTempVec2 = new Vector3f();

    /**
     * Create a instace of GVRMixedReality component.
     *
     * @param gvrContext
     */
    public GVRMixedReality(final GVRContext gvrContext)
    {
        this(gvrContext, false);
    }

    /**
     * Create a instace of GVRMixedReality component and specifies the use of cloud anchors.
     *
     * @param gvrContext
     * @param enableCloudAnchor
     */
    public GVRMixedReality(final GVRContext gvrContext, boolean enableCloudAnchor)
    {
        this(gvrContext.getMainScene(), enableCloudAnchor);
    }

    /**
     * Create a instance of GVRMixedReality component and add it to the specified scene.
     *
     * @param scene
     */
    public GVRMixedReality(GVRScene scene)
    {
        this(scene, false);
    }

    /**
     * Default GVRMixedReality constructor. Create a instace of GVRMixedReality component, set
     * the use of cloud anchors and add it to the specified scened.
     *
     * @param scene
     */
    public GVRMixedReality(GVRScene scene, boolean enableCloudAnchor)
    {
        super(scene.getGVRContext());
        mType = getComponentType();
        mActivityEventsHandler = new ActivityEventsHandler();
        mSession = new ARCoreSession(scene.getGVRContext(), enableCloudAnchor);
        mState = SessionState.ON_PAUSE;
        scene.getMainCameraRig().getOwnerObject().attachComponent(this);
    }

    static public long getComponentType() { return TYPE_MIXEDREALITY; }

    public float getARToVRScale() { return mSession.getARToVRScale(); }
    @Override
    public void resume() {
        if (mState == SessionState.ON_RESUME) {
            return;
        }
        mSession.resume();
        mState = SessionState.ON_RESUME;
    }

    @Override
    public void pause() {
        if (mState == SessionState.ON_PAUSE) {
            return;
        }
        mSession.pause();
        mState = SessionState.ON_PAUSE;
    }

    @Override
    public GVRSceneObject getPassThroughObject() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getPassThroughObject();
    }

    @Override
    public void registerPlaneListener(IPlaneEventsListener listener) {
        mSession.registerPlaneListener(listener);
    }

    @Override
    public void registerAnchorListener(IAnchorEventsListener listener) {
        mSession.registerAnchorListener(listener);
    }

    @Override
    public void registerAugmentedImageListener(IAugmentedImageEventsListener listener) {
        mSession.registerAugmentedImageListener(listener);
    }

    @Override
    public ArrayList<GVRPlane> getAllPlanes() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getAllPlanes();
    }


    @Override
    public GVRAnchor createAnchor(float[] pose) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.createAnchor(pose);
    }

    @Override
    public GVRAnchor createAnchor(float[] pose, GVRSceneObject sceneObject) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.createAnchor(pose, sceneObject);
    }

    @Override
    public void updateAnchorPose(GVRAnchor anchor, float[] pose) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        mSession.updateAnchorPose(anchor, pose);
    }

    @Override
    public void removeAnchor(GVRAnchor anchor) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        mSession.removeAnchor(anchor);
    }

    @Override
    public void hostAnchor(GVRAnchor anchor, ICloudAnchorListener listener) {
        mSession.hostAnchor(anchor, listener);
    }

    @Override
    public void resolveCloudAnchor(String anchorId, ICloudAnchorListener listener) {
        mSession.resolveCloudAnchor(anchorId, listener);
    }

    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor) {
        mSession.setEnableCloudAnchor(enableCloudAnchor);
    }

    @Override
    public GVRHitResult hitTest(GVRPicker.GVRPickedObject collision)
    {
        if (mState == SessionState.ON_PAUSE)
        {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        collision.picker.getWorldPickRay(mTempVec1, mTempVec2);
        if (collision.hitObject != getPassThroughObject())
        {
            mTempVec2.set(collision.hitLocation[0],
                          collision.hitLocation[1],
                          collision.hitLocation[2]);
        }
        GVRPicker.GVRPickedObject hit = GVRPicker.pickSceneObject(getPassThroughObject(), mTempVec1.x, mTempVec1.y, mTempVec1.z,
                                                                  mTempVec2.x, mTempVec2.y, mTempVec2.z);
        if (hit == null)
        {
            return null;
        }
        return mSession.hitTest(hit);
    }

    @Override
    public GVRLightEstimate getLightEstimate() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getLightEstimate();
    }

    @Override
    public void setAugmentedImage(Bitmap image) {
        mSession.setAugmentedImage(image);
    }

    @Override
    public void setAugmentedImages(ArrayList<Bitmap> imagesList) {
        mSession.setAugmentedImages(imagesList);
    }

    @Override
    public ArrayList<GVRAugmentedImage> getAllAugmentedImages() {
        return mSession.getAllAugmentedImages();
    }

    private class ActivityEventsHandler extends GVREventListeners.ActivityEvents {
        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onResume() {
            resume();
        }
    }

    private enum SessionState {
        ON_RESUME,
        ON_PAUSE
    };
}
