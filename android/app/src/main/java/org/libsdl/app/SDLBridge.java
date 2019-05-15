package org.libsdl.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.lang.reflect.Method;
import java.lang.Math;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.os.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.graphics.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;

/**
    SDL SDLBridge
*/
public class SDLBridge {
    private static final String TAG = "SDL";

    public static boolean mIsResumedCalled, mIsSurfaceReady, mHasFocus;

    // Cursor types
    private static final int SDL_SYSTEM_CURSOR_NONE = -1;
    private static final int SDL_SYSTEM_CURSOR_ARROW = 0;
    private static final int SDL_SYSTEM_CURSOR_IBEAM = 1;
    private static final int SDL_SYSTEM_CURSOR_WAIT = 2;
    private static final int SDL_SYSTEM_CURSOR_CROSSHAIR = 3;
    private static final int SDL_SYSTEM_CURSOR_WAITARROW = 4;
    private static final int SDL_SYSTEM_CURSOR_SIZENWSE = 5;
    private static final int SDL_SYSTEM_CURSOR_SIZENESW = 6;
    private static final int SDL_SYSTEM_CURSOR_SIZEWE = 7;
    private static final int SDL_SYSTEM_CURSOR_SIZENS = 8;
    private static final int SDL_SYSTEM_CURSOR_SIZEALL = 9;
    private static final int SDL_SYSTEM_CURSOR_NO = 10;
    private static final int SDL_SYSTEM_CURSOR_HAND = 11;

    protected static final int SDL_ORIENTATION_UNKNOWN = 0;
    protected static final int SDL_ORIENTATION_LANDSCAPE = 1;
    protected static final int SDL_ORIENTATION_LANDSCAPE_FLIPPED = 2;
    protected static final int SDL_ORIENTATION_PORTRAIT = 3;
    protected static final int SDL_ORIENTATION_PORTRAIT_FLIPPED = 4;

    protected static int mCurrentOrientation;

    // Handle the state of the native layer
    public enum NativeState {
           INIT, RESUMED, PAUSED
    }

    public static NativeState mNextNativeState;
    public static NativeState mCurrentNativeState;

    public static boolean mExitCalledFromJava;

    /** If shared libraries (e.g. SDL or the native application) could not be loaded. */
    public static boolean mBrokenLibraries;

    // If we want to separate mouse and touch events.
    //  This is only toggled in native code when a hint is set!
    public static boolean mSeparateMouseAndTouch;

    // Main components
    protected static SDLBridge mSingleton = new SDLBridge();
    protected static SDLSurface mSurface;
    protected static View mTextEdit;
    protected static boolean mScreenKeyboardShown;
    protected static Hashtable<Integer, Object> mCursors;
    protected static int mLastCursorID;

    // This is what SDL runs in. It invokes SDL_main(), eventually
    protected static Thread mSDLThread;

    boolean isLibraryInitialized = false;

    public static SDLBridge getInstance() {
        return mSingleton;
    }

    /**
     * This method returns the name of the shared object with the application entry point
     * It can be overridden by derived classes.
     */
    protected String getMainSharedObject() {
        String library;
        String[] libraries = SDLBridge.mSingleton.getLibraries();
        if (libraries.length > 0) {
            library = "lib" + libraries[libraries.length - 1] + ".so";
        } else {
            library = "libmain.so";
        }
        return getContext().getApplicationInfo().nativeLibraryDir + "/" + library;
    }

    /**
     * This method returns the name of the application entry point
     * It can be overridden by derived classes.
     */
    protected String getMainFunction() {
        return "SDL_main";
    }

