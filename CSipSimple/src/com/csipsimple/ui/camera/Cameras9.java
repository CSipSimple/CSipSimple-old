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

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;

public class Cameras9 extends CamerasWrapper {

	@Override
	public Camera getCamera(boolean useFFC) {
		int maxCam = Camera.getNumberOfCameras();
		for(int i=0; i<maxCam ; i++) {
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(i, cameraInfo );
			if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT && useFFC) {
				return Camera.open(i);
			}else if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK && !useFFC) {
				return Camera.open(i);
			}
		}
		return Camera.open();
	}

	
}
