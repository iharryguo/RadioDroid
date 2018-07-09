package net.programmierecke.radiodroid2.lifecycle;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rustamg.filedialogs.FileDialog;
import com.rustamg.filedialogs.OpenFileDialog;
import com.rustamg.filedialogs.SaveFileDialog;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.CastHandler;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.FragmentAlarm;
import net.programmierecke.radiodroid2.FragmentHistory;
import net.programmierecke.radiodroid2.FragmentPlayer;
import net.programmierecke.radiodroid2.FragmentRecordings;
import net.programmierecke.radiodroid2.FragmentSettings;
import net.programmierecke.radiodroid2.FragmentStarred;
import net.programmierecke.radiodroid2.FragmentTabs;
import net.programmierecke.radiodroid2.HistoryManager;
import net.programmierecke.radiodroid2.MPDClient;
import net.programmierecke.radiodroid2.PlayerServiceUtil;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioBrowserServerManager;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.constant.ConstCommon;
import net.programmierecke.radiodroid2.data.MPDServer;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.presenter.ILifeCyclePresenter;
import net.programmierecke.radiodroid2.splash.SplashManager;
import net.programmierecke.threadpool.ThreadPool;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by harryguo on 2018/7/9.
 */

public class LifeCyclePresenter implements ILifeCyclePresenter {
	private static final String TAG = "LifeCyclePresenter";
	private ActivityMain mActivity;
	private boolean mMainPageInited = false;

	private SearchView mSearchView;
	DrawerLayout mDrawerLayout;
	NavigationView mNavigationView;
	BottomNavigationView mBottomNavigationView;
	FragmentManager mFragmentManager;

	MenuItem menuItemSearch;
	MenuItem menuItemDelete;
	MenuItem menuItemAlarm;
	MenuItem menuItemSave;
	MenuItem menuItemLoad;

	private SharedPreferences sharedPref;
	private int selectedMenuItem;
	private boolean instanceStateWasSaved;
	private Date lastExitTry;
	BroadcastReceiver broadcastReceiver;

	public LifeCyclePresenter(ActivityMain mainActivity)
	{
		mActivity = mainActivity;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		// 先展示splash，同时初始化主页面，主页面初始化好了之后，再显示主页面，避免长时间白屏
		showSplash();
		ThreadPool.getInstance().uiHandler().postDelayed(new Runnable() {
			@Override
			public void run() {
				showMainPage(savedInstanceState);
			}
		}, 2000);
		CastHandler.onCreate(mActivity);
	}

	private void showSplash() {
		View splashView = SplashManager.getInstance().getSplashView(mActivity);
		LifeCycleStatus.isSplashShowing = true;
		mActivity.setContentView(splashView);
	}

