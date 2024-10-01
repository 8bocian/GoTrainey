package pl.gotrainey.gotrainey.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject

class StationAdapter(context: Context, resource: Int, private val originalItems: List<String>) :
    ArrayAdapter<String>(context, resource, originalItems) {

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = originalItems
                results.count = originalItems.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    clear()
                    addAll(results.values as List<String>)
                }
            }
        }
    }
}