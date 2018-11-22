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

import org.gearvrf.GVRHybridObject;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.animation.keyframe.GVRAnimationChannel;
import org.gearvrf.animation.keyframe.GVRFloatAnimation;
import org.gearvrf.animation.keyframe.GVRQuatAnimation;
import org.gearvrf.animation.keyframe.GVRSkeletonAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.gearvrf.animation.GVRPose.Bone;
import org.gearvrf.utility.Log;

public class GVRPoseInterpolator extends GVRAnimation
{
    private GVRPose initialPose;
    private GVRPose finalPose;
    private GVRSkeleton pSkeleton;
    private Bone[] mBones;

    private Vector3f poseOnePos;
    private Vector3f poseTwoPos;
    private Vector3f poseOneScl;
    private Vector3f poseTwoScl;
    private Quaternionf poseOneRot;
    private Quaternionf poseTwoRot;

    private Vector3f tempVec;
    private Quaternionf tempQuat;

    private GVRQuatAnimation mRotInterpolator;
    private GVRFloatAnimation mPosInterpolator;
    private GVRFloatAnimation mSclInterpolator;

    private float pDuration;
    private float[] rotData;
    private float[] posData;
    private float[] sclData;
    private float[] poseData;
    private float[] posIData;
    private float[] sclIData;
    private float[] rotIData;

    private int poseDataSize;
    private int initialPosIndex;
    private int finalPosIndex;
    private int initialRotIndex;
    private int finalRotIndex;
    private int initialSclIndex;
    private int finalSclIndex;
    private float startTime;
    private float endTime;
    private int startTimeIndex;
    private int endTimeIndex;
    private int offset;
    private Matrix4f mat;
    private GVRSceneObject modelTarget;

    //dynamic update
    private GVRSkeletonAnimation skelAnimOne;
    private GVRSkeletonAnimation skelAnimTwo;
    private GVRSkeleton dskeleton;
    private GVRPose combinePose;
    private float[][] posBlend;
    private float[][] rotBlend;
    private float[][] sclBlend;
    private int check =0;
    private int repear =0;
    private GVRPose poseVn = null;
    private GVRPose poseTw = null;
   // GVRPose firstPose = null;


    //dynamic update

    public GVRPoseInterpolator(GVRSceneObject target, float duration, GVRSkeletonAnimation skelOne, GVRSkeletonAnimation skelTwo, GVRSkeleton skeleton)
    {
        super(target, duration);
        //skelAnimOne = new GVRSkeletonAnimation(skelOne);
        skelAnimOne = skelOne;
        skelAnimTwo = skelTwo;
        dskeleton = skeleton;

        rotData = new float[10];
        posData = new float[8];
        sclData = new float[8];
        posData[0]=0;
        rotData[0]=0;
        sclData[0]=0;
        posIData = new float[3];
        sclIData = new float[3];
        rotIData = new float[4];
        mat = new Matrix4f();
        posBlend = new float[skelAnimOne.getSkeleton().getNumBones()][6];
        rotBlend = new float[skelAnimOne.getSkeleton().getNumBones()][8];
        sclBlend = new float[skelAnimOne.getSkeleton().getNumBones()][6];

        GVRPose firstPose = skelAnimOne.computePose(skelAnimOne.getDuration()-pDuration,skelAnimOne.getSkeleton().getPose());
       // poseVn = skelAnimOne.getSkeleton().getPose();
        poseVn = (skelAnimOne.computePose(skelAnimOne.getDuration()-pDuration,skelAnimOne.getSkeleton().getPose()));

      //  dskeleton.setPose(firstPose);
        GVRPose secondPose = skelAnimTwo.computePose(0,skelAnimTwo.getSkeleton().getPose());
        poseTw =(skelAnimTwo.computePose(0,skelAnimTwo.getSkeleton().getPose()));
        for(int j =0; j<firstPose.getNumBones();j++)
        {

            Vector3f poss = new Vector3f(0,0,0);
            firstPose.getLocalPosition(j,poss);
            Vector3f possT = new Vector3f(0,0,0);
            secondPose.getLocalPosition(j,possT);


            //quaternion

            Quaternionf q1 = new Quaternionf(0,0,0,1);
            firstPose.getLocalRotation(j,q1);
            Quaternionf q2 = new Quaternionf(0,0,0,1);
            secondPose.getLocalRotation(j,q2);
            //  combinePose.setLocalRotation(j,q1.x,q1.y,q1.z,q1.w);


            posBlend[j][0] = poss.x;
            posBlend[j][1] = poss.y;
            posBlend[j][2] = poss.z;
            posBlend[j][3] = possT.x;
            posBlend[j][4] = possT.y;
            posBlend[j][5] = possT.z;

            rotBlend[j][0] = q1.x;
            rotBlend[j][1] = q1.y;
            rotBlend[j][2] = q1.z;
            rotBlend[j][3] = q1.w;
            rotBlend[j][4] = q2.x;
            rotBlend[j][5] = q2.y;
            rotBlend[j][6] = q2.z;
            rotBlend[j][7] = q2.w;


            Vector3f scl = new Vector3f(0,0,0);
            firstPose.getLocalScale(j,scl);
            Vector3f sclT = new Vector3f(0,0,0);
            secondPose.getLocalScale(j,sclT);

            sclBlend[j][0] = scl.x;
            sclBlend[j][1] = scl.y;
            sclBlend[j][2] = scl.z;
            sclBlend[j][3] = sclT.x;
            sclBlend[j][4] = sclT.y;
            sclBlend[j][5] = sclT.z;

      }
        pDuration = duration;
        repear =1;


    }


