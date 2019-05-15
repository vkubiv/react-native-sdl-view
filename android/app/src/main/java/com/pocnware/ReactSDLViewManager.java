package com.pocnware;

import android.view.View;
import android.widget.RelativeLayout;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import org.libsdl.app.SDLBridge;
import org.libsdl.app.SDLSurface;

public class ReactSDLViewManager  extends SimpleViewManager<View> {

  public static final String REACT_CLASS = "SDLView";

  private View mComponentView;

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public View createViewInstance(ThemedReactContext context) {

    SDLBridge bridge = SDLBridge.getInstance();

    if (mComponentView == null) {
      SDLSurface view = bridge.create(context);

      RelativeLayout layout = new RelativeLayout(context);
      layout.addView(view);

      mComponentView = layout;
    }
    return mComponentView;
  }

  @ReactProp(name = "textureSrc", defaultBoolean = false)
  public void showTexture(View view, String path) {
    SDLBridge.setTexturePath(path);
  }

}
