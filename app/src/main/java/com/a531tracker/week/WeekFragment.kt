package com.a531tracker.week

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.a531tracker.R
import com.a531tracker.database.DatabaseRepository
import com.a531tracker.databinding.FragmentWeekInformationBinding
import com.a531tracker.databinding.WeekDataBinding
import com.a531tracker.tools.AppConstants
import com.a531tracker.tools.PreferenceUtils
import kotlinx.android.synthetic.main.week_data.view.*

class WeekFragment(private val weekToShow: Int) : Fragment(), ViewBinding {

    private lateinit var binding: FragmentWeekInformationBinding

    private lateinit var weekRecyclerView: WeekRecyclerView

    private lateinit var databaseRepository: DatabaseRepository

    private lateinit var prefUtils: PreferenceUtils

    private var extraDeload: Boolean = false

    private var hideExtras: Boolean = false

    fun newInstance(position: Int) : WeekFragment {
        return WeekFragment(weekToShow = position)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentWeekInformationBinding.inflate(inflater)
        databaseRepository = DatabaseRepository(binding.root.context)
        prefUtils = PreferenceUtils.getInstance(binding.root.context)
        extraDeload = prefUtils.getPreference(binding.root.context.getString(R.string.preference_deload_key)) ?: false
        hideExtras = prefUtils.getPreference(binding.root.context.getString(R.string.preference_remove_extras_key)) ?: false

        weekRecyclerView = WeekRecyclerView(binding.root.context, binding, this)
        weekRecyclerView.setData(weekToShow)

        for (i in 0 until 4) {

            Log.d("Fragnent", "Fragment seeing data ${databaseRepository.getWeekData(i)}")
        }


        return root
    }

    override fun getRoot(): View {
        return binding.root
    }

    fun getWeeklyData(weekToShow: Int): ArrayList<String> {
        return ArrayList<String>().apply {
            addAll(databaseRepository.getWeekData(weekToShow)!![0]!!)
            if (weekToShow == 3 && extraDeload) {
                addAll(databaseRepository.getWeekData(weekToShow)!![1]!!)
            }
            if (weekToShow != 3) {
                addAll(databaseRepository.getWeekData(weekToShow)!![1]!!)
                if (!hideExtras) {
                    addAll(databaseRepository.getWeekData(weekToShow)!![2]!!)
                }
            }
        }
    }

    fun hideExtras(): Boolean {
        return hideExtras
    }

    fun getWeeklyReps(): ArrayList<String> {
        return databaseRepository.getRepData()
    }

    fun getWeightBreakdown(weekToShow: Int): ArrayList<ArrayList<Double>> {
        return databaseRepository.getWeightBreakdown(weekToShow)
    }

    class WeekRecyclerView(private val mContext: Context, private val binding: FragmentWeekInformationBinding, private val fragment: WeekFragment) {

        private lateinit var weekAdapter: WeekAdapter

        private lateinit var recyclerView: RecyclerView

        private lateinit var weekData: ArrayList<String>

        private lateinit var repData: ArrayList<String>

        private lateinit var breakdownData: ArrayList<ArrayList<Double>>

        fun setData(weekToShow: Int) {
            recyclerView = binding.weekRecycler
            weekData = fragment.getWeeklyData(weekToShow)
            Log.d("TestingData", "Fragment weekly data : $weekData")
            repData = fragment.getWeeklyReps()
            Log.d("TestingData", "Fragment rep data : $repData")
            breakdownData = fragment.getWeightBreakdown(weekToShow)
            weekAdapter = WeekAdapter(liftDataset = weekData,
                    repData = repData,
                    breakdown = breakdownData,
                    deload = weekToShow == 3,
                    hideExtras = fragment.hideExtras()
            )
            recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(mContext)
                recycledViewPool.setMaxRecycledViews(0, 0)
                adapter = weekAdapter
            }

            weekAdapter.notifyDataSetChanged()
        }
    }

    class WeekAdapter(
            private val liftDataset: ArrayList<String>,
            private val repData: ArrayList<String>,
            private val breakdown: ArrayList<ArrayList<Double>>,
            private val deload: Boolean,
            private val hideExtras: Boolean) : RecyclerView.Adapter<WeekAdapter.ViewHolder>(), ViewBinding {

        lateinit var binding: WeekDataBinding

        private var hideExtrasNow: Boolean = false

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            binding = WeekDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val liftData = liftDataset[position]

            /*
            val params: CoordinatorLayout.LayoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT)
            when(position) {
                0,4,8 -> params.setMargins(24,48,24,0)
                3,7 -> params.setMargins(24,0,24,32)
                13 -> params.setMargins(24, 0, 24, 48)
                else -> params.setMargins(24,0,24,0)
            }
            holder.itemView.coordinator.layoutParams = params*/
            holder.bind(liftData)
            if (liftData == AppConstants.SET_WARMUP
                    || liftData == AppConstants.SET_CORE
                    || liftData == AppConstants.SET_BBB
                    || liftData == AppConstants.SET_DELOAD
                    || liftData == AppConstants.SET_FSL) {
                holder.itemView.weight_layout.visibility = View.GONE
                holder.itemView.weight_breakdown_layout.visibility = View.GONE

                holder.itemView.header_text.apply {
                    text = if (deload) {
                        "Deload Sets"
                    } else {
                        liftData
                    }
                    visibility = if(deload && position > 3) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
                val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0,0,0,0)
                holder.itemView.coordinator.apply {
                    layoutParams = params
                    cardElevation = 0f
                    setBackgroundColor(resources.getColor(R.color.colorSurface, null))
                }
                if (liftData == AppConstants.SET_DELOAD || liftData == AppConstants.SET_FSL && hideExtras) {
                    hideExtrasNow = true
                }
            } else {
                holder.itemView.header_layout.visibility = View.GONE
                holder.itemView.weight_reps.text = repData[position]
                holder.itemView.weight_amount.text = liftData
                val weightBreakdown = breakdown[position]
                holder.itemView.bar_weight.text = weightBreakdown[0].toString()
                holder.itemView.weight_breakdown.text = formatBreakdownText(weightBreakdown)
            }

            holder.itemView.weight_breakdown_layout.setOnClickListener{
                when (holder.itemView.divider_bar.visibility) {
                    View.VISIBLE -> {
                        holder.itemView.apply {
                            divider_bar.visibility = View.GONE
                            weight_breakdown_info.visibility = View.GONE
                            display_further_info_arrow.isSelected = false
                        }
                    }
                    else -> {
                        holder.itemView.apply {
                            divider_bar.visibility = View.VISIBLE
                            weight_breakdown_info.visibility = View.VISIBLE
                            display_further_info_arrow.isSelected = true
                        }
                    }
                }
            }
        }

        private fun formatBreakdownText(weightBreakdown: ArrayList<Double>): String {
            var bdString = ""

            for (value in 1 until weightBreakdown.size) {
                bdString += "${weightBreakdown[value]} "
                bdString += " | "
            }
            bdString += "&"
            return bdString.replace("| &", " lbs")
        }

        class ViewHolder(binding: WeekDataBinding) : RecyclerView.ViewHolder(binding.root) {
            private lateinit var liftData: String

            fun bind(lift: String) {
                liftData = lift
            }

        }

        override fun getItemCount(): Int {
            return liftDataset.size
        }

        override fun getRoot(): View {
            return binding.root
        }
    }
}