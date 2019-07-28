package com.sample.recyclerviewdraganddroptest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.grid_item.view.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    sealed class Item(val id: Long, val itemType: Int) {
        class HeaderItem(id: Long) : Item(id, ITEM_TYPE_HEADER)
        class NormalItem(id: Long, val data: Long) : Item(id, 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val items = ArrayList<Item>(100)
        var itemDataCounter = 0L
        items.add(Item.HeaderItem(0L))
        for (i in 0 until 100) {
            items.add(Item.NormalItem(itemDataCounter.toLong(), itemDataCounter))
            ++itemDataCounter
        }
        val gridLayoutManager = recyclerView.layoutManager as GridLayoutManager
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (recyclerView.adapter!!.getItemViewType(position)) {
                    ITEM_TYPE_HEADER -> gridLayoutManager.spanCount
                    ITEM_TYPE_NORMAL -> 1
                    else -> throw Exception("unknown item type")
                }
            }
        }
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            init {
                setHasStableIds(true)
            }

            override fun getItemViewType(position: Int): Int = items[position].itemType

            override fun getItemId(position: Int): Long = items[position].id

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = when (viewType) {
                    ITEM_TYPE_HEADER -> LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
                    ITEM_TYPE_NORMAL -> LayoutInflater.from(parent.context).inflate(R.layout.grid_item, parent, false)
                    else -> throw Exception("unknown item type")
                }
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun getItemCount() = items.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (getItemViewType(position)) {
                    ITEM_TYPE_NORMAL -> {
                        val data = (items[position] as Item.NormalItem).data
                        holder.itemView.setBackgroundColor(if (data % 2L == 0L) 0xffff0000.toInt() else 0xff00ff00.toInt())
                        holder.itemView.textView.text = "item $data"
                    }
                    ITEM_TYPE_HEADER -> {
                    }
                    else -> throw Exception("unknown item type")
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun isLongPressDragEnabled(): Boolean {
                //                Log.d("AppLog", "isLongPressDragEnabled")
                return true
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                //                Log.d("AppLog", "isItemViewSwipeEnabled")
                return false
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.itemViewType == ITEM_TYPE_HEADER)
                    return makeMovementFlags(0, 0)
                //                Log.d("AppLog", "getMovementFlags")
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                val swipeFlags = if (isItemViewSwipeEnabled) ItemTouchHelper.START or ItemTouchHelper.END else 0
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //                Log.d("AppLog", "onMove")
                if (viewHolder.itemViewType != target.itemViewType)
                    return false
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(items, fromPosition, toPosition)
                recyclerView.adapter!!.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                items.removeAt(position)
                recyclerView.adapter!!.notifyItemRemoved(position)
            }

        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url = "https://github.com/AndroidDeveloperLB/RecyclerViewDragAndDropTest"
        }
        if (url == null)
            return true
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(intent)
        return true
    }

    companion object {
        const val ITEM_TYPE_HEADER = 0
        const val ITEM_TYPE_NORMAL = 1
    }
}
