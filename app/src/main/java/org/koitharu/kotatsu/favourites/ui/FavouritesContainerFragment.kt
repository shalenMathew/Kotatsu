package org.koitharu.kotatsu.favourites.ui

import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.FragmentFavouritesBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.showPopupMenu
import java.util.*
import kotlin.collections.ArrayList

class FavouritesContainerFragment : BaseFragment<FragmentFavouritesBinding>(),
	FavouritesTabLongClickListener, CategoriesEditDelegate.CategoriesEditCallback {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()
	private val editDelegate by lazy(LazyThreadSafetyMode.NONE) {
		CategoriesEditDelegate(requireContext(), this)
	}
	private var adapterState: Parcelable? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentFavouritesBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = FavouritesPagerAdapter(this, this)
		binding.pager.adapter = adapter
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()

		viewModel.categories.observe(viewLifecycleOwner, ::onCategoriesChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)
		(savedInstanceState?.getParcelable(KEY_ADAPTER_STATE) ?: adapterState)?.let {
			(binding.pager.adapter as FavouritesPagerAdapter).restoreState(it)
		}
	}

	override fun onDestroyView() {
		adapterState = (binding.pager.adapter as? FavouritesPagerAdapter)?.saveState()
		super.onDestroyView()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		adapterState = (bindingOrNull()?.pager?.adapter as? FavouritesPagerAdapter)?.saveState()
			?: adapterState
		outState.putParcelable(KEY_ADAPTER_STATE, adapterState)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.tabs.updatePadding(
			left = insets.left,
			right = insets.right
		)
	}

	private fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		val data = ArrayList<FavouriteCategory>(categories.size + 1)
		data += FavouriteCategory(0L, getString(R.string.all_favourites), -1, Date())
		data += categories
		(binding.pager.adapter as? FavouritesPagerAdapter)?.replaceData(data)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_favourites, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_categories -> {
			context?.let {
				startActivity(CategoriesActivity.newIntent(it))
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun getTitle(): CharSequence? {
		return context?.getString(R.string.favourites)
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
	}

	override fun onTabLongClick(tabView: View, category: FavouriteCategory): Boolean {
		val menuRes = if (category.id == 0L) R.menu.popup_category_empty else R.menu.popup_category
		tabView.showPopupMenu(menuRes) {
			when (it.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(category)
				R.id.action_rename -> editDelegate.renameCategory(category)
				R.id.action_create -> editDelegate.createCategory()
			}
			true
		}
		return true
	}

	override fun onDeleteCategory(category: FavouriteCategory) {
		viewModel.deleteCategory(category.id)
	}

	override fun onRenameCategory(category: FavouriteCategory, newName: String) {
		viewModel.renameCategory(category.id, newName)
	}

	override fun onCreateCategory(name: String) {
		viewModel.createCategory(name)
	}

	companion object {

		private const val KEY_ADAPTER_STATE = "adapter_state"

		fun newInstance() = FavouritesContainerFragment()
	}
}