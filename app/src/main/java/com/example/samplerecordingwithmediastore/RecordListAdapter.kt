package com.example.samplerecordingwithmediastore

import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.samplerecordingwithmediastore.databinding.ListItemBinding

class RecordListAdapter : RecyclerView.Adapter<RecordListAdapter.RecordViewHolder>() {
    var recordList = mutableListOf<Record>()
    var mediaPlayer: MediaPlayer? = null

    fun setNewRecordList(newList: List<Record>) {
        recordList.clear()
        recordList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = recordList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(
            ListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
       holder.bind(recordList[position])
    }

    inner class RecordViewHolder(private val binding: ListItemBinding): RecyclerView.ViewHolder(binding.root) {
        private var recordUri: Uri? = null

        init {
            itemView.setOnClickListener {
                if (mediaPlayer != null) {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
                mediaPlayer = MediaPlayer.create(itemView.context, recordUri)
                mediaPlayer?.start()
            }
        }

        fun bind(record: Record) {
            binding.pathTextView.text = "${record.id} / ${record.displayName}"
            this.recordUri = record.getRecordUri()
        }

    }

}
