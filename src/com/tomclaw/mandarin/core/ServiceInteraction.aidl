package com.tomclaw.mandarin.core;

import  com.tomclaw.mandarin.im.AccountRoot;

interface ServiceInteraction
{
    // Staff methods
	boolean initService();
	long getUpTime();
	// Accounts API
	List getAccountsList();
	void addAccount(in AccountRoot accountRoot);
}