    public void setrepear()
    {
        repear=1;
    }


    public void getSecondPose(float timer)
    {
        GVRPose firstPose = null;// skelAnimOne.computePose(skelAnimOne.getDuration()-pDuration+timer,skelAnimOne.getSkeleton().getPose());
        GVRPose secondPose =null;// skelAnimTwo.computePose(0+timer,skelAnimTwo.getSkeleton().getPose());
        GVRPose fs = null;
        GVRPose ss = null;
        //float blendFac = 0.5f;

        if(repear==2)
       {
           firstPose = skelAnimOne.computePose(skelAnimOne.getDuration()-pDuration+timer,poseVn);
           secondPose = skelAnimTwo.computePose(0+timer,poseTw);
           Log.i("printfirstpose","posece"+timer);
           fs = new GVRPose(poseVn);
           ss =  new GVRPose(poseTw);
           // firstPose = poseVn;
           repear++;
       }
       else
       {

           fs = skelAnimOne.getSkeleton().getPose();
           ss = poseTw;
           firstPose = skelAnimOne.computePose(skelAnimOne.getDuration()-pDuration+timer,fs);
           secondPose = skelAnimTwo.computePose(0+timer,ss);

       }


        float mul = 1/pDuration;


        for(int j =0; j<skelAnimOne.getSkeleton().getNumBones();j++)
        {
            if(j==0)
            {
                Log.i("printjvalue","pos x"+posBlend[j][3]+" pos y "+posBlend[j][4]+" pos z "+posBlend[j][5]);
            }
            Vector3f poss = new Vector3f(0,0,0);
            firstPose.getLocalPosition(j,poss);
            Vector3f possT = new Vector3f(0,0,0);
            secondPose.getLocalPosition(j,possT);

            posBlend[j][3] = ((pDuration-timer)*mul*poss.x)+(possT.x*(timer*mul));
            posBlend[j][4] = ((pDuration-timer)*mul*poss.y)+(possT.y*timer*mul);
            posBlend[j][5] = ((pDuration-timer)*mul*poss.z)+(possT.z*timer*mul);

            Quaternionf q1 = new Quaternionf(0,0,0,1);
            firstPose.getLocalRotation(j,q1);
            Quaternionf q2 = new Quaternionf(0,0,0,1);
            secondPose.getLocalRotation(j,q2);

            rotBlend[j][4] = ((pDuration-timer)*mul*q1.x)+(q2.x*timer*mul);
            rotBlend[j][5] = ((pDuration-timer)*mul*q1.y)+(q2.y*timer*mul);
            rotBlend[j][6] = ((pDuration-timer)*mul*q1.z)+(q2.z*timer*mul);
            rotBlend[j][7] = ((pDuration-timer)*mul*q1.w)+(q2.w*timer*mul);


            Vector3f scl = new Vector3f(0,0,0);
            firstPose.getLocalScale(j,scl);
            Vector3f sclT = new Vector3f(0,0,0);
            secondPose.getLocalScale(j,sclT);

            sclBlend[j][3] = ((pDuration-timer)*mul*scl.x)+(sclT.x*timer*mul);
            sclBlend[j][4] = ((pDuration-timer)*mul*scl.y)+(sclT.y*timer*mul);
            sclBlend[j][5] = ((pDuration-timer)*mul*scl.z)+(sclT.z*timer*mul);


        }
    }
    protected void animate(GVRHybridObject target, float ratio)
    {
        Log.i("printAnima","enni");

        animate(pDuration * ratio);
    }

