package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.core.CoreObject;

interface ServiceInteraction
{
    // Staff methods
	int getServiceState();
	long getUpTime();
    String getAppSession();
	// Accounts API
	List getAccountsList();
	void holdAccount(int accountDbId);
	boolean removeAccount(int accountDbId);
	void updateAccountStatusIndex(String accountType, String userId, int statusIndex);
	void updateAccountStatus(String accountType, String userId, int statusIndex, String statusTitle, String statusMessage);
	void connectAccounts();
	void disconnectAccounts();
	void stopDownloadRequest(String tag);
	void stopUploadingRequest(String tag);
}