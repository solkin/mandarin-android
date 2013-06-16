package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.core.CoreObject;

interface ServiceInteraction
{
    // Staff methods
	boolean initService();
	long getUpTime();
    String getAppSession();
	// Accounts API
	List getAccountsList();
	void addAccount(in CoreObject accountRoot);
	boolean removeAccount(String accountType, String userId);
	void setAccountStatus(String accountType, String userId, int statusIndex);
}