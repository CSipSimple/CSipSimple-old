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
package com.csipsimple.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.csipsimple.api.SipConfigManager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Contacts;
import android.text.TextUtils;

@SuppressWarnings("deprecation")
public class Compatibility {
	
	private static final String THIS_FILE = "Compat";
	private static int currentApi = 0;

	public static int getApiLevel() {

		if (currentApi > 0) {
			return currentApi;
		}

		if (android.os.Build.VERSION.SDK.equalsIgnoreCase("3")) {
			currentApi = 3;
		} else {
			try {
				Field f = android.os.Build.VERSION.class.getDeclaredField("SDK_INT");
				currentApi = (Integer) f.get(null);
			} catch (Exception e) {
				return 0;
			}
		}

		return currentApi;
	}
	
	
	public static boolean isCompatible(int apiLevel) {
		return getApiLevel() >= apiLevel;
	}


	/**
	 * Get the stream id for in call track. Can differ on some devices.
	 * Current device for which it's different :
	 * Archos 5IT
	 * @return
	 */
	public static int getInCallStream() {
		if (android.os.Build.BRAND.equalsIgnoreCase("archos")) {
			//Since archos has no voice call capabilities, voice call stream is not implemented
			//So we have to choose the good stream tag, which is by default falled back to music
			return AudioManager.STREAM_MUSIC;
		}
		//return AudioManager.STREAM_MUSIC;
		return AudioManager.STREAM_VOICE_CALL;
	}
	
	public static boolean shouldUseRoutingApi() {
		Log.d(THIS_FILE, "Current device " + android.os.Build.BRAND + " - " + android.os.Build.DEVICE);

		//HTC evo 4G
		if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
			return true;
		}
		
		if (!isCompatible(4)) {
			//If android 1.5, force routing api use 
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean shouldUseModeApi() {
		Log.d(THIS_FILE, "Current device " + android.os.Build.BRAND + " - " + android.os.Build.DEVICE);
		//ZTE blade
		if(android.os.Build.DEVICE.equalsIgnoreCase("blade")) {
			return true;
		}
		//Samsung GT-I5500
		if(android.os.Build.DEVICE.equalsIgnoreCase("GT-I5500")) {
			return true;
		}
		//HTC evo 4G
		if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
			return true;
		}
		//LG P500
		if(android.os.Build.PRODUCT.equalsIgnoreCase("thunderg")) {
			return true;
		}
		//Huawei
		if(android.os.Build.DEVICE.equalsIgnoreCase("U8150") ||
				android.os.Build.DEVICE.equalsIgnoreCase("U8110") ) {
			return true;
		}
		
		return false;
	}


	public static String guessInCallMode() {
		if (android.os.Build.BRAND.equalsIgnoreCase("sdg")) {
			return "3";
		}
		if(android.os.Build.DEVICE.equalsIgnoreCase("blade")) {
			return Integer.toString(AudioManager.MODE_IN_CALL);
		}

		if (!isCompatible(5)) {
			return Integer.toString(AudioManager.MODE_IN_CALL);
		}

		return Integer.toString(AudioManager.MODE_NORMAL);
	}
	
	public static String getCpuAbi() {
		if (isCompatible(4)) {
			Field field;
			try {
				field = android.os.Build.class.getField("CPU_ABI");
				return field.get(null).toString();
			} catch (Exception e) {
				Log.w(THIS_FILE, "Announce to be android 1.6 but no CPU ABI field", e);
			}

		}
		return "armeabi";
	}
	
