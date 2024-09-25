package pl.gotrainey.gotrainey.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.gotrainey.gotrainey.R

class SeatsAdapter(
    private val seatsList: List<Any>
) : RecyclerView.Adapter<SeatsAdapter.GridViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.seat, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val place = seatsList[position]
        Log.d("SEAT", place.toString())
        holder.seatName.text = place.toString()
        holder.seatName.post {
            holder.seatName.height = holder.seatName.width
        }
    }

    override fun getItemCount(): Int = seatsList.size

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val seatName: TextView = itemView.findViewById(R.id.seatName)
    }
}