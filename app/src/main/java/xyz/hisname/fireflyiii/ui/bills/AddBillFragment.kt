package xyz.hisname.fireflyiii.ui.bills

import android.content.res.ColorStateList
import android.graphics.Color.rgb
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.commit
import androidx.lifecycle.observe
import com.google.android.material.datepicker.MaterialDatePicker
import com.mikepenz.iconics.IconicsColor.Companion.colorList
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.color
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.icon
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.android.synthetic.main.fragment_add_bill.*
import me.toptas.fancyshowcase.FancyShowCaseQueue
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.MarkdownViewModel
import xyz.hisname.fireflyiii.repository.bills.BillsViewModel
import xyz.hisname.fireflyiii.repository.currency.CurrencyViewModel
import xyz.hisname.fireflyiii.repository.models.bills.BillAttributes
import xyz.hisname.fireflyiii.ui.markdown.MarkdownFragment
import xyz.hisname.fireflyiii.ui.ProgressBar
import xyz.hisname.fireflyiii.ui.base.BaseAddObjectFragment
import xyz.hisname.fireflyiii.ui.currency.CurrencyListBottomSheet
import xyz.hisname.fireflyiii.util.DateTimeUtil
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.workers.bill.BillWorker

class AddBillFragment: BaseAddObjectFragment() {

    private var billAttribute: BillAttributes? = null
    private var notes: String? = null
    private var repeatFreq: String = ""
    private var currency = ""
    private val billId by lazy { arguments?.getLong("billId") ?: 0 }
    private var billDescription: String? = ""
    private lateinit var queue: FancyShowCaseQueue
    private val markdownViewModel by lazy { getViewModel(MarkdownViewModel::class.java) }
    private val currencyViewModel by lazy { getImprovedViewModel(CurrencyViewModel::class.java) }
    private val billViewModel by lazy { getImprovedViewModel(BillsViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_add_bill, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showReveal(dialog_add_bill_layout)
        updateEditText()
        showHelpText()
        setFab()
    }

    private fun updateEditText(){
        if(billId != 0L){
            ProgressBar.animateView(progressLayout, View.VISIBLE, 0.4f, 200)
            billViewModel.getBillById(billId).observe(viewLifecycleOwner) {
                billAttribute = it[0].billAttributes
                description_edittext.setText(billAttribute?.name)
                billDescription = billAttribute?.name
                min_amount_edittext.setText(billAttribute?.amount_min.toString())
                max_amount_edittext.setText(billAttribute?.amount_max.toString())
                currency = billAttribute?.currency_code ?: ""
                currencyViewModel.getCurrencyByCode(currency).observe(viewLifecycleOwner) { currencyList ->
                    val currencyData = currencyList[0].currencyAttributes
                    currency_edittext.setText(currencyData?.name + " (" + currencyData?.code + ")")
                }
                bill_date_edittext.setText(billAttribute?.date)
                skip_edittext.setText(billAttribute?.skip.toString())
                notes_edittext.setText(billAttribute?.notes)
                frequency_exposed_dropdown.setText(billAttribute?.repeat_freq?.substring(0, 1)?.toUpperCase()
                        + billAttribute?.repeat_freq?.substring(1))

                // Weird bug where only 1 value will show in the array if I don't use this
                val spinnerAdapter = ArrayAdapter(requireContext(),
                        R.layout.cat_exposed_dropdown_popup_item, resources.getStringArray(R.array.repeat_frequency))
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                frequency_exposed_dropdown.setAdapter(spinnerAdapter)
                ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
            }
        }
    }

    private fun setFab(){
        if(billId != 0L){
            addBillFab.setImageDrawable(IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_update))
        }
        addBillFab.setOnClickListener {
            hideKeyboard()
            ProgressBar.animateView(progressLayout, View.VISIBLE, 0.4f, 200)
            notes = if(notes_edittext.isBlank()){
                null
            } else {
                notes_edittext.getString()
            }
            if(billId == 0L){
                submitData()
            } else {
                updateBill()
            }
        }
    }