	private static boolean needPspWorkaround(PreferencesWrapper preferencesWrapper) {
		//Nexus one is impacted
		if(android.os.Build.DEVICE.equalsIgnoreCase("passion")){
			return true;
		}
		//All htc except....
		if(android.os.Build.PRODUCT.toLowerCase().startsWith("htc") 
				|| android.os.Build.BRAND.toLowerCase().startsWith("htc") 
				|| android.os.Build.PRODUCT.toLowerCase().equalsIgnoreCase("inc") /* For Incredible */ ) {
			if(android.os.Build.DEVICE.equalsIgnoreCase("hero") /* HTC HERO */ 
					|| android.os.Build.DEVICE.equalsIgnoreCase("magic") /* Magic Aka Dev G2 */
					|| android.os.Build.DEVICE.equalsIgnoreCase("tatoo") /* Tatoo */
					|| android.os.Build.DEVICE.equalsIgnoreCase("dream") /* Dream Aka Dev G1 */
					|| android.os.Build.DEVICE.equalsIgnoreCase("legend") /* Legend */
					
					) {
				return false;
			}
			return true;
		}
		//Dell streak
		if(android.os.Build.BRAND.toLowerCase().startsWith("dell") &&
				android.os.Build.DEVICE.equalsIgnoreCase("streak")) {
			return true;
		}
		//Motorola milestone 1 and 2 & motorola droid
		if(android.os.Build.DEVICE.toLowerCase().contains("milestone2") ||
				android.os.Build.BOARD.toLowerCase().contains("sholes") ||
				android.os.Build.PRODUCT.toLowerCase().contains("sholes")  ) {
			return true;
		}
		
		return false;
	}
	

