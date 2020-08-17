package net.minpro.quiz

import android.content.Context
import android.media.SoundPool
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.Toast

/**
 * Created by keybowNew on 2017/10/27.
 */
//拡張関数を作成しておく。
fun SoundPool.play2 (soundId: Int){
    this.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
}
//拡張関数を作成しておく。
fun makeToast(context: Context, message: String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
//fab_sound.setOnClickListener {changeSoundMode{}}から来ており、ラムダ式で唯一の引数it:Viewなので、こちらの引数名は便宜上「v」とした。
fun changeSoundMode(v: View){//MainActivityに呼び出しがある。
    val fab = v as FloatingActionButton////【超重要】ラムダ式で引数がView型というかなり上の親になっているので、送った先ではかならずダウンキャストすること。
    //ダウンキャストのポイントは、もともとの(ここではfabのFloatingActionButton)型に戻すという点。
    //またダウンキャストの要件は、親から子の型にしか変換できない。Any→すべての頂点なので何にでも。View→ その系列の型。
    isSoundOn = !isSoundOn//押される都度Booleanを切り替える。→【重要】勘違いポイント。!SoundOnは、falseにするという意味ではなく、反対にするという意味。
    //画像切り替え。
    if (isSoundOn){//trueの場合。
        fab.setImageResource(R.drawable.ic_volume_up_black_24dp)
    } else {//falseの場合。
        fab.setImageResource(R.drawable.ic_volume_off_black_24dp)
    }

}