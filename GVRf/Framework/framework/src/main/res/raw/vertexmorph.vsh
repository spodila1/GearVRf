#if defined(HAS_blendshapeTexture)
	for (int i = 0; i < u_numblendshapes; ++i)
	{
	    float x = float(i);
	    float y = float(gl_VertexID);

	    vertex.local_position += u_weights[i] * texture(blendshapeTexture, vec2(x, y));
	}
#endif