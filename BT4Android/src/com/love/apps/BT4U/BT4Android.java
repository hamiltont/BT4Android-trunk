package com.love.apps.BT4U;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.Leadbolt.AdController;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;



public class BT4Android extends SherlockFragmentActivity {

	TabHost mTabHost;
	ViewPager  mViewPager;
	TabsAdapter mTabsAdapter;
	private int loaded = 0;
	private RelativeLayout layout = null;
	private Activity act = null;
	public static final String PREFS_NAME = "MyPrefsFile";

	public static GoogleAnalyticsTracker tracker;
	
	static GoogleAnalyticsTracker getTracker()
	{
		return tracker;
	}
	private 		AdController myController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		checkForOldFavorites();
		setupGoogleAnalytics();
		setContentView(R.layout.fragment_tabs_pager);
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();
		mViewPager = (ViewPager)findViewById(R.id.pager);
		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
		mTabsAdapter.addTab(mTabHost.newTabSpec("routes").setIndicator("Routes"),
				Routes.class, null);
		mTabsAdapter.addTab(mTabHost.newTabSpec("favorites").setIndicator("Favorites"),
				Favorites.class, null);
		mTabsAdapter.addTab(mTabHost.newTabSpec("updates").setIndicator("BT Updates"),
				Updates.class, null);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String tab = sharedPref.getString("tab", "0");
		mTabHost.setCurrentTab(Integer.parseInt(tab));

		for(int j=0 ; j < mTabHost.getTabWidget().getChildCount() ; j++) 
		{
			TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(j).findViewById(android.R.id.title);
			tv.setTextColor(0xff990000);
		}         if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

	}

	private void setupGoogleAnalytics() {
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.startNewSession("UA-34219177-1", 10, this);
		tracker.trackPageView("/bt4android");
		tracker.dispatch();		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String tab = sharedPref.getString("tab", "0");
		mTabHost.setCurrentTab(Integer.parseInt(tab));

	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost.  It relies on a
	 * trick.  Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show.  This is not sufficient for switching
	 * between pages.  So instead we make the content part of the tab host
	 * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
	 * view to show as the tab content.  It listens to changes in tabs, and takes
	 * care of switch to the correct paged in the ViewPager whenever the selected
	 * tab changes.
	 */
	public static class TabsAdapter extends FragmentPagerAdapter
	implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final TabHost mTabHost;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final String tag;
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(String _tag, Class<?> _class, Bundle _args) {
				tag = _tag;
				clss = _class;
				args = _args;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context mContext;

			public DummyTabFactory(Context context) {
				mContext = context;
			}

			public View createTabContent(String tag) {
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}
		}

		public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mTabHost = tabHost;
			mViewPager = pager;
			mTabHost.setOnTabChangedListener(this);
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
			tabSpec.setContent(new DummyTabFactory(mContext));
			String tag = tabSpec.getTag();

			TabInfo info = new TabInfo(tag, clss, args);
			mTabs.add(info);
			mTabHost.addTab(tabSpec);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(), info.args);
		}

		public void onTabChanged(String tabId) {
			int position = mTabHost.getCurrentTab();
			Log.i("TABS", position + " ");
			mViewPager.setCurrentItem(position);
			if(position == 1) {
				BT4Android.getTracker().trackPageView("/favorites");
				BT4Android.getTracker().dispatch();
				Favorites favs = Favorites.getFavorites();
				if(favs == null)
				{
					return;
				}
				favs.updateFavorites();
			}
			else if(position==0)
			{
				BT4Android.getTracker().trackPageView("/routes");
				BT4Android.getTracker().dispatch();
				Routes.checkMenu();
			}
			
		}

		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		public void onPageSelected(int position) {
			// Unfortunately when TabHost changes the current tab, it kindly
			// also takes care of putting focus on it when not in touch mode.
			// The jerk.
			// This hack tries to prevent this from pulling focus out of our
			// ViewPager.
			TabWidget widget = mTabHost.getTabWidget();
			int oldFocusability = widget.getDescendantFocusability();
			widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			mTabHost.setCurrentTab(position);
			widget.setDescendantFocusability(oldFocusability);
		}

		public void onPageScrollStateChanged(int state) {
		}
	}
	
	@Override
	  protected void onDestroy() {
	   
		//stop showing ad
		if(myController!=null)
		myController.destroyAd();
	    // Stop the tracker when it is no longer needed.
	    tracker.stopSession();
	    super.onDestroy();
	  }

	private void checkForOldFavorites()
	{
		Log.d("BT4ANDROID STORAGE", "Entering function");
		boolean isSDpresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
		if(isSDpresent){
			Log.d("BT4ANDROID STORAGE", "SD Present");

			try {
				String path =  Environment.getExternalStorageDirectory() + "/BT4U/";
				File root = new File(path);
				File f = new File(root, "favorites.txt");
				if(!f.exists())
				{
					//file doesn't exist we're done
					Log.d("BT4ANDROID STORAGE", "Old Favorites folder does not exist" );

					return;
				}
				String content= "";
				FileReader r = new FileReader(f);
				this.openFileOutput("favorites.txt", MODE_APPEND);
				//BufferedReader br = new BufferedReader(new InputStreamReader(this.openFileInput("favorites.txt"), "UTF-8"));

				BufferedReader br = new BufferedReader(r);
				String line = null;
				while((line = br.readLine()) != null){
					content += line;
					content += "\n";
					Log.d("BT4ANDROID STORAGE",(line));
				}
				Log.d("BT4ANDROID STORAGE", "favorites.txt is delted: " + f.delete()  );
				Log.d("BT4ANDROID STORAGE", "Folder bt4u is deleted: " + root.delete() + " " );
				FileOutputStream fos = this.openFileOutput("favorites.txt", Context.MODE_PRIVATE);
				fos.write(content.getBytes());
				fos.close();
				br.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	

}
