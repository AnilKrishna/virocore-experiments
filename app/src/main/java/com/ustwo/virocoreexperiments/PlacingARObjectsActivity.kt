package com.ustwo.virocoreexperiments

import android.app.AlertDialog
import android.os.Bundle
import com.ustwo.virocoreexperiments.arcomponents.BaseARActivity
import kotlinx.android.synthetic.main.base_ar_activity.*


class PlacingARObjectsActivity : BaseARActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addObject.setOnClickListener { showPopUp() }
    }


    /**
     * Show selection of existing local objects
     */
    private fun showPopUp() {
        val builder = AlertDialog.Builder(this)
        val itemsList = arrayOf<CharSequence>("Coffee mug", "Flowers", "Smile Emoji")
        builder.setTitle("Choose an object")
                .setItems(itemsList) { _, which ->
                    when (which) {
                        0 -> placeObject("file:///android_asset/mug/object_coffee_mug.vrx")
                        1 -> placeObject("file:///android_asset/flower/object_flowers.vrx")
                        2 -> placeObject("file:///android_asset/smile_emoji/emoji_smile.vrx")
                    }
                }
        builder.create().show()
    }
}
