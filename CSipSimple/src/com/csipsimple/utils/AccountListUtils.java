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

import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsuaConstants;

import android.app.Activity;
import android.graphics.Color;
import android.os.RemoteException;

import com.csipsimple.R;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.service.ISipService;


public class AccountListUtils {

	public static final class AccountStatusDisplay {
		public String statusLabel;
		public int statusColor;
		public int checkBoxIndicator;
		public boolean availableForCalls;
	}
	
	
	public static AccountStatusDisplay getAccountDisplay(Activity context, ISipService service, int accountId) {
		AccountStatusDisplay accountDisplay = new AccountStatusDisplay();
		accountDisplay.statusLabel = context.getString(R.string.acct_inactive);
		accountDisplay.statusColor = Color.argb(255, 100, 100, 100); 
		accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
		accountDisplay.availableForCalls = false;
		
		if (service != null) {
			AccountInfo accountInfo;
			try {
				accountInfo = service.getAccountInfo(accountId);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isActive()) {
				if (accountInfo.getAddedStatus() == pjsuaConstants.PJ_SUCCESS) {

					accountDisplay.statusLabel = context.getString(R.string.acct_unregistered);
					accountDisplay.statusColor = Color.argb(255, 255, 194, 0);
					accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;

					if (accountInfo.getPjsuaId() >= 0) {
						String pjStat = accountInfo.getStatusText();	// Used only on error status message
						pjsip_status_code statusCode = accountInfo.getStatusCode();
						if (statusCode == pjsip_status_code.PJSIP_SC_OK) {
							// Log.d(THIS_FILE,
							// "Now account "+account.display_name+" has expires "+accountInfo.getExpires());
							if (accountInfo.getExpires() > 0) {
								// Green
								accountDisplay.statusColor = Color.argb(255, 132, 227, 0);
								accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_on;
								accountDisplay.statusLabel = context.getString(R.string.acct_registered);
								accountDisplay.availableForCalls = true;
							} else {
								// Yellow unregistered
								accountDisplay.statusColor = Color.argb(255, 255, 194, 0);
								accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
								accountDisplay.statusLabel = context.getString(R.string.acct_unregistered);
							}
						} else {
							if (statusCode == pjsip_status_code.PJSIP_SC_PROGRESS || statusCode == pjsip_status_code.PJSIP_SC_TRYING) {
								// Yellow progressing ...
								accountDisplay.statusColor = Color.argb(255, 255, 194, 0);
								accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
								accountDisplay.statusLabel = context.getString(R.string.acct_registering);
							} else {
								// Red : error
								accountDisplay.statusColor = Color.argb(255, 255, 0, 0);
								accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_red;
								accountDisplay.statusLabel = context.getString(R.string.acct_regerror) + " - " + pjStat;	// Why can't ' - ' be in resource?
							}
						}
					}
				} else {
					accountDisplay.statusLabel = context.getString(R.string.acct_regfailed);
					accountDisplay.statusColor = Color.argb(255, 255, 15, 0);
				}
			}
		}
		return accountDisplay;
	}
	
	
	
}
