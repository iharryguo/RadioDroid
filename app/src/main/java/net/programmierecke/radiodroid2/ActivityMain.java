package net.programmierecke.radiodroid2;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.rustamg.filedialogs.FileDialog;

import net.programmierecke.radiodroid2.lifecycle.LifeCyclePresenter;
import net.programmierecke.radiodroid2.presenter.ILifeCyclePresenter;

import java.io.File;

public class ActivityMain extends AppCompatActivity implements SearchView.OnQueryTextListener, IMPDClientStatusChange, NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener, FileDialog.OnFileSelectedListener {
    public static final int LAUNCH_EQUALIZER_REQUEST = 1;
    public static final int FRAGMENT_FROM_BACKSTACK = 777;

    // 与Main Activity相关的，都转到这里面处理
    private ILifeCyclePresenter mLifeCyclePresenter;

    @Override
    public void changed() {
        mLifeCyclePresenter.changed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLifeCyclePresenter = new LifeCyclePresenter(this);
        mLifeCyclePresenter.onCreate(savedInstanceState);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        return mLifeCyclePresenter.onNavigationItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if (!mLifeCyclePresenter.onBackPressed())
            super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mLifeCyclePresenter.onNewIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        mLifeCyclePresenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLifeCyclePresenter.onDestroy();
    }

    @Override
    protected void onPause() {
        mLifeCyclePresenter.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLifeCyclePresenter.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mLifeCyclePresenter.onCreateOptionsMenu(menu);
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
        mLifeCyclePresenter.onFileSelected(dialog, file);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (!mLifeCyclePresenter.onOptionsItemSelected(menuItem))
            return super.onOptionsItemSelected(menuItem);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return mLifeCyclePresenter.onQueryTextSubmit(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mLifeCyclePresenter.onKeyDown(keyCode, event))
            return super.onKeyDown(keyCode, event);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!mLifeCyclePresenter.onKeyUp(keyCode, event))
            return super.onKeyUp(keyCode, event);
        return true;
    }

    public final Toolbar getToolbar() {
        return mLifeCyclePresenter.getToolbar();
    }

    public void Search(String webserviceEndpoint) {
        mLifeCyclePresenter.Search(webserviceEndpoint);
    }
}
