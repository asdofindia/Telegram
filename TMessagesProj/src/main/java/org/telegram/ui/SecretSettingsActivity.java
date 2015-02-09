/*
* This is the source code of Telegram for Android v. 1.3.2.
* It is licensed under GNU GPL v. 2 or later.
* You should have received a copy of the license in this archive (see LICENSE).
*
* Copyright Nikolai Kudashov, 2013.
*/

package org.telegram.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
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
import org.telegram.messenger.Utilities;
import org.telegram.messenger.ApplicationLoader;
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
import org.telegram.ui.LaunchActivity;

public class SecretSettingsActivity extends BaseFragment {
  private ListView listView;
  private boolean reseting = false;

  private int swipeThresholdRow;
  private int headerColorRow;
  private int drawerColorRow;
  private int restartRow;
  private int rowCount = 0;

  @Override
  public boolean onFragmentCreate() {
    swipeThresholdRow = rowCount++;
    headerColorRow = rowCount++;
    drawerColorRow = rowCount++;
    restartRow = rowCount++;

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
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(100);
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
          } else if ( i == restartRow){
            if (getParentActivity() == null) {
              return;
            }
            Utilities.stageQueue.postRunnable(new Runnable() {
              @Override
              public void run() {
                  // http://stackoverflow.com/a/17166729
                Intent mStartActivity = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
                int mPendingIntentId = 17166729;
                PendingIntent mPendingIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
                // System.exit(0);
                // restart(ApplicationLoader.applicationContext,2);
              }
            });
          } else if ( i == headerColorRow || i == drawerColorRow ) {
            if (getParentActivity() == null) {
              return;
            }

            LayoutInflater li = (LayoutInflater)getParentActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.settings_color_dialog_layout, null, false);
            final ColorPickerView colorPickerView = (ColorPickerView)view.findViewById(R.id.color_picker);

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE);
            if (i == headerColorRow) {
              colorPickerView.setOldCenterColor(preferences.getInt("headerColor", 0xff54759e));
            } else if (i == drawerColorRow){
              colorPickerView.setOldCenterColor(preferences.getInt("drawerColor", 0xff4c84b5));
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            if (i == headerColorRow) {
              builder.setTitle("Header Color");
            } else if (i == drawerColorRow) {
              builder.setTitle("Drawer Color");
            }
            builder.setView(view);
            builder.setPositiveButton(LocaleController.getString("Set", R.string.Set), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int which) {
                final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                if (i == headerColorRow) {
                  editor.putInt("headerColor", Integer.parseInt(Integer.toString(colorPickerView.getColor())) );
                } else if (i == drawerColorRow){
                  editor.putInt("drawerColor", colorPickerView.getColor());
                }
                editor.commit();
                listView.invalidateViews();
              }
            });
            builder.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                if (i == headerColorRow) {
                  editor.putInt("headerColor", 0xff54759e);
                } else if (i == drawerColorRow) {
                  editor.putInt("drawerColor", 0xff4c84b5);
                }
                editor.commit();
                listView.invalidateViews();
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
/*
public static void restart(Context context, int delay) {
  // http://stackoverflow.com/a/22377728
  if (delay == 0) {
    delay = 1;
  }
  Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName() );
  PendingIntent intent = PendingIntent.getActivity( context, 0, restartIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
  AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  manager.set(AlarmManager.RTC, System.currentTimeMillis() + delay, intent);
  System.exit(0);
}
*/
private class ListAdapter extends BaseFragmentAdapter {
  private Context mContext;

  public ListAdapter(Context context) {
    mContext = context;
  }

  @Override
  public boolean areAllItemsEnabled() {
    return true;
  }

  @Override
  public boolean isEnabled(int i) {
    return  i == swipeThresholdRow || i == headerColorRow || i == drawerColorRow || i == restartRow;
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
      if (view == null) {
        view = new TextDetailSettingsCell(mContext);
      }
      TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;
      if ( i == restartRow) {
         textCell.setTextAndValue("Restart App", "Some settings might not take effect till you restart", false);
      }
    } else if (type == 3) { // color
      if (view == null) {
        view = new TextColorCell(mContext);
      }

      TextColorCell textCell = (TextColorCell) view;

      SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("extraconfig", Activity.MODE_PRIVATE);
      if (i == headerColorRow) {
        textCell.setTextAndColor("Header Color", preferences.getInt("headerColor", 0xff54759e), true);
      } else if (i == drawerColorRow) {
        textCell.setTextAndColor("Drawer Color", preferences.getInt("drawerColor", 0xff4c84b5), true);
      }
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
    } else if ( i == headerColorRow || i == drawerColorRow){
      return 3;
    } else if ( i == restartRow ) {
      return 2;
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
