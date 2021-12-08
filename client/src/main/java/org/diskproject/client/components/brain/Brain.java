package org.diskproject.client.components.brain;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.paper.widget.PaperSpinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.Config;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.client.rest.StaticREST;
import org.treblereel.gwt.three4g.THREE;
import org.treblereel.gwt.three4g.cameras.PerspectiveCamera;
import org.treblereel.gwt.three4g.core.BufferGeometry;
import org.treblereel.gwt.three4g.core.Face3;
import org.treblereel.gwt.three4g.core.Geometry;
import org.treblereel.gwt.three4g.core.Raycaster;
import org.treblereel.gwt.three4g.core.extra.Intersect;
import org.treblereel.gwt.three4g.extensions.controls.OrbitControls;
import org.treblereel.gwt.three4g.extensions.controls.TrackballControls;
import org.treblereel.gwt.three4g.extensions.resources.TK_3JSResourceUtils;
import org.treblereel.gwt.three4g.lights.DirectionalLight;
import org.treblereel.gwt.three4g.materials.MeshLambertMaterial;
import org.treblereel.gwt.three4g.materials.parameters.MeshLambertMaterialParameters;
import org.treblereel.gwt.three4g.math.Vector2;
import org.treblereel.gwt.three4g.objects.Mesh;
import org.treblereel.gwt.three4g.renderers.WebGLRenderer;
import org.treblereel.gwt.three4g.renderers.parameters.WebGLRendererParameters;
import org.treblereel.gwt.three4g.scenes.Scene;

import elemental2.core.JsArray;
import elemental2.dom.DOMRect;
import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;

public class Brain extends Composite {
  interface Binder extends UiBinder<Widget, Brain> {};
  private static Binder uiBinder = GWT.create(Binder.class);
  
  private static boolean threeLoaded = false;
  private static Brain singleton = null;
  
  @UiField HTMLCanvasElement canvas;
  @UiField HTMLDivElement container;
  @UiField PaperSpinner loading;

  private PerspectiveCamera camera;
  private WebGLRenderer renderer;
  private Scene scene;
  
  //Two cameras:
  private TrackballControls controls;
  private OrbitControls orbitControls;
  private boolean useOrbitControls = true;
  //TODO: cur_picked = null;

  private String manifest_url; //
  private String data_url;
  
  private Map<String, Mesh> meshes;
  private Map<String, MeshProperties> meshProperties;
  private Map<String, String> meshIdByName;
  
  
  public static Brain get () {
      if (Brain.singleton == null) {
          Brain.singleton = new Brain();
      }
      return Brain.singleton;
  }

  public Brain() {
	  // See https://github.com/treblereel/three4g/issues/155
	  if (!Brain.threeLoaded) {
		  ScriptInjector.fromString(TK_3JSResourceUtils.IMPL.getTrackballControls().getText()).setWindow(ScriptInjector.TOP_WINDOW).inject();
		  ScriptInjector.fromString(TK_3JSResourceUtils.IMPL.getOrbitControls().getText()).setWindow(ScriptInjector.TOP_WINDOW).inject();
		  Brain.threeLoaded = false;
	  }

	  initWidget(uiBinder.createAndBindUi(this)); 
	  loading.setVisible(false);

	  initialize();
	  /*if (!Brain.initialized && !Brain.initializing) {
	      Brain.initializing = true;
	      initialize();
	  }*/
  }

