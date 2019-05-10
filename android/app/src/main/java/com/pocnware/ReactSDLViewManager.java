package com.pocnware;

import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;



public class ReactSDLViewManager  extends SimpleViewManager<View> {

  public static final String REACT_CLASS = "SDLView";

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public View createViewInstance(ThemedReactContext context) {

    RelativeLayout layout = new RelativeLayout(context);
    Button button = new Button(context);
    button.setText("This is dummy view!!! not a button :)");
    layout.addView(button);

    return layout;
  }

}
