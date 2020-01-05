package io.github.takusan23.nicohome.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.github.takusan23.nicohome.MainActivity
import io.github.takusan23.nicohome.NicoVideo.NicoVideo
import io.github.takusan23.nicohome.R

class MylistAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<String>>) :
    RecyclerView.Adapter<MylistAdapter.ViewHolder>() {

    lateinit var nicoVideo: NicoVideo

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.mylist_adapter, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.cardView.context

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val name = item.get(1)
        val videoId = item.get(2)
        val thumbnail = item.get(3)

        holder.apply {
            titleTextView.text = name
            idTextView.text = videoId

            //押したら再生？
            cardView.setOnClickListener {
                //MainActivity
                val mainActivity = context as MainActivity
                nicoVideo.mainActivity = mainActivity
                nicoVideo.play(videoId)
            }

            //さむね
            Glide.with(imageView)
                .load(thumbnail)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10))) //←この一行追加
                .into(imageView)

        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView
        var idTextView: TextView
        var cardView: CardView
        var imageView: ImageView

        init {
            titleTextView = itemView.findViewById(R.id.mylist_adapter_title_textview)
            idTextView = itemView.findViewById(R.id.mylist_adapter_id_textview)
            cardView = itemView.findViewById(R.id.mylist_adapter_parent_cardview)
            imageView = itemView.findViewById(R.id.mylist_adapter_imageview)
        }
    }
}