package com.a531tracker.homepage

import android.content.Context
import com.a531tracker.ObjectBuilders.AsManyRepsAsPossible
import com.a531tracker.ObjectBuilders.CompoundLifts
import com.a531tracker.ObjectBuilders.LiftBuilder
import com.a531tracker.mvpbase.BasePresenter
import com.a531tracker.mvpbase.BaseView

class HomePageContract {
    interface Presenter : BasePresenter {
        fun onViewCreated(mContext: Context)
        fun checkForUpdatedLifts(hashHolder: HashMap<String, AsManyRepsAsPossible>)
        fun getDataForUpdate(): LiftBuilder
        fun updatePercent(percent: Float)
    }

    interface View : BaseView<Presenter> {
        fun showHomeFragments()
        fun setHashObserver(hashHolder: HashMap<String, AsManyRepsAsPossible>)
        fun setCycle(cycle: String)
        fun showSnack(success: Boolean)
        fun error(throwable: Throwable)
    }
}