  public void initialize () {
	  GWT.log("initializing brain...");
	  float w = (float) canvas.width;
	  float h = (float) canvas.height;
	  
	  // Set manifest_url
	  manifest_url = Config.getServerURL() + "/public/files.json";

	  //Some important variables
	  this.meshes = new HashMap<String, Mesh>();
	  this.meshProperties = new HashMap<String, MeshProperties>();
	  this.meshIdByName = new HashMap<String, String>();

	  // The Renderer
	  WebGLRendererParameters renderParams = new WebGLRendererParameters();
	  renderParams.canvas = canvas;
	  renderParams.antialias = true;
	  renderParams.alpha = true;
	  this.renderer = new WebGLRenderer( renderParams );
	  this.renderer.setPixelRatio(DomGlobal.window.devicePixelRatio);
	  this.renderer.setSize(w, h);

	  // The Camera
	  // Params: x,y,z starting position
	  this.camera = new PerspectiveCamera((float) 50, (w/h), (float) 0.1, (float) 1e10);
	  this.camera.position.z = 200;
	  
	  // The Controls
	  // Params: None. Just add the camera to controls
	  if (useOrbitControls) {
		  this.orbitControls = this.addOrbitControls(this.camera, container);
	  } else {
		  this.controls = this.addControls(this.camera, container);
	  }
	  

	  //nativeLog(this.controls);

	  // The Scene
	  // Params: None. Just add the camera to the scene
	  this.scene = new Scene();
	  this.scene.add( this.camera );

	  // The Lights!
	  // Params: None for now... add to camera
	  this.addLights(this.camera);

	  // The Mesh
	  // Params: None for now... add to scene
	  this.loadBrain();

	  /*Window.addResizeHandler(new ResizeHandler() { 	TODO: change size when window resize
		  @Override
		  public void onResize(ResizeEvent event) { onWindowResize(); }
	  });*/

	  canvas.addEventListener("click", new EventListener() {
		  @Override
		  public void handleEvent(Event evt) { onClick(evt); }
	  });
	  
	  //Different ways to start the animation:
	  Timer timer = new Timer() {
	      @Override
	      public void run() {
	          animate();
	          this.schedule(1000/12);
	      }
	  };
	  timer.schedule(1000/12);

	  //DomGlobal.requestAnimationFrame(this::eventLoop);
  }

  private void animate () {
	  if (useOrbitControls) {
		  this.orbitControls.update();
	  } else {
		  this.controls.update();
	  }
	  this.renderer.render( this.scene, this.camera );
  }
  
  void eventLoop(double timestamp) {
	  animate();
	  DomGlobal.requestAnimationFrame(this::eventLoop);
  }
  
  private native HTMLElement nativeGetElement (HTMLElement javaElement) /*-{
  	//This is a hack to obtain the javascript element;
  	var elem = null;
  	var keys = Object.keys(javaElement);
  	for (var i = 0; i < keys.length; i++) {
  		var key = keys[i];
  		if (key.length > 5 && key.substring(0, 6) == '__impl') {
  			elem = javaElement[key];
  			break;
  		}
  	}
  	console.log(elem);
  	return elem;
  }-*/;

  private TrackballControls addControls (PerspectiveCamera camera, HTMLElement container) {
	  controls = new TrackballControls(camera, container);
	  
      controls.enabled = true;
      controls.minDistance = 100;
      controls.maxDistance = 500;

	  controls.rotateSpeed = 5f;
	  controls.zoomSpeed = 5;
	  controls.panSpeed = 2;
	  
	  //controls.noZoom = false;
	  //controls.noPan = false;

	  //controls.staticMoving = true;
	  controls.dynamicDampingFactor = 0.3f;
	  return controls;
  }
  
  private OrbitControls addOrbitControls (PerspectiveCamera camera, HTMLElement container) {
	  OrbitControls orbitControls = new OrbitControls(camera, container);

      //controls.addEventListener( "change", render ); // call this only in static scenes (i.e., if there is no animation loop)
      orbitControls.enableDamping = true; // an animation loop is required when either damping or auto-rotation are enabled
      orbitControls.dampingFactor = 0.25f;
      orbitControls.screenSpacePanning = false;
      orbitControls.minDistance = 100;
      orbitControls.maxDistance = 500;
      orbitControls.maxPolarAngle = (float) Math.PI;
      return orbitControls;
  }

  private void addLights (PerspectiveCamera camera) {
	  DirectionalLight dirLight = new DirectionalLight( 0xffffff );
	  dirLight.position.set( 200, 200, 1000 ).normalize();

	  camera.add( dirLight );
	  camera.add( dirLight.target );
  }

  private void onWindowResize () {
	  DOMRect sz = this.canvas.getBoundingClientRect();
	  GWT.log("On resize");
	  nativeLog(sz);
	  this.camera.aspect = (float) (sz.width / sz.height);
	  this.camera.updateProjectionMatrix();

	  this.renderer.setSize( sz.width, sz.height );
	  this.renderer.setClearColor(0xffffff, 1);

	  //this.controls.handleResize();
  }

  private void loadBrain () {
	  if (this.manifest_url == null || this.manifest_url.equals(""))
		  return;
	  /*if (this.label_mapper === null) TODO: I dont understand this
		  return;*/

	  StaticREST.getJSObject(manifest_url, new Callback<JavaScriptObject, Throwable>() {
		  @Override
		  public void onSuccess(JavaScriptObject result) {
			  // TODO Auto-generated method stub
			  reset_mesh_props(result, true);
		  }
		  @Override
		  public void onFailure(Throwable reason) {
			  // TODO Auto-generated method stub
		  }
	  });
  }

