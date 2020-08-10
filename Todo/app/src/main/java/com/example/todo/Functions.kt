package com.example.todo

import android.content.Context
import android.widget.Toast

//トップライン関数を作成できる。


//例：だれでもどこでも楽々トースト術。
//複製したいものだけを{}内にのこす。多面性のあるものは引数にし汎用性を高める。
fun makeToast(context : Context, message:String){
    Toast.makeText(context,message, Toast.LENGTH_SHORT).show()
}
