/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2008 The Android Open Source Project
 * 
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
package com.csipsimple.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.ISipService;
import com.csipsimple.service.MediaManager.MediaState;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;
import com.csipsimple.widgets.InCallControls;
import com.csipsimple.widgets.InCallControls.OnTriggerListener;
import com.csipsimple.widgets.InCallInfo;
import com.csipsimple.widgets.ScreenLocker;


public class InCallActivity extends Activity implements OnTriggerListener, OnDialKeyListener, SensorEventListener, com.csipsimple.widgets.SlidingTab.OnTriggerListener {
	private static String THIS_FILE = "SIP CALL HANDLER";

	private SipCallSession[] callsInfo = null;
	private FrameLayout mainFrame;
	private InCallControls inCallControls;
	private InCallInfo inCallInfo;
	private ScreenLocker lockOverlay;

	//Screen wake lock for incoming call
	private WakeLock wakeLock;
	//Keygard for incoming call
	private boolean manageKeyguard = false;
    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock keyguardLock;

	private Dialpad dialPad;
	private LinearLayout dialPadContainer;
	private EditText dialPadTextView;

	private View callInfoPanel;
	private Timer quitTimer;

	private LinearLayout detailedContainer, holdContainer;

	//True if running unit tests
//	private boolean inTest;

	private MediaState lastMediaState;

	private SensorManager sensorManager;
	private Sensor proximitySensor;
	private DialingFeedback dialFeedback;
	private boolean proximitySensorTracked = false;
	private PowerManager powerManager;
	private WakeLock proximityWakeLock;
	private PreferencesWrapper prefsWrapper;
	
	private final static int PICKUP_SIP_URI = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(THIS_FILE, "Create in call");
		setContentView(R.layout.in_call_main);
		
		/*
		inTest = extras.getBoolean("in_test", false);
		if(!inTest) {
		*/
		//	Log.d(THIS_FILE, "Creating call handler for call " + callInfo.getCallId() );
			bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
			/*
		}
		*/
			
		prefsWrapper = new PreferencesWrapper(this);

//		Log.d(THIS_FILE, "Creating call handler for " + callInfo.getCallId()+" state "+callInfo.getRemoteContact());
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall");
		
		takeKeyEvents(true);
		

		//remoteContact = (TextView) findViewById(R.id.remoteContact);
		mainFrame = (FrameLayout) findViewById(R.id.mainFrame);
		inCallControls = (InCallControls) findViewById(R.id.inCallControls);
		inCallControls.setOnTriggerListener(this);
		
		inCallInfo = (InCallInfo) findViewById(R.id.inCallInfo);
		dialPad = (Dialpad) findViewById(R.id.dialPad);
		dialPad.setOnDialKeyListener(this);
		dialPadContainer = (LinearLayout) findViewById(R.id.dialPadContainer);
		dialPadTextView = (EditText) findViewById(R.id.digitsText);
		callInfoPanel = (View) findViewById(R.id.callInfoPanel);
		
		detailedContainer = (LinearLayout) findViewById(R.id.detailedContainer);
		holdContainer = (LinearLayout) findViewById(R.id.holdContainer);
		
		lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
		lockOverlay.setActivity(this, this);
		