  private void clearBrain (String[] keeper_roi_keys) {
	  GWT.log("clearing brain but keeping " + keeper_roi_keys.length + " rois");
	  if (keeper_roi_keys != null && keeper_roi_keys.length > 0) {
		  for (String key: meshes.keySet()) {
			  if (Arrays.stream(keeper_roi_keys).anyMatch(x -> x == key)) {
				  continue;
			  }
			  removeMesh(key);
		  }
	  }
	  //for (String key: keeper_roi_keys)
		//  GWT.log("+ " + key);
  }
  
  private float[] toFloatArray (String[] elements) {
	  float[] array = new float[3];
	  for (int i = 0; i < 3; i++) {
		  array[i] = Float.parseFloat(elements[i]);
	  }
	  return array;
  }

  private native String nativeGetPropertyValue (JavaScriptObject data, String prop_name, String key, String default_val) /*-{
  	var val = (prop_name in data) ? data[prop_name][key] : default_val;
  	//TODO: theres a static value_key here, not sure for what is used
  	return val;
  }-*/;

  private native String[] nativeGetStringArray (JavaScriptObject data, String prop_name, String key, String[] default_val) /*-{
  	var val = (prop_name in data) ? data[prop_name][key] : default_val;
  	//TODO: theres a static value_key here, not sure for what is used
  	return val;
  }-*/;
  
  private native void nativeLog (Object obj) /*-{
  	console.log(obj);
  }-*/;

  private void reset_mesh_props (JavaScriptObject data, boolean paint_colors) {
	  String[] keys = nativeGetKeySet(data);
	  String key0 = keys[0];
	  String[] roi_keys = nativeGetKeySet(nativeGet(data, key0));
	  clearBrain(roi_keys);
	  String base_url = manifest_url.substring(0, manifest_url.lastIndexOf('/'));
	  GWT.log("base_url: " + base_url);
	  
	  String[] baseColors = {"1", "1", "1"};
	  
	  for (String key: roi_keys) {
		  String mesh_url = nativeGetPropertyValue(data, "filename", key, null);
		  
		  MeshProperties meshProps = new MeshProperties(
			  nativeGetPropertyValue(data, "name", key, key),
			  toFloatArray(paint_colors ? nativeGetStringArray(data, "colors", key, null) : baseColors),
			  nativeGetPropertyValue(data, "values", key, null),
			  key);
		  
		  // Select the needed value
		  /*if (mesh.value && mesh.value.length ) //Casting something array -> elem ??
			  mesh.value = mesh.value[Object.keys(mesh_props.value)[0]];*/
		  //LOAD the 3D file!
		  
		  if (mesh_url != null && !mesh_url.equals("")) {  // Load remote mesh
			  if (mesh_url.charAt(0) != '/') // relative path is relative to manifest
				  mesh_url = base_url + "/models/" + mesh_url;
			  loadMesh(mesh_url, meshProps);
		  } else if (meshes.containsKey(meshProps.roi_key)) {  // Set existing mesh properties
			  //copy_mesh_props(mesh_props, _this.meshes[mesh_props.roi_key]);
		  } else {  // Didn't load mesh, none existing...
			  GWT.log("Mesh URL not specified for" + meshProps.roi_key+", no existing mesh, skipping...");
		  }
		  
	  }
  }

  private native JavaScriptObject nativeGet (JavaScriptObject data, String key) /*-{
  	return data[key];
  }-*/;

  private native String[] nativeGetKeySet (JavaScriptObject data) /*-{
  	return Object.keys(data);
  }-*/;

  private void removeMesh (String roi_key) {
	  //HACK?
	  meshes.remove(roi_key);
	  meshProperties.remove(roi_key);
  }

  @SuppressWarnings("rawtypes")
  private native BufferGeometry<BufferGeometry> VTKLoader (String buffer) /*-{
  	return new THREE.VTKLoader().parse(buffer);
  }-*/;

