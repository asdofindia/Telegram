/*
* This is the source code of Telegram for Android v. 1.3.2.
* It is licensed under GNU GPL v. 2 or later.
* You should have received a copy of the license in this archive (see LICENSE).
*
* Copyright Nikolai Kudashov, 2013.
*/

package org.telegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.android.NotificationsController;
import org.telegram.android.NotificationCenter;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.TLObject;
import org.telegram.messenger.TLRPC;
import org.telegram.messenger.ConnectionsManager;
import org.telegram.messenger.FileLog;
import org.telegram.android.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.RPCRequest;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextColorCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ColorPickerView;
import org.telegram.ui.Components.NumberPicker;

public class SecretSettingsActivity extends BaseFragment {
  private ListView listView;
  private boolean reseting = false;

  private int swipeThresholdRow;
  private int rowCount = 0;

  @Override
  public boolean onFragmentCreate() {
    swipeThresholdRow = rowCount++;

    return super.onFragmentCreate();
  }

  @Override
  public void onFragmentDestroy() {
    super.onFragmentDestroy();
  }

  @Override
  public View createView(LayoutInflater inflater, ViewGroup container) {
    if (fragmentView == null) {
      actionBar.setBackButtonImage(R.drawable.ic_ab_back);
      actionBar.setAllowOverlayTitle(true);
      actionBar.setTitle("Geek settings");
      actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
        @Override
        public void onItemClick(int id) {
          if (id == -1) {
            finishFragment();
          }
        }
      });

      fragmentView = new FrameLayout(getParentActivity());
      FrameLayout frameLayout = (FrameLayout) fragmentView;

      listView = new ListView(getParentActivity());
      listView.setDivider(null);
      listView.setDividerHeight(0);
      listView.setVerticalScrollBarEnabled(false);
      frameLayout.addView(listView);
      FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
      layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
      layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
      listView.setLayoutParams(layoutParams);
      listView.setAdapter(new ListAdapter(getParentActivity()));
      listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
          boolean enabled = false;
          if ( i == swipeThresholdRow ) {
            if (getParentActivity() == null) {
              return;
            }
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE);
            int threshold = preferences.getInt("swipe_threshold", 10);
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle("Swipe Threshold");
            final NumberPicker numberPicker = new NumberPicker(getParentActivity());
            numberPicker.setMinValue(5);
            numberPicker.setMaxValue(30);
            numberPicker.setValue(threshold);
            builder.setView(numberPicker);
            builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                 SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE); 
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("swipe_threshold", numberPicker.getValue());
                editor.commit();
                if (listView != null) {
                  listView.invalidateViews();
                }
              }
            });
            showAlertDialog(builder);
          }
          if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(!enabled);
          }
        }
      });
    } else {
      ViewGroup parent = (ViewGroup)fragmentView.getParent();
      if (parent != null) {
        parent.removeView(fragmentView);
      }
    }
    return fragmentView;
  }



private class ListAdapter extends BaseFragmentAdapter {
  private Context mContext;

  public ListAdapter(Context context) {
    mContext = context;
  }

  @Override
  public boolean areAllItemsEnabled() {
    return false;
  }

  @Override
  public boolean isEnabled(int i) {
    return !( i == swipeThresholdRow);
  }

  @Override
  public int getCount() {
    return rowCount;
  }

  @Override
  public Object getItem(int i) {
    return null;
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    int type = getItemViewType(i);
    if (type == 0) {  //section title
      if (view == null) {
        view = new HeaderCell(mContext);
      }
    } if (type == 1) {  //check mark
    } else if (type == 2) {
    } else if (type == 3) {
    } else if (type == 4) {
      if (view == null) {
        view = new ShadowSectionCell(mContext);
      }
    } else if ( type == 5) {
      if (view == null) {
        view = new TextSettingsCell(mContext);
      }
      TextSettingsCell textCell = (TextSettingsCell) view;
      if (i == swipeThresholdRow) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE);
        int threshold = preferences.getInt("swipe_threshold", 10);
        textCell.setTextAndValue("Swipe Threshold", String.format("%d", threshold), true);
      }
    }
    return view;
  }

  @Override
  public int getItemViewType(int i) {
    if (i == swipeThresholdRow) {
      return 5;
    }
    else {
      return 0;
    }
  }

  @Override
  public int getViewTypeCount() {
    return 6;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
}
