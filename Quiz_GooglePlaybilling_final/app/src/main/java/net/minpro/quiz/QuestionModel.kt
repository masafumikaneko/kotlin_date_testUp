package net.minpro.quiz

import io.realm.RealmObject

/**
 * Created by keybowNew on 2017/10/27.
 */
open class QuestionModel: RealmObject() {

    //id:0
    var id: String = ""
    //級:1
    var gradeId: String = ""
    //問題:2
    var question: String = ""
    //こたえ:3
    var answer:String = ""
    //選択肢1：4
    var choice1: String = ""
    //選択肢2：5
    var choice2: String = ""
    //選択肢3：6
    var choice3: String = ""
    //解説：7
    var explanation: String = ""
}