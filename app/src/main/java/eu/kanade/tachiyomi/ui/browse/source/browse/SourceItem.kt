package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.tfcporciuncula.flow.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.source_comfortable_grid_item.view.outside
import kotlinx.android.synthetic.main.source_grid_item.view.card
import kotlinx.android.synthetic.main.source_grid_item.view.gradient

class SourceItem(val manga: Manga, private val catalogueAsList: Preference<Int>) :
    AbstractFlexibleItem<SourceHolder>() {

    override fun getLayoutRes(): Int {
        return when (catalogueAsList.get()) {
            0 -> R.layout.source_grid_item
            1 -> R.layout.source_list_item
            else -> R.layout.source_comfortable_grid_item
        }
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): SourceHolder {
        val parent = adapter.recyclerView
        return if (parent is AutofitRecyclerView) {
            val coverHeight = parent.itemWidth / 3 * 4
            if (catalogueAsList.get() == 0) {
                view.apply {
                    card.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, coverHeight
                    )
                    gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, coverHeight / 2, Gravity.BOTTOM
                    )
                }
                SourceGridHolder(view, adapter)
            } else {
                view.apply {
                    card.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, coverHeight
                    )
                    gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, coverHeight / 2, Gravity.BOTTOM
                    )
                    outside.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, (coverHeight * 1.2).toInt()
                    )
                }
                SourceComfortableGridHolder(view, adapter)
            }
        } else {
            SourceListHolder(view, adapter)
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: SourceHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.onSetValues(manga)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is SourceItem) {
            return manga.id!! == other.manga.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }
}
