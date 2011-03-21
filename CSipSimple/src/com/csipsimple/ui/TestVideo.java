package com.csipsimple.ui;

import org.pjsip.pjsua.pjsua;

import com.csipsimple.R;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class TestVideo extends Activity {

	private GLSurfaceView surface;
	private TestVideoRenderer renderer;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_test);

		surface = (GLSurfaceView) findViewById(R.id.side_remote); //new GLSurfaceView(this);
		renderer = new TestVideoRenderer(this);
		surface.setRenderer(renderer);
		
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

}
