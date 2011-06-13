/*
* Copyright (C) 2010 Mamadou Diop.
* Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
*
* Contact: Mamadou Diop <diopmamadou(at)doubango.org>
*
* This file was part of imsdroid Project (http://code.google.com/p/imsdroid)
* And has been copied into CSipSimple project 
* Big thanks to imsdroid guys and CSipSimple project contributors should contribute fixes on this class to imsdroid project !!!
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 3
* of the License, or (at your option) any later version.
*
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
*       @author Mamadou Diop <diopmamadou(at)doubango.org>
*       @author Alex Vishnev
*               - Add support for rotation
*               - Camera toggle
*/
package com.csipsimple.ui.camera;


import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;



import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

public class VideoProducer {

        private static String THIS_FILE = VideoProducer.class.getCanonicalName();

        private static int WIDTH = 352;
        private static int HEIGHT = 288;
        private static int FPS = 15;
        private static final int CALLABACK_BUFFERS_COUNT = 3;

        private Context context;
        private int width;
        private int height;
        private int fps;
        private ByteBuffer frame;
        private Preview preview;
        private Camera camera;
        private boolean running;
        private boolean toggle;


        public VideoProducer(Context context){
        		this.context = context;
                this.width = VideoProducer.WIDTH;
                this.height = VideoProducer.HEIGHT;
                this.fps = VideoProducer.FPS;
                this.toggle = false;
        }


        private native void pushToNative(byte[] in, int size);

        public Camera getCamera(){
                return this.camera;
        }

        public void pushBlankPacket(){
                if(this.frame != null){
                        ByteBuffer buffer = ByteBuffer.allocateDirect(this.frame.capacity());
                        pushToNative(buffer.array(), buffer.capacity());
                }
        }

        public void setCamera(Camera cam) {
                this.camera = cam;
        }

        public void toggleCamera(LinearLayout llVideoLocal){
                if (this.preview != null) {
                        this.toggle = !this.toggle;
                        this.reset();
                        this.start();
                        final View local_preview = startPreview();
                        if(local_preview != null){
                                final ViewParent viewParent = local_preview.getParent();
                                if(viewParent != null && viewParent instanceof ViewGroup){
                                        ((ViewGroup)(viewParent)).removeView(local_preview);
                                }
                                llVideoLocal.addView(local_preview);
                                llVideoLocal.setVisibility(View.VISIBLE);
                        }
                }

        }

        private void addCallbackBuffer(Camera camera, byte[] buffer) {
                try {
                        APILevel7.addCallbackBufferMethod.invoke(camera, buffer);
                } catch (Exception e) {
                        Log.e(THIS_FILE, e.toString());
                }
        }

        private void setPreviewCallbackWithBuffer(Camera camera, PreviewCallback callback) {
                try {
                        APILevel7.setPreviewCallbackWithBufferMethod.invoke(camera, callback);
                } catch (Exception e) {
                        Log.e(THIS_FILE, e.toString());
                }
        }

        private void setDisplayOrientation(Camera camera, int degrees) {
                try {
                        if(APILevel8.setDisplayOrientationMethod != null)
                                APILevel8.setDisplayOrientationMethod.invoke(camera, degrees);
                } catch (Exception e) {
                        Log.e(THIS_FILE, e.toString());
                }
        }

        @SuppressWarnings({ "unchecked" })
        private List<Camera.Size> getSupportedPreviewSizes(Camera.Parameters params){
                List<Camera.Size> list = null;
                try {
                        list = (List<Camera.Size>)APILevel5.getSupportedPreviewSizesMethod.invoke(params);
                } catch (Exception e) {
                        Log.e(THIS_FILE, e.toString());
                }
                return list;
        }

        // Must be done in the UI thread
        public final View startPreview(){
                if(this.preview == null){
                        this.preview = new Preview(this);
                }
                else {
                        this.preview.setVisibility(View.VISIBLE);
                        this.preview.getHolder().setSizeFromLayout();
                        this.preview.bringToFront();
                }
                return this.preview;
        }

