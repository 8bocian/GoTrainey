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

    // This method is not used for filtering but just returns the original items
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Skip filtering and return all items
                val results = FilterResults()
                results.values = originalItems // Return the original list
                results.count = originalItems.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                // No action needed; we are not filtering
                if (results != null && results.count > 0) {
                    clear()
                    addAll(results.values as List<String>)
                }
            }
        }
    }
}