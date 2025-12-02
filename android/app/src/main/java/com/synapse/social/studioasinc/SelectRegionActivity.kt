package com.synapse.social.studioasinc

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectRegionActivity : BaseActivity() {

    companion object {
        const val EXTRA_SELECTED_REGION = "selected_region"
        const val EXTRA_CURRENT_REGION = "current_region"
    }

    private lateinit var top: LinearLayout
    private lateinit var back: ImageView
    private lateinit var title: TextView
    private lateinit var searchInput: EditText
    private lateinit var regionsList: RecyclerView
    
    private val allRegions = listOf(
        "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Argentina", "Armenia", "Australia",
        "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium",
        "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei",
        "Bulgaria", "Burkina Faso", "Burundi", "Cambodia", "Cameroon", "Canada", "Cape Verde",
        "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica",
        "Croatia", "Cuba", "Cyprus", "Czech Republic", "Denmark", "Djibouti", "Dominica", "Dominican Republic",
        "East Timor", "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia",
        "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada",
        "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hungary", "Iceland", "India",
        "Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jordan",
        "Kazakhstan", "Kenya", "Kiribati", "North Korea", "South Korea", "Kuwait", "Kyrgyzstan", "Laos", "Latvia",
        "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Macedonia",
        "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Mauritania",
        "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco",
        "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua",
        "Niger", "Nigeria", "Norway", "Oman", "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea",
        "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda",
        "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent and the Grenadines", "Samoa", "San Marino",
        "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore",
        "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Sudan", "Spain", "Sri Lanka",
        "Sudan", "Suriname", "Swaziland", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania",
        "Thailand", "Togo", "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu",
        "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan",
        "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
    )
    
    private var filteredRegions = allRegions.toMutableList()
    private var currentRegion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_region)
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        top = findViewById(R.id.top)
        back = findViewById(R.id.back)
        title = findViewById(R.id.title)
        searchInput = findViewById(R.id.search_input)
        regionsList = findViewById(R.id.regions_list)
        
        currentRegion = intent.getStringExtra(EXTRA_CURRENT_REGION) ?: ""
        
        back.setOnClickListener { onBackPressed() }
        
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                filterRegions(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun initializeLogic() {
        stateColor(
            ContextCompat.getColor(this, R.color.white),
            ContextCompat.getColor(this, R.color.white)
        )
        viewGraphics(
            back,
            ContextCompat.getColor(this, R.color.white),
            ContextCompat.getColor(this, R.color.light_grey),
            300.0,
            0.0,
            Color.TRANSPARENT
        )
        searchInput.background = createStrokeDrawable(
            28,
            3,
            ContextCompat.getColor(this, R.color.light_grey),
            ContextCompat.getColor(this, R.color.white)
        )
        top.elevation = 4f
        
        title.text = "Select Region"
        
        regionsList.layoutManager = LinearLayoutManager(this)
        regionsList.adapter = RegionsAdapter(filteredRegions)
    }

    private fun filterRegions(query: String) {
        filteredRegions.clear()
        if (query.isEmpty()) {
            filteredRegions.addAll(allRegions)
        } else {
            filteredRegions.addAll(allRegions.filter { 
                it.contains(query, ignoreCase = true) 
            })
        }
        regionsList.adapter?.notifyDataSetChanged()
    }

    private fun selectRegion(region: String) {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_SELECTED_REGION, region)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun stateColor(statusColor: Int, navigationColor: Int) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
    }

    private fun viewGraphics(view: View, onFocus: Int, onRipple: Int, radius: Double, stroke: Double, strokeColor: Int) {
        val gradientDrawable = GradientDrawable().apply {
            setColor(onFocus)
            cornerRadius = radius.toFloat()
            setStroke(stroke.toInt(), strokeColor)
        }
        view.background = gradientDrawable
    }

    private fun createStrokeDrawable(radius: Int, stroke: Int, strokeColor: Int, fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setStroke(stroke, strokeColor)
            setColor(fillColor)
        }
    }

    inner class RegionsAdapter(
        private val data: List<String>
    ) : RecyclerView.Adapter<RegionsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val region = data[position]
            holder.textView.text = region
            holder.textView.setPadding(48, 32, 48, 32)
            holder.textView.textSize = 16f
            
            if (region == currentRegion) {
                holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.status_indicator_online))
                holder.textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.overlay_dark))
                holder.textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            
            holder.itemView.setOnClickListener {
                selectRegion(region)
            }
        }

        override fun getItemCount(): Int = data.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(android.R.id.text1)
        }
    }
}
