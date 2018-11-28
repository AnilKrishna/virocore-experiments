package com.ustwo.arplacingobjectskt.arcomponents

import com.viro.core.*
import java.lang.ref.WeakReference

abstract class ARSceneListener(activity: BaseARActivity,
                               viroViewARCore: ViroView?) : ARScene.Listener {

    private val mCurrentActivityWeak: WeakReference<BaseARActivity> = WeakReference(activity)

    private var mInitialized: Boolean = false

    override fun onAnchorRemoved(p0: ARAnchor?, p1: ARNode?) {}

    override fun onTrackingUpdated(trackingState: ARScene.TrackingState?, trackingStateReason: ARScene.TrackingStateReason?) {
        //if it is not already initialised initialise it with ar view to load objects
        if(!mInitialized && trackingState == ARScene.TrackingState.NORMAL){
            mCurrentActivityWeak.get()?.let {
                onARInitialized()
                mInitialized = true
            }
        }
    }

    override fun onAmbientLightUpdate(p0: Float, p1: Vector?) {}

    override fun onAnchorUpdated(p0: ARAnchor?, p1: ARNode?) {}

    override fun onAnchorFound(p0: ARAnchor?, p1: ARNode?) {}

    override fun onTrackingInitialized() {}

    abstract fun onARInitialized()
}
