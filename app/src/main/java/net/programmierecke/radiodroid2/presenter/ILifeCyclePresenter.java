package net.programmierecke.radiodroid2.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.rustamg.filedialogs.FileDialog;

import java.io.File;

/**
 * Created by harryguo on 2018/7/9.
 */

public interface ILifeCyclePresenter {
	void onCreate(Bundle savedInstanceState);

	void changed();

	boolean onNavigationItemSelected(MenuItem menuItem);

	boolean onBackPressed();

	void onNewIntent(Intent intent);

	void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults);

	void onDestroy();

	void onPause();

	void onResume();

	boolean onCreateOptionsMenu(Menu menu);

	void onFileSelected(FileDialog dialog, File file);

	boolean onOptionsItemSelected(MenuItem menuItem);

	boolean onQueryTextSubmit(String query);

	boolean onKeyDown(int keyCode, KeyEvent event);

	boolean onKeyUp(int keyCode, KeyEvent event);

	Toolbar getToolbar();

	void Search(String webserviceEndpoint);
}
