/*===============================================================================
Copyright (c) 2016,2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.ucv.cgproject.SampleApplication.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.lang.ref.WeakReference;


/**
 * This class handles the loading dialog present in all of the activities.
 */
public final class LoadingDialogHandler extends Handler
{
    private final WeakReference<Activity> mActivityRef;
    // Constants for Hiding/Showing Loading dialog
    public static final int HIDE_LOADING_DIALOG = 0;
    public static final int SHOW_LOADING_DIALOG = 1;
    
    public View mLoadingDialogContainer;
    
    
    public LoadingDialogHandler(Activity activity)
    {
        mActivityRef = new WeakReference<>(activity);
    }
    
    
    public void handleMessage(Message msg)
    {
        Activity activity = mActivityRef.get();
        if (activity == null)
        {
            return;
        }
        
        if (msg.what == SHOW_LOADING_DIALOG)
        {
            mLoadingDialogContainer.setVisibility(View.VISIBLE);
            
        } else if (msg.what == HIDE_LOADING_DIALOG)
        {
            mLoadingDialogContainer.setVisibility(View.GONE);
        }
    }
}
