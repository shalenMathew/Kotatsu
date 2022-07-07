package org.koitharu.kotatsu.bookmarks.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.Disposable
import coil.size.Scale
import coil.util.CoilUtils
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.model.BookmarksGroup
import org.koitharu.kotatsu.databinding.ItemBookmarksGroupBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.clearItemDecorations
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.referer

fun bookmarksGroupAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	sharedPool: RecyclerView.RecycledViewPool,
	selectionController: SectionedSelectionController<Manga>,
	bookmarkClickListener: OnListItemClickListener<Bookmark>,
	groupClickListener: OnListItemClickListener<BookmarksGroup>,
) = adapterDelegateViewBinding<BookmarksGroup, ListModel, ItemBookmarksGroupBinding>(
	{ layoutInflater, parent -> ItemBookmarksGroupBinding.inflate(layoutInflater, parent, false) }
) {

	val viewListenerAdapter = object : View.OnClickListener, View.OnLongClickListener {
		override fun onClick(v: View) = groupClickListener.onItemClick(item, v)
		override fun onLongClick(v: View) = groupClickListener.onItemLongClick(item, v)
	}
	var imageRequest: Disposable? = null
	val adapter = BookmarksAdapter(coil, lifecycleOwner, bookmarkClickListener)
	binding.recyclerView.setRecycledViewPool(sharedPool)
	binding.recyclerView.adapter = adapter
	val spacingDecoration = SpacingItemDecoration(context.resources.getDimensionPixelOffset(R.dimen.grid_spacing))
	binding.recyclerView.addItemDecoration(spacingDecoration)
	binding.root.setOnClickListener(viewListenerAdapter)
	binding.root.setOnLongClickListener(viewListenerAdapter)

	bind { payloads ->
		if (payloads.isEmpty()) {
			binding.recyclerView.clearItemDecorations()
			binding.recyclerView.addItemDecoration(spacingDecoration)
			selectionController.attachToRecyclerView(item.manga, binding.recyclerView)
		}
		imageRequest = binding.imageViewCover.newImageRequest(item.manga.coverUrl)
			.referer(item.manga.publicUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.scale(Scale.FILL)
			.allowRgb565(true)
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
		binding.textViewTitle.text = item.manga.title
		adapter.items = item.bookmarks
	}

	onViewRecycled {
		imageRequest?.dispose()
		imageRequest = null
		CoilUtils.dispose(binding.imageViewCover)
		binding.imageViewCover.setImageDrawable(null)
	}
}