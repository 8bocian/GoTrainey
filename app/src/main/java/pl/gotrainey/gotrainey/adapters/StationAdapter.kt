package pl.gotrainey.gotrainey.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject

class StationAdapter(
    private val suggestionList: List<Map<String, Any>>,
    private val recyclerView: RecyclerView,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<StationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestionList[position]["name"]
        holder.textView.text = suggestion.toString()

        holder.itemView.setOnClickListener {
            onItemClick(suggestion.toString())
        }
    }

    override fun getItemCount(): Int = suggestionList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }
}