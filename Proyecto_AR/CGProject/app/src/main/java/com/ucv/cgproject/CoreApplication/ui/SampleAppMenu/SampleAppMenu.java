/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.ucv.cgproject.CoreApplication.ui.SampleAppMenu;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ucv.cgproject.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * This class creates dynamic menus for Vuforia Engine features
 * and handles gestures for opening and closing the menu
 *
 * To configure specific actions for each menu item, look for the menuProcess()
 * function in each activity
  */

public class SampleAppMenu {
    private final GestureListener mGestureListener;
    private final GestureDetector mGestureDetector;
    private final SampleAppMenuAnimator mMenuAnimator;
    private final GLSurfaceView mMovableView;
    private final SampleAppMenuView mParentMenuView;
    private final ArrayList<SampleAppMenuGroup> mSettingsItems = new ArrayList<>();

    private final ArrayList<View> mAdditionalViews;
    private float[] mInitialAdditionalViewsX;
    private final int mScreenWidth;
    private int mListViewWidth = 0;

    // True if dragging and displaying the menu
    private boolean mSwipingMenu = false;

    // True if menu is showing and docked
    private boolean mStartMenuDisplaying = false;

    private static final float SETTINGS_MENU_SCREEN_PERCENTAGE = .80f;
    private static final float SETTINGS_MENU_SCREEN_MIN_PERCENTAGE_TO_SHOW = .1f;

