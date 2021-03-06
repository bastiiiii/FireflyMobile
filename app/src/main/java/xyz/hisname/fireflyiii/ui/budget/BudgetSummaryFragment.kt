package xyz.hisname.fireflyiii.ui.budget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.fragment_budget_summary.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.budget.BudgetViewModel
import xyz.hisname.fireflyiii.repository.currency.CurrencyViewModel
import xyz.hisname.fireflyiii.repository.models.currency.CurrencyData
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.ui.transaction.TransactionByBudgetDialogFragment
import xyz.hisname.fireflyiii.util.DateTimeUtil
import xyz.hisname.fireflyiii.util.LocaleNumberParser
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.util.extension.getViewModel
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class BudgetSummaryFragment: BaseFragment() {

    private val budgetViewModel by lazy { getImprovedViewModel(BudgetViewModel::class.java) }
    private val transactionExtendedFab by bindView<ExtendedFloatingActionButton>(R.id.addTransactionExtended)
    private val currencyViewModel by lazy { getImprovedViewModel(CurrencyViewModel::class.java) }
    private val coloring = arrayListOf<Int>()
    private var budgetSpent = 0f
    private var budgeted = 0f
    private var noExpenses = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.create(R.layout.fragment_budget_summary, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionExtendedFab.isGone = true
        for (col in ColorTemplate.COLORFUL_COLORS) {
            coloring.add(col)
        }
        for (col in ColorTemplate.JOYFUL_COLORS){
            coloring.add(col)
        }
        budgetSummaryPieChart.isDrawHoleEnabled = false
        currencyViewModel.getDefaultCurrency().observe(viewLifecycleOwner) { currency ->
            retrieveData(currency[0])
            setBudgetSummary(currency[0])
        }
    }

    private fun retrieveData(currencyData: CurrencyData){
        val currencyCode = currencyData.currencyAttributes?.code ?: ""
        zipLiveData(zipLiveData(transactionViewModel.getTotalTransactionAmountByDateAndCurrency(DateTimeUtil.getStartOfMonth(),
                DateTimeUtil.getEndOfMonth(), currencyCode, "withdrawal"),
                transactionViewModel.getUniqueBudgetByDate(DateTimeUtil.getStartOfMonth(),
                        DateTimeUtil.getEndOfMonth(), currencyCode, "withdrawal")),
                zipLiveData(budgetViewModel.retrieveSpentBudget(currencyData.currencyAttributes?.code ?: ""),
                        budgetViewModel.retrieveCurrentMonthBudget(currencyData.currencyAttributes?.code ?: ""))).observe(viewLifecycleOwner) { fireflyData ->
            if(fireflyData.first.second.isNotEmpty()) {
                val pieEntryArray: ArrayList<PieEntry> = ArrayList(fireflyData.first.second.size)
                fireflyData.first.second.forEach {  uniqueBudget ->
                    transactionViewModel.getTransactionByDateAndBudgetAndCurrency(DateTimeUtil.getStartOfMonth(),
                            DateTimeUtil.getEndOfMonth(), currencyCode,
                            "withdrawal", uniqueBudget).observe(viewLifecycleOwner) { transactionAmount ->
                        val percentageCategory = transactionAmount.absoluteValue.roundToInt()
                                .toDouble()
                                .div(fireflyData.first.first.absoluteValue.roundToInt().toDouble())
                                .times(100)
                        if (uniqueBudget == null) {
                            pieEntryArray.add(PieEntry(percentageCategory.roundToInt().toFloat(),
                                    requireContext().getString(R.string.expenses_without_budget),
                                    transactionAmount))
                            noExpenses = transactionAmount
                        } else {
                            pieEntryArray.add(PieEntry(percentageCategory.roundToInt().toFloat(), uniqueBudget, transactionAmount))
                        }
                        val pieDataSet = PieDataSet(pieEntryArray, "")
                        pieDataSet.valueFormatter = PercentFormatter()
                        pieDataSet.colors = coloring
                        pieDataSet.valueTextSize = 15f
                        budgetSummaryPieChart.data = PieData(pieDataSet)
                        budgetSummaryPieChart.invalidate()
                    }
                }
            }
            setBudgetData(fireflyData.second, currencyData)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setBudgetData(fireflyData: Pair<String, String>, currencyData: CurrencyData){
        val currencySymbol = currencyData.currencyAttributes?.symbol ?: ""
        budgetSpent = fireflyData.first.toFloat()
        budgeted = fireflyData.second.toFloat()
        budgetAmountValue.text = "$currencySymbol $budgeted"
        actualAmountValue.text = "$currencySymbol $budgetSpent"
        remainingAmountValue.text = currencySymbol + " " +
                LocaleNumberParser.parseDecimal((budgeted - budgetSpent).toDouble(), requireContext())

    }

    @SuppressLint("SetTextI18n")
    private fun setBudgetSummary(currencyData: CurrencyData){
        val currencyCode = currencyData.currencyAttributes?.code ?: ""
        budgetSummaryPieChart.setData {
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    budgetAmountValue.text = currencyData.currencyAttributes?.symbol + " " + budgeted
                    actualAmountValue.text = currencyData.currencyAttributes?.symbol + " " + budgetSpent
                    remainingAmountValue.text = currencyData.currencyAttributes?.symbol + " " +
                            LocaleNumberParser.parseDecimal((budgeted - budgetSpent).toDouble(), requireContext())
                    showTransactionButton.isGone = true
                }

                override fun onValueSelected(entry: Entry, high: Highlight) {
                    val pe = entry as PieEntry
                    showTransactionButton.isVisible = true
                    budgetAmountValue.text = "--.--"
                    actualAmountValue.text = "--.--"
                    remainingAmountValue.text = "--.--"
                    val entryLabel = if(entry.label == requireContext().getString(R.string.expenses_without_budget)){
                        null
                    } else {
                        entry.label
                    }
                    zipLiveData(budgetViewModel.retrieveBudgetLimit(entryLabel, currencyCode, DateTimeUtil.getStartOfMonth(),
                            DateTimeUtil.getEndOfMonth()), budgetViewModel.retrieveSpentBudgetById(entryLabel, currencyCode)).observe(viewLifecycleOwner) { value ->
                        val remainingValue = LocaleNumberParser.parseDecimal((value.first - abs(value.second.toDouble())), requireContext())
                        budgetAmountValue.text = currencyData.currencyAttributes?.symbol + " " + value.first
                        actualAmountValue.text = currencyData.currencyAttributes?.symbol + " " + value.second
                        remainingAmountValue.text = currencyData.currencyAttributes?.symbol + " " + remainingValue
                        if(entryLabel != null) {
                            if (remainingValue.toString().contains("-")) {
                                remainingAmountValue.setTextColor(getCompatColor(R.color.md_red_A700))
                                remainingBudgetText.setTextColor(getCompatColor(R.color.md_red_A700))
                            } else {
                                remainingAmountValue.setTextColor(getCompatColor(R.color.md_green_700))
                                remainingBudgetText.setTextColor(getCompatColor(R.color.md_green_700))
                            }
                        } else {
                            budgetAmountValue.text = "--.--"
                            actualAmountValue.text = currencyData.currencyAttributes?.symbol + " " + LocaleNumberParser.parseDecimal(noExpenses, requireContext())
                            remainingAmountValue.text = "--.--"
                        }
                    }


                    showTransactionButton.setOnClickListener {
                        val transactionDialog = TransactionByBudgetDialogFragment()
                        transactionDialog.arguments = bundleOf("budgetName" to entryLabel)
                        transactionDialog.show(parentFragmentManager, "transaction_budget_dialog")
                    }
                }
            })
        }
        budgetSummaryPieChart.description.isEnabled = false
        budgetDuration.text = DateTimeUtil.getDurationText()
    }

    override fun onAttach(context: Context){
        super.onAttach(context)
        extendedFab.isGone = true
        activity?.activity_toolbar?.title = "Budget Summary"
    }

    override fun onResume() {
        super.onResume()
        extendedFab.isGone = true
        activity?.activity_toolbar?.title = "Budget Summary"
    }

    override fun handleBack() {
        parentFragmentManager.popBackStack()
    }


}