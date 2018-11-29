/*
 * Copyright (c) 2017-present, Viro, Inc.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ustwo.virocoreexperiments

import android.app.Activity
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log

import com.viro.core.ARAnchor
import com.viro.core.ARImageTarget
import com.viro.core.ARNode
import com.viro.core.ARScene
import com.viro.core.Animation
import com.viro.core.AsyncObject3DListener
import com.viro.core.Material
import com.viro.core.Node
import com.viro.core.Object3D
import com.viro.core.OmniLight
import com.viro.core.Spotlight
import com.viro.core.Surface
import com.viro.core.Texture
import com.viro.core.Vector
import com.viro.core.ViroView
import com.viro.core.ViroViewARCore

import java.io.IOException
import java.io.InputStream
import java.util.Arrays

/**
 * Activity that initializes Viro and ARCore. This activity demonstrates how to use an
 * ARImageTarget: in this case, when a Black Panther poster is recognized, a Black Panther
 * model will jump out of the poster.
 */
class PosterAnimationARActivity : Activity() {
    private var mViroView: ViroView? = null
    private var mScene: ARScene? = null
    private var mImageTarget: ARImageTarget? = null
    private var mBlackPantherNode: Node? = null
    private var mAssetManager: AssetManager? = null
    private var mBlackPantherModel: Object3D? = null

    private var mObjLoaded = false
    private var mImageTargetFound = false