    // Parameters:
    // menuInterface - Reference to the object which will be handling the
    // processes from the menu entries
    // activity - The activity where the swipe menu will be used
    // menuTitle - Title to be displayed
    // movableView - SurfaceView where the OpenGL rendering is done
    // parentView - Parent view where the settings layout will be attached
    // additionalViewToHide - Additional view to move with openGl view
    public SampleAppMenu(final SampleAppMenuInterface menuInterface, Activity activity, String menuTitle,
                         GLSurfaceView movableView, RelativeLayout parentView, ArrayList<View> additionalViewsToHide) {
        WeakReference<Activity> mActivityRef = new WeakReference<>(activity);
        mMovableView = movableView;
        mAdditionalViews = additionalViewsToHide;

        mParentMenuView = (SampleAppMenuView) View.inflate(mActivityRef.get(), R.layout.sample_app_menu_layer, null);
        parentView.addView(mParentMenuView);

        LinearLayout mMovableListView = mParentMenuView.findViewById(R.id.settings_menu);
        mMovableListView.setBackgroundColor(Color.WHITE);

        TextView title = mParentMenuView.findViewById(R.id.settings_menu_title);
        title.setText(menuTitle);

        mParentMenuView.findViewById(R.id.linear_map).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuInterface.menuProcess(1);
            }
        });

        mParentMenuView.findViewById(R.id.linear_locations).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuInterface.menuProcess(2);
            }
        });

        mParentMenuView.findViewById(R.id.linear_images).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuInterface.menuProcess(3);
            }
        });

        mMovableView.setVisibility(View.VISIBLE);

        if (mAdditionalViews != null && mAdditionalViews.size() > 0) {
            mInitialAdditionalViewsX = new float[mAdditionalViews.size()];
            setAdditionalViewsX();
        }

        mGestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(mActivityRef.get(), mGestureListener);

        mMenuAnimator = new SampleAppMenuAnimator(this);

        // Screen width is used to slide the menu out as a percentage of the screen
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;

        // Used to set the listView length depending on the glView width
        ViewTreeObserver vto = mMovableView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                int menuWidth = Math.min(mMovableView.getWidth(), mMovableView.getHeight());
                mListViewWidth = (int) (menuWidth * SETTINGS_MENU_SCREEN_PERCENTAGE);

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mListViewWidth, RelativeLayout.LayoutParams.MATCH_PARENT);

                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                mParentMenuView.setLayoutParams(params);

                setMenuDisplaying(false);
                mGestureListener.setMaxSwipe(mListViewWidth);

                LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(mListViewWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

                for (SampleAppMenuGroup group : mSettingsItems)
                    group.getMenuLayout().setLayoutParams(groupParams);

                mMovableView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    // Handles gestures for opening/closing menu
    public boolean processEvent(MotionEvent event) {
        boolean result = mGestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP && !result) {
            mSwipingMenu = false;

            // Hides the menu if it is not docked when releasing
            if (!isMenuDisplaying() || getViewX(mMovableView) < (mScreenWidth * SETTINGS_MENU_SCREEN_PERCENTAGE)) {
                if (isMenuDisplaying() || getViewX(mMovableView) < (mScreenWidth * SETTINGS_MENU_SCREEN_MIN_PERCENTAGE_TO_SHOW))
                    hideMenu();
                else showMenu();
            }
        }

        return result;
    }

    // Slides menu in and out as a percentage of the screen width
    private void startViewsAnimation(boolean display) {
        float targetX = display ? mGestureListener.getMaxSwipe() : 0;

        mMenuAnimator.setStartEndX(getViewX(mMovableView), targetX);
        mMenuAnimator.start();

        if (mAdditionalViews != null) {
            for (int i = 0; i < mAdditionalViews.size(); i++)
                setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i] + targetX);
        }
    }

    private boolean isMenuDisplaying() {
        return mStartMenuDisplaying;
    }

    private void setMenuDisplaying(boolean isMenuDisplaying) {
        // This is used to avoid the ListView to consume the incoming event when the menu is not displayed.
        mParentMenuView.setFocusable(isMenuDisplaying);
        mParentMenuView.setFocusableInTouchMode(isMenuDisplaying);
        mParentMenuView.setClickable(isMenuDisplaying);
        mParentMenuView.setEnabled(isMenuDisplaying);

        mStartMenuDisplaying = isMenuDisplaying;
    }

    void hide() {
        setViewX(mMovableView, 0);

        mParentMenuView.setHorizontalClipping(0);
        mParentMenuView.setVisibility(View.GONE);

        if (mAdditionalViews != null) {
            for (int i = 0; i < mAdditionalViews.size(); i++)
                setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i]);
        }
    }

    private void setViewX(View view, float x) {
        view.setX(x);
    }

    private float getViewX(View view) {
        return view.getX();
    }

    private void showMenu() {
        startViewsAnimation(true);
    }

    void hideMenu() {
        if (!mMenuAnimator.isRunning()) {
            startViewsAnimation(false);
            setMenuDisplaying(false);
        }
    }

    public void toggleMenu() {
        if(isMenuDisplaying())
            hideMenu();
        else showMenu();
    }

    public void attachMenu() {
        hide();
        setMenuDisplaying(false);
    }

    // Stores the animation X coordinate which slides the view across the screen
    void setAnimationX(float animtationX) {
        mParentMenuView.setVisibility(View.VISIBLE);
        setViewX(mMovableView, animtationX);

        mParentMenuView.setHorizontalClipping((int) animtationX);

        if (mAdditionalViews != null) {
            for (int i = 0; i < mAdditionalViews.size(); i++)
                setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i] + animtationX);
        }
    }

    private void setAdditionalViewsX() {
        if (mAdditionalViews == null)
            return;

        for (int i = 0; i < mAdditionalViews.size(); i++)
            mInitialAdditionalViewsX[i] = getViewX(mAdditionalViews.get(i));
    }

    void setDockMenu(boolean isDocked) {
        setMenuDisplaying(isDocked);

        if (!isDocked && mParentMenuView.isEnabled())
            hideMenu();
    }

    // Process the gestures to handle the menu
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        // Minimum distance to start displaying the menu
        final int DISTANCE_TRESHOLD = 10;

        // Minimum velocity to display the menu upon fling
        final int VELOCITY_TRESHOLD = 2000;

        // Maximum x to dock the menu
        float mMaxXSwipe;

        // Called when dragging
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Prepare the views for movement
            if (Math.abs(distanceX) > DISTANCE_TRESHOLD && !mSwipingMenu) {
                mSwipingMenu = true;
                mParentMenuView.setVisibility(View.VISIBLE);

                if (!mStartMenuDisplaying)
                    setAdditionalViewsX();
            }

            // Slide the views along with touch coordinate
            if (mSwipingMenu && getViewX(mMovableView) - distanceX > 0) {
                float deltaX = Math.min(mMaxXSwipe, getViewX(mMovableView) - distanceX);

                setViewX(mMovableView, deltaX);

                mParentMenuView.setHorizontalClipping((int) deltaX);

                if (mAdditionalViews != null) {
                    for (int i = 0; i < mAdditionalViews.size(); i++)
                        setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i] + deltaX);
                }
            }

            // If touch exceeds the intended width of the menu,
            // start the animation to smoothly slide the menu
            if (mMaxXSwipe <= getViewX(mMovableView)) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMenu();
                    }
                }, 100L);
            }

            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (velocityX > VELOCITY_TRESHOLD && !isMenuDisplaying())
                showMenu();
            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            boolean consumeTapUp = isMenuDisplaying();
            hideMenu();
            return consumeTapUp;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!isMenuDisplaying())
                startViewsAnimation(true);
            return true;
        }

        // Percentage of the screen to display and maintain the menu
        private void setMaxSwipe(float maxXSwipe) {
            mMaxXSwipe = maxXSwipe;
            mMenuAnimator.setMaxX(mMaxXSwipe);
            mMenuAnimator.setStartEndX(0.0f, mMaxXSwipe);
        }

        float getMaxSwipe() {
            return mMaxXSwipe;
        }
    }
}