	private void showMainPage(Bundle savedInstanceState) {
		if (sharedPref == null) {
			PreferenceManager.setDefaultValues(mActivity, R.xml.preferences, false);
			sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
		}
		mActivity.setTheme(Utils.getThemeResId(mActivity));
		mActivity.setContentView(R.layout.layout_main);

		try {
			File dir = new File(mActivity.getFilesDir().getAbsolutePath());
			if (dir.isDirectory()) {
				String[] children = dir.list();
				for (String aChildren : children) {
					if (BuildConfig.DEBUG) {
						Log.d("MAIN", "delete file:" + aChildren);
					}
					try {
						new File(dir, aChildren).delete();
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
		}

		final Toolbar myToolbar = mActivity.findViewById(R.id.my_awesome_toolbar);
		mActivity.setSupportActionBar(myToolbar);

		PlayerServiceUtil.bind(mActivity);
		setupBroadcastReceiver();

		selectedMenuItem = sharedPref.getInt("last_selectedMenuItem", -1);
		instanceStateWasSaved = savedInstanceState != null;
		mFragmentManager = mActivity.getSupportFragmentManager();

		mDrawerLayout = mActivity.findViewById(R.id.drawerLayout);
		mNavigationView = mActivity.findViewById(R.id.my_navigation_view);
		mBottomNavigationView = mActivity.findViewById(R.id.bottom_navigation);

		if (Utils.bottomNavigationEnabled(mActivity)) {
			mBottomNavigationView.setOnNavigationItemSelectedListener(mActivity);
			mNavigationView.setVisibility(View.GONE);
			mNavigationView.getLayoutParams().width = 0;
		} else {
			mNavigationView.setNavigationItemSelectedListener(mActivity);
			mBottomNavigationView.setVisibility(View.GONE);

			ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout, R.string.app_name, R.string.app_name);
			mDrawerLayout.addDrawerListener(mDrawerToggle);
			mDrawerToggle.syncState();

			mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			mActivity.getSupportActionBar().setHomeButtonEnabled(true);
		}

		FragmentPlayer f = new FragmentPlayer();
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.playerView, f).commit();
		setupStartUpFragment();
		mMainPageInited = true;

		LifeCycleStatus.isSplashShowing = false;
	}

	@Override
	public void changed() {
		ThreadPool.getInstance().uiHandler().post(new Runnable() {
			@Override
			public void run() {
				mActivity.invalidateOptionsMenu();
			}
		});
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {
		if (!mMainPageInited)
			return false;
		// If menuItem == null method was executed manually
		if (menuItem != null)
			selectedMenuItem = menuItem.getItemId();

		mDrawerLayout.closeDrawers();
		android.support.v4.app.Fragment f = null;
		String backStackTag = String.valueOf(selectedMenuItem);

		switch (selectedMenuItem) {
			case R.id.nav_item_stations:
				f = new FragmentTabs();
				break;
			case R.id.nav_item_starred:
				f = new FragmentStarred();
				break;
			case R.id.nav_item_history:
				f = new FragmentHistory();
				break;
			case R.id.nav_item_recordings:
				f = new FragmentRecordings();
				break;
			case R.id.nav_item_alarm:
				f = new FragmentAlarm();
				break;
			case R.id.nav_item_settings:
				f = new FragmentSettings();
				break;
			default:
		}

		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		if (Utils.bottomNavigationEnabled(mActivity))
			fragmentTransaction.replace(R.id.containerView, f).commit();
		else
			fragmentTransaction.replace(R.id.containerView, f).addToBackStack(backStackTag).commit();

		// User selected a menuItem. Let's hide progressBar
		mActivity.sendBroadcast(new Intent(ConstCommon.ACTION_HIDE_LOADING));
		mActivity.invalidateOptionsMenu();
		checkMenuItems();

		return false;
	}

	@Override
	public boolean onBackPressed() {
		if (!mMainPageInited)
			return false;
		int backStackCount = mFragmentManager.getBackStackEntryCount();
		FragmentManager.BackStackEntry backStackEntry;

		if (backStackCount > 0) {
			// FRAGMENT_FROM_BACKSTACK value added as a backstack name for non-root fragments like Recordings, About, etc
			backStackEntry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 1);
			int parsedId = Integer.parseInt(backStackEntry.getName());
			if (parsedId == ActivityMain.FRAGMENT_FROM_BACKSTACK) {
				mActivity.invalidateOptionsMenu();
				return false;
			}
		}

		// Don't support backstack with BottomNavigationView
		if (Utils.bottomNavigationEnabled(mActivity)) {
			// I'm giving 3 seconds on making a choice
			if (lastExitTry != null && new Date().getTime() < lastExitTry.getTime() + 3 * 1000) {
				PlayerServiceUtil.shutdownService();
				// mActivity.finish();
			} else {
				Toast.makeText(mActivity, R.string.alert_press_back_to_exit, Toast.LENGTH_SHORT).show();
				lastExitTry = new Date();
				return true;
			}
		}

		if (backStackCount > 1) {
			backStackEntry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 2);

			selectedMenuItem = Integer.parseInt(backStackEntry.getName());

			if (!Utils.bottomNavigationEnabled(mActivity)) {
				mNavigationView.setCheckedItem(selectedMenuItem);
			}
			mActivity.invalidateOptionsMenu();

		} else {
			// mActivity.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onNewIntent(Intent intent) {
		final Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}

		final String searchTag = extras.getString(ConstCommon.EXTRA_SEARCH_TAG);
		if (searchTag != null) {
			try {
				String queryEncoded = URLEncoder.encode(searchTag, "utf-8");
				queryEncoded = queryEncoded.replace("+", "%20");
				Search(RadioBrowserServerManager.getWebserviceEndpoint(mActivity, ConstCommon.TAG_SEARCH_URL + "/" + queryEncoded));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		if (!mMainPageInited)
			return;
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "on request permissions result:" + requestCode);
		}
		switch (requestCode) {
			case Utils.REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Fragment currentFragment = mFragmentManager.getFragments().get(mFragmentManager.getFragments().size() - 1);
					if (currentFragment instanceof IFragmentRefreshable) {
						if (BuildConfig.DEBUG) {
							Log.d(TAG, "REFRESH VIEW");
						}
						((IFragmentRefreshable) currentFragment).Refresh();
					}
				} else {
					Toast toast = Toast.makeText(mActivity, mActivity.getResources().getString(R.string.error_record_needs_write), Toast.LENGTH_SHORT);
					toast.show();
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		PlayerServiceUtil.unBind(mActivity);
		MPDClient.StopDiscovery();
		mActivity.unregisterReceiver(broadcastReceiver);
	}

	@Override
	public void onPause() {
		if (!mMainPageInited)
			return;
		// TACCrashSimulator.testJavaCrash();
		SharedPreferences.Editor ed = sharedPref.edit();
		ed.putInt("last_selectedMenuItem", selectedMenuItem);
		ed.apply();

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "PAUSED");
		}

