package com.ustwo.virocoreexperiments.arcomponents

import android.view.View
import android.widget.Toast


fun View.showMessage(msg:String,duration:Int = Toast.LENGTH_SHORT){
    Toast.makeText(context,msg,duration).show()
}