package com.csipsimple.ui;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;

import com.csipsimple.utils.Log;
import com.csipsimple.R;

public class SMSComposer extends Activity implements OnClickListener
//		OnLongClickListener, OnDialKeyListener, TextWatcher
{
	private static final String THIS_FILE = "SMSComposer";
	
	private View smsView;
	private EditText message;
	private Button sendButton, cancelButton;
	private Integer accid;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.sms_activity);
		smsView = (View) findViewById(R.id.sms_composer);
		
		message = (EditText) findViewById(R.id.message);
		
		sendButton = (Button) findViewById(R.id.send_sms);
		sendButton.setOnClickListener(this);
		
		cancelButton = (Button) findViewById(R.id.cancel_sms);
		cancelButton.setOnClickListener(this);
	}
	
	public void onClick(View view) {
		int view_id = view.getId();
		Intent result = new Intent();
		
		if (view_id == R.id.send_sms) {
			Bundle b = new Bundle();
			b.putString("message", message.getText().toString());
			result.putExtras(b);
			setResult(Activity.RESULT_OK, result);
			Log.e(THIS_FILE, "sms: " + message.getText().toString());
		} else if (view_id == R.id.cancel_sms) {
			setResult(Activity.RESULT_CANCELED, result);
			Log.e(THIS_FILE, "sms cancelled");
		}
		finish();
	}
}