    public void animate(float timer) {

        double roundOff = Math.round(timer * 100.0) / 100.0;
        if(roundOff==0.01||roundOff==0.0)
        {


            return;
        }
        if(repear==1)
        {
          Log.i("printoneTime"," print ");
           initialPose = poseVn;
            mat = new Matrix4f();
            for (int i = 0; i < dskeleton.getNumBones(); ++i) {

                Vector3f poss = new Vector3f(0,0,0);
                poseVn.getLocalPosition(i,poss);


                //quaternion

                Quaternionf q1 = new Quaternionf(0,0,0,1);
                poseVn.getLocalRotation(i,q1);

                posBlend[i][0] = poss.x;
                posBlend[i][1] = poss.y;
                posBlend[i][2] = poss.z;

                rotBlend[i][0] = q1.x;
                rotBlend[i][1] = q1.y;
                rotBlend[i][2] = q1.z;
                rotBlend[i][3] = q1.w;

                Vector3f scl = new Vector3f(0,0,0);
                poseVn.getLocalScale(i,scl);


                sclBlend[i][0] = scl.x;
                sclBlend[i][1] = scl.y;
                sclBlend[i][2] = scl.z;



        }
         repear++;

        }
        else if(repear>2)
        {
            initialPose = poseVn;//dskeleton.getPose();
        }

        posData[4]=timer;
        sclData[4]=timer;
        rotData[5]=timer;
        // getFirstPose();
        getSecondPose(timer);

        for (int i = 0; i < dskeleton.getNumBones(); ++i)
        {


            posData[1]= posBlend[i][0];
            posData[2]= posBlend[i][1];
            posData[3]= posBlend[i][2];
            posData[5]= posBlend[i][3];
            posData[6]= posBlend[i][4];
            posData[7]= posBlend[i][5];


            rotData[1]= rotBlend[i][0];
            rotData[2]= rotBlend[i][1];
            rotData[3]= rotBlend[i][2];
            rotData[4]= rotBlend[i][3];
            rotData[6]= rotBlend[i][4];
            rotData[7]= rotBlend[i][5];
            rotData[8]= rotBlend[i][6];
            rotData[9]= rotBlend[i][7];

            sclData[1]= sclBlend[i][0];
            sclData[2]= sclBlend[i][1];
            sclData[3]= sclBlend[i][2];
            sclData[5]= sclBlend[i][3];
            sclData[6]= sclBlend[i][4];
            sclData[7]= sclBlend[i][5];

            mPosInterpolator = new GVRFloatAnimation(posData, 4);
            mRotInterpolator = new GVRQuatAnimation(rotData);
            mSclInterpolator = new GVRFloatAnimation(sclData, 4);
            mPosInterpolator.animate(timer,posIData);
            mRotInterpolator.animate(timer,rotIData);
            mSclInterpolator.animate(timer,sclIData);
            mat.translationRotateScale(posIData[0], posIData[1], posIData[2],rotIData[0], rotIData[1], rotIData[2], rotIData[3],sclIData[0], sclIData[1], sclIData[2]);
            initialPose.setLocalMatrix(i, mat);

            posBlend[i][0] = posIData[0];
            posBlend[i][1] = posIData[1];
            posBlend[i][2] = posIData[2];

            rotBlend[i][0] = rotIData[0];
            rotBlend[i][1] = rotIData[1];
            rotBlend[i][2] = rotIData[2];
            rotBlend[i][3] = rotIData[3];

            sclBlend[i][0] = sclIData[0];
            sclBlend[i][1] = sclIData[1];
            sclBlend[i][2] = sclIData[2];



        }

        dskeleton.poseToBones();
        dskeleton.updateBonePose();
        dskeleton.updateSkinPose();

        posData[0]=timer;
        rotData[0]=timer;
        sclData[0] = timer;



    }



}