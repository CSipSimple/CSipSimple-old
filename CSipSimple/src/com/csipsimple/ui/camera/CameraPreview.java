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
package com.csipsimple.ui.camera;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.csipsimple.utils.Log;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

	private static final String THIS_FILE = "Preview";

	SurfaceHolder mHolder;
	public Camera camera;

	CameraPreview(Context context) {
		super(context);
		installHolder();
	}
	
	public CameraPreview(Context context, AttributeSet attr) {
		super(context, attr);
		installHolder();
	}
	
	private void installHolder() {
		
		

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		camera = Camera.open();
		try {
			camera.setPreviewDisplay(holder);

			camera.setPreviewCallback(new PreviewCallback() {

				public void onPreviewFrame(byte[] data, Camera arg1) {
					//Log.d(THIS_FILE, "We get some datas..." + data);
					CameraPreview.this.invalidate();
				}
			});

		} catch (IOException e) {
			Log.e(THIS_FILE, "Error when setting holder", e);
		}

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		camera.stopPreview();
		
		camera = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = camera.getParameters();
		for(Camera.Size size : parameters.getSupportedPreviewSizes()) {
			Log.d(THIS_FILE, "Supported params "+ size.width+" x "+size.height);
		}
		for(int sFormat : parameters.getSupportedPreviewFormats()) {
			Log.d(THIS_FILE, "Supported format "+ sFormat);
		}
		
	//	parameters.setPreviewSize(w, h);
		parameters.setRotation(0);
		camera.setParameters(parameters);
		
		camera.startPreview();
	}

	/*
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		Paint p = new Paint(Color.RED);
		Log.d(THIS_FILE, "draw");
		canvas.drawText("PREVIEW", canvas.getWidth() / 2, canvas.getHeight() / 2, p);
	}
	*/
}
