
package com.ustwo.virocoreexperiments

import android.app.Activity
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.viro.core.*

import java.io.IOException
import java.io.InputStream
import java.util.Arrays

/**
 * Activity that initializes Viro and ARCore. This activity demonstrates how to create an
 * AR portal. Portals are an AR effect where a 'window' or 'door' is displayed that users
 * can use to peer into a virtual world.
 */
class ARPortalActivity : Activity() {
    private var mViroView: ViroView? = null
    private var mScene: ARScene? = null
    private var mAssetManager: AssetManager? = null
    private var mShipDoorModel: Object3D? = null


    // +---------------------------------------------------------------------------+
    //  Initialization
    // +---------------------------------------------------------------------------+

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViroView = ViroViewARCore(this, object : ViroViewARCore.StartupListener {
            override fun onSuccess() {
                // Override this function to start building your scene here
                onPortalCreate()
            }

            override fun onFailure(error: ViroViewARCore.StartupError, errorMessage: String) {
                // Fail as you wish!
            }
        })
        setContentView(mViroView)
    }

    private fun onPortalCreate() {
        // Create the base ARScene
        mScene = ARScene()


        // Add a Light so the ship door portal entrance will be visible
        val light = OmniLight()
        light.color = Color.WHITE.toLong()
        light.position = Vector(0F,1F,-4F)
        mScene!!.rootNode.addLight(light)

        // Load a model representing the ship door
        mShipDoorModel = Object3D()
        mShipDoorModel!!.loadModel(mViroView?.viroContext, Uri.parse("file:///android_asset/arportal/portal_wood_frame.vrx"), Object3D.Type.FBX,object : AsyncObject3DListener {
            override fun onObject3DLoaded(`object`: Object3D, type: Object3D.Type) {
                Log.e(ARPortalActivity.TAG, "Ship Model Successfully loaded.")
            }

            override fun onObject3DFailed(error: String) {
                Log.e(ARPortalActivity.TAG, "Ship Model Failed to load.")
            }
        })

        // Create a Portal out of the ship door
        val portal = Portal()
        portal.addChildNode(mShipDoorModel)
        portal.setScale(Vector(0.5F,0.5F,0.5F))

        // Create a PortalScene that uses the Portal as an entrance.
        val portalScene = PortalScene()
        portalScene.setPosition(Vector(0.0F,0.0F,-5.0F))
        portalScene.isPassable = true
        portalScene.portalEntrance = portal

        // Add a 'beach' background for the Portal scene
        val beachBackground = getBitmapFromAssets("sydney.jpg")
        val beachTexture = Texture(beachBackground,Texture.Format.RGBA8,true,false)
/*        val beachVideoTexture = VideoTexture(mViroView?.viroContext, Uri.parse("file:///android_asset/crystal_shower_falls.mp4"))
        beachVideoTexture.play()
        beachVideoTexture.loop = true*/
        portalScene.setBackgroundTexture(beachTexture)

        mScene!!.rootNode.addChildNode(portalScene)
        mViroView?.scene = mScene
    }


    // +---------------------------------------------------------------------------+
    //  Lifecycle
    // +---------------------------------------------------------------------------+

    override fun onStart() {
        super.onStart()
        mViroView?.onActivityStarted(this)
    }

    override fun onResume() {
        super.onResume()
        mViroView?.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        mViroView?.onActivityPaused(this)
    }

    override fun onStop() {
        super.onStop()
        mViroView?.onActivityStopped(this)
    }

    // +---------------------------------------------------------------------------+
    //  Utility Functions
    // +---------------------------------------------------------------------------+

    private fun getBitmapFromAssets(assetName: String): Bitmap? {
        if (mAssetManager == null) {
            mAssetManager = resources.assets
        }

        val imageStream: InputStream
        try {
            imageStream = mAssetManager!!.open(assetName)
        } catch (exception: IOException) {
            Log.w("Viro", "Unable to find image [" + assetName + "] in assets! Error: "
                    + exception.message)
            return null
        }

        return BitmapFactory.decodeStream(imageStream)
    }

    companion object {
        private val TAG = ARPortalActivity::class.java.simpleName
    }
}