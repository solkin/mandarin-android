package com.tomclaw.mandarin.core;

interface ServiceInteraction
{
    // Staff methods
	boolean initService();
	long getUpTime();
	// Accounts API
	List getAccountsList();
}