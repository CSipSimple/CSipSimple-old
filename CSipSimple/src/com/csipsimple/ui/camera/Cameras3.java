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

import java.lang.reflect.Method;

import android.hardware.Camera;
import android.util.Log;

public class Cameras3 extends CamerasWrapper {

	private static final String THIS_FILE = "Camera3";

	@Override
	public Camera getCamera(boolean useFFC) {
		if (!useFFC) {
			return Camera.open();
		} else {
			Camera camera = null;

			if (FFC.isAvailable()) {
				camera = FFC.getCamera();
			}
			if (camera == null) {
				camera = Camera.open();
			}
			FFC.switchToFFC(camera);

			return camera;
		}
	}

	/* ================================================== */
	static class FFC {

		private final String className;
		private final String methodName;

		private static Method DualCameraSwitchMethod;
		private static int ffc_index = -1;
		static FFC FFC_VALUES[] = { new FFC("android.hardware.HtcFrontFacingCamera", "getCamera"),
				// Sprint: HTC EVO 4G and Samsung Epic 4G
				// DO not forget to change the manifest if you are using OS 1.6
				// and later
				new FFC("com.sprint.hardware.twinCamDevice.FrontFacingCamera", "getFrontFacingCamera"),
				// Huawei U8230
				new FFC("android.hardware.CameraSlave", "open"),
		// To be continued...
		// Default: Used for test reflection
		// --new FFC("android.hardware.Camera", "open"),
		};

		static {

			//
			//
			//
			int index = 0;
			for (FFC ffc : FFC.FFC_VALUES) {
				try {
					Class.forName(ffc.className).getDeclaredMethod(ffc.methodName);
					FFC.ffc_index = index;
					break;
				} catch (Exception e) {
					Log.d(THIS_FILE, e.toString());
				}

				++index;
			}

			//
			//
			//
			try {
				FFC.DualCameraSwitchMethod = Class.forName("android.hardware.Camera").getMethod("DualCameraSwitch", int.class);
			} catch (Exception e) {
				Log.e(THIS_FILE, e.toString());
			}
		}

		private FFC(String className, String methodName) {
			this.className = className;
			this.methodName = methodName;
		}

		static boolean isAvailable() {
			return (FFC.ffc_index != -1);
		}

		static Camera getCamera() {
			try {
				Method method = Class.forName(FFC.FFC_VALUES[FFC.ffc_index].className).getDeclaredMethod(FFC.FFC_VALUES[FFC.ffc_index].methodName);
				return (Camera) method.invoke(null);
			} catch (Exception e) {
				Log.e(THIS_FILE, e.toString());
			}
			return null;
		}

		static void switchToFFC(Camera camera) {
			try {
				if (FFC.DualCameraSwitchMethod == null) { // Samsung Galaxy S,
															// Epic 4G, ...
					Camera.Parameters parameters = camera.getParameters();
					Log.d(THIS_FILE, "Camera params " + parameters.flatten());
					parameters.set("camera-id", 2);
					camera.setParameters(parameters);
				} else { // Dell Streak, ...
					FFC.DualCameraSwitchMethod.invoke(camera, (int) 1);
				}
			} catch (Exception e) {
				Log.e(THIS_FILE, e.toString());
			}

		}
	}

}
