package com.v2ray.actinium.ui

import android.app.ProgressDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.dinuscxj.itemdecoration.LinearDividerItemDecoration
import com.v2ray.actinium.R
import com.v2ray.actinium.defaultDPreference
import com.v2ray.actinium.util.AppInfo
import com.v2ray.actinium.util.AppManagerUtil
import kotlinx.android.synthetic.main.activity_bypass_list.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.text.Collator
import java.util.*

class BypassListActivity : BaseActivity() {
    companion object {
        const val PREF_BYPASS_LIST_SET = "pref_bypass_list_set"
    }

    private var adapter: BypassListRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bypass_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dividerItemDecoration = LinearDividerItemDecoration(
                this, LinearDividerItemDecoration.LINEAR_DIVIDER_VERTICAL)
        recycler_view.addItemDecoration(dividerItemDecoration)

        val dialog = ProgressDialog(this)
        dialog.isIndeterminate = true
        dialog.setCancelable(false)
        dialog.setMessage(getString(R.string.msg_dialog_progress))
        dialog.show()
        AppManagerUtil.rxLoadNetworkAppList(this)
                .subscribeOn(Schedulers.io())
                .map {
                    val comparator = object : Comparator<AppInfo> {
                        val collator = Collator.getInstance()
                        override fun compare(o1: AppInfo, o2: AppInfo)
                                = collator.compare(o1.appName, o2.appName)
                    }
                    it.sortedWith(comparator)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val blacklist = defaultDPreference.getPrefStringSet(PREF_BYPASS_LIST_SET, null)
                    adapter = BypassListRecyclerAdapter(it, blacklist)
                    recycler_view.adapter = adapter
                    dialog.dismiss()
                }
    }

    override fun onPause() {
        super.onPause()
        adapter?.let {
            defaultDPreference.setPrefStringSet(PREF_BYPASS_LIST_SET, it.blacklist)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bypass_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.select_all -> adapter?.let {
            val pkgNames = it.apps.map { it.packageName }
            if (it.blacklist.containsAll(pkgNames))
                it.blacklist.clear()
            else
                it.blacklist.addAll(pkgNames)

            it.notifyDataSetChanged()
            true
        } ?: false

        else -> super.onOptionsItemSelected(item)
    }
}