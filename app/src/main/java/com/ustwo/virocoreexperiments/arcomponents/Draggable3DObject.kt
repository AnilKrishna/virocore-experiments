package com.ustwo.virocoreexperiments.arcomponents

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.viro.core.*


class Draggable3DObject(val viroContext: ViroContext, val mFileName:String,val context:Context){

    private var rotateStart: Float = 0.toFloat()
    private var scaleStart: Float = 0.toFloat()

    fun addModelToPosition(position: Vector,arScene: ARScene,type:Object3D.Type) {
        val object3D = Object3D()
        object3D.setPosition(position)
        // Shrink the objects as the original size is too large.
        object3D.setScale(Vector(.2f, .2f, .2f))
        object3D.gestureRotateListener = GestureRotateListener { i, node, rotation, rotateState ->
            if (rotateState == RotateState.ROTATE_START) {
                rotateStart = object3D.rotationEulerRealtime.y
            }
            val totalRotationY = rotateStart + rotation
            object3D.setRotation(Vector(0f, totalRotationY, 0f))
        }

        object3D.gesturePinchListener = GesturePinchListener { i, node, scale, pinchState ->
            if (pinchState == PinchState.PINCH_START) {
                scaleStart = object3D.scaleRealtime.x
            } else {
                object3D.setScale(Vector(scaleStart * scale, scaleStart * scale, scaleStart * scale))
            }
        }

        object3D.dragListener = DragListener { i, node, vector, vector1 -> }

        // Load the Android model asynchronously.
        object3D.loadModel(viroContext,Uri.parse(mFileName), type, object : AsyncObject3DListener {
            override fun onObject3DLoaded(`object`: Object3D, type: Object3D.Type) {

            }

            override fun onObject3DFailed(s: String) {
                Toast.makeText(context, "An error occured when loading the 3D Object!",
                        Toast.LENGTH_LONG).show()
            }
        })

        // Make the object draggable.
        object3D.dragType = Node.DragType.FIXED_TO_WORLD
        arScene.rootNode.addChildNode(object3D)
    }
}