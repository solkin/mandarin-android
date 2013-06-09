package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.core.CoreObject;

interface ServiceInteraction
{
    // Staff methods
	boolean initService();
	long getUpTime();
	// Accounts API
	List getAccountsList();
	void addAccount(in CoreObject accountRoot);
	boolean removeAccount(String accountType, String userId);
}