  private void loadMesh (String url, MeshProperties mesh_props) {
	  boolean found = meshProperties.containsKey(mesh_props.roi_key);
	  if (found && url.equals(meshProperties.get(mesh_props.roi_key).filename)) {
		  Mesh mesh = meshes.get(mesh_props.roi_key);
		  copy_mesh_props(mesh_props, mesh);
	  } else {
		  if (found) {// Unreusable mesh; remove it
			  removeMesh(mesh_props.roi_key);
		  }
		  StaticREST.getAsString(url, new Callback<String, Throwable>() {
			  @Override
			  public void onSuccess(String result) {
				  @SuppressWarnings("rawtypes")
				  BufferGeometry bufferGeometry = VTKParser.parse(result);

				  Geometry geometry = new Geometry().fromBufferGeometry(bufferGeometry);
				  geometry.computeFaceNormals();
				  geometry.computeVertexNormals();
				  //geometry.__dirtyColors = true;
				  
				  MeshLambertMaterialParameters materialParams = new MeshLambertMaterialParameters();
				  materialParams.vertexColors = THREE.FaceColors;
				  MeshLambertMaterial material = new MeshLambertMaterial(materialParams);
				  
				  Mesh mesh = new Mesh(geometry, material);
				  copy_mesh_props(mesh_props, mesh);

				  mesh_props.filename = url;
				  //mesh.dynamic = true;
				  
				  mesh.material.transparent = true;
				  mesh.material.opacity = 1;
				  mesh.rotation.y = (float) (Math.PI * 1.01);
				  mesh.rotation.x = (float) (Math.PI * 0.5);
				  mesh.rotation.z = (float) (Math.PI * 1.5 * (url.indexOf("rh_") == -1 ? 1 : -1));
				  
				  if (mesh_props.name != null && !mesh_props.name.equals("")) {
					  mesh.name = mesh_props.name;
				  } else {
					  String tmp[] = url.split("_");
					  mesh.name = tmp[tmp.length-1].split(".vtk")[0];
				  }
				  
				  scene.add(mesh);
				  meshes.put(mesh_props.roi_key, mesh);
				  meshProperties.put(mesh_props.roi_key, mesh_props);
			  }
			  @Override
			  public void onFailure(Throwable reason) {
				  // TODO Auto-generated method stub

			  }
		  });
	  }
  }
  
  private void copy_mesh_props (MeshProperties meshProp, Mesh mesh) {
	  set_mesh_color(mesh, meshProp.color);
	  //TODO hack in mesh properties into mesh.
  }
  
  private void set_mesh_color (Mesh mesh, float[] color) {
	  Geometry geometry = (Geometry) mesh.geometry;
	  for (int i = geometry.faces.length -1; i >= 0; i--) {
		  Face3 face = geometry.faces.getAt(i);
		  if (color != null) {
			  face.color.setHex((int) (Math.random() * 0xffffff));
			  face.color.setRGB(color[0], color[1], color[2]);
		  } else {
			  JsArray<Face3> before_faces = geometry.faces.slice(0,i);
			  JsArray<Face3> after_faces = geometry.faces.slice(i+1, geometry.faces.length);
			  Face3[] after = new Face3[after_faces.length];
			  for (int j = 0; i < after_faces.length; i++) {
				  after[j] = after_faces.getAt(j);
			  }
			  geometry.faces = before_faces.concat(after);
		  }
	  }
	  geometry.colorsNeedUpdate = true;
  }

  private native boolean shiftKeyPressed (Event e) /*-{
  	return !!e.shiftKey;
  }-*/;
  
  private void onClick (Event e) {
	  if (!shiftKeyPressed(e))
		  return;

	  //decreaseOpacityAll();
	  Mesh clickedmesh = selectMeshByMouse(e);
	  if (clickedmesh != null) {
		  this.increaseOpacity(clickedmesh);
		  this.onSelect(clickedmesh);
	  }
  }

  private native float getEventClientX (Event e) /*-{
	  return e.clientX;
  }-*/;
  
  private native float getEventClientY (Event e) /*-{
	  return e.clientY;
  }-*/;

  private Mesh selectMeshByMouse (Event e) {
	  Raycaster raycaster = new Raycaster();
	  Vector2 mouse = new Vector2();
	  DOMRect cpos = renderer.domElement.getBoundingClientRect();
	  
	  mouse.x = (float) (( 2 * (getEventClientX(e) - cpos.left) / (cpos.width) ) - 1);
	  mouse.y = (float) (( 2 * (cpos.top - getEventClientY(e)) / (cpos.height) ) + 1);
	  
	  raycaster.setFromCamera(mouse, camera);
	  Intersect[] intersects = raycaster.intersectObjects(scene.children);
	  
	  if (intersects.length > 0) {
		  String key = intersects[0].object.name;
		  GWT.log("Clicked: " + key);
		  //TODO:
		  return intersects[0].object;
		  //return this.meshes.get(key);
	  }
	  return null;
  }
  