		if (!PlayerServiceUtil.isPlaying()) {
			PlayerServiceUtil.shutdownService();
		} else {
			CastHandler.onPause();
		}

		MPDClient.StopDiscovery();
		MPDClient.setStateChangeListener(null);
	}

	@Override
	public void onResume() {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "RESUMED");
		}

		PlayerServiceUtil.bind(mActivity);
		CastHandler.onResume();

		MPDClient.setStateChangeListener(mActivity);
		MPDClient.StartDiscovery(new WeakReference<Context>(mActivity));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		mActivity.getMenuInflater().inflate(R.menu.menu_main, menu);

		final Toolbar myToolbar = mActivity.findViewById(R.id.my_awesome_toolbar);
		menuItemAlarm = menu.findItem(R.id.action_set_alarm);
		menuItemSearch = menu.findItem(R.id.action_search);
		menuItemDelete = menu.findItem(R.id.action_delete);
		menuItemSave = menu.findItem(R.id.action_save);
		menuItemLoad = menu.findItem(R.id.action_load);
		mSearchView = (SearchView) MenuItemCompat.getActionView(menuItemSearch);
		mSearchView.setOnQueryTextListener(mActivity);

		MenuItem menuItemMPDNok = menu.findItem(R.id.action_mpd_nok);
		MenuItem menuItemMPDOK = menu.findItem(R.id.action_mpd_ok);

		menuItemAlarm.setVisible(false);
		menuItemSearch.setVisible(false);
		menuItemDelete.setVisible(false);
		menuItemSave.setVisible(false);
		menuItemLoad.setVisible(false);
		menuItemMPDOK.setVisible(MPDClient.Discovered() && MPDClient.Connected());
		menuItemMPDNok.setVisible(MPDClient.Discovered() && !MPDClient.Connected());

		switch (selectedMenuItem) {
			case R.id.nav_item_stations: {
				menuItemAlarm.setVisible(true);
				menuItemSearch.setVisible(true);
				myToolbar.setTitle(R.string.nav_item_stations);
				break;
			}
			case R.id.nav_item_starred: {
				menuItemAlarm.setVisible(true);
				menuItemSearch.setVisible(true);
				menuItemSave.setVisible(true);
				menuItemLoad.setVisible(true);

				RadioDroidApp radioDroidApp = (RadioDroidApp) mActivity.getApplication();
				if (radioDroidApp.getFavouriteManager().isEmpty()) {
					menuItemDelete.setVisible(false);
				} else {
					menuItemDelete.setVisible(true).setTitle(R.string.action_delete_favorites);
				}
				myToolbar.setTitle(R.string.nav_item_starred);
				break;
			}
			case R.id.nav_item_history: {
				menuItemAlarm.setVisible(true);
				menuItemSearch.setVisible(true);

				RadioDroidApp radioDroidApp = (RadioDroidApp) mActivity.getApplication();
				if (!radioDroidApp.getHistoryManager().isEmpty()) {
					menuItemDelete.setVisible(true).setTitle(R.string.action_delete_history);
				}
				myToolbar.setTitle(R.string.nav_item_history);
				break;
			}
			case R.id.nav_item_recordings: {
				myToolbar.setTitle(R.string.nav_item_recordings);
				break;
			}
			case R.id.nav_item_alarm: {
				myToolbar.setTitle(R.string.nav_item_alarm);
				break;
			}
			case R.id.nav_item_settings: {
				myToolbar.setTitle(R.string.nav_item_settings);
				break;
			}
		}

		return true;
	}

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            favouriteManager.SaveM3U(filePath, "radiodroid.m3u");
        }
    }*/

	@Override
	public void onFileSelected(FileDialog dialog, File file) {
		try {
			Log.i("MAIN", "save to " + file.getParent() + "/" + file.getName());
			RadioDroidApp radioDroidApp = (RadioDroidApp) mActivity.getApplication();
			FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();

			if (dialog instanceof SaveFileDialog){
				favouriteManager.SaveM3U(file.getParent(), file.getName());
			}else if (dialog instanceof OpenFileDialog) {
				favouriteManager.LoadM3U(file.getParent(), file.getName());
			}
		}
		catch(Exception e){
			Log.e("MAIN",e.toString());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if (!mMainPageInited)
			return false;
		switch (menuItem.getItemId()) {
			case android.R.id.home:
				mDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER
				return true;
			case R.id.action_save:
				try{
					if (Utils.verifyStoragePermissions(mActivity)) {
						SaveFileDialog dialog = new SaveFileDialog();
						dialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.MyMaterialTheme);
						Bundle args = new Bundle();
						args.putString(FileDialog.EXTENSION, ".m3u"); // file extension is optional
						dialog.setArguments(args);
						dialog.show(mActivity.getSupportFragmentManager(), SaveFileDialog.class.getName());
					}
				}
				catch(Exception e){
					Log.e("MAIN",e.toString());
				}

				return true;
			case R.id.action_load:
				try {
					if (Utils.verifyStoragePermissions(mActivity)) {
						OpenFileDialog dialogOpen = new OpenFileDialog();
						dialogOpen.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.MyMaterialTheme);
						Bundle argsOpen = new Bundle();
						argsOpen.putString(FileDialog.EXTENSION, ".m3u"); // file extension is optional
						dialogOpen.setArguments(argsOpen);
						dialogOpen.show(mActivity.getSupportFragmentManager(), OpenFileDialog.class.getName());
					}
				}
				catch(Exception e){
					Log.e("MAIN",e.toString());
				}
				return true;
			case R.id.action_set_alarm:
				changeTimer();
				return true;
			case R.id.action_mpd_nok:
				selectMPDServer();
				return true;
			case R.id.action_mpd_ok:
				MPDClient.Disconnect();
				return true;
			case R.id.action_delete:
				if (selectedMenuItem == R.id.nav_item_history) {
					new AlertDialog.Builder(mActivity)
							.setMessage(mActivity.getString(R.string.alert_delete_history))
							.setCancelable(true)
							.setPositiveButton(mActivity.getString(R.string.yes), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									RadioDroidApp radioDroidApp = (RadioDroidApp) mActivity.getApplication();
									HistoryManager historyManager = radioDroidApp.getHistoryManager();

									historyManager.clear();

									Toast toast = Toast.makeText(mActivity.getApplicationContext(), mActivity.getString(R.string.notify_deleted_history), Toast.LENGTH_SHORT);
									toast.show();
									mActivity.recreate();
								}
							})
							.setNegativeButton(mActivity.getString(R.string.no), null)
							.show();
				}
				if (selectedMenuItem == R.id.nav_item_starred) {
					new AlertDialog.Builder(mActivity)
							.setMessage(mActivity.getString(R.string.alert_delete_favorites))
							.setCancelable(true)
							.setPositiveButton(mActivity.getString(R.string.yes), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									RadioDroidApp radioDroidApp = (RadioDroidApp) mActivity.getApplication();
									FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();

									favouriteManager.clear();

									Toast toast = Toast.makeText(mActivity.getApplicationContext(), mActivity.getString(R.string.notify_deleted_favorites), Toast.LENGTH_SHORT);
									toast.show();
									mActivity.recreate();
								}
							})
							.setNegativeButton(mActivity.getString(R.string.no), null)
							.show();
				}
				return true;
		}
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		if (!mMainPageInited)
			return false;
		String queryEncoded;
		try {
			mSearchView.setQuery("", false);
			mSearchView.clearFocus();
			mSearchView.setIconified(true);
			queryEncoded = URLEncoder.encode(query, "utf-8");
			queryEncoded = queryEncoded.replace("+", "%20");
			Search(RadioBrowserServerManager.getWebserviceEndpoint(mActivity, "json/stations/byname/" + queryEncoded));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return MPDClient.connected && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		String mpd_hostname = "";
		int mpd_port = 0;
		for (MPDServer server : Utils.getMPDServers(mActivity)) {
			if (server.selected) {
				mpd_hostname = server.hostname.trim();
				mpd_port = server.port;
				break;
			}
		}
		if (mpd_port == 0 || !MPDClient.connected)
			return false;

		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				MPDClient.SetVolume(mpd_hostname, mpd_port, -5);
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				MPDClient.SetVolume(mpd_hostname, mpd_port, 5);
				return true;
			default:
				break;
		}
		return false;
	}

	@Override
	public final Toolbar getToolbar() {
		if (!mMainPageInited)
			return null;
		return mActivity.findViewById(R.id.my_awesome_toolbar);
	}

	private void setupStartUpFragment() {
		// This will restore fragment that was shown before activity was recreated
		if (instanceStateWasSaved) {
			mActivity.invalidateOptionsMenu();
			checkMenuItems();
			return;
		}

		RadioDroidApp radioDroidApp = (RadioDroidApp) mActivity.getApplication();
		HistoryManager hm = radioDroidApp.getHistoryManager();
		FavouriteManager fm = radioDroidApp.getFavouriteManager();

		final String startupAction = sharedPref.getString("startup_action", mActivity.getResources().getString(R.string.startup_show_history));

		if (startupAction.equals(mActivity.getResources().getString(R.string.startup_show_history)) && hm.isEmpty()) {
			selectMenuItem(R.id.nav_item_stations);
			return;
		}

		if (startupAction.equals(mActivity.getResources().getString(R.string.startup_show_favorites)) && fm.isEmpty()) {
			selectMenuItem(R.id.nav_item_stations);
			return;
		}

		if (startupAction.equals(mActivity.getResources().getString(R.string.startup_show_history))) {
			selectMenuItem(R.id.nav_item_history);
		} else if (startupAction.equals(mActivity.getResources().getString(R.string.startup_show_favorites))) {
			selectMenuItem(R.id.nav_item_starred);
		} else if (startupAction.equals(mActivity.getResources().getString(R.string.startup_show_all_stations)) || selectedMenuItem < 0) {
			selectMenuItem(R.id.nav_item_stations);
		} else {
			selectMenuItem(selectedMenuItem);
		}
	}

	private void selectMenuItem(int itemId) {
		if (!mMainPageInited)
			return;
		MenuItem item;
		if (Utils.bottomNavigationEnabled(mActivity))
			item = mBottomNavigationView.getMenu().findItem(itemId);
		else
			item = mNavigationView.getMenu().findItem(itemId);

		if (item != null) {
			mActivity.onNavigationItemSelected(item);
		} else {
			selectedMenuItem = R.id.nav_item_stations;
			mActivity.onNavigationItemSelected(null);
		}
	}

	private void checkMenuItems() {
		if (!mMainPageInited)
			return;
		if (mBottomNavigationView.getMenu().findItem(selectedMenuItem) != null)
			mBottomNavigationView.getMenu().findItem(selectedMenuItem).setChecked(true);

		if (mNavigationView.getMenu().findItem(selectedMenuItem) != null)
			mNavigationView.getMenu().findItem(selectedMenuItem).setChecked(true);
	}

	@Override
	public void Search(String query) {
		if (!mMainPageInited)
			return;
		Fragment currentFragment = mFragmentManager.getFragments().get(mFragmentManager.getFragments().size() - 1);
		if (currentFragment instanceof FragmentTabs) {
			((FragmentTabs) currentFragment).Search(query);
		} else {
			String backStackTag = String.valueOf(R.id.nav_item_stations);
			FragmentTabs f = new FragmentTabs();
			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			if (Utils.bottomNavigationEnabled(mActivity)) {
				fragmentTransaction.replace(R.id.containerView, f).commit();
				mBottomNavigationView.getMenu().findItem(R.id.nav_item_stations).setChecked(true);
			} else {
				fragmentTransaction.replace(R.id.containerView, f).addToBackStack(backStackTag).commit();
				mNavigationView.getMenu().findItem(R.id.nav_item_stations).setChecked(true);
			}

			f.Search(query);
			selectedMenuItem = R.id.nav_item_stations;
			mActivity.invalidateOptionsMenu();
		}
	}

	private void setupBroadcastReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConstCommon.ACTION_HIDE_LOADING);
		filter.addAction(ConstCommon.ACTION_SHOW_LOADING);
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (ConstCommon.ACTION_HIDE_LOADING.equals(intent.getAction()))
					hideLoadingIcon();
				else if (ConstCommon.ACTION_SHOW_LOADING.equals(intent.getAction()))
					showLoadingIcon();
			}
		};
		mActivity.registerReceiver(broadcastReceiver, filter);
	}

	// Loading listener
	private void showLoadingIcon() {
		if (!mMainPageInited)
			return;
		mActivity.findViewById(R.id.progressBarLoading).setVisibility(View.VISIBLE);
	}

	private void hideLoadingIcon() {
		if (!mMainPageInited)
			return;
		mActivity.findViewById(R.id.progressBarLoading).setVisibility(View.GONE);
	}

	private void changeTimer() {
		if (!mMainPageInited)
			return;
		final AlertDialog.Builder seekDialog = new AlertDialog.Builder(mActivity);
		View seekView = View.inflate(mActivity, R.layout.layout_timer_chooser, null);

		seekDialog.setTitle(R.string.sleep_timer_title);
		seekDialog.setView(seekView);

		final TextView seekTextView = seekView.findViewById(R.id.timerTextView);
		final SeekBar seekBar = seekView.findViewById(R.id.timerSeekBar);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				seekTextView.setText(String.valueOf(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		long currentTimer = PlayerServiceUtil.getTimerSeconds() > 60 ? Math.abs(PlayerServiceUtil.getTimerSeconds() / 60) : 1;
		seekBar.setProgress((int) currentTimer);
		seekDialog.setPositiveButton(R.string.sleep_timer_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PlayerServiceUtil.clearTimer();
				PlayerServiceUtil.addTimer(seekBar.getProgress() * 60);
			}
		});

		seekDialog.setNegativeButton(R.string.sleep_timer_clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PlayerServiceUtil.clearTimer();
			}
		});

		seekDialog.create();
		seekDialog.show();
	}

	private void selectMPDServer() {
		final List<MPDServer> servers = Utils.getMPDServers(mActivity);
		final List<MPDServer> connectedServers = new ArrayList<>();
		List<String> serversNames = new ArrayList<>();
		for (MPDServer server : servers) {
			server.selected = false;
			if (server.connected) {
				serversNames.add(server.name);
				connectedServers.add(server);
			}
		}
		if (connectedServers.size() == 1) {
			MPDServer selectedServer = servers.get(connectedServers.get(0).id);
			selectedServer.selected = true;
			Utils.saveMPDServers(servers, mActivity.getApplicationContext());
			MPDClient.Connect();

			// Since we connected to one server but have many user should know server's name
			if (servers.size() > 1)
				Toast.makeText(mActivity, mActivity.getString(R.string.action_mpd_connected, selectedServer.name), Toast.LENGTH_SHORT).show();
			return;
		}

		new AlertDialog.Builder(mActivity)
				.setItems(serversNames.toArray(new String[serversNames.size()]), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (MPDServer server : servers) {
							server.selected = false;
						}
						MPDServer selectedServer = servers.get(connectedServers.get(which).id);
						selectedServer.selected = true;
						Utils.saveMPDServers(servers, mActivity.getApplicationContext());
						MPDClient.Connect();
					}
				})
				.setTitle(R.string.alert_select_mpd_server)
				.show();
	}
}
