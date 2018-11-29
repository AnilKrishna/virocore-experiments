package com.ustwo.virocoreexperiments

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Pair

import com.viro.core.ARAnchor
import com.viro.core.ARImageTarget
import com.viro.core.ARNode
import com.viro.core.ARScene
import com.viro.core.AnimationTimingFunction
import com.viro.core.AnimationTransaction
import com.viro.core.AsyncObject3DListener
import com.viro.core.ClickListener
import com.viro.core.ClickState
import com.viro.core.Material
import com.viro.core.Node
import com.viro.core.Object3D
import com.viro.core.Sphere
import com.viro.core.Spotlight
import com.viro.core.Surface
import com.viro.core.Texture
import com.viro.core.Vector
import com.viro.core.ViroView
import com.viro.core.ViroViewARCore

import java.io.IOException
import java.io.InputStream
import java.util.Arrays
import java.util.EnumSet
import java.util.HashMap

/**
 * This activity demonstrates how to use an ARImageTarget. When a Tesla logo is
 * detected, a 3D Tesla car is created over the logo, along with controls that let
 * the user customize the car.
 */
class ImageRecognitionViroARActivity : Activity(), ARScene.Listener {
    private var mViroView: ViroView? = null
    private var mScene: ARScene? = null
    private var mCarModelNode: Node? = null
    private var mColorChooserGroupNode: Node? = null
    private var mTargetedNodesMap: MutableMap<String, Pair<ARImageTarget, Node>>? = null
    private val mCarColorTextures = HashMap<CARMODEL, Texture>()

    private enum class CARMODEL(val carSrc: String, val colorPickerSrc: Vector) {
        WHITE("object_car_main_Base_Color.png", Vector(231f, 231f, 231f)),
        BLUE("object_car_main_Base_Color_blue.png", Vector(19f, 42f, 143f)),
        GREY("object_car_main_Base_Color_grey.png", Vector(75f, 76f, 79f)),
        RED("object_car_main_Base_Color_red.png", Vector(168f, 0f, 0f)),
        YELLOW("object_car_main_Base_Color_yellow.png", Vector(200f, 142f, 31f))
    }

