package pl.gotrainey.gotrainey.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.gotrainey.gotrainey.R

class CartAdapter(
    private val cartsList: List<Map<String, Any>>
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_places, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cartName = cartsList[position]["cart_name"]
        holder.cartName.text = cartName.toString()
        Log.d("CHANGE BIND", cartsList.toString())
//        holder.itemView.setOnClickListener {
//            onItemClick(cartName.toString())
//        }
    }

    override fun getItemCount(): Int = cartsList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cartName: TextView = itemView.findViewById(R.id.cart)
    }
}