        public synchronized int pause(){
                Log.d(THIS_FILE, "pause()");
                return 0;
        }

        public synchronized int start() {
                Log.d(THIS_FILE, "start()");
                if(this.context != null){

                        this.running = true;
                        return 0;
                }
                else{
                        Log.e(THIS_FILE, "Invalid context");
                        return -1;
                }
        }

        public synchronized int stop() {
                Log.d(THIS_FILE, "stop()");
                this.preview = null;
                this.context = null;

                this.running = false;

                return 0;
        }

        public synchronized int reset() {
                Log.d(THIS_FILE, "reset()");

            this.preview.setVisibility(View.INVISIBLE);
                this.running = false;

                return 0;
        }

        public synchronized int prepare(int width, int height, int fps){
                Log.d(THIS_FILE, String.format("prepare(%d, %d, %d)", width, height, fps));
                this.width = width;
                this.height = height;
                this.fps = fps;

                float capacity = (float)(width*height)*1.5f/* (3/2) */;
                this.frame = ByteBuffer.allocateDirect((int)capacity);

                return 0;
        }

        private PreviewCallback previewCallback = new PreviewCallback() {
          public void onPreviewFrame(byte[] _data, Camera _camera) {
                        VideoProducer.this.frame.put(_data);
                        VideoProducer.this.pushToNative(_data, VideoProducer.this.frame.capacity());
                        VideoProducer.this.frame.rewind();

                        if(APILevel7.isAvailable()){
                                VideoProducer.this.addCallbackBuffer(_camera, _data);
                        }
                }
        };

        /* ==================================================*/
        class Preview extends SurfaceView implements SurfaceHolder.Callback {
                private SurfaceHolder holder;
                private Camera camera;

                private final VideoProducer producer;

                Preview(VideoProducer _producer) {
                        super(_producer.context);

                        this.producer = _producer;
                        this.holder = getHolder();
                        this.holder.addCallback(this);

                        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // display
                }

                public void surfaceCreated(SurfaceHolder holder) {
                        try {
                                boolean useFFC = true;
                                Log.d(THIS_FILE, useFFC ? "Using FFC" : "Not using FFC");
                                CamerasWrapper cWrapper = CamerasWrapper.getInstance();
                                this.camera = cWrapper.getCamera(!this.producer.toggle);
                               
                                Camera.Parameters parameters = this.camera.getParameters();
                                Log.d(THIS_FILE, "Parameter of camera "+parameters.flatten());

                                /*
                                 * http://developer.android.com/reference/android/graphics/ImageFormat.html#NV21
                                 * YCrCb format used for images, which uses the NV21 encoding format.
                                 * This is the default format for camera preview images, when not otherwise set with setPreviewFormat(int).
                                 */
                                parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
                               // parameters.setPreviewFormat(ImageFormat.NV21);
                                parameters.setPreviewFrameRate(this.producer.fps);
                                // parameters.set("rotation", degree)
                                this.camera.setParameters(parameters);

                                try{
                                        parameters.setPictureSize(this.producer.width, this.producer.height);
                                        this.camera.setParameters(parameters);
                                }
                                catch(Exception e){
                                        // FFMpeg converter will resize the video stream
                                        Log.d(THIS_FILE, e.toString());
                                }

                                this.camera.setPreviewDisplay(holder);

                                if(APILevel7.isAvailable()){
                                        this.producer.setPreviewCallbackWithBuffer(this.camera, this.producer.previewCallback);
                                }
                                else{
                                        this.camera.setPreviewCallback(this.producer.previewCallback);
                                }

                        } catch (Exception exception) {
                                if(this.camera != null){
                                        this.camera.release();
                                        this.camera = null;
                                }
                                Log.e(THIS_FILE, exception.toString());
                        }
                }