    // +---------------------------------------------------------------------------+
    //  Initialization
    // +---------------------------------------------------------------------------+

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTargetedNodesMap = HashMap()
        mViroView = ViroViewARCore(this, object : ViroViewARCore.StartupListener {
            override fun onSuccess() {
                onRenderCreate()
            }

            override fun onFailure(error: ViroViewARCore.StartupError, errorMessage: String) {
                Log.e(TAG, "Error initializing AR [$errorMessage]")
            }
        })
        setContentView(mViroView)
    }

    /*
     Create the main ARScene. We add an ARImageTarget representing the Tesla logo to the scene,
     then we create a Node (teslaNode) that consists of the Tesla car and the controls to
     customize it. This Node is not yet added to the Scene -- we will wait until the Tesla logo
     is found.
     */
    private fun onRenderCreate() {
        // Create the base ARScene
        mScene = ARScene()
        mScene!!.setListener(this)
        mViroView!!.scene = mScene

        // Create an ARImageTarget for the Tesla logo
        val teslaLogoTargetBmp = getBitmapFromAssets("logo.png")
        val teslaTarget = ARImageTarget(teslaLogoTargetBmp, ARImageTarget.Orientation.Up, 0.188f)
        mScene!!.addARImageTarget(teslaTarget)

        // Build the Tesla car Node and add it to the Scene. Set it to invisible: it will be made
        // visible when the ARImageTarget is found.
        val teslaNode = Node()
        initCarModel(teslaNode)
        initColorPickerModels(teslaNode)
        initSceneLights(teslaNode)
        teslaNode.isVisible = false
        mScene!!.rootNode.addChildNode(teslaNode)

        // Link the Node with the ARImageTarget, such that when the image target is
        // found, we'll render the Node.
        linkTargetWithNode(teslaTarget, teslaNode)
    }

    /*
     Link the given ARImageTarget with the provided Node. When the ARImageTarget is
     found in the scene (by onAnchorFound below), the Node will be made visible and
     the target's transformations will be applied to the Node, thereby rendering the
     Node over the target.
     */
    private fun linkTargetWithNode(imageToDetect: ARImageTarget, nodeToRender: Node) {
        val key = imageToDetect.id
        mTargetedNodesMap!![key] = Pair(imageToDetect, nodeToRender)
    }

    // +---------------------------------------------------------------------------+
    //  ARScene.Listener Implementation
    // +---------------------------------------------------------------------------+

    /*
     When an ARImageTarget is found, lookup the target's corresponding Node in the
     mTargetedNodesMap. Make the Node visible and apply the target's transformations
     to the Node. This makes the Node appear correctly over the target.

     (In this case, this makes the Tesla 3D model and color pickers appear directly
      over the detected Tesla logo)
     */
    override fun onAnchorFound(anchor: ARAnchor, arNode: ARNode) {
        val anchorId = anchor.anchorId
        if (!mTargetedNodesMap!!.containsKey(anchorId)) {
            return
        }

        val imageTargetNode = mTargetedNodesMap!![anchorId]?.second
        val rot = Vector(0f, anchor.rotation.y, 0f)
        imageTargetNode?.setPosition(anchor.position)
        imageTargetNode?.setRotation(rot)
        imageTargetNode?.isVisible = true
        animateCarVisible(mCarModelNode)

        // Stop the node from moving in place once found
        val imgTarget = mTargetedNodesMap!![anchorId]?.first
        mScene!!.removeARImageTarget(imgTarget)
        mTargetedNodesMap!!.remove(anchorId)
    }

    override fun onAnchorRemoved(anchor: ARAnchor, arNode: ARNode) {
        val anchorId = anchor.anchorId
        if (!mTargetedNodesMap!!.containsKey(anchorId)) {
            return
        }

        val imageTargetNode = mTargetedNodesMap!![anchorId]?.second
        imageTargetNode?.isVisible = false
    }

    override fun onAnchorUpdated(anchor: ARAnchor, arNode: ARNode) {
        // No-op
    }

    // +---------------------------------------------------------------------------+
    //  Scene Building Methods
    // +---------------------------------------------------------------------------+

    /*
     Init, loads the the Tesla Object3D, and attaches it to the passed in groupNode.
     */
    private fun initCarModel(groupNode: Node) {
        // Creation of ObjectJni to the right
        val fbxCarNode = Object3D()
        fbxCarNode.setScale(Vector(0.00f, 0.00f, 0.00f))
        fbxCarNode.loadModel(mViroView!!.viroContext, Uri.parse("file:///android_asset/object_car.obj"), Object3D.Type.OBJ, object : AsyncObject3DListener {
            override fun onObject3DLoaded(`object`: Object3D, type: Object3D.Type) {
                preloadCarColorTextures(`object`)
            }

            override fun onObject3DFailed(error: String) {
                Log.e(TAG, "Car Model Failed to load.")
            }
        })

        groupNode.addChildNode(fbxCarNode)
        mCarModelNode = fbxCarNode

        // Set click listeners.
        mCarModelNode!!.clickListener = object : ClickListener {
            override fun onClick(i: Int, node: Node, vector: Vector) {
                // Animate toggling the groupColor picker.
                val setVisibility = !mColorChooserGroupNode!!.isVisible
                mColorChooserGroupNode!!.isVisible = setVisibility
                animateColorPickerVisible(setVisibility, mColorChooserGroupNode)
            }

            override fun onClickState(i: Int, node: Node, clickState: ClickState, vector: Vector) {
                // No-op.
            }
        }
    }

    /*
     Constructs a group of sphere color pickers and attaches them to the passed in group Node.
     These sphere pickers when click will change the diffuse texture of our tesla model.
     */
    private fun initColorPickerModels(groupNode: Node) {
        mColorChooserGroupNode = Node()
        mColorChooserGroupNode!!.transformBehaviors = EnumSet.of(Node.TransformBehavior.BILLBOARD_Y)
        mColorChooserGroupNode!!.setPosition(Vector(0.0, 0.25, 0.0))
        val pickerPositions = floatArrayOf(-.2f, -.1f, 0f, .1f, .2f)

        // Loop through car color model colors
        for ((i, model) in CARMODEL.values().withIndex()) {
            // Create our sphere picker geometry
            val colorSphereNode = Node()
            val posX = pickerPositions[i]
            colorSphereNode.setPosition(Vector(posX, 0f, 0f))
            val colorSphere = Sphere(0.03f)

            // Create sphere picker color that correlates to the car model's color
            val material = Material()
            val c = model.colorPickerSrc
            material.diffuseColor = Color.rgb(c.x.toInt(), c.y.toInt(), c.z.toInt())
            material.lightingModel = Material.LightingModel.PHYSICALLY_BASED

            // Finally, set the sphere's properties
            colorSphere.materials = Arrays.asList(material)
            colorSphereNode.geometry = colorSphere
            colorSphereNode.shadowCastingBitMask = 0
            mColorChooserGroupNode!!.addChildNode(colorSphereNode)

            // Set clickListener on spheres
            colorSphereNode.clickListener = object : ClickListener {
                override fun onClick(i: Int, node: Node, vector: Vector) {
                    //mCarModelNode.getGeometry().setMaterials();
                    val texture = mCarColorTextures[model]
                    val mat = mCarModelNode!!.geometry.materials[0]
                    mat.diffuseTexture = texture
                    animateColorPickerClicked(colorSphereNode)
                }

                override fun onClickState(i: Int, node: Node, clickState: ClickState, vector: Vector) {
                    // No-op.
                }
            }
        }

        mColorChooserGroupNode!!.setScale(Vector(0f, 0f, 0f))
        mColorChooserGroupNode!!.isVisible = false
        groupNode.addChildNode(mColorChooserGroupNode!!)
    }

    private fun initSceneLights(groupNode: Node) {
        val rootLightNode = Node()

        // Construct a spot light for shadows
        val spotLight = Spotlight()
        spotLight.position = Vector(0f, 5f, 0f)
        spotLight.color = Color.parseColor("#FFFFFF").toLong()
        spotLight.direction = Vector(0f, -1f, 0f)
        spotLight.intensity = 300f
        spotLight.innerAngle = 5f
        spotLight.outerAngle = 25f
        spotLight.shadowMapSize = 2048
        spotLight.shadowNearZ = 2f
        spotLight.shadowFarZ = 7f
        spotLight.shadowOpacity = .7f
        spotLight.castsShadow = true
        rootLightNode.addLight(spotLight)

        // Add our shadow planes.
        val material = Material()
        material.shadowMode = Material.ShadowMode.TRANSPARENT
        val surface = Surface(2f, 2f)
        surface.materials = Arrays.asList(material)
        val surfaceShadowNode = Node()
        surfaceShadowNode.setRotation(Vector(Math.toRadians(-90.0), 0.0, 0.0))
        surfaceShadowNode.geometry = surface
        surfaceShadowNode.setPosition(Vector(0.0, 0.0, -0.7))
        rootLightNode.addChildNode(surfaceShadowNode)
        groupNode.addChildNode(rootLightNode)

        val environment = Texture.loadRadianceHDRTexture(Uri.parse("file:///android_asset/garage_1k.hdr"))
        mScene!!.lightingEnvironment = environment
    }

    private fun preloadCarColorTextures(node: Node): Material {
        val metallicTexture = Texture(getBitmapFromAssets("object_car_main_Metallic.png")!!,
                Texture.Format.RGBA8, true, true)
        val roughnessTexture = Texture(getBitmapFromAssets("object_car_main_Roughness.png")!!,
                Texture.Format.RGBA8, true, true)

        val material = Material()
        material.metalnessMap = metallicTexture
        material.roughnessMap = roughnessTexture
        material.lightingModel = Material.LightingModel.PHYSICALLY_BASED
        node.geometry.materials = Arrays.asList(material)

        // Loop through color.
        for (model in CARMODEL.values()) {
            val carBitmap = getBitmapFromAssets(model.carSrc)
            val carTexture = Texture(carBitmap!!, Texture.Format.RGBA8, true, true)
            mCarColorTextures[model] = carTexture

            // Preload our textures into the model
            material.diffuseTexture = carTexture
        }

        material.diffuseTexture = mCarColorTextures[CARMODEL.WHITE]
        return material
    }

    // +---------------------------------------------------------------------------+
    //  Image Loading
    // +---------------------------------------------------------------------------+

    private fun getBitmapFromAssets(assetName: String): Bitmap? {
        val istr: InputStream
        var bitmap: Bitmap?
        try {
            istr = assets.open(assetName)
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            throw IllegalArgumentException("Loading bitmap failed!", e)
        }

        return bitmap
    }

    // +---------------------------------------------------------------------------+
    //  Animation Utilities
    // +---------------------------------------------------------------------------+

    private fun animateScale(node: Node, duration: Long, targetScale: Vector,
                             fcn: AnimationTimingFunction, runnable: Runnable?) {
        AnimationTransaction.begin()
        AnimationTransaction.setAnimationDuration(duration)
        AnimationTransaction.setTimingFunction(fcn)
        node.setScale(targetScale)
        if (runnable != null) {
            AnimationTransaction.setListener { runnable.run() }
        }
        AnimationTransaction.commit()
    }

    private fun animateColorPickerVisible(isVisible: Boolean, groupNode: Node?) {
        if (isVisible) {
            animateScale(groupNode!!, 500, Vector(1f, 1f, 1f), AnimationTimingFunction.Bounce, null)
        } else {
            animateScale(groupNode!!, 200, Vector(0f, 0f, 0f), AnimationTimingFunction.Bounce, null)
        }
    }

    private fun animateCarVisible(car: Node?) {
        animateScale(car!!, 500, Vector(0.09f, 0.09f, 0.09f), AnimationTimingFunction.EaseInEaseOut, null)
    }

    private fun animateColorPickerClicked(picker: Node) {
        animateScale(picker, 50, Vector(0.8f, 0.8f, 0.8f), AnimationTimingFunction.EaseInEaseOut, Runnable { animateScale(picker, 50, Vector(1f, 1f, 1f), AnimationTimingFunction.EaseInEaseOut, null) })
    }

    // +---------------------------------------------------------------------------+
    //  Lifecycle
    // +---------------------------------------------------------------------------+

    override fun onStart() {
        super.onStart()
        mViroView!!.onActivityStarted(this)
    }

    override fun onResume() {
        super.onResume()
        mViroView!!.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        mViroView!!.onActivityPaused(this)
    }

    override fun onStop() {
        super.onStop()
        mViroView!!.onActivityStopped(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mViroView!!.onActivityDestroyed(this)
    }

    override fun onTrackingInitialized() {
        // No-op
    }

    override fun onTrackingUpdated(state: ARScene.TrackingState, reason: ARScene.TrackingStateReason) {
        // No-op
    }

    override fun onAmbientLightUpdate(value: Float, v: Vector) {
        // No-op
    }

    companion object {
        private val TAG = ImageRecognitionViroARActivity::class.java.simpleName
    }
}