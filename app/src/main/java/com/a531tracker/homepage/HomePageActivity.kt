package com.a531tracker.homepage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.a531tracker.BaseActivity
import com.a531tracker.ObjectBuilders.AsManyRepsAsPossible
import com.a531tracker.R
import com.a531tracker.adapters.FragmentCommunicator
import com.a531tracker.adapters.HomepagePagerAdapter
import com.a531tracker.database.DatabaseRepository
import com.a531tracker.databinding.ActivityHomepageBinding
import com.a531tracker.dialogs.BottomDialog
import com.a531tracker.dialogs.BottomDialogTool
import com.a531tracker.lifts.SetLiftsActivity
import com.a531tracker.mvpbase.DependencyInjectorClass
import com.a531tracker.tools.AppConstants
import com.a531tracker.tools.AppUtils
import com.a531tracker.tools.Snack
import com.a531tracker.week.WeekActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_homepage_toolbar.view.*
import kotlinx.android.synthetic.main.fragment_homepage_toolbar.view.bottom_toolbar
import kotlinx.android.synthetic.main.fragment_toolspage.view.*

class HomePageActivity : BaseActivity(), ViewBinding, FragmentCommunicator, HomePageContract.View, BottomDialogTool.BottomDialogToolsClick {

    private lateinit var binding: ActivityHomepageBinding

    private lateinit var presenter: HomePageContract.Presenter

    private lateinit var pager: ViewPager2

    private lateinit var tabLayout: TabLayout

    private lateinit var depInjector: DependencyInjectorClass

    private lateinit var currentCycle: String

    private var listData: HashMap<String, AsManyRepsAsPossible> = HashMap()

    override fun onResume() {
        super.onResume()
        presenter.checkForUpdatedLifts(hashHolder = listData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(root)

        pager = binding.root.home_pager_view
        tabLayout = binding.root.tab_view_homepage

        depInjector = DependencyInjectorClass()

        setPresenter(HomePagePresenter(this, depInjector, AppUtils(), this))
        setSupportActionBar(binding.root.bottom_toolbar)

        presenter.onViewCreated(this)

        root.bottom_toolbar.setNavigationOnClickListener {
            BottomDialog(this).newInstance(
                    hashMapOf(AppConstants.NAVIGATION_MENU to 0)
            ).show(supportFragmentManager, "navigation")
        }
    }

    override fun setHashObserver(hashHolder: HashMap<String, AsManyRepsAsPossible>) {
        for (liftName in AppConstants.LIFT_ACCESS_LIST) {
            listData[liftName] = hashHolder[liftName]!!
        }
    }

    private fun getCurrentCycle(): Int {
        return currentCycle.toInt()
    }

    override fun showHomeFragments() {
        pager.adapter = createViewAdapter()
        TabLayoutMediator(tabLayout, pager) { tab, position ->
            when(position) {
                0 -> tab.text = getString(R.string.home_page_tab)
                1 -> tab.text = getString(R.string.update_cycle_tag)
            }
        }.attach()
    }

    private fun createViewAdapter(): HomepagePagerAdapter {
        return HomepagePagerAdapter(this)
    }

    override fun setCycle(cycle: String) {
        currentCycle = cycle
    }

    override fun error(throwable: Throwable) {
        Log.e("Error", "Error : $throwable")
    }

    override fun getRoot(): View {
        return binding.root
    }

    override fun setPresenter(presenter: HomePageContract.Presenter) {
        this.presenter = presenter
    }

    override fun showSnack(success: Boolean) {
            Snack(this).apply {
                if(success) {
                    info(binding.root.bottom_toolbar, getString(R.string.lift_tools_update_percent_success))
                } else {
                    error(binding.root.bottom_toolbar, getString(R.string.amrap_error))
                }
            }
    }

    override fun startWeekActivity(hashMap: HashMap<String, String>) {
        startActivity(Intent(this, WeekActivity::class.java)
                .apply {
                    putExtra(AppConstants.MAIN_LIFT, hashMap[AppConstants.MAIN_LIFT])
                    putExtra(AppConstants.SWAP_LIFT, hashMap[AppConstants.SWAP_LIFT])
                    putExtra(AppConstants.CYCLE_NUM, getCurrentCycle())
                })
    }

    override fun launchTool(tool: Int) {
        when (tool) {
            AppConstants.NAVIGATION_TOOL_PERCENT -> {
                presenter.updatePercent(binding.root.lift_tools_percent_seekbar.progress.toFloat())
            }
            AppConstants.NAVIGATION_TOOL_CYCLE -> {
                val liftBuilder = presenter.getDataForUpdate()
                if (liftBuilder.isFilled()) {
                    BottomDialogTool(this, DatabaseRepository(this)).newInstance(
                            liftBuilder = liftBuilder,
                    ).show(supportFragmentManager, "tools")
                }
            }
            AppConstants.NAVIGATION_TOOL_TM -> {
                startActivity(Intent(this, SetLiftsActivity::class.java).apply{
                    putExtra(AppConstants.FRESH_LAUNCH, false)
                })
            }
        }
    }

    override fun confirmUpdate() {
        presenter.onViewCreated(this)
        Snack(this).info(binding.root.bottom_toolbar, getString(R.string.lift_tools_update_all_success))
    }
}