    /**
     * This method is called by SDL before loading the native shared libraries.
     * It can be overridden to provide names of shared libraries to be loaded.
     * The default implementation returns the defaults. It never returns null.
     * An array returned by a new implementation must at least contain "SDL2".
     * Also keep in mind that the order the libraries are loaded may matter.
     * @return names of shared libraries to be loaded (e.g. "SDL2", "main").
     */
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            // "SDL2_image",
            // "SDL2_mixer",
            // "SDL2_net",
            // "SDL2_ttf",
            "main"
        };
    }

    // Load the .so
    public void loadLibraries() {
       for (String lib : getLibraries()) {
          SDL.loadLibrary(lib);
       }
    }

    /**
     * This method is called by SDL before starting the native application thread.
     * It can be overridden to provide the arguments after the application name.
     * The default implementation returns an empty array. It never returns null.
     * @return arguments for the native application.
     */
    protected String[] getArguments() {
        return new String[0];
    }

    public static void initialize() {
        // The static nature of the singleton and Android quirkyness force us to initialize everything here
        // Otherwise, when exiting the app and returning to it, these variables *keep* their pre exit values
        mSingleton = new SDLBridge();
        mSurface = null;
        mTextEdit = null;
        mCursors = new Hashtable<Integer, Object>();
        mLastCursorID = 0;
        mSDLThread = null;
        mExitCalledFromJava = false;
        mBrokenLibraries = false;
        mIsResumedCalled = false;
        mIsSurfaceReady = false;
        mHasFocus = true;
        mNextNativeState = NativeState.INIT;
        mCurrentNativeState = NativeState.INIT;
    }

    public SDLBridge() {
    }

    private void setupSDL() {
        Log.v(TAG, "Device: " + Build.DEVICE);
        Log.v(TAG, "Model: " + Build.MODEL);
        Log.v(TAG, "onCreate()");

        // Load shared libraries
        String errorMsgBrokenLib = "";
        try {
            loadLibraries();
        } catch(UnsatisfiedLinkError e) {
            System.err.println(e.getMessage());
            mBrokenLibraries = true;
            errorMsgBrokenLib = e.getMessage();
        } catch(Exception e) {
            System.err.println(e.getMessage());
            mBrokenLibraries = true;
            errorMsgBrokenLib = e.getMessage();
        }

        if (mBrokenLibraries)
        {
            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(getContext());
            dlgAlert.setMessage("An error occurred while trying to start the application. Please try again and/or reinstall."
                    + System.getProperty("line.separator")
                    + System.getProperty("line.separator")
                    + "Error: " + errorMsgBrokenLib);
            dlgAlert.setTitle("SDL Error");
            dlgAlert.setPositiveButton("Exit",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,int id) {
                        }
                    });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
        }

        // Set up JNI
        SDL.setupJNI();

        // Initialize state
        SDL.initialize();
    }

    // Setup
    public SDLSurface create(Context context) {


        synchronized (mSingleton) {
            if (!isLibraryInitialized) {
                setupSDL();
                isLibraryInitialized = true;
            }
        }

        // So we can call stuff from static callbacks
        SDL.setContext(context);

        // Set up the surface
        mSurface = new SDLSurface(getContext());

        // Get our current screen orientation and pass it down.
        mCurrentOrientation = SDLBridge.getCurrentOrientation();
        SDLBridge.onNativeOrientationChanged(mCurrentOrientation);

        this.onResume();

        return mSurface;
    }

    protected void onPause() {
        // TODO: reimplement this
        Log.v(TAG, "onPause()");
        mNextNativeState = NativeState.PAUSED;
        mIsResumedCalled = false;

        if (SDLBridge.mBrokenLibraries) {
           return;
        }

        SDLBridge.handleNativeState();
    }


    public void onResume() {
        Log.v(TAG, "onResume()");
        // TODO: reimplement this
        mNextNativeState = NativeState.RESUMED;
        mIsResumedCalled = true;

        if (SDLBridge.mBrokenLibraries) {
           return;
        }

        SDLBridge.handleNativeState();
    }

    public static int getCurrentOrientation() {
        final Context context = SDLBridge.getContext();
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int result = SDL_ORIENTATION_UNKNOWN;

        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                result = SDL_ORIENTATION_PORTRAIT;
                break;
    
            case Surface.ROTATION_90:
                result = SDL_ORIENTATION_LANDSCAPE;
                break;
    
            case Surface.ROTATION_180:
                result = SDL_ORIENTATION_PORTRAIT_FLIPPED;
                break;
    
            case Surface.ROTATION_270:
                result = SDL_ORIENTATION_LANDSCAPE_FLIPPED;
                break;
        }

        return result;
    }


    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO: reimplement this
        Log.v(TAG, "onWindowFocusChanged(): " + hasFocus);

        if (SDLBridge.mBrokenLibraries) {
           return;
        }

        SDLBridge.mHasFocus = hasFocus;
        if (hasFocus) {
           mNextNativeState = NativeState.RESUMED;
        } else {
           mNextNativeState = NativeState.PAUSED;
        }

        SDLBridge.handleNativeState();
    }

    public void onLowMemory() {
        Log.v(TAG, "onLowMemory()");
        // TODO: reimplement this

        if (SDLBridge.mBrokenLibraries) {
           return;
        }

        SDLBridge.nativeLowMemory();
    }


    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        // TODO: reimplement this

        if (SDLBridge.mBrokenLibraries) {
           // Reset everything in case the user re opens the app
           SDLBridge.initialize();
           return;
        }

        mNextNativeState = NativeState.PAUSED;
        SDLBridge.handleNativeState();

        // Send a quit message to the application
        SDLBridge.mExitCalledFromJava = true;
        SDLBridge.nativeQuit();

        // Now wait for the SDL thread to quit
        if (SDLBridge.mSDLThread != null) {
            try {
                SDLBridge.mSDLThread.join();
            } catch(Exception e) {
                Log.v(TAG, "Problem stopping thread: " + e);
            }
            SDLBridge.mSDLThread = null;

            //Log.v(TAG, "Finished waiting for SDL thread");
        }


        // Reset everything in case the user re opens the app
        SDLBridge.initialize();
    }

    // Called by JNI from SDL.
    public static void manualBackButton() {
    }

    /* Transition to next state */

    public static void handleNativeState() {

        if (mNextNativeState == mCurrentNativeState) {
            // Already in same state, discard.
            return;
        }

        // Try a transition to init state
        if (mNextNativeState == NativeState.INIT) {

            mCurrentNativeState = mNextNativeState;
            return;
        }

        // Try a transition to paused state
        if (mNextNativeState == NativeState.PAUSED) {
            nativePause();
            mCurrentNativeState = mNextNativeState;
            return;
        }

        // Try a transition to resumed state
        if (mNextNativeState == NativeState.RESUMED) {
            if (mIsSurfaceReady && mHasFocus && mIsResumedCalled) {
                if (mSDLThread == null) {
                    // This is the entry point to the C app.
                    // Start up the C app thread and enable sensor input for the first time
                    // FIXME: Why aren't we enabling sensor input at start?

                    mSDLThread = new Thread(new SDLMain(), "SDLThread");
                    mSDLThread.start();
                }

                nativeResume();
                mSurface.handleResume();
                mCurrentNativeState = mNextNativeState;
            }
        }
    }

    /* The native thread has finished */
    public static void handleNativeExit() {
        SDLBridge.mSDLThread = null;
        if (mSingleton != null) {
            // TODO: reimplement this
            //mSingleton.finish();
        }
    }


    // Messages from the SDLMain thread
    static final int COMMAND_CHANGE_TITLE = 1;
    static final int COMMAND_CHANGE_WINDOW_STYLE = 2;
    static final int COMMAND_TEXTEDIT_HIDE = 3;
    static final int COMMAND_SET_KEEP_SCREEN_ON = 5;

    protected static final int COMMAND_USER = 0x8000;

    protected static boolean mFullscreenModeActive;

    /**
     * This method is called by SDL if SDL did not handle a message itself.
     * This happens if a received message contains an unsupported command.
     * Method can be overwritten to handle Messages in a different class.
     * @param command the command of the message.
     * @param param the parameter of the message. May be null.
     * @return if the message was handled in overridden method.
     */
    protected boolean onUnhandledMessage(int command, Object param) {
        return false;
    }

    /**
     * A Handler class for Messages from native SDL applications.
     * It uses current Activities as target (e.g. for the title).
     * static to prevent implicit references to enclosing object.
     */
    protected static class SDLCommandHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Context context = SDL.getContext();
            if (context == null) {
                Log.e(TAG, "error handling message, getContext() returned null");
                return;
            }
            switch (msg.arg1) {
            case COMMAND_CHANGE_TITLE:
                if (context instanceof Activity) {
                    ((Activity) context).setTitle((String)msg.obj);
                } else {
                    Log.e(TAG, "error handling message, getContext() returned no Activity");
                }
                break;
            case COMMAND_CHANGE_WINDOW_STYLE:
                if (Build.VERSION.SDK_INT < 19) {
                    // This version of Android doesn't support the immersive fullscreen mode
                    break;
                }
                if (context instanceof Activity) {
                    Window window = ((Activity) context).getWindow();
                    if (window != null) {
                        if ((msg.obj instanceof Integer) && (((Integer) msg.obj).intValue() != 0)) {
                            int flags = View.SYSTEM_UI_FLAG_FULLSCREEN |
                                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.INVISIBLE;
                            window.getDecorView().setSystemUiVisibility(flags);        
                            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                            SDLBridge.mFullscreenModeActive = true;
                        } else {
                            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_VISIBLE;
                            window.getDecorView().setSystemUiVisibility(flags);
                            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            SDLBridge.mFullscreenModeActive = false;
                        }
                    }
                } else {
                    Log.e(TAG, "error handling message, getContext() returned no Activity");
                }
                break;
            case COMMAND_TEXTEDIT_HIDE:
                if (mTextEdit != null) {
                    // Note: On some devices setting view to GONE creates a flicker in landscape.
                    // Setting the View's sizes to 0 is similar to GONE but without the flicker.
                    // The sizes will be set to useful values when the keyboard is shown again.
                    mTextEdit.setLayoutParams(new RelativeLayout.LayoutParams(0, 0));

                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
                    
                    mScreenKeyboardShown = false;
                }
                break;
            case COMMAND_SET_KEEP_SCREEN_ON:
            {
                if (context instanceof Activity) {
                    Window window = ((Activity) context).getWindow();
                    if (window != null) {
                        if ((msg.obj instanceof Integer) && (((Integer) msg.obj).intValue() != 0)) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }
                }
                break;
            }
            default:
                // TODO: reimplement this
                /*if ((context instanceof SDLBridge) && !((SDLBridge) context).onUnhandledMessage(msg.arg1, msg.obj)) {
                    Log.e(TAG, "error handling message, command is " + msg.arg1);
                }*/
            }
        }
    }

    // Handler for the messages
    Handler commandHandler = new SDLCommandHandler();

    // Send a message from the SDLMain thread
    boolean sendCommand(int command, Object data) {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        boolean result = commandHandler.sendMessage(msg);

        if ((Build.VERSION.SDK_INT >= 19) && (command == COMMAND_CHANGE_WINDOW_STYLE)) {
            // Ensure we don't return until the resize has actually happened,
            // or 500ms have passed.

            boolean bShouldWait = true;

            if (bShouldWait) {
                // We'll wait for the surfaceChanged() method, which will notify us
                // when called.  That way, we know our current size is really the
                // size we need, instead of grabbing a size that's still got
                // the navigation and/or status bars before they're hidden.
                //
                // We'll wait for up to half a second, because some devices 
                // take a surprisingly long time for the surface resize, but
                // then we'll just give up and return.
                //
                synchronized(SDLBridge.getContext()) {
                    try {
                        SDLBridge.getContext().wait(500);
                    }
                    catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }

        return result;
    }

    public static void runCommand(String function, int value) {
        String library = SDLBridge.mSingleton.getMainSharedObject();
        SDLBridge.nativeRunCommand(library, function, value);
    }

    public static void setTexturePath(String path) {
        String library = SDLBridge.mSingleton.getMainSharedObject();
        SDLBridge.nativeSetTexturePath(library, path);
    }

    // C functions we call
    public static native int nativeSetupJNI();
    public static native int nativeRunMain(String library, String function, Object arguments);
    public static native int nativeRunCommand(String library, String function, int value);
    public static native int nativeSetTexturePath(String library, String path);

    public static native void nativeLowMemory();
    public static native void nativeQuit();
    public static native void nativePause();
    public static native void nativeResume();

    public static native void onNativeDropFile(String filename);
    public static native void onNativeResize(int surfaceWidth, int surfaceHeight, int deviceWidth, int deviceHeight, int format, float rate);
    public static native void onNativeKeyDown(int keycode);
    public static native void onNativeKeyUp(int keycode);
    public static native void onNativeKeyboardFocusLost();
    public static native void onNativeMouse(int button, int action, float x, float y, boolean relative);
    public static native void onNativeTouch(int touchDevId, int pointerFingerId,
                                            int action, float x,
                                            float y, float p);
    public static native void onNativeAccel(float x, float y, float z);
    public static native void onNativeClipboardChanged();
    public static native void onNativeSurfaceChanged();
    public static native void onNativeSurfaceDestroyed();
    public static native String nativeGetHint(String name);
    public static native void nativeSetenv(String name, String value);
    public static native void onNativeOrientationChanged(int orientation);

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean setActivityTitle(String title) {
        // Called from SDLMain() thread and can't directly affect the view
        return mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static void setWindowStyle(boolean fullscreen) {
        // Called from SDLMain() thread and can't directly affect the view
        mSingleton.sendCommand(COMMAND_CHANGE_WINDOW_STYLE, fullscreen ? 1 : 0);
    }

    /**
     * This method is called by SDL using JNI.
     * This is a static method for JNI convenience, it calls a non-static method
     * so that is can be overridden  
     */
    public static void setOrientation(int w, int h, boolean resizable, String hint)
    {
        if (mSingleton != null) {
            mSingleton.setOrientationBis(w, h, resizable, hint);
        }
    }
   
    /**
     * This can be overridden
     */
    public void setOrientationBis(int w, int h, boolean resizable, String hint) 
    {
        int orientation = -1;

        if (hint.contains("LandscapeRight") && hint.contains("LandscapeLeft")) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        } else if (hint.contains("LandscapeRight")) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if (hint.contains("LandscapeLeft")) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else if (hint.contains("Portrait") && hint.contains("PortraitUpsideDown")) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        } else if (hint.contains("Portrait")) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else if (hint.contains("PortraitUpsideDown")) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }

        /* no valid hint */
        if (orientation == -1) {
            if (resizable) {
                /* no fixed orientation */
            } else {
                if (w > h) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                }
            }
        }

        Log.v("SDL", "setOrientation() orientation=" + orientation + " width=" + w +" height="+ h +" resizable=" + resizable + " hint=" + hint);
        if (orientation != -1) {
            // TODO: reimplement this
            //mSingleton.setRequestedOrientation(orientation);
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean isScreenKeyboardShown() 
    {
        if (mTextEdit == null) {
            return false;
        }

        if (!mScreenKeyboardShown) {
            return false;
        }

        InputMethodManager imm = (InputMethodManager) SDL.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm.isAcceptingText();

    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean supportsRelativeMouse()
    {
       return false;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean setRelativeMouseEnabled(boolean enabled)
    {
      return false;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean sendMessage(int command, int param) {
        if (mSingleton == null) {
            return false;
        }
        return mSingleton.sendCommand(command, Integer.valueOf(param));
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static Context getContext() {
        return SDL.getContext();
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean isAndroidTV() {
        return false;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean isTablet() {
        DisplayMetrics metrics = new DisplayMetrics();
        Activity activity = (Activity)getContext();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        double dWidthInches = metrics.widthPixels / (double)metrics.xdpi;
        double dHeightInches = metrics.heightPixels / (double)metrics.ydpi;

        double dDiagonal = Math.sqrt((dWidthInches * dWidthInches) + (dHeightInches * dHeightInches));

        // If our diagonal size is seven inches or greater, we consider ourselves a tablet.
        return (dDiagonal >= 7.0);
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean isChromebook() {
        return getContext().getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
    }    

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean isDeXMode() {
        if (Build.VERSION.SDK_INT < 24) {
            return false;
        }
        try {
            final Configuration config = getContext().getResources().getConfiguration();
            final Class configClass = config.getClass();
            return configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                    == configClass.getField("semDesktopModeEnabled").getInt(config);
        } catch(Exception ignored) {
            return false;
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static DisplayMetrics getDisplayDPI() {
        return getContext().getResources().getDisplayMetrics();
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean getManifestEnvironmentVariables() {
        try {
            ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            if (bundle == null) {
                return false;
            }
            String prefix = "SDL_ENV.";
            final int trimLength = prefix.length();
            for (String key : bundle.keySet()) {
                if (key.startsWith(prefix)) {
                    String name = key.substring(trimLength);
                    String value = bundle.get(key).toString();
                    nativeSetenv(name, value);
                }
            }
            /* environment variables set! */
            return true; 
        } catch (Exception e) {
           Log.v("SDL", "exception " + e.toString());
        }
        return false;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean showTextInput(int x, int y, int w, int h) {
        return false;
    }

    public static boolean isTextInputEvent(KeyEvent event) {
        return false;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static Surface getNativeSurface() {
        if (SDLBridge.mSurface == null) {
            return null;
        }
        return SDLBridge.mSurface.getNativeSurface();
    }

    // Input

    /**
     * This method is called by SDL using JNI.
     * @return an array which may be empty but is never null.
     */
    public static int[] inputGetInputDeviceIds(int sources) {
        int[] ids = InputDevice.getDeviceIds();
        int[] filtered = new int[ids.length];
        int used = 0;
        for (int i = 0; i < ids.length; ++i) {
            InputDevice device = InputDevice.getDevice(ids[i]);
            if ((device != null) && ((device.getSources() & sources) != 0)) {
                filtered[used++] = device.getId();
            }
        }
        return Arrays.copyOf(filtered, used);
    }

    // APK expansion files support

    /** com.android.vending.expansion.zipfile.ZipResourceFile object or null. */
    private static Object expansionFile;

    /** com.android.vending.expansion.zipfile.ZipResourceFile's getInputStream() or null. */
    private static Method expansionFileMethod;

    /**
     * This method is called by SDL using JNI.
     * @return an InputStream on success or null if no expansion file was used.
     * @throws IOException on errors. Message is set for the SDL error message.
     */
    public static InputStream openAPKExpansionInputStream(String fileName) throws IOException {
        // Get a ZipResourceFile representing a merger of both the main and patch files
        if (expansionFile == null) {
            String mainHint = nativeGetHint("SDL_ANDROID_APK_EXPANSION_MAIN_FILE_VERSION");
            if (mainHint == null) {
                return null; // no expansion use if no main version was set
            }
            String patchHint = nativeGetHint("SDL_ANDROID_APK_EXPANSION_PATCH_FILE_VERSION");
            if (patchHint == null) {
                return null; // no expansion use if no patch version was set
            }

            Integer mainVersion;
            Integer patchVersion;
            try {
                mainVersion = Integer.valueOf(mainHint);
                patchVersion = Integer.valueOf(patchHint);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                throw new IOException("No valid file versions set for APK expansion files", ex);
            }

            try {
                // To avoid direct dependency on Google APK expansion library that is
                // not a part of Android SDK we access it using reflection
                expansionFile = Class.forName("com.android.vending.expansion.zipfile.APKExpansionSupport")
                    .getMethod("getAPKExpansionZipFile", Context.class, int.class, int.class)
                    .invoke(null, SDL.getContext(), mainVersion, patchVersion);

                expansionFileMethod = expansionFile.getClass()
                    .getMethod("getInputStream", String.class);
            } catch (Exception ex) {
                ex.printStackTrace();
                expansionFile = null;
                expansionFileMethod = null;
                throw new IOException("Could not access APK expansion support library", ex);
            }
        }

        // Get an input stream for a known file inside the expansion file ZIPs
        InputStream fileStream;
        try {
            fileStream = (InputStream)expansionFileMethod.invoke(expansionFile, fileName);
        } catch (Exception ex) {
            // calling "getInputStream" failed
            ex.printStackTrace();
            throw new IOException("Could not open stream from APK expansion file", ex);
        }

        if (fileStream == null) {
            // calling "getInputStream" was successful but null was returned
            throw new IOException("Could not find path in APK expansion file");
        }

        return fileStream;
    }

    // Messagebox

    /** Result of current messagebox. Also used for blocking the calling thread. */
    protected final int[] messageboxSelection = new int[1];

    /** Id of current dialog. */
    protected int dialogs = 0;

    /**
     * This method is called by SDL using JNI.
     * Shows the messagebox from UI thread and block calling thread.
     * buttonFlags, buttonIds and buttonTexts must have same length.
     * @param buttonFlags array containing flags for every button.
     * @param buttonIds array containing id for every button.
     * @param buttonTexts array containing text for every button.
     * @param colors null for default or array of length 5 containing colors.
     * @return button id or -1.
     */
    public int messageboxShowMessageBox(
            final int flags,
            final String title,
            final String message,
            final int[] buttonFlags,
            final int[] buttonIds,
            final String[] buttonTexts,
            final int[] colors) {

        messageboxSelection[0] = -1;


        // block the calling thread

        synchronized (messageboxSelection) {
            try {
                messageboxSelection.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        // return selected value

        return messageboxSelection[0];
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean clipboardHasText() {
        return false;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static String clipboardGetText() {
        return "";
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static void clipboardSetText(String string) {
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static int createCustomCursor(int[] colors, int width, int height, int hotSpotX, int hotSpotY) {
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
        ++mLastCursorID;
        // This requires API 24, so use reflection to implement this
        try {
            Class PointerIconClass = Class.forName("android.view.PointerIcon");
            Class[] arg_types = new Class[] { Bitmap.class, float.class, float.class };
            Method create = PointerIconClass.getMethod("create", arg_types);
            mCursors.put(mLastCursorID, create.invoke(null, bitmap, hotSpotX, hotSpotY));
        } catch (Exception e) {
            return 0;
        }
        return mLastCursorID;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean setCustomCursor(int cursorID) {
        // This requires API 24, so use reflection to implement this
        try {
            Class PointerIconClass = Class.forName("android.view.PointerIcon");
            Method setPointerIcon = SDLSurface.class.getMethod("setPointerIcon", PointerIconClass);
            setPointerIcon.invoke(mSurface, mCursors.get(cursorID));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean setSystemCursor(int cursorID) {
        int cursor_type = 0; //PointerIcon.TYPE_NULL;
        switch (cursorID) {
        case SDL_SYSTEM_CURSOR_ARROW:
            cursor_type = 1000; //PointerIcon.TYPE_ARROW;
            break;
        case SDL_SYSTEM_CURSOR_IBEAM:
            cursor_type = 1008; //PointerIcon.TYPE_TEXT;
            break;
        case SDL_SYSTEM_CURSOR_WAIT:
            cursor_type = 1004; //PointerIcon.TYPE_WAIT;
            break;
        case SDL_SYSTEM_CURSOR_CROSSHAIR:
            cursor_type = 1007; //PointerIcon.TYPE_CROSSHAIR;
            break;
        case SDL_SYSTEM_CURSOR_WAITARROW:
            cursor_type = 1004; //PointerIcon.TYPE_WAIT;
            break;
        case SDL_SYSTEM_CURSOR_SIZENWSE:
            cursor_type = 1017; //PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW;
            break;
        case SDL_SYSTEM_CURSOR_SIZENESW:
            cursor_type = 1016; //PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW;
            break;
        case SDL_SYSTEM_CURSOR_SIZEWE:
            cursor_type = 1014; //PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW;
            break;
        case SDL_SYSTEM_CURSOR_SIZENS:
            cursor_type = 1015; //PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW;
            break;
        case SDL_SYSTEM_CURSOR_SIZEALL:
            cursor_type = 1020; //PointerIcon.TYPE_GRAB;
            break;
        case SDL_SYSTEM_CURSOR_NO:
            cursor_type = 1012; //PointerIcon.TYPE_NO_DROP;
            break;
        case SDL_SYSTEM_CURSOR_HAND:
            cursor_type = 1002; //PointerIcon.TYPE_HAND;
            break;
        }
        // This requires API 24, so use reflection to implement this
        try {
            Class PointerIconClass = Class.forName("android.view.PointerIcon");
            Class[] arg_types = new Class[] { Context.class, int.class };
            Method getSystemIcon = PointerIconClass.getMethod("getSystemIcon", arg_types);
            Method setPointerIcon = SDLSurface.class.getMethod("setPointerIcon", PointerIconClass);
            setPointerIcon.invoke(mSurface, getSystemIcon.invoke(null, SDL.getContext(), cursor_type));
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}

/**
    Simple runnable to start the SDL application
*/
class SDLMain implements Runnable {
    @Override
    public void run() {
        // Runs SDL_main()
        String library = SDLBridge.mSingleton.getMainSharedObject();
        String function = SDLBridge.mSingleton.getMainFunction();
        String[] arguments = SDLBridge.mSingleton.getArguments();

        Log.v("SDL", "Running main function " + function + " from library " + library);
        SDLBridge.nativeRunMain(library, function, arguments);

        Log.v("SDL", "Finished main function");

        // Native thread has finished, let's finish the Activity
        if (!SDLBridge.mExitCalledFromJava) {
            SDLBridge.handleNativeExit();
        }
    }
}


/* This is a fake invisible editor view that receives the input and defines the
 * pan&scan region
 */