    // +---------------------------------------------------------------------------+
    //  Initialization
    // +---------------------------------------------------------------------------+

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViroView = ViroViewARCore(this, object : ViroViewARCore.StartupListener {
            override fun onSuccess() {
                // Override this function to start building your scene here
                onRenderCreate()
            }

            override fun onFailure(error: ViroViewARCore.StartupError, errorMessage: String) {
                // Fail as you wish!
            }
        })
        setContentView(mViroView)
    }

    private fun onRenderCreate() {
        // Create the base ARScene
        mScene = ARScene()

        // Create an ARImageTarget out of the Black Panther poster
        val blackPantherPoster = getBitmapFromAssets("logo.jpg")
        mImageTarget = ARImageTarget(blackPantherPoster, ARImageTarget.Orientation.Up, 0.188f)
        mScene!!.addARImageTarget(mImageTarget!!)

        // Create a Node containing the Black Panther model
        mBlackPantherNode = initBlackPantherNode()
        mBlackPantherNode!!.addChildNode(initLightingNode())
        mScene!!.rootNode.addChildNode(mBlackPantherNode!!)

        mViroView?.scene = mScene
        trackImageNodeTargets()
    }

    /*
     Sets up our ARScene.Listener such that when we detect the Black Panther poster, we activate
     the Black Panther model, making it jump out of the poster.
     */
    private fun trackImageNodeTargets() {

        mScene!!.setListener(object : ARScene.Listener {
            override fun onTrackingInitialized() {
                // No-op
            }

            override fun onAmbientLightUpdate(lightIntensity: Float, color: Vector) {
                // No-op
            }

            override fun onTrackingUpdated(state: ARScene.TrackingState, reason: ARScene.TrackingStateReason) {
                // No-op
            }

            override fun onAnchorFound(anchor: ARAnchor, arNode: ARNode) {
                val anchorId = anchor.anchorId
                if (!mImageTarget!!.id.equals(anchorId, ignoreCase = true)) {
                    return
                }

                val anchorPos = anchor.position
                val pos = Vector(anchorPos.x.toDouble(), anchorPos.y - 0.4, anchorPos.z - 0.15)
                mBlackPantherNode!!.setPosition(pos)
                mBlackPantherNode!!.setRotation(anchor.rotation)
                mBlackPantherModel!!.isVisible = true
                mImageTargetFound = true
                startPantherExperience()
            }

            override fun onAnchorUpdated(anchor: ARAnchor, arNode: ARNode) {
                //No-op
            }

            override fun onAnchorRemoved(anchor: ARAnchor, arNode: ARNode) {
                val anchorId = anchor.anchorId
                if (!mImageTarget!!.id.equals(anchorId, ignoreCase = true)) {
                    return
                }

                mBlackPantherNode!!.isVisible = false
            }
        })
    }

    private fun startPantherExperience() {
        if (!mObjLoaded || !mImageTargetFound) {
            return
        }

        // Animate the black panther's jump animation
        val animationJump = mBlackPantherModel!!.getAnimation("01")
        animationJump.listener = object : Animation.Listener {
            override fun onAnimationStart(animation: Animation) {
                //No-op
            }

            override fun onAnimationFinish(animation: Animation, canceled: Boolean) {
                // After jump animation is finished set the panther's idle animation
                val animationIdle = mBlackPantherModel!!.getAnimation("02")
                animationIdle.play()
            }
        }
        animationJump.play()
    }

    // +---------------------------------------------------------------------------+
    //  3D Scene Construction
    // +---------------------------------------------------------------------------+

    private fun initBlackPantherNode(): Node {
        val blackPantherNode = Node()
        mBlackPantherModel = Object3D()
        mBlackPantherModel!!.setRotation(Vector(Math.toRadians(-90.0), 0.0, 0.0))
        mBlackPantherModel!!.setScale(Vector(0.2f, 0.2f, 0.2f))
        mBlackPantherModel!!.loadModel(mViroView?.viroContext, Uri.parse("file:///android_asset/blackpanther/object_bpanther_anim.vrx"), Object3D.Type.FBX, object : AsyncObject3DListener {
            override fun onObject3DLoaded(`object`: Object3D, type: Object3D.Type) {
                mObjLoaded = true
                startPantherExperience()
            }

            override fun onObject3DFailed(error: String) {
                Log.e(TAG, "Black Panther Object Failed to load.")
            }
        })

        mBlackPantherModel!!.isVisible = false
        blackPantherNode.addChildNode(mBlackPantherModel!!)
        return blackPantherNode
    }

    private fun initLightingNode(): Node {
        val omniLightPositions = arrayOf(Vector(-3.0, 3.0, 0.3), Vector(3f, 3f, 1f), Vector(-3f, -3f, 1f), Vector(3f, -3f, 1f))

        val lightingNode = Node()
        for (pos in omniLightPositions) {
            val light = OmniLight()
            light.position = pos
            light.color = Color.parseColor("#FFFFFF").toLong()
            light.intensity = 20f
            light.attenuationStartDistance = 6f
            light.attenuationEndDistance = 9f

            lightingNode.addLight(light)
        }

        // The spotlight will cast the shadows
        val spotLight = Spotlight()
        spotLight.position = Vector(0.0, 5.0, -0.5)
        spotLight.color = Color.parseColor("#FFFFFF").toLong()
        spotLight.direction = Vector(0f, -1f, 0f)
        spotLight.intensity = 50f
        spotLight.shadowOpacity = 0.4f
        spotLight.shadowMapSize = 2048
        spotLight.shadowNearZ = 2f
        spotLight.shadowFarZ = 7f
        spotLight.innerAngle = 5f
        spotLight.outerAngle = 20f
        spotLight.castsShadow = true

        lightingNode.addLight(spotLight)

        // Add a lighting environment for realistic PBR rendering
        val environment = Texture.loadRadianceHDRTexture(Uri.parse("file:///android_asset/wakanda_360.hdr"))
        mScene!!.lightingEnvironment = environment

        // Add shadow planes: these are "invisible" surfaces on which virtual shadows will be cast,
        // simulating real-world shadows
        val material = Material()
        material.shadowMode = Material.ShadowMode.TRANSPARENT

        val surface = Surface(3f, 3f)
        surface.materials = Arrays.asList(material)

        val surfaceShadowNode = Node()
        surfaceShadowNode.setRotation(Vector(Math.toRadians(-90.0), 0.0, 0.0))
        surfaceShadowNode.geometry = surface
        surfaceShadowNode.setPosition(Vector(0.0, 0.0, 0.0))
        lightingNode.addChildNode(surfaceShadowNode)

        lightingNode.setRotation(Vector(Math.toRadians(-90.0), 0.0, 0.0))
        return lightingNode
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
        private val TAG = PosterAnimationARActivity::class.java.simpleName
    }
}