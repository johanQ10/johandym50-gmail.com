/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.ucv.cgproject.SampleApplication.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Support class for the Vuforia Engine sample applications
 * Responsible for setting up and configuring the OpenGL surface view.
 *
 * This class does not contain any Vuforia Engine specific code.
 * You can use your own OpenGL implementation.
 */
public class SampleApplicationGLView extends GLSurfaceView {
    private static final String LOGTAG = "VuforiaEngine_SampleGLView";

    public SampleApplicationGLView(Context context) {
        super(context);
    }

    @SuppressLint("LongLogTag")
    public void init(boolean translucent, int depth, int stencil) {
        // By default GLSurfaceView tries to find a surface that is as close
        // as possible to a 16-bit RGB frame buffer with a 16-bit depth buffer.
        // This function can override the default values and set custom values.
        
        // By default, GLSurfaceView() creates a RGB_565 opaque surface.
        // If we want a translucent one, we should change the surface's
        // format here, using PixelFormat.TRANSLUCENT for GL Surfaces
        // is interpreted as any 32-bit surface with alpha by SurfaceFlinger.
        
        Log.i(LOGTAG, "Using OpenGL ES 2.0");
        Log.i(LOGTAG, "Using " + (translucent ? "translucent" : "opaque")
            + " GLView, depth buffer size: " + depth + ", stencil size: " + stencil);
        
        // If required set translucent format to allow camera image to
        // show through in the background
        if (translucent)
            this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        
        // Setup the context factory for 2.0 rendering
        setEGLContextFactory(new ContextFactory());
        
        // We need to choose an EGLConfig that matches the format of
        // our surface exactly. This is going to be done in our
        // custom config chooser. See ConfigChooser class definition
        // below.
        setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth,
            stencil) : new ConfigChooser(5, 6, 5, 0, depth, stencil));
    }
    
    // Creates OpenGL contexts.
    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        @SuppressLint("LongLogTag")
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            EGLContext context;

            Log.i(LOGTAG, "Creating OpenGL ES 2.0 context");
            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list_gl20 = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list_gl20);

            checkEglError("After eglCreateContext", egl);
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }
    }

    @SuppressLint("LongLogTag")
    private static void checkEglError(String prompt, EGL10 egl) {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS)
            Log.e(LOGTAG, String.format("%s: EGL error: 0x%x", prompt, error));
    }

    private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {
        private ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
            mRedSize = r;
            mGreenSize = g;
            mBlueSize = b;
            mAlphaSize = a;
            mDepthSize = depth;
            mStencilSize = stencil;
        }

        private EGLConfig getMatchingConfig(EGL10 egl, EGLDisplay display, int[] configAttribs) {
            // Get the number of minimally matching EGL configurations
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, configAttribs, null, 0, num_config);
            
            int numConfigs = num_config[0];
            if (numConfigs <= 0)
                throw new IllegalArgumentException("No matching EGL configs");
            
            // Allocate then read the array of minimally matching EGL configs
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, configAttribs, configs, numConfigs, num_config);
            
            // Now return the "best" one
            return chooseConfig(egl, display, configs);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            // This EGL config specification is used to specify 2.0
            // rendering. We use a minimum size of 4 bits for
            // red/green/blue, but will perform actual matching in
            // chooseConfig() below.
            final int EGL_OPENGL_ES2_BIT = 0x0004;
            final int[] s_configAttribs_gl20 = { EGL10.EGL_RED_SIZE, 4,
                    EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_NONE };
                
            return getMatchingConfig(egl, display, s_configAttribs_gl20);
        }

        private EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE);
                int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE);
                
                // We need at least mDepthSize and mStencilSize bits
                if (d < mDepthSize || s < mStencilSize)
                    continue;
                
                // We want an *exact* match for red/green/blue/alpha
                int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE);
                int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE);
                int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE);
                int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE);
                
                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
                    return config;
            }
            
            return null;
        }
        
        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute) {
            if (egl.eglGetConfigAttrib(display, config, attribute, mValue))
                return mValue[0];
            
            return 0;
        }
        
        // Subclasses can adjust these values:
        private final int mRedSize;
        private final int mGreenSize;
        private final int mBlueSize;
        private final int mAlphaSize;
        private final int mDepthSize;
        private final int mStencilSize;
        private final int[] mValue = new int[1];
    }
}
