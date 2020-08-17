package net.minpro.quiz

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog

/**
 * Created by keybowNew on 2017/10/30.
 */
//【前提】
//当クラスはダイアログ表示のためのFragmentである。
//ダイアログFragmentの必要性は、通常のダイアログの場合、スマホの向き変更などによるActivityの死によって、
//ダイアログもまた死亡するため、ダイアログの死亡防止のために行われる。
//その対策の概要は、Activityの死による引数の消滅対策と同じで「saveInstanceState:Bundle」にて行われる。
//→まあ、画面変更不可処理を行っておけば問題がないので、不要な教示になると思われるが、他の構成で学べる点はあるだろう。
//そして、当クラスはクラスでなくFragmentである。にも拘わらず、layoutにおいてactivity無しで存在可能な理由は、
//当Fragmentがktでもって作成管理されるFragmentであるからである。他の例示として、カレンダーFragmentがそれにあたる。
//---------------------------------------------

//DialogFragmentを継承し、onCreateDialogメソッドを継承する。
class AlertDialogFragment: DialogFragment() {

    var listerner: AlertDialogListerner? = null//【重要】interfaceListenerの構築の一部。

    interface AlertDialogListerner {//【重要】interfaceListenerの構築の一部。
        fun onPositiveButtonClicked()
        fun onNegativeButtonClicked()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //【重要】ここからは、通常のダイアログ処理。ただし、最後のメソッドチェーンにおいて、通常show()のところ
        //create()に置き換わっている。
        return AlertDialog.Builder(activity).apply {
            setTitle(R.string.alert_go_test_title)
            setMessage(getString(R.string.alert_go_test_message))
            setPositiveButton("実行"){ dialogInterface, i ->
                listerner?.onPositiveButtonClicked()//【重要】interfaceListenerの構築の一部。
            }
            setNegativeButton("キャンセル"){ dialogInterface, i ->
                listerner?.onNegativeButtonClicked()//【重要】interfaceListenerの構築の一部。
            }
        }.create()//今回ダイアログの表示に当たっては、Fragmentそのものを表示対象にするので、show()メソッドはActivity側で行う。
  }

    override fun onAttach(context: Context?) {//【重要】interfaceListenerの構築の一部。
        super.onAttach(context)
        if (context is AlertDialogListerner){
            listerner = context
        }
    }


}