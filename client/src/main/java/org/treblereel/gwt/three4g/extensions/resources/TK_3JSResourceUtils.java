package org.treblereel.gwt.three4g.extensions.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface TK_3JSResourceUtils extends ClientBundle {

    TK_3JSResourceUtils IMPL = GWT.create(TK_3JSResourceUtils.class);

    /*public static void addLib (TextResource libText) {
        ScriptInjector.fromString(libText.getText()).setWindow(ScriptInjector.TOP_WINDOW).inject();
    }*/

    @Source("js/loaders/DRACOLoader.js")
    TextResource getDRACOLoader();

    @Source("js/loaders/GLTFLoader.js")
    TextResource getGLTFLoader();

    @Source("js/controls/OrbitControls.js")
    TextResource getOrbitControls();

    @Source("js/controls/TrackballControls.js")
    TextResource getTrackballControls();
}
