package xyz.hisname.fireflyiii.ui.markdown

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.android.synthetic.main.fragment_markdown.*
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.data.local.pref.AppPref
import xyz.hisname.fireflyiii.repository.MarkdownViewModel
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.util.extension.getViewModel

class MarkdownFragment: BaseFragment() {

    private val toolbar by lazy {requireActivity().findViewById<Toolbar>(R.id.activity_toolbar) }
    private val appPref by lazy { AppPref(PreferenceManager.getDefaultSharedPreferences(requireContext())) }
    private val markdownViewModel by lazy { getViewModel(MarkdownViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_markdown, container)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setWidget()
        parseText()
        handleClick()
    }

    private fun setWidget(){
        toolbar.visibility = View.GONE
        discardButton.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_close
                    colorRes = R.color.md_white_1000
                    sizeDp = 12
                },null, null, null)
        doneButton.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_done
                    colorRes = R.color.md_white_1000
                    sizeDp =12
                },null, null, null)
        boldMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_format_bold
                    colorRes = setIconColor()
                    sizeDp = 18
                })
        italicMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_format_italic
                    colorRes = setIconColor()
                    sizeDp = 18
                })
        hyperlinkMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_insert_link
                    colorRes = setIconColor()
                    sizeDp = 18
                })
        strikeThroughMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_format_strikethrough
                    colorRes = setIconColor()
                    sizeDp = 18
                })
        quoteMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).apply {
                    icon =GoogleMaterial.Icon.gmd_format_quote
                    colorRes = setIconColor()
                    sizeDp = 18
                })
        bulletMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).apply {
                    icon = GoogleMaterial.Icon.gmd_format_list_bulleted
                    colorRes = setIconColor()
                    sizeDp = 18
                })
        editableText.setText(markdownViewModel.markdownText.value)
        displayText.text = markdownViewModel.markdownText.value
        discardButton.setOnClickListener {
            handleBack()
        }
        doneButton.setOnClickListener {
            markdownViewModel.markdownText.postValue(editableText.getString())
            handleBack()
        }
        discardButton.setBackgroundColor(getCompatColor(R.color.colorPrimary))
        doneButton.setBackgroundColor(getCompatColor(R.color.colorPrimary))
    }

    private fun setIconColor(): Int{
        return if(appPref.nightModeEnabled){
            R.color.md_white_1000
        } else {
            R.color.md_black_1000
        }
    }

    private fun parseText(){
        val extensions = arrayListOf(StrikethroughExtension.create(), AutolinkExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        editableText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(editable: Editable) {

            }

            override fun beforeTextChanged(charsequence: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun onTextChanged(charsequence: CharSequence, start: Int, before: Int, count: Int) {
                val document = parser.parse(charsequence.toString())
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    displayText.text = Html.fromHtml(renderer.render(document), Html.FROM_HTML_MODE_COMPACT)
                } else {
                    displayText.text = Html.fromHtml(renderer.render(document))
                }
            }

        })

    }

    // https://github.com/k0shk0sh/FastHub/blob/eb13021b9a45ac1ae29815b48247647005a661bd/app/src/main/java/com/fastaccess/provider/markdown/MarkDownProvider.java
    private fun handleClick(){
        boldMarkdown.setOnClickListener {
            val markdownText = editableText.getString()
            val markdownTextStart = editableText.selectionStart
            val markdownTextEnd = editableText.selectionEnd
            val markdownSubString = markdownText.substring(markdownTextStart, markdownTextEnd)
            val result = "**$markdownSubString** "
            editableText.text.replace(markdownTextStart, markdownTextEnd, result)
            editableText.setSelection(result.length + markdownTextStart - 3)
        }
        italicMarkdown.setOnClickListener {
            val markdownText = editableText.getString()
            val markdownTextStart = editableText.selectionStart
            val markdownTextEnd = editableText.selectionEnd
            val markdownSubString = markdownText.substring(markdownTextStart, markdownTextEnd)
            val result = "_" + markdownSubString + "_ "
            editableText.text.replace(markdownTextStart, markdownTextEnd, result)
            editableText.setSelection(result.length + markdownTextStart - 2)
        }
        strikeThroughMarkdown.setOnClickListener {
            val markdownText = editableText.getString()
            val markdownTextStart = editableText.selectionStart
            val markdownTextEnd = editableText.selectionEnd
            val markdownSubString = markdownText.substring(markdownTextStart, markdownTextEnd)
            val result = "~~$markdownSubString~~ "
            editableText.text.replace(markdownTextStart, markdownTextEnd, result)
            editableText.setSelection(result.length + markdownTextStart - 3)
        }
        quoteMarkdown.setOnClickListener {
            val markdownText = editableText.getString()
            val markdownTextStart = editableText.selectionStart
            val markdownTextEnd = editableText.selectionEnd
            val markdownSubString = markdownText.substring(markdownTextStart, markdownTextEnd)
            val result = if (hasNewLine(markdownText, markdownTextStart)) {
                "> $markdownSubString"
            } else {
                "\n> $markdownSubString"
            }
            editableText.text.replace(markdownTextStart, markdownTextEnd, result)
            editableText.setSelection(result.length + markdownTextStart)
        }
        bulletMarkdown.setOnClickListener {
            editableText.append("•")
        }
        hyperlinkMarkdown.setOnClickListener {
            val layoutView = layoutInflater.inflate(R.layout.dialog_hyperlink, null)
            val urlText = layoutView.findViewById<EditText>(R.id.linktextEditText)
            urlText.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext()).apply {
                icon = GoogleMaterial.Icon.gmd_format_underlined
                sizeDp = 16
            },null, null, null)
            val url = layoutView.findViewById<EditText>(R.id.linkEditText)
            url.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext()).apply {
                icon = GoogleMaterial.Icon.gmd_insert_link
                sizeDp = 16
            },null, null, null)
            val alert = AlertDialog.Builder(requireContext())
            alert.apply {
                setTitle(R.string.insert_link)
                setView(layoutView)
                setPositiveButton(android.R.string.ok) { dialogInterface, which ->
                    editableText.append("[" + urlText.getString() + "]" + "(" + url.getString() + ")")
                }
                setNegativeButton(android.R.string.cancel){ dialogInterface, which ->

                }
                show()
            }

        }
    }

    private fun hasNewLine(source: String, selectionStart: Int): Boolean{
        try {
            if(source.isEmpty()){
                return true
            }
            val textSource = source.substring(0, selectionStart)
            return textSource[source.length - 1].equals(10)
        } catch(e: StringIndexOutOfBoundsException){
            return false
        }
    }

    override fun handleBack() {
        parentFragmentManager.popBackStack()
        toolbar.visibility = View.VISIBLE
        hideKeyboard()
    }
}