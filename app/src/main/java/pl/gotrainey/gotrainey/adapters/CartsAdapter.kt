package pl.gotrainey.gotrainey.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.gotrainey.gotrainey.R

class CartsAdapter(
    private val cartsList: List<Map<String, Any>>
) : RecyclerView.Adapter<CartsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cartName = cartsList[position]["number"]
        val seatsList = cartsList[position]["seats"] as List<Any>
        holder.cartName.text = "WAGON\n${cartName.toString()}"

        val gridAdapter = SeatsAdapter(seatsList)
        holder.seatsGrid.adapter = gridAdapter

        val gridLayoutManager = GridLayoutManager(holder.itemView.context, 4)
        holder.seatsGrid.layoutManager = gridLayoutManager
    }

    override fun getItemCount(): Int = cartsList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cartName: TextView = itemView.findViewById(R.id.cartName)
        val seatsGrid: RecyclerView = itemView.findViewById(R.id.seatsGrid)
    }
}