                public void surfaceDestroyed(SurfaceHolder holder) {
                        Log.d(THIS_FILE,"Destroy Preview");
                        if(this.camera != null){
                                // stop preview
                                Log.d(THIS_FILE,"Close Camera");
                                this.camera.stopPreview();
                                if(APILevel7.isAvailable()){
                                        this.producer.setPreviewCallbackWithBuffer(this.camera, null);
                                }
                                else{
                                        this.camera.setPreviewCallback(null);
                                }
                                this.camera.release();
                                this.camera = null;
                        }
                }

                public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                        Log.d(THIS_FILE,"Surface Changed Callback");
                        if(this.camera != null && this.producer != null && this.producer.frame != null){
                                try{
                                        Camera.Parameters parameters = this.camera.getParameters();
                                        parameters.setPreviewSize(this.producer.width, this.producer.height);
                                        this.camera.setParameters(parameters);
                                }
                                catch(Exception e){
                                        Log.e(THIS_FILE, e.toString());
                                }

                                if(APILevel7.isAvailable()){
                                        // Camera Orientation
                                		int orientation = Configuration.ORIENTATION_LANDSCAPE;
                                        switch(orientation){
                                                case Configuration.ORIENTATION_LANDSCAPE:
                                                        this.producer.setDisplayOrientation(this.camera, 0);
                                                        Log.d(THIS_FILE, "Orientation=landscape");
                                                        break;
                                                case Configuration.ORIENTATION_PORTRAIT:
                                                        this.producer.setDisplayOrientation(this.camera, 90);
                                                        Log.d(THIS_FILE, "Orientation=portrait");
                                                        break;
                                        }
                                        // Callback Buffers
                                        for(int i=0; i<VideoProducer.CALLABACK_BUFFERS_COUNT; i++){
                                                this.producer.addCallbackBuffer(this.camera, new byte[this.producer.frame.capacity()]);
                                        }
                                }

                                this.camera.startPreview();
                        }
                }
        }




        /* ==================================================*/
        static class APILevel8{
                static Method setDisplayOrientationMethod = null;
                static boolean isOK = false;

                static {
                        try {
                                APILevel8.setDisplayOrientationMethod = Camera.class.getMethod(
                                                "setDisplayOrientation", int.class);

                                APILevel8.isOK = true;
                        } catch (Exception e) {
                                Log.d(THIS_FILE, e.toString());
                        }
                }

                static boolean isAvailable(){
                        return APILevel8.isOK;
                }
        }

        /* ==================================================*/
        static class APILevel7{
                static Method addCallbackBufferMethod = null;
                static Method setPreviewCallbackWithBufferMethod = null;
                static boolean isOK = false;

                static {
                        try {
                                // According to http://developer.android.com/reference/android/hardware/Camera.html both addCallbackBuffer and setPreviewCallbackWithBuffer
                                // are only available starting API level 8. But it's not true as these functions exist in API level 7 but are hidden.
                                APILevel7.addCallbackBufferMethod = Camera.class.getMethod(
                                                "addCallbackBuffer", byte[].class);
                                APILevel7.setPreviewCallbackWithBufferMethod = Camera.class.getMethod(
                                                "setPreviewCallbackWithBuffer", PreviewCallback.class);

                                APILevel7.isOK = true;
                        } catch (Exception e) {
                                Log.d(THIS_FILE, e.toString());
                        }
                }

                static boolean isAvailable(){
                        return APILevel7.isOK;
                }
        }

        /* ==================================================*/
        static class APILevel5{
                static Method getSupportedPreviewSizesMethod = null;
                static boolean isOK = false;

                static {
                        try {
                                APILevel5.getSupportedPreviewSizesMethod = Camera.Parameters.class.getDeclaredMethod("getSupportedPreviewSizes");

                                APILevel5.isOK = true;
                        } catch (Exception e) {
                                Log.d(THIS_FILE, e.toString());
                        }
                }

                static boolean isAvailable(){
                        return APILevel5.isOK;
                }
        }


}