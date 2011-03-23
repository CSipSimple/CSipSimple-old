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
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.csipsimple.utils.Log;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

	private static final String THIS_FILE = "Preview";

	SurfaceHolder mHolder;
	public Camera camera;

	private static final int HAS_FRAME = 0;
	private Handler previewHandler; 
	
	class PreviewHandler extends Handler {
		
		
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case HAS_FRAME:
				pushToNative( (byte[]) message.obj, message.arg1, message.arg2, 0);
				if(camera != null) {
					camera.setOneShotPreviewCallback(CameraPreview.this);
				}
				break;
			}
		}
	};

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
		previewHandler = new PreviewHandler();
		
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		camera = Camera.open();
		try {
			Log.d(THIS_FILE, ">>> Surface has been created guys !");
			camera.setPreviewDisplay(holder);
			// camera.setPreviewCallback(this);
			camera.setOneShotPreviewCallback(this);
		} catch (IOException e) {
			Log.e(THIS_FILE, "Error when setting holder", e);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		camera.stopPreview();

		camera = null;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = camera.getParameters();
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			Log.d(THIS_FILE, "Supported params " + size.width + " x " + size.height);
		}
		for (int sFormat : parameters.getSupportedPreviewFormats()) {
			Log.d(THIS_FILE, "Supported format " + sFormat);
		}

		int cw = parameters.getPreviewSize().width;
		int ch = parameters.getPreviewSize().height;
		Log.d(THIS_FILE, "Current format : " + cw + ", " + ch + " @" + parameters.getPreviewFrameRate());
		
		int fw = 352;
		int fh = 288;
		int fq = 15;
		parameters.setPreviewSize(fw, fh);
		parameters.setPreviewFrameRate(fq);
		Log.d(THIS_FILE, "Applied format : " + fw + ", " + fh + " @" + fq);

		camera.setParameters(parameters);

		camera.startPreview();
	}

	public void onPreviewFrame(byte[] data, Camera camera) {

		Log.d(THIS_FILE, ">>> On preview frame");
		
		Camera.Parameters parameters = camera.getParameters();
		int cw = parameters.getPreviewSize().width;
		int ch = parameters.getPreviewSize().height;

		if (previewHandler != null) {
			Message message = previewHandler.obtainMessage(HAS_FRAME, cw, ch, data);
			message.sendToTarget();
		} else {
			Log.d(THIS_FILE, "Got preview callback, but no handler for it");
		}
		
		
	}


    private native void pushToNative(byte[] in, int width, int height, int textureSize);

}
