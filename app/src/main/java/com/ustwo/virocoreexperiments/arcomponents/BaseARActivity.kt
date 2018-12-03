package com.ustwo.virocoreexperiments.arcomponents

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ustwo.virocoreexperiments.R
import com.viro.core.*
import kotlinx.android.synthetic.main.base_ar_activity.*


// Constants used to determine if plane or point is within bounds. Units in meters.
private const val MIN_DISTANCE = 0.2f
private const val MAX_DISTANCE = 10f
abstract class BaseARActivity:AppCompatActivity(),ViroViewARCore.StartupListener{

    //In future when we want to add multiple objects in scene we can add..
    // In initial version we will have only one view
    private lateinit var mDraggableObjects: MutableList<Draggable3DObject>

    //this basically is an entry point to our vr world and their components
    private var mScene: ARScene? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_ar_activity)
        mDraggableObjects = mutableListOf()
        viroViewARCore.setStartupListener(this)
    }



    //ViroView ARCore startup callback
    override fun onSuccess() {
        //on Successful initialization of VRView create the scene
        mScene = ARScene()
        // Add a listener to the scene so we can update the 'AR Initialized' text.
        mScene?.setListener(object: ARSceneListener(this, viroViewARCore){
            override fun onAmbientLightUpdate(p0: Float, p1: Vector?) {

            }

            override fun onARInitialized() {
                trackingText.visibility = View.VISIBLE
                trackingText.setText(R.string.ar_is_initialised)
                addObject.visibility = View.VISIBLE
            }
        })
        // Add a light to the scene so our models show up
        mScene?.rootNode?.addLight(AmbientLight(Color.WHITE.toLong(), 1000f))

        viroViewARCore?.scene = mScene
    }

    //ViroView ARCore startup failure callback
    override fun onFailure(p0: ViroViewARCore.StartupError?, errorMessage: String?) {
        viroViewARCore?.showMessage("Error Loading AR $errorMessage")
    }

    override fun onStart() {
        super.onStart()
        viroViewARCore?.onActivityStarted(this)
    }

    override fun onResume() {
        super.onResume()
        viroViewARCore?.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        viroViewARCore?.onActivityPaused(this)
    }

    override fun onStop() {
        super.onStop()
        viroViewARCore?.onActivityStopped(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        viroViewARCore?.onActivityDestroyed(this)
    }

    /**
     * Perform a hit-test and place the object (identified by its file name) at the intersected
     * location.
     *
     * @param fileName The resource name of the object to place.
     * @param type specifies the type of object, if it is not specified it defaults to FBX.
     */
    fun placeObject(fileName: String,type:Object3D.Type = Object3D.Type.FBX) {
        val cameraPos = viroViewARCore?.lastCameraPositionRealtime
        viroViewARCore?.performARHitTestWithRay(viroViewARCore.lastCameraForwardRealtime, ARHitTestListener { arHitTestResults ->
            arHitTestResults?.isNotEmpty()?.let {
                for (i in arHitTestResults.indices) {
                    val result = arHitTestResults[i]
                    val distance = result.position.distance(cameraPos)
                    if (distance > MIN_DISTANCE && distance < MAX_DISTANCE) {
                        // If we found a plane or feature point further than 0.2m and less 10m away,
                        // then choose it!
                        add3DDraggableObject(viroViewARCore.viroContext,fileName, result.position,type)
                        return@ARHitTestListener
                    }
                }
            }
            viroViewARCore.showMessage("Unable to find suitable point or plane to place object!")
        })
    }

    /**
     * Add a 3D object with the given filename to the scene at the specified world position.
     */
    private fun add3DDraggableObject(viroContext: ViroContext,filename: String, position: Vector,type:Object3D.Type) {
        val draggable3DObject = Draggable3DObject(viroContext,filename,this)
        mDraggableObjects.add(draggable3DObject)
        mScene?.let {
            draggable3DObject.addModelToPosition(position,mScene!!,type)
        }
    }
}