	private static boolean needToneWorkaround(PreferencesWrapper prefWrapper) {
		if(android.os.Build.PRODUCT.toLowerCase().startsWith("gt-i5800") ||
				android.os.Build.PRODUCT.toLowerCase().startsWith("gt-i5801") ) {
			return true;
		}
		return false;
	}
	
	
	private static void resetCodecsSettings(PreferencesWrapper preferencesWrapper) {
		//Disable iLBC if not armv7
		boolean supportFloating = getCpuAbi().equalsIgnoreCase("armeabi-v7a");
		
		
		//For Narrowband
		preferencesWrapper.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "60");
		preferencesWrapper.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "50");
		preferencesWrapper.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "220");
		preferencesWrapper.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "230");
		preferencesWrapper.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB, supportFloating ? "240" : "0");
		preferencesWrapper.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "235");
		preferencesWrapper.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
		preferencesWrapper.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_NB, "237");
		
		
		//For Wideband
		preferencesWrapper.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "60");
		preferencesWrapper.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "50");
		preferencesWrapper.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB, "70"); /*This is for addressing asterisk bug */
		preferencesWrapper.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "219");
		preferencesWrapper.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB, "220");
		preferencesWrapper.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "10");
		preferencesWrapper.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB, "235");
		preferencesWrapper.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB, "0");
		preferencesWrapper.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB, supportFloating ? "100" : "0");
		preferencesWrapper.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "80");
		preferencesWrapper.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "75");
		preferencesWrapper.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "230");
		preferencesWrapper.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "240");
		preferencesWrapper.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_WB, "0");
		
		
		// Bands repartition
		preferencesWrapper.setPreferenceStringValue("band_for_wifi",  SipConfigManager.CODEC_WB);
		preferencesWrapper.setPreferenceStringValue("band_for_other",  SipConfigManager.CODEC_WB);
		preferencesWrapper.setPreferenceStringValue("band_for_3g",  SipConfigManager.CODEC_NB);
		preferencesWrapper.setPreferenceStringValue("band_for_gprs", SipConfigManager.CODEC_NB);
		preferencesWrapper.setPreferenceStringValue("band_for_edge", SipConfigManager.CODEC_NB);
		
	}
	
	public static void setFirstRunParameters(PreferencesWrapper preferencesWrapper) {
		resetCodecsSettings(preferencesWrapper);
		
		preferencesWrapper.setPreferenceStringValue(SipConfigManager.SND_MEDIA_QUALITY, getCpuAbi().equalsIgnoreCase("armeabi-v7a") ? "4" : "3");
		preferencesWrapper.setPreferenceStringValue(SipConfigManager.SND_AUTO_CLOSE_TIME, isCompatible(4) ? "1" : "5");
		preferencesWrapper.setPreferenceStringValue(SipConfigManager.SND_CLOCK_RATE, isCompatible(4) ? "16000" : "8000");
		preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, isCompatible(4) ? true : false);
		//HTC PSP mode hack
		preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.KEEP_AWAKE_IN_CALL, needPspWorkaround(preferencesWrapper));
		
		//Proximity sensor inverted
		if( android.os.Build.PRODUCT.equalsIgnoreCase("SPH-M900") /*Sgs moment*/) {
			preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.INVERT_PROXIMITY_SENSOR, true);
		}
		
		// Galaxy S default settings
		if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000")) {
			preferencesWrapper.setPreferenceFloatValue(SipConfigManager.SND_MIC_LEVEL, (float) 0.4);
			preferencesWrapper.setPreferenceFloatValue(SipConfigManager.SND_SPEAKER_LEVEL, (float) 0.2);
			preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.USE_SOFT_VOLUME, true);
		}
		//HTC evo 4G
		if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
			preferencesWrapper.setPreferenceFloatValue(SipConfigManager.SND_MIC_LEVEL, (float) 0.5);
			preferencesWrapper.setPreferenceFloatValue(SipConfigManager.SND_SPEAKER_LEVEL, (float) 1.5);
			
		}
		
		//Use routing API?
		preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.USE_ROUTING_API, shouldUseRoutingApi());
		preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.USE_MODE_API, shouldUseModeApi());
		preferencesWrapper.setPreferenceBooleanValue(SipConfigManager.SET_AUDIO_GENERATE_TONE, needToneWorkaround(preferencesWrapper));
	}

	public static boolean useFlipAnimation() {
		if (android.os.Build.BRAND.equalsIgnoreCase("archos")) {
			return false;
		}
		return true;
	}
	
	public static List<ResolveInfo> getPossibleActivities(Context ctxt, Intent i){
		PackageManager pm = ctxt.getPackageManager();
		try {
			return pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
		}catch(NullPointerException e) {
			return new ArrayList<ResolveInfo>();
		}
	}
	

	public static Intent getPriviledgedIntent(String number) {
		Intent i = new Intent("android.intent.action.CALL_PRIVILEGED");
		Builder b = new Uri.Builder(); 
		b.scheme("tel").appendPath(number);
		i.setData( b.build() );
		return i;
	}
	
	private static List<ResolveInfo> callIntents = null;
	public final static List<ResolveInfo> getIntentsForCall(Context ctxt){
		if(callIntents == null) {
			callIntents = getPossibleActivities(ctxt, getPriviledgedIntent("123"));
		}
		return callIntents;
	}
	
	public static boolean canResolveIntent(Context context, final Intent intent) {
		 final PackageManager packageManager = context.getPackageManager();
		 //final Intent intent = new Intent(action);
		 List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		 return list.size() > 0;
	}
	
	private static Boolean canMakeGSMCall = null;
	private static Boolean canMakeSkypeCall = null;
	
	public static boolean canMakeGSMCall(Context context) {
		PreferencesWrapper prefs = new PreferencesWrapper(context);
		if(prefs.getGsmIntegrationType() == PreferencesWrapper.GSM_TYPE_AUTO) {
			if (canMakeGSMCall == null) {
				Intent intentMakePstnCall = new Intent(Intent.ACTION_CALL);
				intentMakePstnCall.setData(Uri.fromParts("tel", "12345", null));
				canMakeGSMCall = canResolveIntent(context, intentMakePstnCall);
			}
			return canMakeGSMCall;
		}
		if(prefs.getGsmIntegrationType() == PreferencesWrapper.GSM_TYPE_PREVENT) {
			return false;
		}
		return true;
	}
	
	public static boolean canMakeSkypeCall(Context context) {
		if (canMakeSkypeCall == null) {
			try {
			    PackageInfo skype = context.getPackageManager().getPackageInfo("com.skype.raider", 0);
			    if(skype != null) {
			    	canMakeSkypeCall = true;
			    }
			} catch (NameNotFoundException e) {
				canMakeSkypeCall = false;
			}
		}
		return canMakeSkypeCall;
	}
	

	public static Intent getContactPhoneIntent() {
    	Intent intent = new Intent(Intent.ACTION_PICK);
    	/*
		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.setType(Contacts.Phones.CONTENT_ITEM_TYPE);
		*/
    	if (!isCompatible(5)) {
    		intent.setData(Contacts.People.CONTENT_URI);
    	}else {
    		intent.setData(Uri.parse("content://com.android.contacts/contacts"));
    	}
    	

		return intent;
		
    }


	public static void updateVersion(PreferencesWrapper prefWrapper, int lastSeenVersion, int runningVersion) {
		if (lastSeenVersion < 14) {
			
			// Galaxy S default settings
			if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000")) {
				prefWrapper.setPreferenceFloatValue(SipConfigManager.SND_MIC_LEVEL, (float) 0.4);
				prefWrapper.setPreferenceFloatValue(SipConfigManager.SND_SPEAKER_LEVEL, (float) 0.2);
			}
			
			if (TextUtils.isEmpty(prefWrapper.getStunServer())) {
				prefWrapper.setPreferenceStringValue(SipConfigManager.STUN_SERVER, "stun.counterpath.com");
			}
		}
		if (lastSeenVersion < 15) {
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, false);
		}
		//Now we use svn revisions
		if (lastSeenVersion < 369) {
			// Galaxy S default settings
			if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000")) {
				prefWrapper.setPreferenceBooleanValue(SipConfigManager.USE_SOFT_VOLUME, true);
			}
			
		}
		
		if(lastSeenVersion < 385) {
			if(needPspWorkaround(prefWrapper)) {
				prefWrapper.setPreferenceBooleanValue(SipConfigManager.KEEP_AWAKE_IN_CALL, true);
			}
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.USE_ROUTING_API, shouldUseRoutingApi());
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.USE_MODE_API, shouldUseModeApi());
			prefWrapper.setPreferenceStringValue(SipConfigManager.SIP_AUDIO_MODE, guessInCallMode());
		}
		
		if(lastSeenVersion < 394) {
			//HTC PSP mode hack
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.KEEP_AWAKE_IN_CALL, needPspWorkaround(prefWrapper));
		}
		if(lastSeenVersion < 575) {
			prefWrapper.setPreferenceStringValue(SipConfigManager.THREAD_COUNT, "3");
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.SET_AUDIO_GENERATE_TONE, needToneWorkaround(prefWrapper));

			if(lastSeenVersion > 0) {
				prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP_SERVICE, true);
			}
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.ENABLE_QOS, false);
			//HTC evo 4G
			if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
				prefWrapper.setPreferenceFloatValue(SipConfigManager.SND_MIC_LEVEL, (float) 0.5);
				prefWrapper.setPreferenceFloatValue(SipConfigManager.SND_SPEAKER_LEVEL, (float) 1.5);
				prefWrapper.setPreferenceBooleanValue(SipConfigManager.USE_ROUTING_API, true);
			}
			
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.KEEP_AWAKE_IN_CALL, needPspWorkaround(prefWrapper));
			//Proximity sensor inverted
			if( android.os.Build.PRODUCT.equalsIgnoreCase("SPH-M900") /*Sgs moment*/) {
				prefWrapper.setPreferenceBooleanValue(SipConfigManager.INVERT_PROXIMITY_SENSOR, true);
			}
		}
		if(lastSeenVersion < 591) {
			resetCodecsSettings(prefWrapper);
		}
		if(lastSeenVersion < 596) {
			prefWrapper.setPreferenceBooleanValue(SipConfigManager.USE_MODE_API, shouldUseModeApi());
		}
		
	}


}

