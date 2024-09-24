package pl.gotrainey.gotrainey.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.gotrainey.gotrainey.R

class CartsAdapter(
    private val suggestionList: List<Map<String, Any>>
) : RecyclerView.Adapter<CartsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cartName = suggestionList[position]["number"]
        holder.textView.text = "WAGON ${cartName.toString()}"

    }

    override fun getItemCount(): Int = suggestionList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.cartName)
    }
}