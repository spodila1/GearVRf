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

package org.gearvrf;

import static android.opengl.GLES30.GL_RGB32F;

public class GVRMeshMorph extends GVRBehavior
{
    static private long TYPE_MESHMORPH = newComponentType(GVRMeshMorph.class);

    final protected int mNumBlendShapes;
    final protected boolean mMorphNormals;
    protected int mFloatsPerVertex;
    protected int mTexWidth;
    protected int mTexHeight;
    protected float[] mWeights;
    protected float[] mBlendShapeDiffs;
    protected float[] mBaseBlendShape;

    GVRMeshMorph(GVRContext ctx, int numBlendShapes, boolean morphNormals)
    {
        super(ctx, 0);
        mType = getComponentType();
        mNumBlendShapes = numBlendShapes;
        mMorphNormals = morphNormals;
        if (numBlendShapes <= 0)
        {
            throw new IllegalArgumentException("Number of blend shapes must be positive");
        }
        mFloatsPerVertex = 3;
        mTexWidth = numBlendShapes * 3; // 3 floats for position
        if (morphNormals)
        {
            mTexWidth *= 2;             // 3 more for normal
            mFloatsPerVertex *= 3;
        }
    }

    static public long getComponentType() { return TYPE_MESHMORPH; }

    public void onAttach(GVRSceneObject sceneObj)
    {
        super.onAttach(sceneObj);
        GVRComponent comp = getComponent(GVRRenderData.getComponentType());
        if (comp == null)
        {
            throw new IllegalStateException("Cannot attach a morph to a scene object without a base mesh");
        }
        GVRMesh mesh = ((GVRRenderData) comp).getMesh();
        if (mesh == null)
        {
            throw new IllegalStateException("Cannot attach a morph to a scene object without a base mesh");
        }
        GVRShaderData mtl = getMaterial();
        if ((mtl == null) || !mtl.getTextureNames().contains("blendshapeTexture"))
        {
            throw new IllegalStateException("Scene object shader does not support morphing");
        }
        mtl.setFloat("u_numblendshapes", mNumBlendShapes);
        initialize(mesh.getVertexBuffer());
    }

    public void onDetach(GVRSceneObject sceneObj)
    {
        mBlendShapeDiffs = null;
        mBaseBlendShape = null;
        mTexHeight = 0;
    }

    protected void initialize(GVRVertexBuffer baseShape)
    {
        mTexHeight = baseShape.getVertexCount();
        if (mTexHeight <= 0)
        {
            throw new IllegalArgumentException("Base shape has no vertices");
        }
        mBaseBlendShape = new float[mFloatsPerVertex * mTexHeight];
        mWeights = new float[mTexWidth];
        mBlendShapeDiffs = new float[mTexWidth * mTexHeight];
    }

    protected void copyBlendShape(int shapeofs, int baseofs, float[] vec3data)
    {
        int numVerts = vec3data.length;
        if (mBaseBlendShape == null)
        {
            throw new IllegalStateException("Must be attached to a scene object to set blend shapes");
        }
        if (mTexHeight != numVerts)
        {
            throw new IllegalArgumentException("All blend shapes must have the same number of vertices");
        }

        for (int i = 0; i < numVerts; ++i)
        {
            int b = i * mTexWidth;
            int s = shapeofs + b;
            b += baseofs;
            mBlendShapeDiffs[s] = vec3data[i * 3] - mBaseBlendShape[b];
            mBlendShapeDiffs[s + 1] = vec3data[i * 3 + 1] - mBaseBlendShape[b + 1];
            mBlendShapeDiffs[s + 2] = vec3data[i * 3 + 2] - mBaseBlendShape[b + 2];
        }
    }

    protected void copyBaseShape(GVRVertexBuffer vbuf)
    {
        float[] vec3data = vbuf.getFloatArray("a_position");
        mTexHeight = vbuf.getVertexCount();
        for (int i = 0; i < mTexHeight; ++i)
        {
            int t = i * mTexWidth;
            mBaseBlendShape[t] = vec3data[i * 3];
            mBaseBlendShape[t + 1] = vec3data[i * 3 + 1];
            mBaseBlendShape[t + 2] = vec3data[i * 3 + 2];
        }
    }

    public void setWeight(int index, float weight)
    {
        mWeights[index] = weight;
    }

    public float getWeight(int index)
    {
        return mWeights[index];
    }

    public void setWeights(float[] weights)
    {
        GVRMaterial mtl = getMaterial();
        System.arraycopy(weights, 0, mWeights, 0, mWeights.length);
        if (mtl != null)
        {
            mtl.setFloatArray("u_blendweights", mWeights);
        }
    }

    public void setBlendShape(int index, GVRVertexBuffer vbuf)
    {
        copyBlendShape(index * mFloatsPerVertex, 0, vbuf.getFloatArray("a_position"));
        if (mMorphNormals)
        {
            copyBlendShape(index * mFloatsPerVertex + 3, 3, vbuf.getFloatArray("a_normal"));
        }
    }

    public void setBlendPositions(int index, float[] vec3data)
    {
        copyBlendShape(index * mFloatsPerVertex, 0, vec3data);
    }

    public void setBlendNormals(int index, float[] vec3data)
    {
        copyBlendShape(index * mFloatsPerVertex + 3, 3, vec3data);
    }

    private GVRMaterial getMaterial()
    {
        GVRComponent comp = getComponent(GVRRenderData.getComponentType());
        if (comp == null)
        {
            return null;
        }
        return ((GVRRenderData) comp).getMaterial();
    }

    public boolean update()
    {
        GVRTexture blendshapeTex;
        GVRFloatImage blendshapeImage;
        GVRMaterial mtl = getMaterial();

        if ((mBlendShapeDiffs == null) || (mtl == null))
        {
            return false;
        }
        if (mtl.hasTexture("blendshapeTexture"))
        {
            blendshapeTex = mtl.getTexture("blendShapeTexture");
            blendshapeImage = (GVRFloatImage) blendshapeTex.getImage();
        }
        else
        {
            blendshapeImage = new GVRFloatImage(getGVRContext(), GL_RGB32F);
            blendshapeTex = new GVRTexture(getGVRContext());
            blendshapeTex.setImage(blendshapeImage);
            mtl.setTexture("blendshapeTexture", blendshapeTex);
        }
        blendshapeImage.update(mTexWidth, mTexHeight, mBlendShapeDiffs);
        return true;
    }
}