  private Mesh selectMeshByName (String meshName) {
	  for (String key: meshes.keySet()) {
		  Mesh cur = meshes.get(key);
		  if (cur.name.equals(meshName)) {
			  return cur;
		  }
	  }
	  return null;
  }
   
  private void decreaseOpacity (Mesh mesh) {
	  mesh.material.opacity = (float) 0.05;
  }

  private void decreaseOpacityAll () {
	  for (String key: meshes.keySet()) {
		  decreaseOpacity(meshes.get(key));
	  }
  }

  private void noOpacityAll () {
	  for (String key: meshes.keySet()) {
		  Mesh mesh = meshes.get(key);
		  mesh.material.opacity = 0;
	  }
  }

  private void increaseOpacity (Mesh mesh) {
	  mesh.material.opacity = (float) 1;
  }

  private void increaseOpacityAll () {
	  for (String key: meshes.keySet()) {
		  increaseOpacity(meshes.get(key));
	  }
  }
  
  private void clearColors () {
	  float[] color = {0.1f, 0.1f, 0.1f};
	  for (String key: meshes.keySet()) {
		  Mesh mesh = meshes.get(key);
		  set_mesh_color(mesh, color);
	  }
  }
  
  private void onSelect (Mesh mesh) {
	  /* TODO: check onSelect function.
	  if (checked.length ==0) brain.decreaseOpacityAll();
	  console.log("Mouse selected mesh: "+mesh.name);
	  var checkbox = document.getElementsByName(mesh.name)[0];
	  if (checkbox.checked) {
	      removeMesh(mesh, checkbox.id);
	      checkbox.checked=false;
	  }
	  else {
	      addMesh(mesh, checkbox.id);
	      checkbox.checked=true;
	  }
	  console.log("checked array:"+ show(checked));*/
  }

  private void readConfig (List<BrainConfigLine> config) {
	  decreaseOpacityAll();
	  clearColors();
	  for (BrainConfigLine item: config) {
		  Mesh mesh = selectMeshByName(item.name);
		  if (mesh != null) {
			  if (item.pval > 0) {
				  mesh.material.opacity = .25f + item.pval * .75f;
				  
				  float[] color = {1f, 0f, 0f};
				  set_mesh_color(mesh, color);
			  } else if (item.color != null && item.color.length == 3) {
				  mesh.material.opacity = 1f;
				  set_mesh_color(mesh, item.color);
			  }
		  }
	  }
  }

  public void loadBrainConfigurationFromJSObject (JavaScriptObject obj) {
	  int len = nativeLen(obj);
	  
	  if (len == 0) return;
	  List<BrainConfigLine> config = new ArrayList<BrainConfigLine>();

	  
	  for (int i = 0; i < len; i++) {
		  String name = getJSConfigName(obj, i);
		  float pval = getJSConfigPVal(obj, i);
		  //String color = getJSConfigColor(obj, i);
		  config.add( new BrainConfigLine(name, pval, null) );
		  GWT.log("name: " + name + ", pval: " + pval);
	  }
	  
	  readConfig(config);
  }
  
  public void loadConfigFile (String brainConfigURL) {
        loading.setVisible(true);
        String URL = brainConfigURL.replaceAll("^.*#", "");
		DiskREST.getDataFromWingsAsJS(URL, new Callback<JavaScriptObject, Throwable>() {
			@Override
			public void onSuccess(JavaScriptObject result) {
				loadBrainConfigurationFromJSObject(result);
				loading.setVisible(false);
			}
			@Override
			public void onFailure(Throwable reason) {
				// TODO Auto-generated method stub
				loading.setVisible(false);
			}
		});
      
  }

  private native int nativeLen (JavaScriptObject obj) /*-{
  	return obj ? obj.length : 0;
  }-*/;
  
  private native String getJSConfigName (JavaScriptObject obj, int i) /*-{
  	return obj[i].name;
  }-*/;

  private native float getJSConfigPVal (JavaScriptObject obj, int i) /*-{
  	return obj[i].pval;
  }-*/;
}