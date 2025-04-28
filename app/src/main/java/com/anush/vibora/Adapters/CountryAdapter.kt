package com.anush.vibora.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.anush.vibora.Models.CountryModel
import com.anush.vibora.R
import com.anush.vibora.databinding.CountryCodeItemListBinding

class CountryAdapter(
    private val context: Context,
    private val countryList: List<CountryModel>,
    private val listener: OnCountryClickListener
) : RecyclerView.Adapter<CountryAdapter.ViewHolder>() {

    private var selectedCountryName: String = ""

    interface OnCountryClickListener {
        fun onCountryClick(countryCode: String, countryName: String)
    }

    fun setSelectedCountry(countryName: String) {
        selectedCountryName = countryName
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: CountryCodeItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(country: CountryModel) {
            binding.countryCodeNameTxt.text = "${country.countryName} ( ${country.countryCode} )"

            val colorRes = if (country.countryName == selectedCountryName) {
                R.color.purple
            } else {
                R.color.fixed_text_dark
            }
            binding.countryCodeNameTxt.setTextColor(ContextCompat.getColor(context, colorRes))

            binding.root.setOnClickListener {
                selectedCountryName = country.countryName
                notifyDataSetChanged()
                listener.onCountryClick(country.countryCode, country.countryName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CountryCodeItemListBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(countryList[position])
    }

    override fun getItemCount(): Int = countryList.size
}
