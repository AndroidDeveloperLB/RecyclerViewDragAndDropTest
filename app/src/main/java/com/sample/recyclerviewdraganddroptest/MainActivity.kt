package com.sample.recyclerviewdraganddroptest

import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
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

    enum class ItemActionState {
        IDLE, LONG_TOUCH_OR_SOMETHING_ELSE, DRAG, SWIPE, HANDLED_LONG_TOUCH
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val items = ArrayList<Item>(100)
        var itemDataCounter = 0L
        items.add(Item.HeaderItem(0L))
        for (i in 0 until 100) {
            items.add(Item.NormalItem(itemDataCounter, itemDataCounter))
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
                        holder.itemView.setBackgroundColor(when (data % 4L) {
                            0L -> 0xffff0000.toInt()
                            1L -> 0xffffff00.toInt()
                            2L -> 0xff00ff00.toInt()
                            else -> 0xff00ffff.toInt()
                        })
                        holder.itemView.textView.text = "item $data"
                    }
                    ITEM_TYPE_HEADER -> {
                    }
                    else -> throw Exception("unknown item type")
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            val touchSlop = ViewConfiguration.get(this@MainActivity).scaledTouchSlop
            val longTouchTimeout = ViewConfiguration.getLongPressTimeout() * 2
            var touchState: ItemActionState = ItemActionState.IDLE
            var lastViewHolderPosHandled: Int? = null
            val handler = Handler()
            val longTouchRunnable = Runnable {
                if (lastViewHolderPosHandled != null && touchState == ItemActionState.LONG_TOUCH_OR_SOMETHING_ELSE) {
                    //                    Log.d("AppLog", "timer timed out to trigger long touch")
                    onItemLongTouch(lastViewHolderPosHandled!!)
                }
            }

            private fun onItemLongTouch(pos: Int) {
                //                Log.d("AppLog", "longTouchTimeout:$longTouchTimeout")
                //                Toast.makeText(this@MainActivity, "long touch on :$pos ", Toast.LENGTH_SHORT).show()
                AlertDialog.Builder(this@MainActivity).setTitle("long touch").setMessage("long tough on $pos").show()
                touchState = ItemActionState.HANDLED_LONG_TOUCH
                lastViewHolderPosHandled = null
                handler.removeCallbacks(longTouchRunnable)
            }

            override fun onChildDrawOver(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                //                Log.d("AppLog", "onChildDrawOver $dX $dY pos:${viewHolder?.adapterPosition} actionState:$actionState isCurrentlyActive:$isCurrentlyActive")
                if (touchState == ItemActionState.LONG_TOUCH_OR_SOMETHING_ELSE && (dX >= touchSlop || dY >= touchSlop)) {
                    lastViewHolderPosHandled = null
                    handler.removeCallbacks(longTouchRunnable)
                    touchState = if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) ItemActionState.DRAG else ItemActionState.SWIPE
                    Log.d("AppLog", "decided it's not a long touch, but $touchState instead")
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                //                Log.d("AppLog", "onSelectedChanged adapterPosition: ${viewHolder?.adapterPosition} actionState:$actionState")
                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_IDLE -> {
                        //user finished drag or long touch
                        if (touchState == ItemActionState.LONG_TOUCH_OR_SOMETHING_ELSE)
                            onItemLongTouch(lastViewHolderPosHandled!!)
                        touchState = ItemActionState.IDLE
                        handler.removeCallbacks(longTouchRunnable)
                        lastViewHolderPosHandled = null
                    }
                    ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.ACTION_STATE_SWIPE -> {
                        if (touchState == ItemActionState.IDLE) {
                            lastViewHolderPosHandled = viewHolder!!.adapterPosition
                            //                            Log.d("AppLog", "setting timer to trigger long touch")
                            handler.removeCallbacks(longTouchRunnable)
                            //started as long touch, but could also be dragging or swiping ...
                            touchState = ItemActionState.LONG_TOUCH_OR_SOMETHING_ELSE
                            handler.postDelayed(longTouchRunnable, longTouchTimeout.toLong())
                        }
                    }
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //                Log.d("AppLog", "onMove")
                if (touchState == ItemActionState.LONG_TOUCH_OR_SOMETHING_ELSE) {
                    lastViewHolderPosHandled = null
                    handler.removeCallbacks(longTouchRunnable)
                    touchState = ItemActionState.DRAG
                }
                if (viewHolder.itemViewType != target.itemViewType)
                    return false
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                //                val item = items.removeAt(fromPosition)
                //                recyclerView.adapter!!.notifyItemRemoved(fromPosition)
                //                items.add(toPosition, item)
                //                recyclerView.adapter!!.notifyItemInserted(toPosition)
                Collections.swap(items, fromPosition, toPosition)
                recyclerView.adapter!!.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun isLongPressDragEnabled(): Boolean = true

            override fun isItemViewSwipeEnabled(): Boolean = false

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.itemViewType == ITEM_TYPE_HEADER)
                    return makeMovementFlags(0, 0)
                //                Log.d("AppLog", "getMovementFlags")
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                val swipeFlags = if (isItemViewSwipeEnabled) ItemTouchHelper.START or ItemTouchHelper.END else 0
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (touchState == ItemActionState.LONG_TOUCH_OR_SOMETHING_ELSE) {
                    lastViewHolderPosHandled = null
                    handler.removeCallbacks(longTouchRunnable)
                    touchState = ItemActionState.DRAG
                }
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
        @Suppress("DEPRECATION")
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
