package com.ustwo.virocoreexperiments

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlacingObjectsLaunch.setOnClickListener {
            val intent = Intent(this, PlacingARObjectsActivity::class.java)
            startActivity(intent)
        }

        btnCarModelLaunch.setOnClickListener {
            val intent = Intent(this, CarModelExperienceARActivity::class.java)
            startActivity(intent)
        }

        btnMotionPosterLaunch.setOnClickListener {
            val intent = Intent(this, PosterAnimationARActivity::class.java)
            startActivity(intent)
        }

        btnARPortalLaunch.setOnClickListener {
            val intent = Intent(this, ARPortalActivity::class.java)
            startActivity(intent)
        }

    }
}