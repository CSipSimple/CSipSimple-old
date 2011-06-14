/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.widgets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.pjsip.pjsua.pjsua;


import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;

public class VideoRenderer implements Renderer {

	// private static final String THIS_FILE = "TestVideoRenderer";
	// private Context context;

	public VideoRenderer(Context aContext) {
		// context = aContext;
	}

	private Integer streamId = null;

	public void setStream(int aStreamId) {
		streamId = aStreamId;
	}

	private IntBuffer texturesBuffer = null;

	private void loadTextures(GL10 gl) {
		if (texturesBuffer != null) {
			//gl.glDeleteTextures(1, texturesBuffer);
			return;
		}
		
		// create textures
		texturesBuffer = IntBuffer.allocate(1);
		gl.glGenTextures(1, texturesBuffer);

		// setup video texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(0));
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			

		

	}

	private static float TOP_X = 1f;
	private static float TOP_Y = 1f;
	private static float BOT_X = 0f;
	private static float BOT_Y = 0f;

	private static float[] quadCoords = new float[] {
		BOT_X, TOP_Y, 0, 
		TOP_X, TOP_Y, 0, 
		BOT_X, BOT_Y, 0, 
		TOP_X, BOT_Y, 0 };

	private static float[] quadTexCoords = new float[] { 
		0, 0, 
		1, 0, 
		0, 1, 
		1, 1 };

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glClearColor(0, 0, 0, 0);

		gl.glClearDepthf(1.0f);
	}
	

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		
		// avoid division by zero
		if (height == 0) {
			height = 1;
		}
		

		//Disable unnessary things
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glDisable(GL10.GL_CULL_FACE);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		
		// draw on the entire screen
		gl.glViewport(0, 0, width, height);
		// setup projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		// Switch to ortho projection
		gl.glOrthof(0f, 1.0f, 0f, 1.0f, -1.0f, 1.0f);


		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		loadTextures(gl);
	}

	protected static FloatBuffer makeFloatBuffer(float[] arr) {
		ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(arr);
		fb.position(0);
		return fb;
	}

	private boolean isRendering = false;
	
	public void onDrawFrame(GL10 gl) {
		
		// Reset for this draw seq
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        
        
        // Recompute texture 
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(0));

		float[] w = new float[] { 0.0f };
		float[] h = new float[] { 0.0f };
		
		// TODO : use stream id here to manage several streams 
		if(streamId == null) {
			//We should return or display a default image
		}
		// -- end of placeholder
		
		pjsua.pjmedia_ogl_surface_draw(w, h);
		
		if(w[0] > 0 && h[0] > 0) {
			if(!isRendering) {
				if(renderListener != null ) {
					renderListener.onRenderDisplayed(true);
				}
				isRendering = true;
			}
		//	Log.d("VideoRenderer", "Has been drawn at "+w[0]+","+h[0]);
			quadTexCoords[2] = w[0];
			quadTexCoords[6] = w[0];
			quadTexCoords[5] = h[0];
			quadTexCoords[7] = h[0];
			
			// draw quad
			gl.glTranslatef(0, 0, -0.5f);
		//	gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f);
	
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, makeFloatBuffer(quadCoords));
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, makeFloatBuffer(quadTexCoords));
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		}else {
			if(isRendering) {
				if(renderListener != null ) {
					renderListener.onRenderDisplayed(false);
				}
				isRendering = false;
			}
		}
	}

	private OnRenderListener renderListener = null;
	public interface OnRenderListener {
		public void onRenderDisplayed(boolean show);
	}
	
	public void setRenderListener(OnRenderListener renderListener) {
		this.renderListener = renderListener;
	}

}
