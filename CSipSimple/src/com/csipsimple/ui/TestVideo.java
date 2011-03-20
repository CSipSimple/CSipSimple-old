package com.csipsimple.ui;

import org.pjsip.pjsua.pjsua;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class TestVideo extends Activity {

	private GLSurfaceView surface;
	private TestVideoRenderer renderer;

	private GestureDetector gestureDetector;
	private static boolean fullscreen;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (fullscreen) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		gestureDetector = new GestureDetector(this, new GlAppGestureListener(this));

		surface = new GLSurfaceView(this);
		renderer = new TestVideoRenderer(this);
		surface.setRenderer(renderer);
		setContentView(surface);
		
		Thread t = new Thread() {
			public void run() {
				pjsua.test_video_dev();
			};
		};
		t.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		surface.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		surface.onResume();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}

	private class GlAppGestureListener extends GestureDetector.SimpleOnGestureListener {
		private TestVideo glApp;

		public GlAppGestureListener(TestVideo glApp) {
			this.glApp = glApp;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// toggle fullscreen flag
			TestVideo.fullscreen = !TestVideo.fullscreen;

			// start a new one
			Intent intent = new Intent(glApp, TestVideo.class);
			startActivity(intent);

			// close current activity
			glApp.finish();

			return true;
		}
	}

}