		//Listen to media & sip events to update the UI
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_MEDIA_CHANGED));
		
		// Sensor management
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		Log.d(THIS_FILE, "Proximty sensor : "+proximitySensor);
		
		dialFeedback = new DialingFeedback(this, true);
		

		if(!prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.PREVENT_SCREEN_ROTATION)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
	}
	
	
	@Override
	protected void onStart() {
		Log.d(THIS_FILE, "Start in call");
		super.onStart();
        if (keyguardManager == null) {
            keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock("com.csipsimple.inCallKeyguard");
        }
        
        // If this line is uncommented keyguard will be prevented only if in keyguard mode is locked 
        // when incoming call arrives
        //if(keyguardManager.inKeyguardRestrictedInputMode()) {
        
        manageKeyguard = true;
        keyguardLock.disableKeyguard();
        //}
        
        if(quitTimer == null) {
    		quitTimer = new Timer();
        }
        
        if(proximitySensor != null) {
			WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo winfo = wman.getConnectionInfo();
			if(winfo == null || !prefsWrapper.keepAwakeInCall()) {
				// Try to use powermanager proximity sensor
				if(powerManager != null) {
					try {
						Method method = powerManager.getClass().getDeclaredMethod("getSupportedWakeLockFlags");
						int supportedFlags = (Integer) method.invoke(powerManager);
						Log.d(THIS_FILE, ">>> Flags supported : "+supportedFlags);
						Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
						int proximityScreenOffWakeLock = (Integer) f.get(null);
						if( (supportedFlags & proximityScreenOffWakeLock) != 0x0 ) {
							Log.d(THIS_FILE, ">>> We can use native screen locker !!");
							proximityWakeLock = powerManager.newWakeLock(proximityScreenOffWakeLock, "com.csipsimple.CallProximity");
							proximityWakeLock.setReferenceCounted(false);
						}
						
					} catch (Exception e) {
						Log.d(THIS_FILE, "Impossible to get power manager supported wake lock flags");
					} 
					/*
					if ((powerManager.getSupportedWakeLockFlags()  & PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) != 0x0) {
						mProximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, THIS_FILE);
					}
					*/
				}
			}
			
			if(proximityWakeLock == null) {
				//Fall back to manual mode
				isFirstRun = true;
				sensorManager.registerListener(this, 
		                proximitySensor,
		                SensorManager.SENSOR_DELAY_NORMAL);
				proximitySensorTracked  = true;
			}
			
		}
        dialFeedback.resume();
        handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(THIS_FILE, "Stop in call");
		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		
		if(proximitySensor != null) {
			proximitySensorTracked = false;
			sensorManager.unregisterListener(this);
		}
		
		dialFeedback.pause();
		
		if(quitTimer != null) {
			quitTimer.cancel();
			quitTimer.purge();
			quitTimer = null;
		}
		
		lockOverlay.tearDown();
		
		if(manageKeyguard) {
			keyguardLock.reenableKeyguard();
		}
	}
	

	@Override
	protected void onDestroy() {
		Log.d(THIS_FILE, "Destroy in call");
		try {
			unbindService(connection);
		}catch(Exception e) {
			//Just ignore that
		}
		service = null;
		if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		try {
			unregisterReceiver(callStateReceiver);
		}catch (IllegalArgumentException e) {
			//That's the case if not registered (early quit)
		}
		super.onDestroy();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		//TODO : update UI
		Log.d(THIS_FILE, "New intent is launched");
		
		
		super.onNewIntent(intent);
	}
	

	private static final int UPDATE_FROM_CALL = 1;
	private static final int UPDATE_FROM_MEDIA = 2;
	// Ui handler
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_FROM_CALL:
				updateUIFromCall();
				break;
			case UPDATE_FROM_MEDIA:
				updateUIFromMedia();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PICKUP_SIP_URI:
			if(resultCode == RESULT_OK) {
				String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				try {
					if(service != null) {

						SipCallSession currentCall = getCurrentCallInfo();
						if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
							service.xfer(currentCall.getCallId(), callee);
						}
					}
				} catch (RemoteException e) {
					//TODO : toaster 
				}
			}
			return;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public static final int AUDIO_SETTINGS_MENU = Menu.FIRST + 1;
	public static final int RECORD_MENU = Menu.FIRST + 2;
	public static final int XFER_MENU = Menu.FIRST + 3;
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem recItem = menu.findItem(RECORD_MENU);
		boolean valueOk = false;
		
		if(service != null) {
			try {
				boolean isRecording = (service.getRecordedCall() != -1);
				if(isRecording) {
					recItem.setTitle(R.string.stop_recording);
					recItem.setIcon(R.drawable.stop);
					recItem.setEnabled(true);
					valueOk = true;
				}else {
					SipCallSession currentCall = getCurrentCallInfo();
					if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
						if(service.canRecord(currentCall.getCallId())) {
							recItem.setTitle(R.string.record);
							recItem.setIcon(R.drawable.record);
							recItem.setEnabled(true);
							valueOk = true;
						}
					}
				}
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Can't call services methods", e);
			}
		}
		if(!valueOk) {
			recItem.setEnabled(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, AUDIO_SETTINGS_MENU, Menu.NONE, R.string.prefs_media).setIcon(R.drawable.ic_menu_media);
		menu.add(Menu.NONE, RECORD_MENU, Menu.NONE, R.string.record).setIcon(R.drawable.record);
		menu.add(Menu.NONE, XFER_MENU, Menu.NONE, R.string.xfer).setIcon(android.R.drawable.ic_menu_share);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case AUDIO_SETTINGS_MENU:
			startActivity(new Intent(this, InCallMediaControl.class));
			return true;
		case RECORD_MENU:
			try {
				if(service != null) {
					SipCallSession currentCall = getCurrentCallInfo();
					if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
						service.startRecording(currentCall.getCallId());
					}
				}
			} catch (RemoteException e) {
				//TODO : toaster 
			}
			return true;
		case XFER_MENU:
			Intent pickupIntent = new Intent(this, PickupSipUri.class);
			startActivityForResult(pickupIntent, PICKUP_SIP_URI);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	private SipCallSession getCurrentCallInfo() {
		SipCallSession currentCallInfo = null;
		if(callsInfo == null) {
			return null;
		}
		for(SipCallSession callInfo : callsInfo) {
			int state = callInfo.getCallState();
			switch (state) {
				case SipCallSession.InvState.NULL:
				case SipCallSession.InvState.DISCONNECTED:
					break;
				default:
					currentCallInfo = callInfo;
					break;
			}
			if(currentCallInfo != null) {
				break;
			}
		}
		return currentCallInfo;
	}
	
	private synchronized void updateUIFromCall() {
		if(!serviceConnected) {
			return;
		}
		SipCallSession currentCallInfo = getCurrentCallInfo();
		
		Log.d(THIS_FILE, ">> Call : "+currentCallInfo);

		//Update in call actions
		inCallInfo.setCallState(currentCallInfo);
		inCallControls.setCallState(currentCallInfo);
		
		//If not any active call active
		if(currentCallInfo == null && serviceConnected) {
			delayedQuit();
			return;
		}

		Log.d(THIS_FILE, "Update ui from call " + currentCallInfo.getCallId() + " state " + CallsUtils.getStringCallState(currentCallInfo, this));
		int state = currentCallInfo.getCallState();
		
		int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
		
		//We manage wake lock
		switch (state) {
		case SipCallSession.InvState.INCOMING:
		case SipCallSession.InvState.EARLY:
		case SipCallSession.InvState.CALLING:
			Log.d(THIS_FILE, "Acquire wake up lock");
			if(wakeLock != null && !wakeLock.isHeld()) {
				wakeLock.acquire();
			}
			if(proximitySensor == null && proximityWakeLock == null) {
				if(currentCallInfo.isIncoming()) {
					lockOverlay.hide();
				}else {
					lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_START);
				}
			}
			
			if(proximityWakeLock != null) {
				if(currentCallInfo.isIncoming()) {
					if(proximityWakeLock.isHeld()) {
						proximityWakeLock.release();
					}
				}else {
					if(!proximityWakeLock.isHeld()) {
						proximityWakeLock.acquire();
					}
				}
			}
			break;
		case SipCallSession.InvState.CONFIRMED:
			backgroundResId = R.drawable.bg_in_call_gradient_connected;
			if(lastMediaState != null && lastMediaState.isBluetoothScoOn) {
				backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
			}
			if (wakeLock != null && wakeLock.isHeld()) {
				Log.d(THIS_FILE, "Releasing wake up lock - confirmed");
                wakeLock.release();
            }
			if(proximitySensor == null && proximityWakeLock == null) {
				lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_START);
			}
			
			if(proximityWakeLock != null && !proximityWakeLock.isHeld()) {
				proximityWakeLock.acquire();
			}
			
			break;
		case SipCallSession.InvState.NULL:
			Log.i(THIS_FILE, "WTF?");
		case SipCallSession.InvState.DISCONNECTED:
			//Set background to red and delay quit
			delayedQuit();
			return;
		case SipCallSession.InvState.CONNECTING:
			
			break;
		}
		
		int mediaStatus = currentCallInfo.getMediaStatus();
		switch (mediaStatus) {
		case SipCallSession.MediaState.ACTIVE:
			break;
		case SipCallSession.MediaState.REMOTE_HOLD:
		case SipCallSession.MediaState.LOCAL_HOLD:
		case SipCallSession.MediaState.NONE:
			if(backgroundResId == R.drawable.bg_in_call_gradient_connected ||
					backgroundResId == R.drawable.bg_in_call_gradient_bluetooth) {
				backgroundResId = R.drawable.bg_in_call_gradient_on_hold;
			}
			break;
		case SipCallSession.MediaState.ERROR:
		default:
			break;
		}
		
		
		mainFrame.setBackgroundResource(backgroundResId);
		Log.d(THIS_FILE, "we leave the update ui function");
	}
	
	
	private synchronized void updateUIFromMedia() {
		if(SipService.pjService.mediaManager != null && serviceConnected) {
			MediaState mediaState = SipService.pjService.mediaManager.getMediaState();
			Log.d(THIS_FILE, "Media update ....");
			if(!mediaState.equals(lastMediaState)) {
				SipCallSession callInfo = getCurrentCallInfo();
				lastMediaState = mediaState;
				
				if(callInfo != null) {
					int state = callInfo.getCallState();
					
					// Background
					if(state == SipCallSession.InvState.CONFIRMED) {
						mainFrame.setBackgroundResource(lastMediaState.isBluetoothScoOn?R.drawable.bg_in_call_gradient_bluetooth:R.drawable.bg_in_call_gradient_connected);
					}
				}
				
				// Actions
				inCallControls.setMediaState(lastMediaState);
			}
		}
	}
	
	private synchronized void delayedQuit() {
		
		if (wakeLock != null && wakeLock.isHeld()) {
			Log.d(THIS_FILE, "Releasing wake up lock");
            wakeLock.release();
        }
		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		
		//Update ui
		lockOverlay.hide();
		setDialpadVisibility(View.GONE);
		callInfoPanel.setVisibility(View.VISIBLE);
		inCallControls.setVisibility(View.GONE);
		mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_ended);
		
		Log.d(THIS_FILE, "Start quit timer");
		if(quitTimer != null) {
			quitTimer.schedule(new QuitTimerTask(), 3000);
		}else {
			finish();
		}
	}
	
	private class QuitTimerTask extends TimerTask{
		@Override
		public void run() {
			Log.d(THIS_FILE, "Run quit timer");
			finish();
		}
	};
	
	
	private void setDialpadVisibility(int visibility) {
		dialPadContainer.setVisibility(visibility);
		int antiVisibility = visibility == View.GONE? View.VISIBLE:View.GONE;
		detailedContainer.setVisibility(antiVisibility);
		holdContainer.setVisibility(antiVisibility);
		callInfoPanel.setVisibility(antiVisibility);
	}
	
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	//
    		// Volume has been adjusted by the user.
    		//
        	Log.d(THIS_FILE, "onKeyDown: Volume button pressed");
        	int action = AudioManager.ADJUST_RAISE;
        	if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
        		action = AudioManager.ADJUST_LOWER;
        	}

        	// Detect if ringing
        	SipCallSession currentCallInfo = getCurrentCallInfo();
    		//If not any active call active
    		if(currentCallInfo == null && serviceConnected) {
    			break;
    		}
    		
    		if(service != null) {
        		try {
					service.adjustVolume(currentCallInfo, action, AudioManager.FLAG_SHOW_UI);
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Can't adjust volume", e);
				}
        	}
    		
    		
        	return true;
        case KeyEvent.KEYCODE_CALL:
		case KeyEvent.KEYCODE_ENDCALL:
        	return inCallControls.onKeyDown(keyCode, event);
		case KeyEvent.KEYCODE_SEARCH:
			//Prevent search
			return true;
        default:
        	//Nothing to do	
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_CALL:
		case KeyEvent.KEYCODE_ENDCALL:
		case KeyEvent.KEYCODE_SEARCH:
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	
	private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(SipManager.ACTION_SIP_CALL_CHANGED)){
				if(service != null) {
					try {
						callsInfo = service.getCalls();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Not able to retrieve calls");
					}
				}

				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
			}else if(action.equals(SipManager.ACTION_SIP_MEDIA_CHANGED)) {
				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_MEDIA));
			}
		}
	};
	
	
	/**
	 * Service binding
	 */
	private boolean serviceConnected = false;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			try {
				//Log.d(THIS_FILE, "Service started get real call info "+callInfo.getCallId());
				callsInfo = service.getCalls();
				serviceConnected = true;
				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_MEDIA));
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Can't get back the call", e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serviceConnected = false;
			callsInfo = null;
		}
	};


	private boolean showDetails = true;

	private boolean isFirstRun = true;

	@Override
	public void onTrigger(int whichAction, SipCallSession call) {
		Log.d(THIS_FILE, "In Call Activity is triggered");
		Log.d(THIS_FILE, "We have a current call : " + call);
		if(call == null) {
			Log.e(THIS_FILE, "Try to do an action on a null call !!!");
		}
		if(call.getCallId() == SipCallSession.INVALID_CALL_ID) {
			Log.e(THIS_FILE, "Try to do an action on an invalid call !!!");
		}
		
		//Reset proximity sensor timer
		if(proximitySensor == null && proximityWakeLock == null) {
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
		}
		
		try {
			switch(whichAction) {
				case TAKE_CALL:{
					if (service != null) {
						service.answer(call.getCallId(), SipCallSession.StatusCode.OK);
					}
					break;
				}
				case DECLINE_CALL: 
				case CLEAR_CALL:
				{
					if (service != null) {
						service.hangup(call.getCallId(), 0);
					}
					break;
				}
				case MUTE_ON:
				case MUTE_OFF:
				{
					if ( service != null) {
						service.setMicrophoneMute((whichAction == MUTE_ON)?true:false);
					}
					break;
				}
				case SPEAKER_ON :
				case SPEAKER_OFF :
				{
					if (service != null) {
						service.setSpeakerphoneOn((whichAction == SPEAKER_ON)?true:false);
					}
					break;
				}
				case BLUETOOTH_ON:
				case BLUETOOTH_OFF: {
					if (service != null) {
						service.setBluetoothOn((whichAction == BLUETOOTH_ON)?true:false);
					}
					break;
				}
				case DIALPAD_ON:
				case DIALPAD_OFF: {
					setDialpadVisibility((whichAction == DIALPAD_ON)?View.VISIBLE:View.GONE);
					break;
				}
				case DETAILED_DISPLAY:{
					inCallInfo.switchDetailedInfo( showDetails );
					showDetails = !showDetails;
					break;
				}
				case TOGGLE_HOLD:{
					if (service != null) {
						//Log.d(THIS_FILE, "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
						if(call.getMediaStatus() == SipCallSession.MediaState.LOCAL_HOLD ||
								call.getMediaStatus() == SipCallSession.MediaState.NONE ) {
							service.reinvite(call.getCallId(), true);
						}else {
							service.hold(call.getCallId());
						}
					}
					break;
				}
				case MEDIA_SETTINGS:{
					startActivity(new Intent(this, InCallMediaControl.class));
					break;
				}
			}
		} catch (RemoteException e) {
			Log.e(THIS_FILE, "Was not able to call service method", e);
		}
	}
	

	@Override
	public void onTrigger(int keyCode, int dialTone) {
		if(proximitySensor == null && proximityWakeLock == null) {
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
		}
		
		if (service != null) {
			SipCallSession currentCall = getCurrentCallInfo();
			if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
				try {
					service.sendDtmf(currentCall.getCallId(), keyCode);
					dialFeedback.giveFeedback(dialTone);
					KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
					char nbr = event.getNumber();
					dialPadTextView.getText().append(nbr);
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Was not able to send dtmf tone", e);
				}
			}
		}
		
	}

	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}


	private static final float PROXIMITY_THRESHOLD = 5.0f;
	@Override
	public void onSensorChanged(SensorEvent event) {
		//Log.d(THIS_FILE, "Tracked : "+proximitySensorTracked);
		if(proximitySensorTracked && !isFirstRun) {
			float distance = event.values[0];
			boolean active = (distance >= 0.0 && distance < PROXIMITY_THRESHOLD && distance < event.sensor.getMaximumRange());
			Log.d(THIS_FILE, "Distance is now " + distance);
			boolean isValidCallState = false;
			for(SipCallSession callInfo : callsInfo) {
				int state = callInfo.getCallState();
				isValidCallState |= ( 
					(state == SipCallSession.InvState.CONFIRMED ) || 
					(state == SipCallSession.InvState.CONNECTING )|| 
					(state == SipCallSession.InvState.CALLING )|| 
					(state == SipCallSession.InvState.EARLY && !callInfo.isIncoming() )
				);
				
				
			}
			
			if( isValidCallState && active) {
				lockOverlay.show();
			}else {
				lockOverlay.hide();
			}
		}
		if(isFirstRun) {
			isFirstRun = false;
		}
	}


	@Override
	public void onTrigger(View v, int whichHandle) {
		switch (whichHandle) {
		case LEFT_HANDLE:
			Log.d(THIS_FILE, "We unlock");
			lockOverlay.hide();
			lockOverlay.reset();
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
			break;
		case RIGHT_HANDLE:
			Log.d(THIS_FILE, "We clear the call");
			onTrigger(OnTriggerListener.CLEAR_CALL, getCurrentCallInfo());
			lockOverlay.reset();
		default:
			break;
		}
		
	}
}
