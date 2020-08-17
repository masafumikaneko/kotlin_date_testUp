package net.minpro.quiz

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_finish.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FinishFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FinishFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
//content_testのｺﾝﾃﾅにいれるFragmentであり、合否について2パターンの表示の場合があるクラス。
class FinishFragment : Fragment() {

    private var gradeOfTest: Int = 0
    //private var testStatus: TestStatus? = null
    private var testStatus: Enum<TestStatus>? = null

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            gradeOfTest = arguments.getInt(ARG_PARAM1)
            testStatus = arguments.getSerializable(ARG_PARAM2) as TestStatus
        }


    }

    //ここはいつもなにもいじらない。
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_finish, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //testStatusが勝利なのかロスなのかで表示する画面が変わる。
        when (testStatus){
            TestStatus.FINISH_WIN -> {//enum class
                imageFinish.setImageResource(R.drawable.image_win) //fragment_finishにあるimageFinishの画面を変える。
                if (isSoundOn) soundPool?.play2(soundIdApplause)
            }
            TestStatus.FINISH_LOSE -> {
                imageFinish.setImageResource(R.drawable.image_lose)
                if (isSoundOn) soundPool?.play2(soundIdTin)

            }
        }

        //MainActivityに戻る処理を記載。
        //ただし、自分のFragment内のViewを超えた操作は、引っ付きActivityで行われるので、引っ付きActivity(TestActivity)で
        //finish処理を行う。
        btnGoNext.setOnClickListener {
            mListener?.onGoNextBtnOnFinishFragmentClicked()
            fragmentManager.beginTransaction().remove(this).commit()
        }
    }



    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onGoNextBtnOnFinishFragmentClicked()
    }

    //content_testのｺﾝﾃﾅにいれるFragmentであり、合否について2パターンの表示の場合がある。
    companion object {
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = IntentKey.GRADE_FINISH.name
        private val ARG_PARAM2 = IntentKey.TEST_STATUS.name

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FinishFragment.
         */
        //gradeOfTestは何級のテストを受けたのかが入っている。
        //TestStatusは、テスト中なのか勝ったのか負けたのかの現在の判定が入っている。渡される。
        fun newInstance(gradeOfTest: Int, testStatus: TestStatus): FinishFragment {
            val fragment = FinishFragment()
            val args = Bundle()
            args.putInt(ARG_PARAM1, gradeOfTest)
            args.putSerializable(ARG_PARAM2, testStatus)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