    override fun setIcons(){
        currency_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_money_bill
                    colorRes = R.color.md_green_400
                    sizeDp = 24
                },null, null, null)
        min_amount_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_dollar_sign
                    colorRes = R.color.md_yellow_A700
                    sizeDp = 16
                },null, null, null)
        max_amount_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_dollar_sign
                    colorRes = R.color.md_yellow_A700
                    sizeDp = 16
                },null, null, null)
        bill_date_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_calendar
                    color = colorList(ColorStateList.valueOf(rgb(18, 122, 190)))
                    sizeDp = 24
                },null, null, null)
        skip_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_sort_numeric_up
                    colorRes = R.color.md_red_500
                    sizeDp = 24
                },null, null, null)
        addBillFab.setBackgroundColor(getCompatColor(R.color.colorPrimaryDark))
        addBillFab.setImageDrawable(IconicsDrawable(requireContext()).apply {
            icon = FontAwesome.Icon.faw_save
            colorRes = R.color.md_black_1000
            sizeDp = 24
        })
        if(billId != 0L) {
            placeHolderToolbar.inflateMenu(R.menu.delete_menu)
            placeHolderToolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.menu_item_delete) {
                    AlertDialog.Builder(requireContext())
                            .setTitle(resources.getString(R.string.delete_bill_message, billDescription))
                            .setMessage(resources.getString(R.string.delete_bill_message, billDescription))
                            .setIcon(IconicsDrawable(requireContext()).apply {
                                icon = FontAwesome.Icon.faw_trash
                                sizeDp = 15
                                colorRes = R.color.md_green_600
                            })
                            .setPositiveButton(R.string.delete_permanently){ _,_ ->
                                ProgressBar.animateView(progressLayout, View.VISIBLE, 0.4f, 200)
                                billViewModel.deleteBillById(billId).observe(viewLifecycleOwner) { billDeleted ->
                                    if(billDeleted){
                                        toastSuccess(resources.getString(R.string.bill_deleted, billDescription))
                                    } else {
                                        toastError(resources.getString(R.string.issue_deleting, "bill"),
                                                Toast.LENGTH_LONG)
                                    }
                                    ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
                                    handleBack()
                                }
                            }
                            .setNegativeButton("No") { _, _ ->
                                toastInfo("Bill not deleted")
                            }
                            .show()
                }
                true
            }
        }
    }

    private fun showHelpText(){
        queue = FancyShowCaseQueue()
                .add(showCase(R.string.bills_create_intro, "addBillDescriptionCaseView", appbar))
                .add(showCase(R.string.bills_create_name, "descriptionCaseView", description_edittext))
                .add(showCase(R.string.bills_create_amount_min_holder, "minMaxAmountCaseView",
                        min_amount_layout))
                .add(showCase(R.string.bills_create_skip_holder, "skipCaseView", skip_layout))
                .add(showCase(R.string.bills_create_repeat_freq_holder,
                        "freqCaseView", frequency_menu))

        queue.show()
    }

    private fun getFreq(item: String): String {
        return when (item) {
            "Weekly" -> "weekly"
            "Monthly" -> "monthly"
            "Quarterly" -> "quarterly"
            "Half-yearly" -> "half-year"
            "Yearly" -> "yearly"
            else -> ""
        }
    }

    override fun setWidgets(){
        val spinnerAdapter = ArrayAdapter(requireContext(),
                R.layout.cat_exposed_dropdown_popup_item, resources.getStringArray(R.array.repeat_frequency))
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        frequency_exposed_dropdown.setAdapter(spinnerAdapter)
        frequency_exposed_dropdown.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            repeatFreq = when (position) {
                0 -> "weekly"
                1 -> "monthly"
                2 -> "quarterly"
                3 -> "half-year"
                4 -> "yearly"
                else -> ""
            }
        }
        bill_date_edittext.setOnClickListener {
            val materialDatePicker = MaterialDatePicker.Builder.datePicker()
            val picker = materialDatePicker.build()
            picker.show(parentFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { time ->
                bill_date_edittext.setText(DateTimeUtil.getCalToString(time.toString()))
            }
        }
        currency_edittext.setOnClickListener{
            CurrencyListBottomSheet().show(parentFragmentManager, "currencyList" )
        }
        currencyViewModel.currencyCode.observe(viewLifecycleOwner) {
            currency = it
        }

        currencyViewModel.currencyDetails.observe(viewLifecycleOwner) {
            currency_edittext.setText(it)
        }
        placeHolderToolbar.setNavigationOnClickListener{ handleBack() }
        currencyViewModel.getDefaultCurrency().observe(viewLifecycleOwner) { defaultCurrency ->
            val currencyData = defaultCurrency[0].currencyAttributes
            currency_edittext.setText(currencyData?.name + " (" + currencyData?.code + ")")
            currency = currencyData?.code.toString()
        }
        markdownViewModel.markdownText.observe(viewLifecycleOwner){ markdownText ->
            notes_edittext.setText(markdownText)
        }
        notes_edittext.setOnClickListener {
            markdownViewModel.markdownText.postValue(notes_edittext.getString())
            parentFragmentManager.commit {
                replace(R.id.bigger_fragment_container, MarkdownFragment())
                addToBackStack(null)
            }
        }
    }

    override fun submitData(){
        billViewModel.addBill(description_edittext.getString(),
                min_amount_edittext.getString(), max_amount_edittext.getString(),
                bill_date_edittext.getString(), repeatFreq, skip_edittext.getString(), "1",
                    currency, notes).observe(viewLifecycleOwner) { response ->
                    ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
                    val errorMessage = response.getErrorMessage()
                    if (errorMessage != null) {
                        toastError(errorMessage)
                    } else if (response.getError() != null) {
                        BillWorker.initWorker(requireContext(), description_edittext.getString(),
                                min_amount_edittext.getString(), max_amount_edittext.getString(),
                                bill_date_edittext.getString(), repeatFreq, skip_edittext.getString(),
                                currency, notes)
                        toastOffline(getString(R.string.data_added_when_user_online, "Bill"))
                        handleBack()
                    } else if (response.getResponse() != null) {
                        toastSuccess(getString(R.string.stored_new_bill, description_edittext.getString()))
                        handleBack()
                    }
                }
    }

    private fun updateBill(){
        billViewModel.updateBill(billId, description_edittext.getString(),
                min_amount_edittext.getString(), max_amount_edittext.getString(),
                bill_date_edittext.getString(), getFreq(frequency_exposed_dropdown.getString()),
                skip_edittext.getString(), "1", currency, notes).observe(viewLifecycleOwner) { response ->
            ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
            if (response.getErrorMessage() != null) {
                toastError(response.getErrorMessage())
            } else if (response.getError() != null) {
                toastError(response.getError()?.localizedMessage)
            } else if (response.getResponse() != null) {
                val billName = response.getResponse()?.data?.billAttributes?.name
                toastSuccess(getString(R.string.updated_bill, billName))
                handleBack()
            }
        }
    }

    override fun handleBack() {
        unReveal(dialog_add_bill_layout, true)
    }

}