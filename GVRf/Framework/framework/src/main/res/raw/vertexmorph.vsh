#if defined(HAS_blendshapeTexture)
	for (int i = 0; i < u_numblendshapes; ++i)
	{
	    //vertex.local_position += u_blendweights[i] * texelFetch(blendshapeTexture, ivec2(i, gl_VertexID), 0);
	}
	vertex.local_position =  texelFetch(blendshapeTexture, ivec2(0, gl_VertexID), 0);

#endif