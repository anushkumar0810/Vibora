package com.anush.vibora.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.anush.vibora.Adapters.CountryAdapter
import com.anush.vibora.Models.CountryModel
import com.anush.vibora.databinding.DialogCountryPickerBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections

class CountryPickerDialog(
    context: Context,
    private val preSelectedCountryName: String?,
    private val listener: OnCountrySelectedListener
) : Dialog(context), CountryAdapter.OnCountryClickListener {

    private lateinit var binding: DialogCountryPickerBinding
    private lateinit var countryAdapter: CountryAdapter
    private val countryList = mutableListOf<CountryModel>()

    interface OnCountrySelectedListener {
        fun onCountrySelected(countryCode: String, countryName: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCountryPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        setupRecyclerView()
        fetchCountryData()
    }

    private fun setupRecyclerView() {
        binding.countryRecycler.layoutManager = LinearLayoutManager(context)
        countryAdapter = CountryAdapter(context, countryList, this)
        binding.countryRecycler.adapter = countryAdapter
    }

    private fun fetchCountryData() {
        binding.countryProgress.visibility = android.view.View.VISIBLE
        val url = "https://restcountries.com/v3.1/all"
        val queue = Volley.newRequestQueue(context)

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                binding.countryProgress.visibility = android.view.View.GONE
                parseCountryData(response)
            },
            { error ->
                binding.countryProgress.visibility = android.view.View.GONE
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
                Log.e("CountryPickerDialog", "Error: ${error.message}")
            })

        queue.add(request)
    }

    private fun parseCountryData(response: JSONArray) {
        try {
            for (i in 0 until response.length()) {
                val countryObject = response.getJSONObject(i)
                val countryName = countryObject.getJSONObject("name").getString("common")
                var countryCode = ""

                if (countryObject.has("idd")) {
                    val iddObject = countryObject.getJSONObject("idd")
                    val root = iddObject.optString("root", "")
                    val suffixes = iddObject.optJSONArray("suffixes")

                    if (suffixes != null && suffixes.length() > 0) {
                        val suffix = suffixes.getString(0)
                        countryCode = root + suffix
                    }
                }

                if (countryCode.isNotEmpty()) {
                    countryList.add(CountryModel(countryName, countryCode))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                countryList.sortBy { it.countryName }
            }

            countryAdapter.notifyDataSetChanged()

            preSelectedCountryName?.takeIf { it.isNotEmpty() }?.let {
                countryAdapter.setSelectedCountry(it)
                scrollToSelectedCountry(it)
            }

        } catch (e: Exception) {
            Log.e("CountryPickerDialog", "parseCountryData: ", e)
        }
    }

    private fun scrollToSelectedCountry(countryName: String) {
        countryList.indexOfFirst { it.countryName.equals(countryName, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?.let { binding.countryRecycler.scrollToPosition(it) }
    }

    override fun onCountryClick(countryCode: String, countryName: String) {
        listener.onCountrySelected(countryCode, countryName)
        dismiss()
    }
}
